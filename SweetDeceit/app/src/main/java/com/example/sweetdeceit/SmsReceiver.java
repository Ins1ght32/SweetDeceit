package com.example.sweetdeceit;

import static com.example.sweetdeceit.FileObserverService.actionFile;
import static com.example.sweetdeceit.FileObserverService.statusFile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.crypto.SecretKey;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSThenTrigger";
    private static final String SPECIFIC_STRING = "YOUR_SPECIFIC_STRING"; // Replace with your string
    private static final String SPECIFIC_STRING2 = "Set Action 1"; // Replace with your string
    private static final String SPECIFIC_STRING3 = "Set Action 0"; // Replace with your string

    private static SecretKey key;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve the SMS message passed in
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";

        try {
            key = KeyStoreHelper.getKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (bundle != null) {
            // Retrieve the SMS received
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                str += "SMS from " + msgs[i].getOriginatingAddress();
                str += " : ";
                str += msgs[i].getMessageBody();
                str += "\n";

                // Check if the message contains the specific string
                if (msgs[i].getMessageBody().contains(SPECIFIC_STRING)) {
                    Log.d(TAG, "Specific string received: " + msgs[i].getMessageBody());
                    String statusContent = "";

                    if (statusFile.exists()) {
                        try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
                            statusContent = br.readLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    try {
                        if ("1".equals(statusContent)){
                            //Decrypt high value dynamic directories
                            new Thread(FolderTraversal::decryptPackageData).start();

                            //Decryption for new areas
                            new Thread(FolderTraversal::decryptDefined).start();

                            //Set Status file back to 0 after decryption of data de to Honey Token Trigger
                            Process processWrite = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo 0 > " + statusFile.getAbsolutePath()});
                            processWrite.waitFor();
                            Log.d(TAG, "Status file updated to 0 with root privileges");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                if (msgs[i].getMessageBody().contains(SPECIFIC_STRING2) || msgs[i].getMessageBody().contains(SPECIFIC_STRING3)) {
                    try {
                        // Check if the action file exists
                        if (actionFile.exists()) {
                            // Write new content to the action file
                            try (FileWriter writer = new FileWriter(actionFile, false)) { // `false` to overwrite existing content
                                if (msgs[i].getMessageBody().contains(SPECIFIC_STRING2)) {
                                    writer.write("1"); // Replace with the content you want to write
                                } else if (msgs[i].getMessageBody().contains(SPECIFIC_STRING3)) {
                                    writer.write("0");
                                }
                                Log.d(TAG, "Action file updated successfully.");
                            }
                        } else {
                            // Consider recalling the creation of cnfig file here.
                            Log.d(TAG, "Action file does not exist.");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error updating action file", e);
                    }
                }

            }
            // Optionally, log the complete SMS message
            Log.d(TAG, "SMS Received: " + str);
        }
    }
}
