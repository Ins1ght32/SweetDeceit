package com.example.sweetdeceit;

import static com.example.sweetdeceit.ApkInstallReceiver.TRACKED_PACKAGES;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

public class FileObserverService extends Service {
    private static final String TAG = "FileObserverService";

    static File statusFile = new File( "/data/data/com.example.sweetdeceit/cache/status.config");
    static File actionFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "action.config");
    static File shutFile = new File("/data/data/com.example.sweetdeceit/cache/shut.config");

    static SecretKey key;

    // Used by RestartServiceReceiver to send intent
    public static final String ACTION_RESTART_OBSERVERS = "com.example.sweetdeceit.RESTART_OBSERVERS";
    static Map<String, Process> processMap = new HashMap<>(); // Map to store processes against file paths
    ApkInstallReceiver apkInstallReceiver; // Receiver for APK installation events
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static DevicePolicyManager devicePolicyManager;
    private static ComponentName adminComponent;
    private static final int SMS_PERMISSION_CODE = 101;

    private ServiceMonitor serviceMonitor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // No binding needed for a background service
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (isDeviceRooted()) {
            Log.d(TAG, "Device has root access.");
        } else {
            Log.d(TAG, "Device does not have root access.");

            // Sleep indefinitely due to no root perms
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Log.e(TAG, String.valueOf(e));
            }
        }

        changeMountNSMode();

        try {
            if (KeyStoreHelper.isKeyPresent()) {
                key = KeyStoreHelper.getKey();
                Log.d(TAG, "There is a key that exists already");
            } else {
                key = KeyStoreHelper.generateKey();
                Log.d(TAG, "Making new key");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Register APK install broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        //intentFilter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        intentFilter.addDataScheme("package");

        apkInstallReceiver = new ApkInstallReceiver();
        registerReceiver(apkInstallReceiver, intentFilter);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Unable to sleep before critical HT operations");
        }

        //Write the config files if they exist
        writeConfigFiles();

        decryptFilesUponBoot();

        // Create Pre-defined Honey Tokens
        writePredefinedHoneyTokens();

        // Re-monitor package install HTs
        restartTrackedPackageHoneyTokens();

        // Set the alarm to restart file monitoring periodically
        setUpAlarm();

        // Monitor for ADB Backup
        serviceMonitor = new ServiceMonitor();
        serviceMonitor.startMonitoring();

        // Initialize DevicePolicyManager and the admin component
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        grantSystemPermissions(this);
    }

    public static boolean isDeviceRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/system/sbin/su", "/sbin/su", "/vendor/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private void grantSystemPermissions(Context context) {
        String[] permissions = {
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SEND_SMS
        };

        PackageManager pm = context.getPackageManager();
        for (String permission : permissions) {
            if (pm.checkPermission(permission, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                pm.addPermission(new android.content.pm.PermissionInfo());
                // Automatically grant permissions
                // As a system app, this should succeed without user interaction
            }
        }
    }

    public void changeMountNSMode() {
        try {
            // Build the command to create a file at the specified file path
            String suCommand = "su -c sqlite3 /data/adb/magisk.db 'UPDATE settings SET value = \"0\" WHERE key = \"mnt_ns\";'";
            Process process = Runtime.getRuntime().exec(suCommand);
            process.waitFor();  // Wait for the command to finish

            Log.i(TAG, "Successfully changed mount namespace mode");
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error changing mount namespace mode", e);
        }
    }

    // Monitor the file using inotifywait with su privileges
    public static void monitorFileWithInotify(final String filePath) {
        new Thread(() -> {
            try {
                // Use 'su' to run inotifywait command to monitor file access in the /data directory
                String suCommand = "su -c inotifyd - " + filePath;
                Process process = Runtime.getRuntime().exec(suCommand);
                processMap.put(filePath, process);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    Log.i(TAG, "File event detected: " + line);
                    try {
                        // Read content of status.txt
                        String statusContent = "";
                        if (statusFile.exists()) {
                            try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
                                statusContent = br.readLine();
                            }
                        }

                        String actionContent = "";
                        if (actionFile.exists()) {
                            try (BufferedReader br = new BufferedReader(new FileReader(actionFile))) {
                                actionContent = br.readLine();
                            }
                        }

                        if ("1".equals(actionContent)){
                            // Attempt to wipe data (factory reset)
                            devicePolicyManager.wipeData(0);
                        } // If not default to encrypt

                        if ("0".equals(statusContent)) {
                            try {
                                // Command to change the file content
                                Process processWrite = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo 1 > " + statusFile.getAbsolutePath()});
                                processWrite.waitFor();
                                Log.d(TAG, "Status file updated to 1 with root privileges");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to write to status file with root privileges", e);
                            }

                            FolderTraversal.encryptDefined();
                            FolderTraversal.encryptPackageData();
                        }
                    } catch (SecurityException e) {
                        // Handle the exception if the user cannot be wiped (e.g., system user)
                        Log.i(TAG,"Cannot Factoryreset :(");
                        e.printStackTrace();
                    } catch (Exception e) {
                        Log.i(TAG,"Cannot encrypt :(");
                        throw new RuntimeException(e);
                    }
                }

                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Error monitoring file with inotifywait in /data.", e);
            }
        }).start();
    }

    // Monitor the file only if the process is not already running
    void monitorFileWithInotifyIfNotRunning(String filePath) {
        Process process = processMap.get(filePath);
        if (process != null && isProcessRunning(process)) {
            Log.i(TAG, "Process for " + filePath + " is already running. No need to restart.");
            return; // Process is running, no need to restart
        }

        Log.i(TAG, "Starting new inotifyd process for: " + filePath);
        monitorFileWithInotify(filePath);
    }

    // Check if the process is still running
    private boolean isProcessRunning(Process process) {
        try {
            process.exitValue(); // If the process has terminated, this will throw an exception
            return false; // Process is not running
        } catch (IllegalThreadStateException e) {
            return true; // Process is still running
        }
    }

    // Set up the AlarmManager to periodically restart the file monitoring process
    private void setUpAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set the alarm to repeat every 9 minutes
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                540000, pendingIntent);

        Log.i(TAG, "Alarm set to restart file monitoring every 9 minutes.");
    }


    // Method to write the text file to the Documents folder
    private void writeConfigFiles() {
        try {
            // Write to action.txt if it doesn't exist
            if (!actionFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(actionFile)) {
                    fos.write('0');
                    Log.d(TAG, "Action file written successfully: " + actionFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing action file", e);
                }
            } else {
                Log.d(TAG, "Action file already exists, no need to write.");
            }

            // Write to status.txt if it doesn't exist, and set it as read-only
            if (!statusFile.exists()) {
                try (FileOutputStream fos2 = new FileOutputStream(statusFile)) {
                    fos2.write('0');
                    Log.d(TAG, "Status file written successfully: " + statusFile.getAbsolutePath());

                    // Set the status file to read-only
                    if (!statusFile.setReadOnly()) {
                        Log.e(TAG, "Failed to set status file as read-only");
                    } else {
                        Log.d(TAG, "Status file set as read-only");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing status file", e);
                }
            } else {
                Log.d(TAG, "Status file already exists, no need to write.");
            }

            // Write to shut.txt if it doesn't exist, and set it as read-only
            if (!shutFile.exists()) {
                try (FileOutputStream fos2 = new FileOutputStream(shutFile)) {
                    fos2.write('0');
                    Log.d(TAG, "Status file written successfully: " + shutFile.getAbsolutePath());

                    // Set the status file to read-only
                    if (!shutFile.setReadOnly()) {
                        Log.e(TAG, "Failed to set shut file as read-only");
                    } else {
                        Log.d(TAG, "Status file set as read-only");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing shut file", e);
                }
            } else {
                Log.d(TAG, "Shut file already exists, no need to write.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to write file", e);
        }
    }

    // This function also restarts the honey tokens on bootup.
    private void writePredefinedHoneyTokens(){
        // Sleep before monitoring / writing any tokens
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Unable to sleep before monitoring / writing any tokens");
        }

        String firstHT = ".AFileThatIsImportant.txt";
        DefinedHoneyTokenUtils.txtInExternalRoot(this, firstHT);

        String pictureFileName = ".drug_sample5324_1243.jpg"; // The file name in Pictures
        int drawableId = R.drawable.drug_sample5324_1243; // The drawable resource ID
        DefinedHoneyTokenUtils.imageInPicture(this, pictureFileName, drawableId);

        String documentFileName = ".drug_delivery_schedule_applewood_019385.txt"; // The file name in Documents
        DefinedHoneyTokenUtils.txtInDocumentOrDownload(this, documentFileName, true);

        String downloadFileName = ".drug_shipping_schedule_mapletree_1914.txt";
        DefinedHoneyTokenUtils.txtInDocumentOrDownload(this, downloadFileName, false);

        String recordingFileName = ".drug_delivery_instructions_19385.mp3";
        DefinedHoneyTokenUtils.copyMp4ToRecordings(this, recordingFileName, R.raw.drug_delivery_instructions_19385);

        List<String> packageNames = Arrays.asList("com.android.providers.contacts", "com.android.providers.telephony");
        for (String packageName : packageNames) {
            try {
                DefinedHoneyTokenUtils.createFileHTInDirectory(packageName);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error processing package: " + packageName, e);
            }
        }
    }

    private void decryptFilesUponBoot() {
        try {
            if (shutFile.exists()) {
                String shutContent = "";
                try (BufferedReader br = new BufferedReader(new FileReader(shutFile))) {
                    shutContent = br.readLine();
                } catch (IOException e) {
                    Log.e(TAG, "Error writing action file", e);
                }

                if ("1".equals(shutContent)) {

                    FolderTraversal.decryptPackageData();
                    FolderTraversal.decryptDefined();

                    try {
                        // Command to change the file content
                        Process processWrite = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo 0 > " + shutFile.getAbsolutePath()});
                        processWrite.waitFor();
                        Log.d(TAG, "Shut file updated to 0 with root privileges");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to write to Shut file with root privileges", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to decrypt files upon Boot");
        }
    }

    private void restartTrackedPackageHoneyTokens() {
        for (String packageName : TRACKED_PACKAGES) {
            // Build folder path for each token
            String token1dir = "/storage/emulated/0/Android/data/" + packageName + "/files/";
            String token2dir = "/data/data/" + packageName + "/files/";

            try {
                // Path to check dirs for tokens
                String listCommand = "su -c ls '" + token1dir + "'";
                Process listProcess = Runtime.getRuntime().exec(listCommand);
                BufferedReader reader = new BufferedReader(new InputStreamReader(listProcess.getInputStream()));
                String itemName;

                while ((itemName = reader.readLine()) != null) {
                    if (itemName.contains("action_history_") && itemName.contains(".log")){
                        FileObserverService.monitorFileWithInotify(token1dir + itemName);
                    }
                }

                String listCommand2 = "su -c ls '" + token2dir + "'";
                Process listProcess2 = Runtime.getRuntime().exec(listCommand2);
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(listProcess2.getInputStream()));
                String itemName2;

                while ((itemName2 = reader2.readLine()) != null) {
                    if (itemName2.contains("action_history_") && itemName2.contains(".log")){
                        FileObserverService.monitorFileWithInotify(token2dir + itemName2);
                    }
                }

            }catch (Exception e){
                System.out.println("Dir check failed");
            }

        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(apkInstallReceiver);
        ServiceMonitor.stopMonitoring();
        super.onDestroy();
    }
}
