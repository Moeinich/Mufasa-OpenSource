package helpers.adb;

import helpers.Logger;
import helpers.ThreadManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.Constants.OS_WIN;
import static utils.Constants.PLATFORM_TOOLS;
import static utils.SystemUtils.getOperatingSystem;
import static utils.SystemUtils.getSystemPath;

public class ADBHandler {
    private final AtomicBoolean isADBChecked = new AtomicBoolean(false);
    private volatile boolean isADBInstalled = false;
    private final Logger logger;
    private Process adbServerProcess;

    // ExecutorService to handle concurrent ADB commands
    private final ThreadManager executorService = ThreadManager.getInstance();
    private final ExecutorService deviceGetter = ThreadManager.getInstance().getUnifiedExecutor();

    /**
     * Constructs an AdbHandler with the provided Logger.
     * Starts the ADB server upon initialization.
     *
     * @param logger The logger for logging messages.
     */
    public ADBHandler(Logger logger) {
        this.logger = logger;
        startADBServer();
    }

    /**
     * Checks if ADB is installed at the specified path.
     * This check is performed only once for efficiency.
     *
     * @param adbPath The path to the ADB executable.
     * @return true if ADB exists and is a regular file, false otherwise.
     */
    public boolean isADBInstalledAtPath(Path adbPath) {
        if (adbPath == null) {
            logger.err("ADB path is null.");
            return false;
        }

        // If already checked, return the cached result
        if (isADBChecked.get()) {
            return isADBInstalled;
        }

        // Attempt to set isADBChecked to true to perform the check
        if (isADBChecked.compareAndSet(false, true)) {
            // Perform the check
            isADBInstalled = Files.exists(adbPath) && Files.isRegularFile(adbPath) && Files.isExecutable(adbPath);
            if (isADBInstalled) {
                logger.print("ADB is installed at: " + adbPath.toAbsolutePath());
            } else {
                logger.err("ADB is not installed at the specified path: " + adbPath.toAbsolutePath());
            }
        }

        return isADBInstalled;
    }

    /**
     * Starts the ADB server if it's not already running.
     */
    public synchronized void startADBServer() {
        if (adbServerProcess == null || !adbServerProcess.isAlive()) {
            try {
                List<String> command = List.of(getADBPath().toString(), "start-server");
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true); // Merge stderr with stdout

                adbServerProcess = processBuilder.start();
                handleErrorStreamAsync(adbServerProcess);

                // Wait for the ADB server to start
                if (adbServerProcess.waitFor(10, TimeUnit.SECONDS)) {
                    logger.print("ADB server started successfully.");
                } else {
                    logger.print("ADB server started, but it is still running.");
                }
            } catch (IOException | InterruptedException e) {
                logger.print("Failed to start ADB server: " + e.getMessage());
                e.printStackTrace();
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        } else {
            logger.print("ADB server is already running.");
        }
    }

    /**
     * Stops the ADB server if it's running.
     */
    public synchronized void shutdown() {
        if (adbServerProcess != null && adbServerProcess.isAlive()) {
            try {
                List<String> command = new ArrayList<>();
                command.add(getADBPath().toString());
                command.add("kill-server");

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true); // Merge stderr with stdout

                Process process = processBuilder.start();
                handleErrorStreamAsync(process);

                // Optionally, wait for a short duration to ensure the server stops
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (finished) {
                    logger.print("ADB server stopped successfully.");
                } else {
                    logger.print("ADB server stopped, but the process is still running.");
                }

                adbServerProcess = null;
            } catch (IOException | InterruptedException e) {
                logger.print("Failed to stop ADB server: " + e.getMessage());
                e.printStackTrace();
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        } else {
            logger.print("ADB server is not running.");
        }
    }

