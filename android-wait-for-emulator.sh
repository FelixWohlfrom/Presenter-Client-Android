#!/usr/bin/env bash

if [ "x`adb devices`" = "x" ]; then
    echo "No device found. Did the emulator crash?"
    exit 1
fi

# ensure that adb daemon is properly startet
# otherwise it will show some "adb daemon is starting"
adb devices

failcounter=0
timeout=360
boot_complete=`adb shell getprop dev.bootcomplete | tr -d '\r'`

while [[ "x${boot_complete}" = "x" ]]; do
    let "failcounter += 1"

    if [[ ${failcounter} -gt ${timeout} ]]; then
       echo "Emulator failed to start"
       exit 1
    fi

    echo "Waiting for emulator to start"
    sleep 1
    boot_complete=`adb shell getprop dev.bootcomplete | tr -d '\r'`
done

echo "Emulator is ready"
exit 0
