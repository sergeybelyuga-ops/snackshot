# Forwards Firebase emulator and fake LogMeal ports from the connected device to this PC.
adb reverse tcp:9099 tcp:9099   # Auth emulator
adb reverse tcp:5001 tcp:5001   # Functions emulator (broker)
adb reverse tcp:8090 tcp:8090   # Fake LogMeal server
adb reverse --list
