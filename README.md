# Sweet Deceit

Sweet Deceit is a second-layer anti-forensics framework which utilizes honey token tripwires to deploy anti-forensic payloads.

Note that as part of this framework, "first-layer" Android anti-forensics techniques (i.e., killing ADB backup and forensic software agent processes) have also been implemented in order to allow Sweet Deceit to be tested holistically against forensic tools.

## Table of Contents

- [Introduction](#introduction)
- [Concept](#concept)
- [Folder Structure](#folder-structure)
  - [AOSP Edits](#aosp-edits)
  - [SweetDeceit](#sweetdeceit)
- [Installation](#installation)
- [Usage](#usage-guide)
  - [Sweet Deceit](#using-sweetdeceit)
- [Contributing](#contributing)
- [License](#license)

## Introduction

This README will contain simple guiding steps on how to setup an environment and install Sweet Deceit Accordingly. It will also contain key concept introductions to the methodology behind the implementation of Sweet Deceit.

## Concept

- **Main Sweet Deceit Component (APK)** (i.e., `SweetDeceit`).

This APK can be modified accordingly to better suit your forensic tool or testing environemnt needs. In retrospect, this APK, if set up done properly, will run as a system service in the background, that manages both first and second layer defenses. 

The concept behind Sweet Deceit is to utilise a combination of existing "First Layer Android Anti-Forensic Measures" and our new introduced Honey-Token traps which sit at the "Second Layer":
- **First Layer Android Anti-Forensic Measures**: These are anti-forensic measures that outrightly prevent the any form of acquisition from occuring. This means that, if implemented properly, forensic data will remain on the target device only and not be transfered to the acquisition controller if that acquisition approach is affected by the anti-forensic measure deployed. In the context of Sweet Deceit, the 2 first layer anti-forensic measures implemented are:
  - ADB Backup Process Killing. If Sweet Deceit detects that the ADB Backup process has been spawned through the identification of the process name, it will kill the process.
  - Agent Process Killing. If Sweet detects the package installation of certain package names which are indicative whether these packages are certain forensic vendor agents, Sweet Deceit will kill the installed agent's process via its package name.
- **Second Layer Android Anti-Forensic Measures**: These are anti-forensic measures that instead of denying the access of such forensic tools for forensic acquisition, allows them access instead. But upon detection of high-value forensic actions, like in our implementation case of forensic data acquisition, will only then perform countermeasures just-in-time. This is all in a bid to hopefully, reduce the analyst suspicion of the device as low-value forensic actions are still permitted (e.g. forensic triaging). In the context of Sweet Deceit, the second layer anti-forensic measure implemented are:
  - Honey Token deployment and monitoring. Sweet Deceit will deploy Honey Tokens in high-value areas on first device boot and during package installations that are of key interest to forensic analysts. Monitoring of the honey tokens are performed through the "inotify" API, whereby reading of such files will, by Sweet Deceit, classified as forensic acquisition occuring. This is designed in the manner whereby the location of the deployment of such Honey Tokens are in locations where a normal user will not typically access, and access would mean with high confidence, forensic activity.

The combination of these 2 layers of anti-forensic measure will thereby allow for the filtering of forensic processes to areas whereby the Honey-Token traps can be most effective, while hopfully reducing the suspicion of devices that implement Sweet Deceit.
<p align="center">
  <img src="https://github.com/user-attachments/assets/a0d18ec1-d899-4289-a3eb-930b0801d824" alt="Flow-Honey Token 2nd layer defense">
</p>

## Folder Structure

### AOSP Edits

- **Description:** Folder containing key AOSP edits we made to ensure Sweet Deceit's OS-level operationability. 
- **Contents:** Key components for root detection.
  - `AOSP Edits/prebuilts`: This folder contains the various applications necessary for Sweet Deceit's full functionality. The sub folder in here include SweetDeceit. In the event you the user wishes to rename any of the application name, please ensure you align the contents of the "Android.bp" files in each sub folder accordingly.

---

### SweetDeceit

- **Description:** Main Sweet Deceit component, which runs Sweet Deceit as a system service on the device. All operating logic is contained within this Android project.
- **Contents:** Key components for Sweet Deceit Service.
  - `SweetDeceit/app/src/main/java/com/example/sweetdeceit`: Contains all Java classes necessary in supporting the full functionality of Sweet Deceit.
  - `SweetDeceit/app/src/main/java/com/example/sweetdeceit/FileObserverService.java`: The main function for this system service application.

## Installation

### Pre-Requisites

Things you need:
- Computer (at least 32GB Ram & SSD with at least 200GB)
- Google Pixel 5a

To hide Magisk App, hide it with a new name "NetfIix" \
Netflix with capital i instead of l

---

Follow the steps below to set up the project:

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/your-repository.git
   ```
2. Download and build AOSP 12:
   ```bash
   repo init -u https://android.googlesource.com/platform/manifest -b android-12.0.0_r28 --depth=1
   repo sync -qc -j15
   ```
3. Download radio drivers from:
   ```bash
   https://dl.google.com/dl/android/aosp/google_devices-barbet-sq1a.220205.002-fdf2a40a.tgz
   https://dl.google.com/dl/android/aosp/qcom-barbet-sq1a.220205.002-c1211158.tgz
   ```
4. Unzip and place those two files in your repo folder and run the scripts
5. Copy allowroot.sh, vendor.mk, prebuilts folder from AOSP Edits to this directory /vendor/google_devices/barbet
6. Replace the init.rc file in /system/core/rootdir with the one in AOSP Edits
7. Replace the handheld_system.mk file in /build/target/product with the one in AOSP Edits
8. Run the below commands to build AOSP code
   ```bash
   export ANDROID_PRODUCT_OUT=/home/android/aosp/pixel5a/out/target/product/barbet
   source build/envsetup.sh
   lunch aosp_barbet-userdebug
   m droid -j4
   ```
9. Flash custom AOSP to your device (ensure that bootloader is unlocked)
   ```bash
   adb reboot bootloader
   fastboot flashall -w
   ```
10. Install Magisk on the phone and patch the boot image
   ```bash
   adb install magisk.apk
   adb push /out/target/product/barbet/boot.img /storage/emulated/0/Documents/
   ```
   Open magisk app and patch the boot.img in Documents folder\
11. Root the phone with patched boot image
   ```bash
   adb pull /storage/emulated/0/Download/magisk_patched-28000_g6dlc.img
   adb reboot bootloader
   fastboot flash boot magisk_patched-28000_g6dlc.img
   fastboot reboot
   ```
12. Hide the magisk app
    Open the magisk app\
    Click on settings > hide the magisk app > enable from this source > click back and enter NetfIix then click ok

## Usage Guide

Please find the various usage guides below, for SweetDeceit.

### Using Sweet Deceit

- **Description:** The Sweet Deceit service is meant to run with as little user intervention as possible. The only items that the user will need to know for "usage" of the framework would be countermeasure handling components. 
- **Contents:** Key countermeasure handling components
  - `action.config`: Located in the Documents folder of the Android Device. The default value is set to 0 whereby if honey tokens are triggered, the encryption countermeasure will be conducted. The user can utilise any text editor tool to edit this value to "1" to change the honey token triggered countermeasure to factory reset the device instead. (And vice versa)
  - `SMS String 1: SPECIFIC_STRING`: If the user sends this SMS string to the device with Sweet Deceit installed and running, they can perform decryption of the device in the event it was previously encrypted by Honey Token Triggering.
  - `SMS String 2: SPECIFIC_STRING2`: If the user sends this SMS string to the device with Sweet Deceit installed and running, they can edit "remotely" change the contents of the action.config file to "1" instead.
  - `SMS String 3: SPECIFIC_STRING3`: If the user sends this SMS string to the device with Sweet Deceit installed and running, they can edit "remotely" change the contents of the action.config file to "0" instead. 

## Contributing

This framework was created as part of the ICT3215 Digital Forensics module for our university, **Singapore Institute of Technology**.

For authorship information, please refer to the [CITATION](CITATION.cff) file for details.

## License

This project is licensed under the **GNU General Public License v3.0**.

See the [LICENSE](LICENSE.md) file for details.
