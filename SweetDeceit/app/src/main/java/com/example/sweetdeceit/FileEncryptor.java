package com.example.sweetdeceit;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class FileEncryptor {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    private static final int MODULUS = 1000000; // Define a range for the integer (e.g., 0 to 999,999)
    private static final String key = "9351499787323297e0ae5e197891689f";

    // Encrypt the file using AES-CTR mode with 'su' commands
    public static void encryptFile(String inputFile, String outputFile, SecretKey key) {
        //String tempOutputFile = outputFile + ".tmp";
        String filename = new File(outputFile).getName();
        String tempOutputFile;
        if (filename.contains(":")){
            String newFilename = filename.replaceAll(":","&&&!!");
            tempOutputFile = new File("/storage/emulated/0/Documents/",newFilename+ ".tmp").getAbsolutePath();
        }
        else {
        tempOutputFile = new File("/storage/emulated/0/Documents/",filename + ".tmp").getAbsolutePath();
        }

        Base64.Encoder base64Encoder = Base64.getEncoder();

        try {
            // Initialize cipher for AES-GCM
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Write IV as the first part of the output file (Base64 encoded)
            byte[] iv = cipher.getIV();
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempOutputFile)) {
                String ivBase64 = base64Encoder.encodeToString(iv) + "\n";
                fileOutputStream.write(ivBase64.getBytes(StandardCharsets.UTF_8));
            }

            // Read file in binary mode
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "dd if=" + "'" + inputFile + "' bs=1M status=none");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream actualInputStream = process.getInputStream();

            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            int bytesRead;

            try (FileOutputStream fileOutputStream = new FileOutputStream(tempOutputFile, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

                while ((bytesRead = actualInputStream.read(buffer)) != -1) {
                    byte[] inputData = Arrays.copyOf(buffer, bytesRead);
                    byte[] encryptedData = cipher.update(inputData);

                    if (encryptedData != null) {
                        // Encode the encrypted data to Base64
                        String encryptedBase64 = base64Encoder.encodeToString(encryptedData);

                        // Print Base64-encoded encrypted data to the console
                        System.out.println("Encrypted Data (Base64): " + encryptedBase64);

                        // Write the Base64-encoded encrypted data to the file (line by line)
                        bufferedWriter.write(encryptedBase64);
                        bufferedWriter.newLine(); // Ensure each chunk is on a new line
                    }
                }

                // Final encryption block
                byte[] finalEncryptedData = cipher.doFinal();
                if (finalEncryptedData != null) {
                    String finalEncryptedBase64 = base64Encoder.encodeToString(finalEncryptedData);
                    bufferedWriter.write(finalEncryptedBase64);
                    bufferedWriter.newLine();
                }

                bufferedWriter.flush(); // Ensure all data is written
            }

            process.waitFor();
            replaceFile(tempOutputFile, outputFile);
            System.out.println("Encryption completed: " + outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            deleteFile(tempOutputFile); // Cleanup temporary file
        }
    }

    private static void changeFileOwnerAndGroup(String filePath, String owner, String group) {
        try {
            // Use `chown` to change the owner and group of the file
            String command = "chown " + owner + ":" + group + " " + filePath;
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.waitFor();

            Log.e("File ownership changed: ", "Owner: " + owner + ", Group: " + group);
        } catch (Exception e) {
            Log.e("Error changing file owner and group: ", e.getMessage(), e);
        }
    }

    private static void writeToFile(String filePath, String data, boolean append) {
        try {
            // Use `su` to write data to the file
            String command = (append ? "echo -n " : "echo ") + "'" + data.replace("'", "'\\''") + "' >> " + filePath;
            if (!append) {
                command = "echo '" + data.replace("'", "'\\''") + "' > " + "'" + filePath + "'";
            }
            Log.e("IV Command: ", command);

            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            Log.e("Error writing to file with 'su': ", e.getMessage(), e);
        }
    }

    private static void replaceFile(String sourceFile, String targetFile) {
        try {
            // Use 'ls -l' to check the file's owner and group
            String command = "ls -l " + targetFile;
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.waitFor();

            // Read output of the command to find owner and group
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    // Extract the owner and group from the `ls -l` output
                    String[] parts = line.split("\\s+");
                    String owner = parts[2]; // Owner is the 3rd column
                    String group = parts[3]; // Group is the 4th column

                    Log.e("File Owner: ", owner);
                    Log.e("File Group: ", group);

                    ProcessBuilder processBuilderMV = new ProcessBuilder("su", "-c", "mv " + "'" + sourceFile + "' '" + targetFile + "'");
                    processBuilderMV.redirectErrorStream(true);

                    Process processMV = processBuilderMV.start();
                    processMV.waitFor();
                    changeFileOwnerAndGroup(targetFile, owner, group);

                }
            }
        } catch (Exception e) {
            Log.e("Error replacing file: ", e.getMessage(), e);
        }
    }

    private static void deleteFile(String filePath) {
        try {
            // Use `rm` command with `su` to delete the file
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "rm -f " + "'" + filePath + "'");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            Log.e("Error deleting file: ", e.getMessage(), e);
        }
    }

    // Decrypt the file using AES-CTR in streaming mode with 'su' commands
    public static void decryptFile(String inputFile, String outputFile, SecretKey key) {
        // Define the temporary output file path

        String filename = new File(outputFile).getName();
        String tempOutputFile;
        if (filename.contains(":")){
            String newFilename = filename.replaceAll(":","&&&!!");
            tempOutputFile = new File("/storage/emulated/0/Documents/",newFilename+ ".tmp").getAbsolutePath();
        }
        else {
            tempOutputFile = new File("/storage/emulated/0/Documents/",filename + ".tmp").getAbsolutePath();
        }
        Base64.Decoder base64Decoder = Base64.getDecoder();

        try {
            // Read IV (first line from the encrypted file)
            ProcessBuilder ivProcessBuilder = new ProcessBuilder("su", "-c", "head -n 1 '" + inputFile + "'");
            Process ivProcess = ivProcessBuilder.start();
            BufferedReader ivReader = new BufferedReader(new InputStreamReader(ivProcess.getInputStream()));
            String ivBase64 = ivReader.readLine();
            ivReader.close();
            ivProcess.waitFor();

            if (ivBase64 == null || ivBase64.isEmpty()) {
                throw new IOException("Missing IV in encrypted file");
            }

            byte[] iv = base64Decoder.decode(ivBase64);

            // Initialize cipher for AES-GCM decryption
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Read encrypted Base64 data (excluding IV line)
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "tail -n +2 '" + inputFile + "'");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream processInputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream));

            try (FileOutputStream fileOutputStream = new FileOutputStream(tempOutputFile, false)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    byte[] encryptedData = base64Decoder.decode(line);
                    byte[] decryptedData = cipher.update(encryptedData);
                    if (decryptedData != null) {
                        fileOutputStream.write(decryptedData);
                    }
                }

                // Final decryption block
                byte[] finalDecryptedData = cipher.doFinal();
                if (finalDecryptedData != null) {
                    fileOutputStream.write(finalDecryptedData);
                }
            }

            process.waitFor();
            replaceFile(tempOutputFile, outputFile);
            System.out.println("Decryption completed: " + outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            deleteFile(tempOutputFile);
        }
    }

    // Helper function to read a file's content with 'su'
    private static StringBuilder readFileWithSu(String filePath) {

        StringBuilder content = new StringBuilder();
        try {
            // Pass each part of the command as a separate argument
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "cat " + "'" + filePath + "'");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
                //Log.e("line in file ", line);
            }

            process.waitFor();
        } catch (Exception e) {
            Log.e("Error reading file with 'su': ", String.valueOf(e));
        }

        return content;
    }

    static void writeFileWithSu(String filePath, String content) {
        final int CHUNK_SIZE = 50000; // Define the size of each chunk (in characters)

        try {
            // Calculate the number of chunks needed
            int length = content.length();
            int offset = 0;

            // Write the first chunk to replace the current content in the file
            if (length > 0) {
                // Remove null characters and sanitize the chunk
                String firstChunk = content.substring(offset, Math.min(CHUNK_SIZE, length))
                        .replace("\0", "")
                        .replace("\"", "\\\"");  // Escape any quotes

                // Use ProcessBuilder to write the first chunk, replacing the current content
                ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "echo \"" + firstChunk + "\" > " + "'" + filePath + "'");
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                process.waitFor(); // Wait for the process to finish

                // Move the offset for the next chunk
                offset += firstChunk.length();
            }

            // Append the remaining chunks to the file
            while (offset < length) {
                // Determine the size of the next chunk
                int size = Math.min(CHUNK_SIZE, length - offset);
                // Sanitize each chunk by removing null characters and escaping quotes
                String chunk = content.substring(offset, offset + size)
                        .replace("\0", "")
                        .replace("\"", "\\\"");

                // Use ProcessBuilder to append the chunk to the file
                ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", "echo \"" + chunk + "\" >> " + "'" + filePath + "'");
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                process.waitFor(); // Wait for the process to finish

                // Move the offset for the next chunk
                offset += size;
            }
        } catch (Exception e) {
            Log.e("Error writing file with 'su': ", String.valueOf(e));
        }
    }

    public static String encryptText(String plainText, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV(); // IV generated during encryption
            byte[] encryptedData = cipher.doFinal(plainText.getBytes());

            // Base64 encode IV and encrypted data for storage
            String base64IV = Base64.getEncoder().encodeToString(iv);
            String base64EncryptedData = Base64.getEncoder().encodeToString(encryptedData);

            // Combine IV and encrypted data for this entry
            Log.i("Encrypted text is: ", base64IV + ":" + base64EncryptedData);
            return base64IV + ":" + base64EncryptedData;
        } catch (Exception e){
            Log.e("Error encrypting text 'su': ", String.valueOf(e));
        }
        return plainText;
    }

    public static String decryptText(String encryptedText, SecretKey key) {
        try {
            // Split the content into IV and encrypted data
            String[] parts = encryptedText.split(":", 2);

            //System.out.println(Arrays.toString(parts));

            if (parts.length < 2) {
                Log.i("decryptText", "Invalid encrypted text format, skipping text");
                //throw new IllegalArgumentException("Invalid encrypted text format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedData = Base64.getDecoder().decode(parts[1]);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, "UTF-8");

        } catch (Exception e) {
            System.err.println("Error decrypting database entry: " + e.getMessage());
            return null;
        }
    }

    private static int deriveOffset() throws Exception {
        // Use a hash function to derive an integer offset from the key
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(key.getBytes());

        // Use the first 4 bytes of the hash to get a consistent integer offset
        return Math.abs(ByteBuffer.wrap(hash).getInt()) % MODULUS;
    }

    public static int encryptInteger(int plainNumber) throws Exception {
        int offset = deriveOffset();
        return (plainNumber + offset) % MODULUS;
    }

    public static int decryptInteger(int encryptedNumber) throws Exception {
        int offset = deriveOffset();
        return (encryptedNumber - offset + MODULUS) % MODULUS;
    }
}
