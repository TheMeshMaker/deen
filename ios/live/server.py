#!/usr/bin/env python3
"""Serves a live, hands-on iOS Simulator session (started by .github/workflows/ios-live.yml).

The browser gets the Simulator's screen as a stream of JPEG frames and sends taps, swipes and key presses back.
Those are queued for LiveSessionTests (a UI test running inside the Simulator), which polls the runner port and
carries each one out. Everything on the browser port except /health needs the session key, which reaches the
person who started the session only inside the encrypted link (see ios/live/start.py).

  server.py --udid <simulator> --app <Deen.app> --token <key> [--minutes 60] [--stop-file <path>]
"""
import argparse
import hashlib
import hmac
import http.server
import io
import json
import signal
import struct
import subprocess
import sys
import tempfile
import threading
import time
import urllib.parse
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # frames are sent at full size without Pillow
    Image = None

HERE = Path(__file__).resolve().parent
COOKIE = "deen_live"
FRAME_WIDTH = 640
INPUTS = {"tap", "longpress", "swipe", "type", "home", "open", "launch", "terminate"}


class Session:
    def __init__(self, args):
        self.args = args
        self.simctl = subprocess.run(["xcrun", "-f", "simctl"], capture_output=True, text=True).stdout.strip() or "simctl"
        self.deadline = time.time() + args.minutes * 60
        self.stopped = threading.Event()
        # Screen frames.
        self.frames = threading.Condition()
        self.frame = b""
        self.digest = b""
        self.seq = 0
        self.frame_started = 0.0
        self.frame_times = []
        self.watched = 0.0
        # Commands for the UI test.
        self.queue = threading.Condition()
        self.commands = []
        self.results = {}
        self.next_id = 0
        self.points = None
        self.last_poll = 0.0
        self.appearance = "light"
        self.language = ""

    # Commands ------------------------------------------------------------------------------------------------

    def submit(self, command, wait=30.0):
        with self.queue:
            self.next_id += 1
            cid = self.next_id
            self.commands.append(dict(command, id=cid))
            self.queue.notify_all()
            end = time.time() + wait
            while cid not in self.results and not self.stopped.is_set():
                left = end - time.time()
                if left <= 0:
                    break
                self.queue.wait(left)
            return self.results.pop(cid, {"ok": False, "error": "The Simulator didn't answer in time"})

    def take(self, wait=1.0):
        with self.queue:
            self.last_poll = time.time()
            end = time.time() + wait
            while not self.commands:
                left = end - time.time()
                if left <= 0:
                    return None
                self.queue.wait(left)
            return self.commands.pop(0)

    def finish(self, result):
        with self.queue:
            self.results[result.get("id")] = {"ok": bool(result.get("ok")), "error": result.get("error") or ""}
            self.queue.notify_all()

    def simctl_run(self, *arguments):
        return subprocess.run([self.simctl, *arguments], capture_output=True, text=True, timeout=120)

    def control(self, body):
        kind = body.get("type")
        if kind == "appearance":
            mode = "dark" if body.get("mode") == "dark" else "light"
            self.simctl_run("ui", self.args.udid, "appearance", mode)
            self.appearance = mode
        elif kind == "location":
            lat, lon = float(body["lat"]), float(body["lon"])
            self.simctl_run("location", self.args.udid, "set", f"{lat:.5f},{lon:.5f}")
        elif kind == "reset":
            self.submit({"type": "terminate"})
            self.simctl_run("uninstall", self.args.udid, self.args.bundle)
            installed = self.simctl_run("install", self.args.udid, self.args.app)
            if installed.returncode != 0:
                return {"ok": False, "error": "Couldn't reinstall the app"}
            return self.submit({"type": "launch", "language": self.language})
        elif kind == "stop":
            self.stop()
        else:
            return {"ok": False, "error": f"Unknown control {kind}"}
        return {"ok": True, "error": ""}

    def stop(self):
        if self.stopped.is_set():
            return
        with self.queue:
            self.commands.append({"type": "stop", "id": 0})
            self.queue.notify_all()
        self.stopped.set()
        with self.frames:
            self.frames.notify_all()

    # Frames --------------------------------------------------------------------------------------------------

    def capture(self, worker):
        path = Path(tempfile.gettempdir()) / f"deen-live-{worker}.jpg"
        while not self.stopped.is_set():
            if time.time() - self.watched > 20:  # nobody is watching
                time.sleep(0.3)
                continue
            started = time.time()
            shot = subprocess.run(
                [self.simctl, "io", self.args.udid, "screenshot", "--type=jpeg", str(path)], capture_output=True
            )
            if shot.returncode != 0:
                time.sleep(1)
                continue
            self.publish(shrink(path.read_bytes()), started)

    def publish(self, data, started):
        digest = hashlib.sha1(data).digest()
        with self.frames:
            if started < self.frame_started:
                return  # a slower worker's older frame
            self.frame_started = started
            now = time.time()
            self.frame_times = [t for t in self.frame_times if now - t < 5] + [now]
            if digest == self.digest:
                return
            self.frame, self.digest = data, digest
            self.seq += 1
            self.frames.notify_all()

    def wait_frame(self, after, timeout):
        self.watched = time.time()
        with self.frames:
            end = time.time() + timeout
            while self.seq <= after and not self.stopped.is_set():
                left = end - time.time()
                if left <= 0:
                    break
                self.frames.wait(left)
            return self.seq, self.frame

    def info(self):
        now = time.time()
        fps = len([t for t in self.frame_times if now - t < 5]) / 5
        return {
            "ready": self.points is not None and now - self.last_poll < 30,
            "points": self.points,
            "remaining": max(0, int(self.deadline - now)),
            "appearance": self.appearance,
            "language": self.language,
            "fps": round(fps, 1),
            "ended": self.stopped.is_set(),
        }


