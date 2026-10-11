"""Converts the Android adhan recordings (Ogg Vorbis, which iOS can't play) for the iOS app.

For each voice it writes, into ios/Deen/Resources/Sounds:
  adhan_<voice>_alarm.caf   the first 30 seconds with a short fade-out, IMA4 ADPCM mono 22.05 kHz:
                            the format and length iOS accepts for alarm and notification sounds
  adhan_<voice>.m4a         the whole recording, AAC, for playing inside the app

Requires ffmpeg on the PATH. Run from the repository root:
    python -I tools/audio/build_ios_sounds.py

The recordings keep their licences (CONTENT_SOURCES.md); the shortened alarm clips are changed
versions under the same licence.
"""
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "core", "alarm", "src", "main", "res", "raw")
OUT = os.path.join(ROOT, "ios", "Deen", "Resources", "Sounds")
VOICES = ["default", "traditional", "aaqib"]
CLIP_SECONDS = 29.5
FADE_SECONDS = 2.0


def run(args):
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", *args], check=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    for voice in VOICES:
        src = os.path.join(SOURCE, f"adhan_{voice}.ogg")
        run(["-i", src, "-t", str(CLIP_SECONDS),
             "-af", f"afade=t=out:st={CLIP_SECONDS - FADE_SECONDS}:d={FADE_SECONDS}",
             "-ac", "1", "-ar", "22050", "-c:a", "adpcm_ima_qt", "-map_metadata", "-1",
             os.path.join(OUT, f"adhan_{voice}_alarm.caf")])
        run(["-i", src, "-ac", "1", "-c:a", "aac", "-b:a", "80k", "-map_metadata", "-1",
             os.path.join(OUT, f"adhan_{voice}.m4a")])
        print("ok", voice)


if __name__ == "__main__":
    sys.exit(main())
