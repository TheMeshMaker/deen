#!/usr/bin/env python3
"""Serves a live, hands-on iOS Simulator session (started by .github/workflows/ios-live.yml).

The Simulator's screen comes from `axe stream-video` (MJPEG) and goes to the browser as it's captured; taps,
swipes, long presses, buttons and typing come back from the browser and are sent to the Simulator with AXe
(https://github.com/cameroncooke/AXe) and simctl. Everything except /health needs the session key, which
reaches the person who started the session only inside the encrypted link (see ios/live/start.py).

  server.py --udid <simulator> --app <Deen.app> --token <key> [--minutes 60] [--stop-file <path>]
"""
import argparse
import hashlib
import hmac
import http.server
import json
import re
import signal
import struct
import subprocess
import sys
import threading
import time
import urllib.parse
from pathlib import Path

HERE = Path(__file__).resolve().parent
COOKIE = "deen_live"
SCALE = 0.5  # of the screen's pixels, for the stream
INPUTS = {"tap", "longpress", "swipe", "type", "home", "open", "launch", "terminate"}
KEY_ENTER, KEY_BACKSPACE = "40", "42"  # HID usage codes


def clamp(value, low, high):
    return min(max(float(value), low), high)


class Session:
    def __init__(self, args):
        self.args = args
        self.udid = args.udid
        self.deadline = time.time() + args.minutes * 60
        self.stopped = threading.Event()
        self.gestures = threading.Lock()  # one input at a time, in order
        # Screen frames.
        self.frames = threading.Condition()
        self.frame = b""
        self.digest = b""
        self.seq = 0
        self.frame_times = []
        self.points = None
        self.appearance = "light"
        self.language = ""

    # Simulator ----------------------------------------------------------------------------------------------

    def run(self, *command, stdin=None, timeout=60):
        started = time.time()
        result = subprocess.run(command, input=stdin, capture_output=True, text=True, encoding="utf-8",
                                errors="replace", timeout=timeout)
        took = int((time.time() - started) * 1000)
        if result.returncode != 0:
            print(f"{command[0]} {command[1]} failed in {took} ms: {(result.stderr or result.stdout).strip()[:300]}",
                  flush=True)
        return result

    def axe(self, *arguments, stdin=None):
        return self.run("axe", *arguments, "--udid", self.udid, stdin=stdin)

    def simctl(self, *arguments, stdin=None):
        return self.run("xcrun", "simctl", *arguments, stdin=stdin, timeout=120)

    def launch(self, language=""):
        self.simctl("terminate", self.udid, self.args.bundle)
        extra = ["-AppleLanguages", f"({language})", "-AppleLocale", language] if language else []
        return self.simctl("launch", self.udid, self.args.bundle, *extra).returncode == 0

    def screen_points(self):
        """The screen's size in points (AXe taps in points): the largest frame in the accessibility tree, or
        else the stream's frame size at 3 pixels per point."""
        sizes = []

        def walk(node):
            if isinstance(node, dict):
                if isinstance(node.get("width"), (int, float)) and isinstance(node.get("height"), (int, float)):
                    sizes.append((float(node["width"]), float(node["height"])))
                for value in node.values():
                    walk(value)
            elif isinstance(node, list):
                for value in node:
                    walk(value)

        try:
            walk(json.loads(self.axe("describe-ui").stdout or "null"))
        except ValueError:
            pass
        sizes = [s for s in sizes if s[0] >= 300 and s[1] >= 500]
        if sizes:
            return list(max(sizes, key=lambda s: s[0] * s[1]))
        size = jpeg_size(self.frame)
        return [round(size[0] / SCALE / 3), round(size[1] / SCALE / 3)] if size else None

    def stream_video(self):
        """Keeps `axe stream-video` running and publishes every JPEG it writes."""
        while not self.stopped.is_set():
            process = subprocess.Popen(
                ["axe", "stream-video", "--format", "mjpeg", "--fps", str(self.args.fps), "--quality", "60",
                 "--scale", str(SCALE), "--udid", self.udid],
                stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
            )
            buffer = b""
            while not self.stopped.is_set():
                chunk = process.stdout.read1(256 * 1024) if hasattr(process.stdout, "read1") else process.stdout.read(65536)
                if not chunk:
                    break
                buffer += chunk
                while True:
                    start = buffer.find(b"\xff\xd8")
                    if start < 0:
                        buffer = b""
                        break
                    end = buffer.find(b"\xff\xd9", start + 2)
                    if end < 0:
                        buffer = buffer[start:]
                        break
                    self.publish(buffer[start:end + 2])
                    buffer = buffer[end + 2:]
            process.kill()
            if not self.stopped.is_set():
                print("The video stream stopped; restarting it", flush=True)
                time.sleep(2)

    def publish(self, data):
        digest = hashlib.sha1(data).digest()
        with self.frames:
            now = time.time()
            self.frame_times = [t for t in self.frame_times if now - t < 5] + [now]
            if digest == self.digest:
                return
            self.frame, self.digest = data, digest
            self.seq += 1
            if self.seq == 1:
                size = jpeg_size(data)
                print(f"First frame received ({size} pixels)", flush=True)
                if self.points is None and size:
                    self.points = [round(size[0] / SCALE / 3), round(size[1] / SCALE / 3)]
            self.frames.notify_all()

    def wait_frame(self, after, timeout):
        with self.frames:
            end = time.time() + timeout
            while self.seq <= after and not self.stopped.is_set():
                left = end - time.time()
                if left <= 0:
                    break
                self.frames.wait(left)
            return self.seq, self.frame

    # Input ---------------------------------------------------------------------------------------------------

    def point(self, body, x="x", y="y"):
        w, h = self.points or (402, 874)
        return f"{clamp(body[x], 0, 1) * w:.1f}", f"{clamp(body[y], 0, 1) * h:.1f}"

    def input(self, body):
        kind = body.get("type")
        with self.gestures:
            if kind == "tap":
                x, y = self.point(body)
                result = self.axe("tap", "-x", x, "-y", y)
            elif kind == "longpress":
                x, y = self.point(body)
                seconds = f"{clamp(body.get('seconds', 1), 0.5, 5):.2f}"
                result = self.axe("touch", "-x", x, "-y", y, "--down", "--up", "--delay", seconds)
            elif kind == "swipe":
                x1, y1 = self.point(body, "x1", "y1")
                x2, y2 = self.point(body, "x2", "y2")
                distance = ((float(x2) - float(x1)) ** 2 + (float(y2) - float(y1)) ** 2) ** 0.5
                seconds = clamp(distance / clamp(body.get("velocity", 1500), 100, 8000), 0.08, 2)
                if float(body.get("hold") or 0) > 0:  # slow drag that stops where it ends
                    result = self.axe("drag", "--start-x", x1, "--start-y", y1, "--end-x", x2, "--end-y", y2,
                                      "--duration", f"{max(seconds, 0.5):.2f}")
                else:
                    result = self.axe("swipe", "--start-x", x1, "--start-y", y1, "--end-x", x2, "--end-y", y2,
                                      "--duration", f"{seconds:.2f}", "--delta", "20")
            elif kind == "type":
                return self.type_text(str(body.get("text") or ""))
            elif kind == "home":
                result = self.axe("button", "home")
            elif kind == "open":
                result = self.simctl("launch", self.udid, self.args.bundle)
            elif kind == "terminate":
                result = self.simctl("terminate", self.udid, self.args.bundle)
            elif kind == "launch":
                self.language = str(body.get("language") or "")
                return {"ok": self.launch(self.language), "error": ""}
            else:
                return {"ok": False, "error": "Unknown input"}
        return {"ok": result.returncode == 0, "error": "" if result.returncode == 0 else "The Simulator refused that"}

    def type_text(self, text):
        """Plain keyboard text is typed; anything else (Arabic, emoji…) goes to the phone's clipboard."""
        if any(ord(c) > 126 or (ord(c) < 32 and c not in "\n\b") for c in text):
            self.simctl("pbcopy", self.udid, stdin=text)
            return {"ok": True, "error": "", "note": "Copied to the phone's clipboard: tap the text field again and choose Paste."}
        for part in re.split(r"([\n\b])", text):
            if part == "\n":
                result = self.axe("key", KEY_ENTER)
            elif part == "\b":
                result = self.axe("key", KEY_BACKSPACE)
            elif part:
                result = self.axe("type", "--stdin", stdin=part)
            else:
                continue
            if result.returncode != 0:
                return {"ok": False, "error": "Couldn't type that. Is a text field selected?"}
        return {"ok": True, "error": ""}

    def control(self, body):
        kind = body.get("type")
        if kind == "appearance":
            mode = "dark" if body.get("mode") == "dark" else "light"
            self.simctl("ui", self.udid, "appearance", mode)
            self.appearance = mode
        elif kind == "location":
            lat, lon = clamp(body["lat"], -90, 90), clamp(body["lon"], -180, 180)
            self.simctl("location", self.udid, "set", f"{lat:.5f},{lon:.5f}")
        elif kind == "reset":
            with self.gestures:
                self.simctl("terminate", self.udid, self.args.bundle)
                self.simctl("uninstall", self.udid, self.args.bundle)
                if self.simctl("install", self.udid, self.args.app).returncode != 0:
                    return {"ok": False, "error": "Couldn't reinstall the app"}
                return {"ok": self.launch(self.language), "error": ""}
        elif kind == "stop":
            self.stop()
        else:
            return {"ok": False, "error": f"Unknown control {kind}"}
        return {"ok": True, "error": ""}

    def stop(self):
        self.stopped.set()
        with self.frames:
            self.frames.notify_all()

    def info(self):
        now = time.time()
        return {
            "ready": self.seq > 0,
            "points": self.points,
            "remaining": max(0, int(self.deadline - now)),
            "appearance": self.appearance,
            "language": self.language,
            "fps": round(len([t for t in self.frame_times if now - t < 5]) / 5, 1),
            "ended": self.stopped.is_set(),
        }