def shrink(data):
    """Halves the screenshot while decoding (fast for JPEG) so frames cross the tunnel quickly."""
    if Image is None:
        return data
    try:
        image = Image.open(io.BytesIO(data))
        image.draft("RGB", (image.width // 2, image.height // 2))
        image = image.convert("RGB")
        if image.width > FRAME_WIDTH:
            image = image.resize((FRAME_WIDTH, round(image.height * FRAME_WIDTH / image.width)), Image.BILINEAR)
        out = io.BytesIO()
        image.save(out, "JPEG", quality=72)
        return out.getvalue()
    except Exception:
        return data


class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    session: Session = None

    def log_message(self, format, *args):  # never log query strings: the first request carries the key
        sys.stderr.write(f"{self.command} {urllib.parse.urlsplit(self.path).path} {args[1] if len(args) > 1 else ''}\n")

    def send(self, status, body=b"", content_type="application/json", headers=()):
        if isinstance(body, (dict, list)):
            body = json.dumps(body).encode()
        elif isinstance(body, str):
            body = body.encode()
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        for name, value in headers:
            self.send_header(name, value)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def body(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length > 100_000:
            raise ValueError("too large")
        return json.loads(self.rfile.read(length) or b"{}")


class BrowserHandler(Handler):
    def authorised(self, query):
        token = self.session.args.token
        offered = query.get("k", [""])[0]
        for part in (self.headers.get("Cookie") or "").split(";"):
            name, _, value = part.strip().partition("=")
            if name == COOKIE:
                offered = offered or value
        return bool(offered) and hmac.compare_digest(offered, token)

    def do_GET(self):
        url = urllib.parse.urlsplit(self.path)
        query = urllib.parse.parse_qs(url.query)
        s = self.session
        if url.path == "/health":
            return self.send(200, "ready" if s.info()["ready"] else "starting", "text/plain")
        if not self.authorised(query):
            return self.send(403, "This session link isn't valid, or the session has ended.", "text/plain")
        if "k" in query:  # swap the key in the address for a cookie
            cookie = f"{COOKIE}={s.args.token}; Path=/; HttpOnly; Secure; SameSite=Strict"
            return self.send(303, "", "text/plain", [("Location", "/"), ("Set-Cookie", cookie)])
        if url.path == "/":
            return self.send(200, (HERE / "index.html").read_bytes(), "text/html; charset=utf-8")
        if url.path == "/info":
            return self.send(200, s.info())
        if url.path == "/frame":
            after = int(query.get("after", ["0"])[0] or 0)
            seq, frame = s.wait_frame(after, 10)
            if not frame:
                return self.send(204)
            return self.send(200, frame, "image/jpeg", [("X-Seq", str(seq))])
        if url.path == "/stream":
            return self.stream()
        return self.send(404, "Not found", "text/plain")

    def stream(self):
        """Every new frame as soon as it's captured: a 4-byte length, then the JPEG (length 0 keeps it open)."""
        s = self.session
        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Cache-Control", "no-store, no-transform")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True
        seq, sent = 0, time.time()
        try:
            while not s.stopped.is_set():
                new_seq, frame = s.wait_frame(seq, 5)
                if frame and new_seq != seq:
                    seq, sent = new_seq, time.time()
                    self.wfile.write(struct.pack(">I", len(frame)) + frame)
                elif time.time() - sent >= 10:
                    sent = time.time()
                    self.wfile.write(struct.pack(">I", 0))
                self.wfile.flush()
        except OSError:  # the viewer went away
            pass

    def do_POST(self):
        url = urllib.parse.urlsplit(self.path)
        if not self.authorised({}):
            return self.send(403, {"ok": False, "error": "Not allowed"})
        try:
            body = self.body()
        except ValueError:
            return self.send(400, {"ok": False, "error": "Bad request"})
        s = self.session
        if s.stopped.is_set():
            return self.send(410, {"ok": False, "error": "The session has ended"})
        if url.path == "/input":
            if body.get("type") not in INPUTS:
                return self.send(400, {"ok": False, "error": "Unknown input"})
            if body["type"] == "launch":
                s.language = str(body.get("language") or "")
            started = time.time()
            result = s.submit(body)
            result["ms"] = int((time.time() - started) * 1000)
            return self.send(200, result)
        if url.path == "/control":
            try:
                return self.send(200, s.control(body))
            except (KeyError, ValueError, subprocess.TimeoutExpired) as error:
                return self.send(400, {"ok": False, "error": str(error)})
        return self.send(404, {"ok": False, "error": "Not found"})


class RunnerHandler(Handler):
    """Only for LiveSessionTests: bound to the loopback address and never tunnelled."""

    def do_GET(self):
        if urllib.parse.urlsplit(self.path).path != "/next":
            return self.send(404)
        command = self.session.take()
        return self.send(200, command) if command else self.send(204)

    def do_POST(self):
        path = urllib.parse.urlsplit(self.path).path
        body = self.body()
        if path == "/hello":
            self.session.points = [body.get("width"), body.get("height")]
            self.session.last_poll = time.time()
            print(f"UI test ready, screen {self.session.points} points", flush=True)
        elif path == "/done":
            self.session.finish(body)
        return self.send(200, {"ok": True})


class Server(http.server.ThreadingHTTPServer):
    daemon_threads = True

    def handle_error(self, request, client_address):
        if not isinstance(sys.exc_info()[1], OSError):  # viewers closing the page aren't errors
            super().handle_error(request, client_address)


def serve(handler, port, session):
    handler.session = session
    server = Server(("127.0.0.1", port), handler)
    server.daemon_threads = True
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--udid", required=True)
    parser.add_argument("--app", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--bundle", default="io.github.themeshmaker.dunya")
    parser.add_argument("--minutes", type=float, default=60)
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--runner-port", type=int, default=8081)
    parser.add_argument("--workers", type=int, default=3)
    parser.add_argument("--stop-file")
    args = parser.parse_args()

    session = Session(args)
    signal.signal(signal.SIGTERM, lambda *_: session.stop())
    serve(BrowserHandler, args.port, session)
    serve(RunnerHandler, args.runner_port, session)
    for worker in range(args.workers):
        threading.Thread(target=session.capture, args=(worker,), daemon=True).start()
    print(f"Serving on {args.port} (runner {args.runner_port}) for {args.minutes:g} minutes", flush=True)

    while not session.stopped.wait(2):
        if time.time() > session.deadline:
            print("Time is up", flush=True)
            session.stop()
    time.sleep(3)  # lets the UI test pick up "stop"
    if args.stop_file:
        Path(args.stop_file).write_text("stopped\n")
    print("Session ended", flush=True)


if __name__ == "__main__":
    main()
