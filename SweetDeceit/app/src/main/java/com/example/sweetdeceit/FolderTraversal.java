package com.example.sweetdeceit;

import static com.example.sweetdeceit.ApkInstallReceiver.TRACKED_PACKAGES;
import static com.example.sweetdeceit.FileObserverService.key;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.SecretKey;

public class FolderTraversal {
    private static final String TAG = "FolderTraversalEncrypt";

    public static void encryptPackageData() {
        for (String packageName : TRACKED_PACKAGES) {
            // Build folder path for each package
            String folderPath = "/storage/emulated/0/Android/data/" + packageName + "/files/";
            String folderPath2 = "/data/data/" + packageName + "/files/";

            // Stop Application and Disable it to prevent potential changes
            try {
                String forceStopCommand = "su -c am force-stop " + packageName;
                Process forceStop = Runtime.getRuntime().exec(forceStopCommand);
                forceStop.waitFor();

                String disableCommand = "su -c pm disable " + packageName;
                Process disable = Runtime.getRuntime().exec(disableCommand);
                disable.waitFor();
            } catch (Exception e){
                System.out.println("Error Running either force-stop or disable");
            }

            // Encrypt contents of each folder
            encryptFolderContentsWithSu(folderPath);
            encryptFolderContentsWithSu(folderPath2);
        }
    }

    public static void decryptPackageData() {
        for (String packageName : TRACKED_PACKAGES) {
            // Build folder path for each package
            String folderPath = "/storage/emulated/0/Android/data/" + packageName + "/files/";
            String folderPath2 = "/data/data/" + packageName + "/files/";

            // Decrypt contents of each folder
            decryptFolderContentsWithSu(folderPath);
            decryptFolderContentsWithSu(folderPath2);

            try {
                String enableCommand = "su -c pm enable " + packageName;
                Process enable = Runtime.getRuntime().exec(enableCommand);
                enable.waitFor();
            } catch (Exception e){
                System.out.println("Error running pm enable");
            }
        }
    }

    public static void encryptFolderContentsWithSu(String folderPath) {
        try {
            // Use `su` to list all items in the directory
            String listCommand = "su -c ls '" + folderPath + "'";
            Process listProcess = Runtime.getRuntime().exec(listCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(listProcess.getInputStream()));
            String itemName;


            while ((itemName = reader.readLine()) != null) {
                if (itemName.equals(".") || itemName.equals("..")) {
                    continue; // Skip current and parent directory entries
                }

                if (itemName.contains("action_history_") || itemName.equals(".drug_sample5324_1243.jpg") || itemName.equals(".drug_delivery_schedule_applewood_019385.txt")
                        || itemName.equals(".drug_shipping_schedule_mapletree_1914.txt") || itemName.equals("action.config") || itemName.equals(".drug_delivery_instructions_19385.mp3") || itemName.contains("ign0re")) {
                    Log.e(TAG, "Skipping encryption for Honey Token: " + itemName);
                    continue;
                }

                String itemPath = folderPath + "/" + itemName;

                if (isDirectoryWithSu(itemPath)) {
                    // If the item is a directory, recursively encrypt its contents
                    encryptFolderContentsWithSu(itemPath);
                    //Log.e(TAG, "FIlepath: " + itemPath);
                } else {
                    // If the item is a file, encrypt it
                    FileEncryptor.encryptFile(itemPath,itemPath,key);
                }
            }

            reader.close();
            listProcess.waitFor();

        } catch (Exception e) {
            Log.e(TAG, "Error accessing directory with su: " + folderPath, e);
        }
    }

