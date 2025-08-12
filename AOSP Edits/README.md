# AOSP Edited Files

The below are files that are edited/added to the base AOSP.

## Added allowroot.sh, vendor.mk, prebuilts folder

Added allowroot.sh, vendor.mk, prebuilts folder to this directory /home/android/aosp/pixel5a/vendor/google_devices/barbet

## Edited /home/android/aosp/pixel5a/system/core/rootdir/init.rc

Added the lines below to init.rc located in /home/android/aosp/pixel5a/system/core/rootdir
```
service allowroot /system/bin/allowroot.sh
    seclabel u:r:su:s0
    class main
    user root
    group root system
    disabled
    oneshot

on property:dev.bootcomplete=1
    start allowroot
``` 

## Edited /home/android/aosp/pixel5a/build/target/product/handheld_system.mk

Added the lines below to handheld_system.mk /home/android/aosp/pixel5a/build/target/product
```
SweetDeceit \

vendor/google_devices/barbet/allowroot.sh:system/bin/allowroot.sh \
```

## Edited /home/android/aosp/pixel5a/out/target/product/barbet/system/etc/permissions/privapp-permissions-platform.xml

Added the lines below to /home/android/aosp/pixel5a/out/target/product/barbet/system/etc/permissions/privapp-permissions-platform.xml
```
<permissions>
    <privapp-permissions package="com.example.sweetdeceit">
        <permission name="android.permission.REBOOT"/>
    </privapp-permissions>
</permissions>
```
