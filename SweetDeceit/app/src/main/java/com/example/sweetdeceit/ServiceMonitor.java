package com.example.sweetdeceit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceMonitor {
    private static ExecutorService executorService = null;
    private static volatile boolean keepMonitoring = true;

    public ServiceMonitor() {
        // Create a single-threaded executor for monitoring
        executorService = Executors.newSingleThreadExecutor();
    }

    public void startMonitoring() {
        executorService.submit(() -> {
            while (keepMonitoring) {
                try {
                    // Execute 'ps' to get the list of running processes
                    Process process = Runtime.getRuntime().exec("su -c ps");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("com.android.backupconfirm")) {
                            // Extract PID
                            String[] tokens = line.split("\\s+");
                            if (tokens.length > 1) {
                                String pid = tokens[1]; // Assuming PID is in the second column

                                // Kill the process
                                Runtime.getRuntime().exec("su -c kill -9 " + pid);
                                System.out.println("Terminated service: com.android.backupconfirm");
                            }
                        }
                        if (line.contains("com.client.appA")) {
                            // Extract PID
                            String[] tokens = line.split("\\s+");
                            if (tokens.length > 1) {
                                String pid = tokens[1]; // Assuming PID is in the second column

                                // Kill the process
                                Runtime.getRuntime().exec("su -c kill -9 " + pid);
                                System.out.println("Terminated service: com.client.appA");
                            }
                        }
                    }
                    reader.close();
                    process.waitFor();

                    // Add a short delay before checking again
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void stopMonitoring() {
        keepMonitoring = false;
        executorService.shutdownNow(); // Gracefully stop the executor
    }
}