def jpeg_size(data):
    """(width, height) from a JPEG's start-of-frame marker."""
    i = 2
    while i + 9 < len(data):
        if data[i] != 0xFF:
            return None
        marker, length = data[i + 1], struct.unpack(">H", data[i + 2:i + 4])[0]
        if 0xC0 <= marker <= 0xCF and marker not in (0xC4, 0xC8, 0xCC):
            height, width = struct.unpack(">HH", data[i + 5:i + 9])
            return width, height
        i += 2 + length
    return None


class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    session: Session = None

    def log_message(self, format, *args):  # never log query strings: the first request carries the key
        path = urllib.parse.urlsplit(self.path).path
        if path not in ("/info", "/frame"):
            sys.stderr.write(f"{self.command} {path} {args[1] if len(args) > 1 else ''}\n")

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
        self.wfile.write(body)

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
            if not frame or seq <= after:
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
        self.send_header("X-Accel-Buffering", "no")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True
        seq, sent, frames, started = 0, time.time(), 0, time.time()
        try:
            while not s.stopped.is_set():
                new_seq, frame = s.wait_frame(seq, 5)
                if frame and new_seq != seq:
                    seq, sent = new_seq, time.time()
                    self.wfile.write(struct.pack(">I", len(frame)) + frame)
                    frames += 1
                elif time.time() - sent >= 10:
                    sent = time.time()
                    self.wfile.write(struct.pack(">I", 0))
                self.wfile.flush()
        except OSError:  # the viewer went away
            pass
        print(f"Stream closed after {int(time.time() - started)} s, {frames} frames", flush=True)

    def do_POST(self):
        url = urllib.parse.urlsplit(self.path)
        if not self.authorised({}):
            return self.send(403, {"ok": False, "error": "Not allowed"})
        try:
            length = int(self.headers.get("Content-Length") or 0)
            if length > 100_000:
                raise ValueError("too large")
            body = json.loads(self.rfile.read(length) or b"{}")
        except ValueError:
            return self.send(400, {"ok": False, "error": "Bad request"})
        s = self.session
        if s.stopped.is_set():
            return self.send(410, {"ok": False, "error": "The session has ended"})
        try:
            if url.path == "/input" and body.get("type") in INPUTS:
                started = time.time()
                result = s.input(body)
                result["ms"] = int((time.time() - started) * 1000)
                return self.send(200, result)
            if url.path == "/control":
                return self.send(200, s.control(body))
        except (KeyError, TypeError, ValueError, subprocess.TimeoutExpired) as error:
            return self.send(400, {"ok": False, "error": str(error)})
        return self.send(404, {"ok": False, "error": "Not found"})


class Server(http.server.ThreadingHTTPServer):
    daemon_threads = True

    def handle_error(self, request, client_address):
        if not isinstance(sys.exc_info()[1], OSError):  # viewers closing the page aren't errors
            super().handle_error(request, client_address)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--udid", required=True)
    parser.add_argument("--app", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--bundle", default="io.github.themeshmaker.dunya")
    parser.add_argument("--minutes", type=float, default=60)
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--fps", type=int, default=12)
    parser.add_argument("--stop-file")
    args = parser.parse_args()

    session = Session(args)
    signal.signal(signal.SIGTERM, lambda *_: session.stop())
    session.simctl("install", args.udid, args.app)
    session.launch()
    session.points = session.screen_points()
    print(f"Screen: {session.points} points", flush=True)

    Handler.session = session
    server = Server(("127.0.0.1", args.port), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    threading.Thread(target=session.stream_video, daemon=True).start()
    print(f"Serving on {args.port} for {args.minutes:g} minutes", flush=True)

    while not session.stopped.wait(2):
        if time.time() > session.deadline:
            print("Time is up", flush=True)
            session.stop()
    if args.stop_file:
        Path(args.stop_file).write_text("stopped\n")
    print("Session ended", flush=True)


if __name__ == "__main__":
    main()