    /**
     * Captures a screenshot from the specified device using ImageIO.
     *
     * @param device The device identifier.
     * @return A BufferedImage representing the screenshot, or null if failed.
     */
    public BufferedImage captureScreenshot(String device) {
        String command = "exec-out screencap -p";

        try (InputStream adbOutput = runADBCommandAsStream(command, device)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted before screenshot capture.");
            }

            BufferedImage screenshot = ImageIO.read(adbOutput);
            if (screenshot == null) {
                throw new IOException("ImageIO.read returned null for device: " + device);
            }
            return screenshot;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (IOException e) {
            logger.err("IO error during screenshot capture for device " + device + ": " + e.getMessage());
        } catch (Exception e) {
            logger.err("Unexpected error during screenshot capture for device " + device + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the Path to the ADB executable.
     *
     * @return Path to adb executable.
     */
    public Path getADBPath() {
        String os = getOperatingSystem();
        Path systemPath = Paths.get(getSystemPath());

        String adbExecutable = "adb" + (os.contains(OS_WIN) ? ".exe" : "");
        Path adbPath = systemPath.resolve(Paths.get(PLATFORM_TOOLS, adbExecutable));

        if (!Files.exists(adbPath)) {
            throw new IllegalStateException("ADB executable not found at: " + adbPath.toAbsolutePath());
        }

        return adbPath.toAbsolutePath();
    }

    /**
     * Executes an ADB command and processes the output immediately.
     *
     * @param command The ADB command to execute.
     * @param device  The device identifier. If "none", the command is not executed.
     */
    public void executeADBCommand(String command, String device) {
        if ("none".equals(device)) {
            logger.print("No device specified. Command not executed.");
            return;
        }

        Future<Void> future = null;
        try {
            // Submit the ADB command task
            future = executorService.getADBExecutor(device).submit(() -> {
                String output = runADBCommand(command, device, executorService.getADBExecutor(device));
                BufferedReader reader = new BufferedReader(new StringReader(output));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        logger.print("Thread interrupted. Cancelling ADB command execution.");
                        break;
                    }
                    System.out.println(line); // Process the line
                }
                return null;
            });

            // Use a timeout to avoid indefinite waiting
            future.get(2, TimeUnit.SECONDS); // Timeout after 2 seconds
        } catch (TimeoutException e) {
            future.cancel(true);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true); // Ensure the task is cancelled
            }
        }
    }

    /**
     * Executes an ADB command and collects the output lines.
     *
     * @param command The ADB command to execute.
     * @param device  The device identifier.
     * @return A list of output lines from the ADB command.
     */
    public List<String> executeADBCommandWithOutput(String command, String device) {
        try {
            Future<List<String>> future = deviceGetter.submit(() -> {
                List<String> outputLines = new ArrayList<>();
                String output = runADBCommand(command, device, deviceGetter);
                BufferedReader reader = new BufferedReader(new StringReader(output));
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);  // Collect output lines
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
                return outputLines;
            });
            return future.get(); // Wait for the command output
        } catch (InterruptedException | ExecutionException e) {
            logger.err("Failed to execute ADB command with output: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        return Collections.emptyList();
    }

    /**
     * Executes an ADB command and returns its standard output as a String.
     *
     * @param command The ADB command to execute.
     * @param device  The device identifier.
     * @return The standard output from the ADB command.
     * @throws IOException          If an I/O error occurs.
     */
    public String runADBCommand(String command, String device, ExecutorService executor) throws IOException {
        List<String> commandList = new ArrayList<>();
        commandList.add(getADBPath().toString());
        if (device != null && !device.isEmpty()) {
            commandList.add("-s");
            commandList.add(device);
        }
        commandList.addAll(parseCommand(command));

        ProcessBuilder builder = new ProcessBuilder(commandList);
        builder.redirectErrorStream(true); // Merge stderr with stdout
        Process process = builder.start();

        // Start a separate thread to process the output asynchronously
        StringBuilder output = new StringBuilder();
        Future<Void> outputFuture = executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                logger.err("Error reading ADB output: " + e.getMessage());
            }
            return null;
        });

        int exitCode;
        try {
            exitCode = process.waitFor(); // Wait for the process to complete
            outputFuture.get(); // Ensure the output is fully captured
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupt status
            throw new IOException("ADB command interrupted.", e);
        } catch (ExecutionException e) {
            throw new IOException("Error during asynchronous output processing.", e);
        }

        if (exitCode != 0) {
            throw new IOException("ADB command failed with exit code " + exitCode + ". Output: " + output);
        }

        return output.toString();
    }

    /**
     * Executes an ADB command and returns its standard output as an InputStream.
     * Useful for commands that produce binary output, such as screenshots.
     *
     * @param command The ADB command to execute.
     * @param device  The device identifier.
     * @return An InputStream of the ADB command's standard output.
     * @throws IOException If an I/O error occurs.
     */
    public InputStream runADBCommandAsStream(String command, String device) throws IOException {
        List<String> commandList = new ArrayList<>();
        commandList.add(getADBPath().toString());
        if (device != null && !device.isEmpty()) {
            commandList.add("-s");
            commandList.add(device);
        }
        commandList.addAll(parseCommand(command));

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Handle the error stream asynchronously
        handleErrorStreamAsync(process);

        // Return an InputStream wrapper that ensures proper cleanup
        InputStream processInputStream = process.getInputStream();
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return processInputStream.read();
            }

            @Override
            public void close() throws IOException {
                try {
                    processInputStream.close();
                } finally {
                    if (process.isAlive()) {
                        process.destroy();
                    }
                }
            }
        };
    }

    /**
     * Pushes file data to the device by writing the data to a temporary file and using ADB push.
     *
     * @param remotePath The full remote path where the file should be uploaded.
     * @param data       The file data as a byte array.
     * @param deviceId   The unique device identifier.
     * @throws IOException If an error occurs during file push.
     */
    public void pushFileToDevice(String remotePath, byte[] data, String deviceId) throws IOException {
        // Create a temporary file to hold the data.
        File tempFile = File.createTempFile("adb_push_", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            throw new IOException("Failed to write temporary file for push", e);
        }

        // Construct the adb push command.
        List<String> command = new ArrayList<>();
        command.add(getADBPath().toString());
        if (deviceId != null && !deviceId.isEmpty()) {
            command.add("-s");
            command.add(deviceId);
        }
        command.add("push");
        command.add(tempFile.getAbsolutePath());
        command.add(remotePath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture adb push output for logging.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.print("adb push: " + line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("adb push failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("adb push interrupted", e);
        } finally {
            // Delete the temporary file.
            if (!tempFile.delete()) {
                logger.err("Temporary file " + tempFile.getAbsolutePath() + " could not be deleted.");
            }
        }
    }

    /**
     * Handles the error stream of a process asynchronously to prevent blocking.
     *
     * @param process The process whose error stream is to be handled.
     */
    private void handleErrorStreamAsync(Process process) {
        executorService.getUnifiedExecutor().submit(() -> { //TODO: Figure out some way to handle error streams in its device-specific context.
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (process.isAlive() && (line = errorReader.readLine()) != null) {
                    logger.err("ADB Error: " + line);
                }
            } catch (IOException ioe) {
                logger.err("Error reading ADB error stream: " + ioe.getMessage());
                ioe.printStackTrace();
            }
        });
    }

    /**
     * Parses a command string into a list of arguments, handling quotes and spaces.
     *
     * @param command The command string to parse.
     * @return A list of command arguments.
     */
    private List<String> parseCommand(String command) {
        List<String> commands = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : command.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    commands.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            commands.add(current.toString());
        }
        return commands;
    }
}