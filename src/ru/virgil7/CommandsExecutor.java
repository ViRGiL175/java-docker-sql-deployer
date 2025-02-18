package ru.virgil7;


import java.util.concurrent.Executors;
import java.util.function.Consumer;

class CommandsExecutor {

    public static void executeCommand(String command) throws Exception {
        executeCommand(command, System.out::println);
    }

    public static void executeCommand(String command, Consumer<String> onNewInput) throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (!isWindows) {
            throw new Exception("Script written for Windows OS!");
        }
        ProcessBuilder builder = new ProcessBuilder()
                .command("cmd.exe", "/c", command);
        System.out.println("Executing " + builder.command());
        Process process = builder.start();
        System.out.println("Starting...");
        ConsoleStreamListener consoleStreamListener = new ConsoleStreamListener(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(consoleStreamListener);
        System.out.println("Waiting for result...\n");
        int exitCode = process.waitFor();
        assert exitCode == 0;
        System.out.println("\nFinished!");
    }

}