    public static void decryptFolderContentsWithSu(String folderPath) {
        try {
            // Use `su` to list all items in the directory
            String listCommand = "su -c ls '" + folderPath + "'";
            Process listProcess = Runtime.getRuntime().exec(listCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(listProcess.getInputStream()));
            //Log.e(TAG, "reader: " + reader);
            String itemName;

            while ((itemName = reader.readLine()) != null) {
                if (itemName.equals(".") || itemName.equals("..")) {
                    continue; // Skip current and parent directory entries
                }

                if (itemName.contains("action_history_") || itemName.equals(".drug_sample5324_1243.jpg") || itemName.equals(".drug_delivery_schedule_applewood_019385.txt")
                        || itemName.equals(".drug_shipping_schedule_mapletree_1914.txt") || itemName.equals("action.config") || itemName.equals(".drug_delivery_instructions_19385.mp3")
                        || itemName.equals("3c71b4071e5eb70a168884daf40fd75680cb6a57bcfb3f335dfdf60ed6ba40e4") || itemName.contains("ign0re")) {
                    Log.e(TAG, "Skipping decryption for Honey Token: " + itemName);
                    continue;
                }

                String itemPath = folderPath + "/" + itemName;

                if (isDirectoryWithSu(itemPath)) {
                    // If the item is a directory, recursively decrypt its contents
                    decryptFolderContentsWithSu(itemPath);
                    //Log.e(TAG, "FIlepath: " + itemPath);
                } else {
                    // If the item is a file, decrypt it
                    FileEncryptor.decryptFile(itemPath, itemPath, key);
                }
            }

            reader.close();
            listProcess.waitFor();

        } catch (Exception e) {
            Log.e(TAG, "Error accessing directory with su: " + folderPath, e);
        }
    }


    private static boolean isDirectoryWithSu(String path) {
        try {
            String command = "su -c stat -c %F '" + path + "'";
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();

            return "directory".equals(result);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if path is a directory: " + path, e);
            return false;
        }
    }

