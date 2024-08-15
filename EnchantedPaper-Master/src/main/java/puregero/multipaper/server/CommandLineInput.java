package puregero.multipaper.server;

import java.util.Scanner;

public class CommandLineInput {

    public boolean run() {
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.equalsIgnoreCase("shutdown")) {
                System.out.println("Shutting down servers...");
                ServerConnection.shutdownAndWait();
                System.out.println("Shutdown complete");
                return true;
            } else if (line.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                return true;
            } else if (line.equalsIgnoreCase("threaddump")) {
                System.out.println("Printing thread dump...");
                for (Thread thread : Thread.getAllStackTraces().keySet()) {
                    System.out.println();
                    System.out.println(thread.getName() + " " + thread.getState());
                    for (StackTraceElement element : thread.getStackTrace()) {
                        System.out.println("    " + element);
                    }
                }
            } else {
                System.out.println("Unknown command, use 'shutdown' to shutdown all servers or 'exit' to stop just this master server");
            }
        }

        // No command line detected
        return false;
    }

}
