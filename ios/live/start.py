#!/usr/bin/env python3
"""Starts a live, hands-on iPhone running Deen and opens it in your browser. No Mac needed.

  python ios/live/start.py [--minutes 60] [--no-open]

The iOS Simulator runs on GitHub's Mac (.github/workflows/ios-live.yml, free for public repositories) with the
latest `ios` branch; its screen comes to your browser and your clicks, swipes and typing go back to it. The
first run makes a key pair in ~/.deen-live: the public half travels with the request, the session link comes
back encrypted with it, and only this computer can open it. Needs git, the GitHub CLI (signed in) and openssl
(Git for Windows includes it). Takes about ten minutes to start.
"""
import argparse
import base64
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import webbrowser
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
KEYS = Path.home() / ".deen-live"
WORKFLOW = "ios-live.yml"
ARTIFACT = "live-session"


def run(*command, capture=True, check=True):
    result = subprocess.run(command, cwd=ROOT, capture_output=capture, text=True, encoding="utf-8", errors="replace")
    if check and result.returncode != 0:
        sys.exit(f"Failed: {' '.join(command)}\n{result.stderr or result.stdout}")
    return result.stdout.strip() if capture else ""


def find_openssl():
    found = shutil.which("openssl")
    if found:
        return found
    for git in filter(None, [shutil.which("git")]):
        for candidate in (Path(git).parents[1] / "usr/bin/openssl.exe", Path(git).parents[1] / "mingw64/bin/openssl.exe"):
            if candidate.exists():
                return str(candidate)
    sys.exit("openssl wasn't found. Install Git for Windows (it includes openssl) or add openssl to PATH.")


def key_pair(openssl):
    KEYS.mkdir(exist_ok=True)
    private, public = KEYS / "viewer.key", KEYS / "viewer.pub"
    if not private.exists():
        run(openssl, "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(private))
        print(f"Made a key pair in {KEYS}")
    if not public.exists():
        run(openssl, "pkey", "-in", str(private), "-pubout", "-out", str(public))
    return private, public.read_text()


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--minutes", type=int, default=60, help="how long the session lasts (up to 120)")
    parser.add_argument("--no-open", action="store_true", help="print the link instead of opening it")
    args = parser.parse_args()

    openssl = find_openssl()
    private, public = key_pair(openssl)
    repo = run("gh", "repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner")

    run("git", "fetch", "--quiet", "origin", "ios")
    tag = "ios-live-" + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    message = f"Deen live session\n\nminutes: {min(max(args.minutes, 5), 120)}\n\n{public}"
    with tempfile.NamedTemporaryFile("w", delete=False, suffix=".txt") as note:
        note.write(message)
    try:
        run("git", "tag", "-a", tag, "origin/ios", "-F", note.name)
    finally:
        os.unlink(note.name)
    run("git", "push", "--quiet", "origin", f"refs/tags/{tag}")
    print(f"Requested a session ({tag}). The iPhone takes about ten minutes to start...")

    try:
        run_id = wait_for_run(repo, tag)
        print(f"Progress: https://github.com/{repo}/actions/runs/{run_id}")
        link = wait_for_link(repo, run_id, openssl, private)
    finally:
        run("git", "push", "--quiet", "origin", f":refs/tags/{tag}", check=False)
        run("git", "tag", "-d", tag, check=False)

    print("\nYour iPhone is ready:\n" + link + "\n(keep this link to yourself; it works until the session ends)")
    if not args.no_open:
        webbrowser.open(link)


def wait_for_run(repo, tag):
    for _ in range(60):
        runs = json.loads(run("gh", "api", f"repos/{repo}/actions/runs?event=push&per_page=30"))
        for item in runs.get("workflow_runs", []):
            if item.get("head_branch") == tag and item.get("path", "").endswith(WORKFLOW):
                return item["id"]
        time.sleep(5)
    sys.exit("GitHub didn't start the session. Check the Actions tab.")


def wait_for_link(repo, run_id, openssl, private):
    started, shown = time.time(), -1
    while time.time() - started < 40 * 60:
        status = json.loads(run("gh", "run", "view", str(run_id), "--json", "status,conclusion"))
        artifacts = json.loads(run("gh", "api", f"repos/{repo}/actions/runs/{run_id}/artifacts"))
        if any(a["name"] == ARTIFACT for a in artifacts.get("artifacts", [])):
            with tempfile.TemporaryDirectory() as folder:
                run("gh", "run", "download", str(run_id), "-n", ARTIFACT, "-D", folder)
                sealed = Path(folder, "session.txt").read_text().strip()
                encrypted = Path(folder, "session.bin")
                encrypted.write_bytes(base64.b64decode(sealed))
                return run(openssl, "pkeyutl", "-decrypt", "-inkey", str(private),
                           "-pkeyopt", "rsa_padding_mode:oaep", "-in", str(encrypted))
        if status.get("status") == "completed":
            sys.exit(f"The session didn't start ({status.get('conclusion')}). See the run's logs on GitHub.")
        minutes = int((time.time() - started) / 60)
        if minutes != shown:
            shown = minutes
            print(f"  still starting... {minutes} min", flush=True)
        time.sleep(15)
    sys.exit("The session took too long to start.")


if __name__ == "__main__":
    main()