    public static void encryptDefined(){
        // Contacts2.db encryption
        encryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "data", // tableName
                new String[]{"data1", "data2", "data4"}, // selectColumns - columns to retrieve
                new String[]{"data1", "data2", "data4"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        encryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "phone_lookup", // tableName
                new String[]{"normalized_number"}, // selectColumns - columns to retrieve
                new String[]{"normalized_number"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        encryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "raw_contacts", // tableName
                new String[]{"display_name", "display_name_alt", "sort_key", "sort_key_alt"}, // selectColumns - columns to retrieve
                new String[]{"display_name", "display_name_alt", "sort_key", "sort_key_alt"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        encryptDatabase("/data/data/com.android.providers.contacts/databases/calllog.db",
                "calls",
                new String[]{"number", "date", "duration", "type", "subscription_component_name", "subscription_id", "name",
                        "countryiso", "geocoded_location", "normalized_number", "last_modified"},
                new String[]{"number", "date", "duration", "type", "subscription_component_name", "subscription_id", "name",
                        "countryiso", "geocoded_location", "normalized_number", "last_modified"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.contacts/databases/calllog.db",
                "calls",
                new String[]{"features", "source_package", "transcription", "state", "deleted", "backed_up", "restored", "archived"},
                new String[]{"features", "source_package", "transcription", "state", "deleted", "backed_up", "restored", "archived"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "sms",
                new String[]{"address", "date", "date_sent", "body", "service_center", "creator", "type"},
                new String[]{"address", "date", "date_sent", "body", "service_center", "creator", "type"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "canonical_addresses",
                new String[]{"address"},
                new String[]{"address"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "threads",
                new String[]{"date", "snippet"},
                new String[]{"date", "snippet"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "threads",
                new String[]{"date", "snippet"},
                new String[]{"date", "snippet"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "words",
                new String[]{"index_text"},
                new String[]{"index_text"},
                key
        );

        encryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "words_content",
                new String[]{"c1index_text"},
                new String[]{"c1index_text"},
                key
        );

        encryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        encryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath());
        encryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        encryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS).getAbsolutePath());
    }

    public static void decryptDefined(){
        decryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "data", // tableName
                new String[]{"data1", "data2", "data4"}, // selectColumns - columns to retrieve
                new String[]{"data1", "data2", "data4"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        decryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "phone_lookup", // tableName
                new String[]{"normalized_number"}, // selectColumns - columns to retrieve
                new String[]{"normalized_number"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        decryptDatabase("/data/data/com.android.providers.contacts/databases/contacts2.db", // dbPath
                "raw_contacts", // tableName
                new String[]{"display_name", "display_name_alt", "sort_key", "sort_key_alt"}, // selectColumns - columns to retrieve
                new String[]{"display_name", "display_name_alt", "sort_key", "sort_key_alt"}, // encryptColumns - columns to encrypt
                key // encryption key
        );

        decryptDatabase("/data/data/com.android.providers.contacts/databases/calllog.db",
                "calls",
                new String[]{"number", "date", "duration", "type", "subscription_component_name", "subscription_id", "name",
                        "countryiso", "geocoded_location", "normalized_number", "last_modified"},
                new String[]{"number", "date", "duration", "type", "subscription_component_name", "subscription_id", "name",
                        "countryiso", "geocoded_location", "normalized_number", "last_modified"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.contacts/databases/calllog.db",
                "calls",
                new String[]{"features", "source_package", "transcription", "state", "deleted", "backed_up", "restored", "archived"},
                new String[]{"features", "source_package", "transcription", "state", "deleted", "backed_up", "restored", "archived"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "sms",
                new String[]{"address", "date", "date_sent", "body", "service_center", "creator", "type"},
                new String[]{"address", "date", "date_sent", "body", "service_center", "creator", "type"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "canonical_addresses",
                new String[]{"address"},
                new String[]{"address"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "threads",
                new String[]{"date", "snippet"},
                new String[]{"date", "snippet"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "threads",
                new String[]{"date", "snippet"},
                new String[]{"date", "snippet"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "words",
                new String[]{"index_text"},
                new String[]{"index_text"},
                key
        );

        decryptDatabase("/data/data/com.android.providers.telephony/databases/mmssms.db",
                "words_content",
                new String[]{"c1index_text"},
                new String[]{"c1index_text"},
                key
        );

        decryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        decryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath());
        decryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        decryptFolderContentsWithSu(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS).getAbsolutePath());
    }

    public static void encryptDatabase(String dbPath, String tableName, String[] selectColumns, String[] encryptColumns, SecretKey key) {
        System.out.println("Entered encryptDatabase");
        try {
            // Check for null selectColumns or encryptColumns
            if (selectColumns == null || encryptColumns == null) {
                System.out.println("ERROR: selectColumns or encryptColumns array is null");
                return;
            }

            // Construct the SELECT statement dynamically
            String selectCols = String.join(", ", selectColumns);

            String selectQuery;

            if (Objects.equals(tableName, "phone_lookup")) {
                selectQuery = "SELECT data_id, " + selectCols + " FROM " + tableName + ";";
            } else if (Objects.equals(tableName, "words_content")) {
                selectQuery = "SELECT docid, " + selectCols + " FROM " + tableName + ";";
            } else{
                selectQuery = "SELECT _id, " + selectCols + " FROM " + tableName + ";";
            }

            String[] selectCommand = {
                    "su",
                    "-c",
                    "sqlite3 " + dbPath + " '" + selectQuery + "'"
            };
            System.out.println(Arrays.toString(selectCommand));

            Process process = Runtime.getRuntime().exec(selectCommand);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Log any errors from the SELECT command
            String line2;
            while ((line2 = stdError.readLine()) != null) {
                System.out.println("ERROR: " + line2);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                String[] columns = line != null ? line.split("\\|") : new String[0];
                if (columns.length == 0) continue; // Skip if columns array is empty

                String id = columns[0];

                // Prepare encrypted data array for each column
                String[] encryptedData = new String[encryptColumns.length];

                for (int i = 0; i < encryptColumns.length; i++) {
                    String data = (i + 1 < columns.length) ? columns[i + 1] : null;

                    if (data != null && !data.isEmpty()) {
                        boolean isIntegerColumn = encryptColumns[i] != null && isIntegerColumn(encryptColumns[i]);

                        encryptedData[i] = FileEncryptor.encryptText(data, key);

                    } else {
                        encryptedData[i] = null;
                    }
                }

                StringBuilder updateBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
                boolean hasDataToUpdate = false;

                for (int i = 0; i < encryptColumns.length; i++) {
                    if (encryptedData[i] != null) {
                        if (hasDataToUpdate) {
                            updateBuilder.append(", ");
                        }
                        updateBuilder.append(encryptColumns[i])
                                .append("=\"").append(encryptedData[i]).append("\"");
                        hasDataToUpdate = true;
                    }
                }

                if (Objects.equals(tableName, "phone_lookup")) {
                    updateBuilder.append(" WHERE data_id=").append(id).append(";");
                } else if (Objects.equals(tableName, "words_content")) {
                    updateBuilder.append(" WHERE docid=").append(id).append(";");
                } else{
                    updateBuilder.append(" WHERE _id=").append(id).append(";");
                }

                String[] updateCommand = {
                        "su",
                        "-c",
                        "sqlite3 " + dbPath + " '" + updateBuilder + "'"
                };
                Process process2 = Runtime.getRuntime().exec(updateCommand);
                process2.waitFor();

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process2.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.out.println("ERROR: " + errorLine);
                }
                errorReader.close();
            }

            reader.close();
            stdError.close();
        } catch (Exception e) {
            Log.e("DB_ENCRYPTION_ERROR", "DB encryption failure", e);
        }
    }


    // Utility method to determine if a column is an integer column
    private static boolean isIntegerColumn(String columnName) {
        return columnName.toLowerCase().endsWith("id");
    }


    public static void decryptDatabase(String dbPath, String tableName, String[] selectColumns, String[] decryptColumns, SecretKey key) {
        System.out.println("Entered decryptDatabase");
        try {
            // Check for null selectColumns or decryptColumns
            if (selectColumns == null || decryptColumns == null) {
                System.out.println("ERROR: selectColumns or decryptColumns array is null");
                return;
            }

            // Construct the SELECT statement dynamically
            String selectCols = String.join(", ", selectColumns);

            String selectQuery;
            if (Objects.equals(tableName, "phone_lookup")) {
                selectQuery = "SELECT data_id, " + selectCols + " FROM " + tableName + ";";
            } else if (Objects.equals(tableName, "words_content")) {
                selectQuery = "SELECT docid, " + selectCols + " FROM " + tableName + ";";
            } else{
                selectQuery = "SELECT _id, " + selectCols + " FROM " + tableName + ";";
            }

            String[] selectCommand = {
                    "su",
                    "-c",
                    "sqlite3 " + dbPath + " '" + selectQuery + "'"
            };
            System.out.println(Arrays.toString(selectCommand));

            Process process = Runtime.getRuntime().exec(selectCommand);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Log any errors from executing the SELECT command
            String line2;
            while ((line2 = stdError.readLine()) != null) {
                System.out.println("ERROR: " + line2);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Processing line: " + line);
                String[] columns = line.split("\\|");
                if (columns.length == 0) continue; // Skip if columns array is empty

                String id = columns[0];

                // Prepare decrypted data array for each column
                String[] decryptedData = new String[decryptColumns.length];

                for (int i = 0; i < decryptColumns.length; i++) {
                    // Check if the column exists in the array to avoid ArrayIndexOutOfBoundsException
                    String encryptedData = (i + 1 < columns.length) ? columns[i + 1] : null;

                    if (encryptedData != null && !encryptedData.isEmpty()) {
                        boolean isIntegerColumn = decryptColumns[i] != null && isIntegerColumn(decryptColumns[i]);

                        decryptedData[i] = FileEncryptor.decryptText(encryptedData, key);

                    } else {
                        decryptedData[i] = null;
                    }
                }

                // Build the UPDATE command dynamically only if there is data to update
                StringBuilder updateBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
                boolean hasDataToUpdate = false;

                for (int i = 0; i < decryptColumns.length; i++) {
                    if (decryptedData[i] != null) {
                        if (hasDataToUpdate) {
                            updateBuilder.append(", ");
                        }
                        // Enclose each decryptedData value in double quotes for SQL syntax
                        updateBuilder.append(decryptColumns[i])
                                .append("=\"").append(decryptedData[i]).append("\"");
                        hasDataToUpdate = true;
                    }
                }
                if (hasDataToUpdate) { // Only proceed if there's data to update
                    if (Objects.equals(tableName, "phone_lookup")) {
                        updateBuilder.append(" WHERE data_id=").append(id).append(";");
                    } else if (Objects.equals(tableName, "words_content")) {
                        updateBuilder.append(" WHERE docid=").append(id).append(";");
                    } else{
                        updateBuilder.append(" WHERE _id=").append(id).append(";");
                    }

                    String[] updateCommand = {
                            "su",
                            "-c",
                            "sqlite3 " + dbPath + " '" + updateBuilder.toString() + "'"
                    };
                    Process process2 = Runtime.getRuntime().exec(updateCommand);
                    process2.waitFor();

                    // Capture any errors during the update execution for debugging
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process2.getErrorStream()));
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        System.out.println("ERROR: " + errorLine);
                    }
                    errorReader.close();
                }
            }

            // Close resources
            reader.close();
            stdError.close();
        } catch (Exception e) {
            Log.e("DBDecryptor", "DB decryption failure: " + e.getMessage());
        }
    }
}
