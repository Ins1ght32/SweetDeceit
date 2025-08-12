package com.example.sweetdeceit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class ApkInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "ApkInstallReceiver";

    // Define the allowed list of package names
    static final List<String> TRACKED_PACKAGES = Arrays.asList(
            "org.telegram.messenger.web",
            "org.mozilla.firefox"
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            String packageName = intent.getData().getSchemeSpecificPart();

            PackageManager packageManager = context.getPackageManager();
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                String appName = (String) packageManager.getApplicationLabel(appInfo);

                Log.d(TAG, "New tracked app installed: " + appName + " (" + packageName + ")");

                //Tracking masked Magisk app to hide.
                if (appName.equals("NetfIix")) {
                    Thread.sleep(15000);
                    String commandPmHide = "su -c pm hide " + packageName;
                    try {
                        Process hidingProcess = Runtime.getRuntime().exec(commandPmHide);
                        hidingProcess.waitFor();
                    } catch (IOException | InterruptedException e) {
                        Log.e(TAG, "Error executing pm hide command", e);
                    }
                } else {
                    String commandDenylist = "su -c magisk --denylist add " + packageName + " && monkey -p " + packageName + " 1 && sleep 0.3 && pm disable " + packageName + " && pm enable " + packageName;
                    try {
                        // Enforce deny for all kinds of packages to prevent root detection.
                        Process denylistProcess = Runtime.getRuntime().exec(commandDenylist);
                        denylistProcess.waitFor();
                        if (TRACKED_PACKAGES.contains(packageName)) {
                            Log.e(TAG, "Detected Tracked Package Installation");

                            // Create the file in the specified path for tracked packagesusing su
                            createFileInAppDirectory(packageName);
                        }

                        if (packageName.contains("belkasoft") || packageName.contains("cellebrite")){
                            //String commandDenyPackage= "su -c pm disable " + packageName;
                            String commandDenyPackage= "su -c do pkill -f " + packageName;
                            Process denyPackage = Runtime.getRuntime().exec(commandDenyPackage);
                            denyPackage.waitFor();
                        }
                    } catch (IOException | InterruptedException e) {
                        Log.e(TAG, "Error executing denylist command", e);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "App name not found for package: " + packageName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createFileInAppDirectory(String packageName) throws InterruptedException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "action_history_" + timeStamp + ".log";
        String filePath = "/storage/emulated/0/Android/data/" + packageName + "/files/" + fileName;
        String filePath2 = "/data/data/" + packageName + "/files/" + fileName;
        //Thread.sleep(30000);
        Log.d(TAG, filePath);
        Log.d(TAG, filePath2);
        String commandCreateFile = "su -c touch " + filePath;
        String commandCreateFile2 = "su -c touch " + filePath2;

        try {
            // Create the file using su. 2 files, in the potential directories
            Process createProcess = Runtime.getRuntime().exec(commandCreateFile);
            createProcess.waitFor();
            addFakeContentToLogFileWithSu(filePath, packageName);

            Process createProcess2 = Runtime.getRuntime().exec(commandCreateFile2);
            createProcess2.waitFor();
            addFakeContentToLogFileWithSu(filePath2, packageName);

            // Get the owner of an existing file (e.g., the first file in the directory)
            String commandGetOwner = "su -c ls -l /storage/emulated/0/Android/data/" + packageName + " | awk 'NR==2 {print $3, $4}'";
            Process ownerProcess = Runtime.getRuntime().exec(commandGetOwner);

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(ownerProcess.getInputStream()));
            String ownerInfo = reader.readLine(); // Expecting format: owner group

            //Handling 2nd honey token
            // Get the owner of an existing file (e.g., the first file in the directory)
            String commandGetOwner2 = "su -c ls -l /data/data/" + packageName + " | awk 'NR==2 {print $3, $4}'";
            Process ownerProcess2 = Runtime.getRuntime().exec(commandGetOwner2);

            // Read the output
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(ownerProcess2.getInputStream()));
            String ownerInfo2 = reader2.readLine(); // Expecting format: owner group

            if (ownerInfo != null) {
                String[] parts = ownerInfo.split(" ");
                String owner = parts[0];
                String group = parts[1];

                // Change ownership of file1.txt to match
                String commandChangeOwner = "su -c chown " + owner + ":" + group + " " + filePath;
                Process chownProcess = Runtime.getRuntime().exec(commandChangeOwner);
                chownProcess.waitFor();

                Log.d(TAG, "File created and ownership changed to: " + owner + ":" + group);

                FileObserverService.monitorFileWithInotify(filePath);
            }

            if (ownerInfo2 != null) {
                String[] parts = ownerInfo2.split(" ");
                String owner = parts[0];
                String group = parts[1];

                // Change ownership of file1.txt to match
                String commandChangeOwner2 = "su -c chown " + owner + ":" + group + " " + filePath2;
                Process chownProcess2 = Runtime.getRuntime().exec(commandChangeOwner2);
                chownProcess2.waitFor();

                Log.d(TAG, "File created and ownership changed to: " + owner + ":" + group);

                FileObserverService.monitorFileWithInotify(filePath2);
            }

            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "Error creating file with su: " + filePath, e);
        }
    }

    private static void addFakeContentToLogFileWithSu(String filePath, String packageName) {
        try {
            // Determine context based on packageName
            String context;
            if (packageName.toLowerCase().contains("telegram")) {
                context = "telegram";
            } else if (packageName.toLowerCase().contains("mozilla")) {
                context = "mozilla";
            } else {
                context = "generic"; // Fallback if package name is neither
            }

            // Define context-specific actions and data
            String[] telegramActions = {"MESSAGE_SENT", "MESSAGE_RECEIVED", "USER_LOGGED_IN", "FILE_SHARED", "VOICE_CALL_STARTED", "GROUP_JOINED"};
            String[] mozillaActions = {"VISITED_URL", "DOWNLOADED_FILE", "ADDED_BOOKMARK", "CLEARED_CACHE", "INSTALLED_EXTENSION"};
            String[] genericActions = {"USER_LOGGED_IN", "SETTINGS_CHANGED", "FILE_ACCESSED"};
            String[] ipAddresses = {"192.168.1.10", "10.0.0.5", "172.16.0.8", "192.168.1.20"};
            String[] userIDs = {"user123", "analyst007", "admin", "guest"};
            String[] telegramDetails = {"Message content hidden", "Attachment: image.jpg", "Login from new device", "Group chat: Security Analysts"};
            String[] mozillaDetails = {"URL visited: https://example.com", "Downloaded: report.pdf", "Bookmark added: Security Resources", "Cache cleared"};
            String[] genericDetails = {"System preferences updated", "File accessed: settings.conf", "User session timed out"};

            String[] actions;
            String[] details;

            // Select actions and details based on context
            switch (context) {
                case "telegram":
                    actions = telegramActions;
                    details = telegramDetails;
                    break;
                case "mozilla":
                    actions = mozillaActions;
                    details = mozillaDetails;
                    break;
                default:
                    actions = genericActions;
                    details = genericDetails;
            }

            // Generate a random number of log entries (between 5 and 20)
            int numberOfEntries = 5 + (int) (Math.random() * 16);  // Range: 5 to 20

            StringBuilder logContent = new StringBuilder();
            // Generate and write fake log entries based on context
            for (int i = 0; i < numberOfEntries; i++) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                String action = actions[(int) (Math.random() * actions.length)];
                String ipAddress = ipAddresses[(int) (Math.random() * ipAddresses.length)];
                String userID = userIDs[(int) (Math.random() * userIDs.length)];
                String detail = details[(int) (Math.random() * details.length)];

                String logEntry = String.format("[%s] Action: %s, User: %s, IP: %s, Details: %s\n",
                        timestamp, action, userID, ipAddress, detail);

                logContent.append(logEntry);
            }
            FileEncryptor.writeFileWithSu(filePath, logContent.toString());

        } catch (Exception e) {
            Log.e("LogFile", "Error writing fake content to log file with su", e);
        }
    }
}