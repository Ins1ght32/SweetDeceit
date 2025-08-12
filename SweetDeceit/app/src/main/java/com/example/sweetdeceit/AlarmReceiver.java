package com.example.sweetdeceit;

import static com.example.sweetdeceit.FileObserverService.processMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "SweetDeceit";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm received. Restarting file monitoring.");

        // Restart the file monitoring processes if needed
        FileObserverService activity = new FileObserverService();
        
        // Main code (Restart code for predefined HTs)
        for (String filePath : processMap.keySet()) {
            activity.monitorFileWithInotifyIfNotRunning(filePath);
        }
    }
}
