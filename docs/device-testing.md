# Device testing: making sure the adhan plays

Alarms that work on an emulator can still fail on real phones, because phone makers add their own
battery managers. Run these checks on a real device before every release (docs/BLUEPRINT.md §18).

`PKG` below is `io.github.themeshmaker.dunya` (release) or `io.github.themeshmaker.dunya.debug` (debug).

## 1. Are our alarms registered?

```bash
adb shell dumpsys alarm | grep -A3 "$PKG"
```

Expect one `RTC_WAKEUP` alarm per enabled prayer (plus reminders), each with `window=0` and
`exactAllowReason=policy_permission` (FOSS build, `USE_EXACT_ALARM`) or `permission` (Play build).
`adb shell dumpsys alarm | grep -A2 "Next alarm clock"` should show the next prayer.

## 2. The in-app test

More → *Make sure the adhan always plays* → **Test adhan in 1 minute**, then lock the phone.
The adhan should start within a few seconds of the minute. The result appears under *Recent alarms*.

## 3. Doze and standby

```bash
adb shell dumpsys deviceidle force-idle         # deep Doze now
adb shell am set-standby-bucket "$PKG" restricted
# … wait for a test adhan or a prayer …
adb shell dumpsys deviceidle unforce
adb shell am set-standby-bucket "$PKG" active
```

Alarm-clock alarms must still fire on time in both cases.

## 4. Android 17 background-audio hardening

```bash
adb shell cmd audio set-enable-hardening throw   # strictest mode; crashes on violations
# run a test adhan
adb shell cmd audio set-enable-hardening disable
adb shell dumpsys audio | grep -i AudioHardening
```

The adhan plays on the alarm stream (`USAGE_ALARM`), which Android 17 exempts for apps holding the
exact-alarm permission.

## 5. Reboot before unlock (direct boot)

Schedule a test adhan or wait for a prayer, reboot, and **don't unlock**. The adhan must still play:
alarms are re-planned from device-protected storage by `SystemEventsReceiver` on `LOCKED_BOOT_COMPLETED`.

## 6. Phone-maker killers

| ROM | What to try | Expected |
|---|---|---|
| ColorOS / OxygenOS | Swipe Deen away in Recents; "Clear all"; leave overnight | Alarms survive once *Allow background activity* is on. If Recents "clear all" removes them, the next app start logs **Cleared by the phone** |
| HyperOS / MIUI | Same, with Autostart off then on | |
| OriginOS / Funtouch | Same | |
| One UI | Put Deen in *Sleeping apps*, then remove it | |

Findings so far (a ColorOS 17 CN foldable, Android 17):
- New apps start with notifications **blocked**; the checklist's *Allow* opens the system prompt.
- `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` shows ColorOS's own *"Allow all background activity?"*
  and switches the app from *Smart mode* to *Allow background activity*.
- `adb shell pm clear` is blocked by ColorOS; uninstall to reset app data.
- The phone has two displays; `screencap` needs `-d <display id>` (see `dumpsys SurfaceFlinger --display-id`).

## 7. The health log

Every alarm is logged with its scheduled and actual time and the outcome. Bug reports about missed
adhans should include a screenshot of *Recent alarms* plus the phone model and ROM version.
