package com.example.sweetdeceit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class DefinedHoneyTokenUtils {

    private static final String TAG = "DefinedHoneyTokenUtils";

    public static void imageInPicture(Context context, String fileName, int drawableImageId) {
        // Define the path to the Pictures directory
        String picturesDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        File destFile = new File(picturesDirPath, fileName);

        // Check if the file already exists
        if (destFile.exists()) {
            Log.i(TAG, "File already exists: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
            return;
        }

        try {
            // Ensure the Pictures directory exists
            File picturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots");
            if (!picturesDir.exists() && !picturesDir.mkdirs()) {
                Log.e("TAG", "Failed to create Pictures/Screenshots directory.");
                return;
            }

            // Open the image from resources
            InputStream inputStream = context.getResources().openRawResource(drawableImageId);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Write the image to the Screenshots directory
            FileOutputStream outputStream = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.i("TAG", "Image successfully copied to: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("TAG", "Error copying image to Pictures directory.", e);
        }
    }


    public static void txtInDocumentOrDownload(Context context, String fileName, boolean docOrDown) {
        File destFile;
        File documentsDir;
        // Path to the Documents directory
        if (docOrDown) {
            documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        } else{
            documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        destFile = new File(documentsDir, fileName);

        // Check if the file already exists
        if (destFile.exists()) {
            Log.i(TAG, "File already exists: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
            return;
        }

        // Ensure the Documents directory exists
        if (!documentsDir.exists() && !documentsDir.mkdirs()) {
            Log.e(TAG, "Failed to create Documents directory.");
            return;
        }

        // Generate fake delivery schedules
        StringBuilder content = generateFakeDeliverySchedules();

        // Write the content to the file
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            outputStream.write(content.toString().getBytes());
            outputStream.flush();
            Log.i(TAG, "Delivery schedule file created at: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing delivery schedule to Documents directory.", e);
        }

    }

    private static StringBuilder generateFakeDeliverySchedules() {
        StringBuilder content = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Random random = new Random();

        // Define locations
        String[] locations = {
                "Los Angeles Distribution Center",
                "New York Warehouse",
                "Chicago Storage Facility",
                "San Francisco Bay Area Hub",
                "Miami Shipping Dock",
                "Dallas Fulfillment Center",
                "Seattle Drop-off Point",
                "Atlanta Main Office",
                "Denver Logistics Station",
                "Boston Supply Depot"
        };

        int numberOfEntries = 5 + random.nextInt(30); // Random number of entries between 5 and 20

        for (int i = 0; i < numberOfEntries; i++) {
            // Generate a random date, some in the past, some in the future
            Calendar calendar = Calendar.getInstance();
            int daysOffset = random.nextInt(180) - 90; // Random offset between -90 and +90 days
            calendar.add(Calendar.DAY_OF_YEAR, daysOffset);
            Date date = calendar.getTime();
            String formattedDate = dateFormat.format(date);

            // Generate random delivery details
            String[] items = {"Cocaine", "Weed", "Codeine", "Ecstasy", "Heroine", "White Magic", "Ketamine"};
            String item = items[random.nextInt(items.length)];
            int quantity = 1 + random.nextInt(50); // Random quantity between 1 and 50
            String location = locations[random.nextInt(locations.length)]; // Random location

            // Build the delivery entry
            String deliveryEntry = String.format("Delivery Date: %s\nItem: %s\nQuantity: %d\nLocation: %s\n\n",
                    formattedDate, item, quantity, location);
            content.append(deliveryEntry);
        }

        return content;
    }

    private static void monitor(String filepath){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Unable to sleep before defined honeytokens");
        }
        FileObserverService.monitorFileWithInotify(filepath);
    }

    public static void copyMp4ToRecordings(Context context, String fileName, int drawableResourceId) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // Define the recordings directory and output file path
            File recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS);
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs(); // Create the directory if it doesn't exist
            }

            // Define the output file in the recordings directory
            File outputFile = new File(recordingsDir, fileName);

            // Check if the file already exists
            if (outputFile.exists()) {
                System.out.println("File already exists: " + outputFile.getAbsolutePath());
                monitor(outputFile.getAbsolutePath()); // Monitor the existing file
                return;
            }

            // Open the file from drawable resources
            inputStream = context.getResources().openRawResource(drawableResourceId);
            outputStream = new FileOutputStream(outputFile);

            // Copy the file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            System.out.println("File copied to " + outputFile.getAbsolutePath());

            // Start monitoring the newly copied file
            monitor(outputFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the streams
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

     static void createFileHTInDirectory(String packageName) throws InterruptedException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "history_" + timeStamp + ".log";
        String filePath = "/data/data/" + packageName + "/" + fileName;
        Log.d(TAG, filePath);

        // Command to check if the file exists
        String commandCheckFileExists = "su -c 'test -f " + filePath + " && echo exists || echo not_exists'";

        try {
            // Check if the file already exists
            Process checkProcess = Runtime.getRuntime().exec(commandCheckFileExists);
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String fileExists = reader.readLine();
            checkProcess.waitFor();

            if ("exists".equals(fileExists)) {
                // If file exists, monitor it directly
                Log.d(TAG, "File already exists: " + filePath);
                monitor(filePath);
                return;
            }

            // Command to create the file
            String commandCreateFile = "su -c touch " + filePath;

            // Create the file as it does not exist
            Process createProcess = Runtime.getRuntime().exec(commandCreateFile);
            createProcess.waitFor();

            // Add fake content to the file
            addFakeContentToLogFileWithSu(filePath, packageName);

            // Get the owner of the newly created file
            String commandGetOwner = "su -c ls -l /data/data/" + packageName + "/ | awk 'NR==2 {print $3, $4}'";
            Process ownerProcess = Runtime.getRuntime().exec(commandGetOwner);

            // Read the output to get owner and group
            BufferedReader ownerReader = new BufferedReader(new InputStreamReader(ownerProcess.getInputStream()));
            String ownerInfo = ownerReader.readLine();

            if (ownerInfo != null) {
                String[] parts = ownerInfo.split(" ");
                String owner = parts[0];
                String group = parts[1];

                // Change ownership of the file to match the package owner and group
                String commandChangeOwner = "su -c chown " + owner + ":" + group + " " + filePath;
                Process chownProcess = Runtime.getRuntime().exec(commandChangeOwner);
                chownProcess.waitFor();

                Log.d(TAG, "File created and ownership changed to: " + owner + ":" + group);
            }

            // Start monitoring the file
            monitor(filePath);

            // Close readers
            reader.close();
            ownerReader.close();
        } catch (Exception e) {
            Log.e(TAG, "Error creating file with su: " + filePath, e);
        }
    }


    private static void addFakeContentToLogFileWithSu(String filePath, String packageName) {
        StringBuilder fakeContent = new StringBuilder();

        // Generate fake content based on the package name
        if ("com.android.provider.contacts".equals(packageName)) {
            fakeContent.append("Contact Sync Log:\n");
            int syncCount = 20 + (int) (Math.random() * 80);  // Randomize the number of contacts synced (between 20 and 100)

            for (int i = 0; i < syncCount; i++) {
                fakeContent.append("Contact ID: ").append((int) (Math.random() * 10000)).append(" synced.\n")
                        .append("Status: ").append(Math.random() > 0.1 ? "SUCCESS" : "FAILED").append("\n");  // Random success/fail status
                if (Math.random() > 0.8) {
                    fakeContent.append("Retry count: ").append((int) (Math.random() * 3)).append("\n");  // Occasionally add retry info
                }
            }
            fakeContent.append("Total contacts synced: ").append(syncCount).append("\n");
        } else if ("com.android.provider.telephony".equals(packageName)) {
            fakeContent.append("SMS Log:\n");
            int messageCount = 5 + (int) (Math.random() * 15);  // Randomize the number of messages (between 5 and 20)

            for (int i = 0; i < messageCount; i++) {
                fakeContent.append("Message ID: ").append((int) (Math.random() * 100000)).append("\n")
                        .append("Recipient: +1").append((int) (1000000000 + Math.random() * 900000000)).append("\n")
                        .append("Sent at: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n")
                        .append("Status: ").append(Math.random() > 0.1 ? "Delivered" : "Failed").append("\n\n");  // Random success/fail status
            }
            fakeContent.append("Total messages processed: ").append(messageCount).append("\n");
        } else {
            // Default log entry with varying lengths
            int entryCount = 3 + (int) (Math.random() * 10);  // Randomize number of entries (between 3 and 13)

            for (int i = 0; i < entryCount; i++) {
                fakeContent.append("Operation ID: ").append((int) (Math.random() * 100000)).append("\n")
                        .append("Performed at: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n")
                        .append("Status: SUCCESS\n\n");
            }
        }

        // Command to append the generated fake content to the specified file
        String commandAddContent = "su -c 'echo \"" + fakeContent.toString() + "\" >> " + filePath + "'";

        try {
            // Write the fake content to the file
            Process addContentProcess = Runtime.getRuntime().exec(commandAddContent);
            addContentProcess.waitFor();
            Log.d(TAG, "Fake content added to " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error adding fake content with su to file: " + filePath, e);
        }
    }

    public static void txtInExternalRoot(Context context, String fileName) {
        File destFile;
        File documentsDir;
        // Path to the root external directory
        documentsDir = Environment.getExternalStorageDirectory();
        destFile = new File(documentsDir, fileName);

        // Check if the file already exists
        if (destFile.exists()) {
            Log.i(TAG, "File already exists: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
            return;
        }

        // Ensure the Documents directory exists
        if (!documentsDir.exists() && !documentsDir.mkdirs()) {
            Log.e(TAG, "Failed to create Documents directory.");
            return;
        }

        // Generate fake delivery schedules
        StringBuilder content = generateFakeDeliverySchedules();

        // Write the content to the file
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            outputStream.write(content.toString().getBytes());
            outputStream.flush();
            Log.i(TAG, "Delivery schedule file created at: " + destFile.getAbsolutePath());
            monitor(destFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing delivery schedule to root directory.", e);
        }

    }

    public static void createSymlinkToUrandom(Context context, String fileName) {
        // Get the path to the external storage file
        String externalFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
        String urandomPath = "/dev/urandom";

        // Construct the symlink command
        String command = "ln -s " + urandomPath + " " + externalFilePath;

        // Execute the command with root privileges
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Symlink created successfully.");
            } else {
                System.err.println("Failed to create symlink. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
