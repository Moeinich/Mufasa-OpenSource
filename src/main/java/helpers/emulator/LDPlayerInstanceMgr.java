package helpers.emulator;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import helpers.ThreadManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class LDPlayerInstanceMgr {
    private ExecutorService executor;

    public LDPlayerInstanceMgr() {
        this.executor = ThreadManager.getInstance().getUnifiedExecutor();
    }

    public String createInstances() {
        return createInstances(1);
    }

    public String createInstances(int instanceCount) {
        Path tempPSFile = null;
        Path tempOutputFile = null;

        try {
            // Find LDPlayer installation location via registry
            String command = "reg query \"HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\LDPlayer9\" /v UninstallString";
            Process process = Runtime.getRuntime().exec(command);
            String dnconsolePath = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .filter(line -> line.contains("UninstallString"))
                    .map(line -> line.split("REG_SZ")[1].trim())
                    .map(uninstallPath -> Paths.get(uninstallPath).getParent().resolve("dnconsole.exe").toString())
                    .findFirst()
                    .orElse(null);

            if (dnconsolePath == null || !new File(dnconsolePath).exists()) {
                System.err.println("LDPlayer installation not found.");
                return "LDPlayer installation not found.";
            }

            System.out.println("LDPlayer installation found at: " + dnconsolePath);

            // Create temporary files for output and PowerShell script
            tempOutputFile = Files.createTempFile("ldplayer_output", ".txt");
            String tempOutputPath = tempOutputFile.toString().replace("\\", "\\\\"); // escape backslashes for PowerShell

            // Build the PowerShell script with a loop for instance creation.
            String psCommands = "$dnconsolePath = \"" + dnconsolePath + "\"\n" +
                    "$ldPlayerRoot = [System.IO.Path]::GetDirectoryName($dnconsolePath)\n" +
                    "$ldMultiplayerPath = [System.IO.Path]::Combine((Get-Item $ldPlayerRoot).Parent.FullName, \"ldmutiplayer\", \"dnmultiplayerex.exe\")\n" +
                    "Write-Output \"Calculated path to dnmultiplayerex.exe: $ldMultiplayerPath\" | Out-File \"" + tempOutputPath + "\" -Append\n" +
                    "\n" +
                    "# Stop multiplayer process if running\n" +
                    "$processName = \"dnmultiplayerex\"\n" +
                    "if (Get-Process -Name $processName -ErrorAction SilentlyContinue) {\n" +
                    "    Stop-Process -Name $processName -Force\n" +
                    "    Write-Output \"Stopped process: $processName\" | Out-File \"" + tempOutputPath + "\" -Append\n" +
                    "}\n" +
                    "\n" +
                    "# Get existing instance indexes and ensure it's an array\n" +
                    "$dnconsoleListOutput = & $dnconsolePath list2\n" +
                    "$existingIndexes = @($dnconsoleListOutput | ForEach-Object { ($_ -split ',')[0] } | ForEach-Object { [int]$_ })\n" +
                    "\n" +
                    "for ($i = 0; $i -lt " + instanceCount + "; $i++) {\n" +
                    "    $firstAvailableIndex = 0\n" +
                    "    while ($existingIndexes -contains $firstAvailableIndex) {\n" +
                    "        $firstAvailableIndex++\n" +
                    "    }\n" +
                    "    $existingIndexes = $existingIndexes + $firstAvailableIndex\n" +
                    "\n" +
                    "    $adb_port = 5555 + ($firstAvailableIndex * 2)\n" +
                    "    $instance_name = \"Mufasa - ADB: $adb_port\"\n" +
                    "    & $dnconsolePath add --name \"$instance_name\"\n" +
                    "    & $dnconsolePath modify --name \"$instance_name\" --cpu 2 --resolution 894,540,160 --memory 2048 --imei auto --imsi auto --simserial auto\n" +
                    "    Write-Output \"Created instance: $instance_name\" | Out-File \"" + tempOutputPath + "\" -Append\n" +
                    "\n" +
                    "    $configPath = [System.IO.Path]::Combine($ldPlayerRoot, \"vms\", \"config\", \"leidian$firstAvailableIndex.config\")\n" +
                    "    $configJson = Get-Content $configPath -Raw | ConvertFrom-Json\n" +
                    "    $configJson | Add-Member -Type NoteProperty -Name 'basicSettings.adbDebug' -Value 1 -Force\n" +
                    "    $configJson.'basicSettings.adbDebug' = 1\n" +
                    "    $configJson | Add-Member -Type NoteProperty -Name 'basicSettings.lockWindow' -Value $true -Force\n" +
                    "    $configJson.'basicSettings.lockWindow' = $true\n" +
                    "    $configJson | ConvertTo-Json -Depth 10 | Set-Content $configPath\n" +
                    "}\n" +
                    "\n" +
                    "# Restart multiplayer process after creating all instances\n" +
                    "Start-Process -FilePath $ldMultiplayerPath\n" +
                    "Write-Output \"Started process: $ldMultiplayerPath\" | Out-File \"" + tempOutputPath + "\" -Append\n" +
                    "Start-Sleep -Seconds 3\n";

            tempPSFile = Files.createTempFile("ldplayer_commands", ".ps1");
            Files.write(tempPSFile, psCommands.getBytes());

            // Run the PowerShell script asynchronously
            Path finalTempOutputFile = tempOutputFile;
            Path finalTempPSFile = tempPSFile;
            Future<String> future = executor.submit(() -> {
                // Elevate to run as administrator
                Shell32.INSTANCE.ShellExecuteA(0, "runas", "powershell.exe",
                        "-ExecutionPolicy Bypass -WindowStyle hidden -File \"" + finalTempPSFile + "\"", null, 1);

                // Wait for the output file to be created and populated
                while (!Files.exists(finalTempOutputFile) || Files.size(finalTempOutputFile) == 0) {
                    Thread.sleep(100); // avoid busy waiting
                }

                // Read and return the output
                byte[] fileBytes = Files.readAllBytes(finalTempOutputFile);
                String output = new String(fileBytes, StandardCharsets.UTF_8);
                return output.replaceAll("[^\\x20-\\x7E]", ""); // clean non-ASCII characters
            });

            String output = future.get(60, TimeUnit.SECONDS);
            System.out.println(output);
            return output;

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("An error occurred: " + e.getMessage());
            return "An error occurred.";

        } finally {
            // Clean up temporary files
            if (tempPSFile != null && Files.exists(tempPSFile)) {
                try {
                    Files.delete(tempPSFile);
                } catch (IOException e) {
                    System.err.println("Failed to delete temporary PowerShell file: " + e.getMessage());
                }
            }
            if (tempOutputFile != null && Files.exists(tempOutputFile)) {
                try {
                    Files.delete(tempOutputFile);
                } catch (IOException e) {
                    System.err.println("Failed to delete temporary output file: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        int count = 1;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (Exception e) {
                count = 1;
            }
        }
        new LDPlayerInstanceMgr().createInstances(count);
    }

    public interface Shell32 extends StdCallLibrary {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);
        void ShellExecuteA(int hwnd, String operation, String file, String params, String directory, int showCmd);
    }
}