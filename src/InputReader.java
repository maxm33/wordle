package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.GsonBuilder;

public class InputReader extends Thread {
    private final File file = new File("files/database.json");
    private final BufferedReader stdin = Server.stdin;
    private final AccountList alist;
    private final TemporaryList tlist;
    private final BooleanFlag guard;

    public InputReader(AccountList alist, TemporaryList tlist, BooleanFlag guard) {
        this.alist = alist;
        this.tlist = tlist;
        this.guard = guard;
    }

    @Override
    public void run() {
        String message;
        try {
            while (true) {
                message = stdin.readLine();
                if (message == null)
                    return;

                switch (message) {
                    case "savestate" -> {
                        System.out.println("Saving server state...");
                        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                                .toJson(alist);
                        try (BufferedWriter printerJSON = new BufferedWriter(new FileWriter(file))) {
                            printerJSON.write(json);
                        }
                        System.out.println("Done.");
                    }

                    case "shutdown" -> {
                        System.out.println("Every progress not saved will be lost. Continue? y/n");

                        message = stdin.readLine();
                        if (message == null)
                            return;

                        if (message.contentEquals("y")) {
                            try (stdin) {
                                System.out.println("Shutting down server...");
                                guard.flag = false;
                            }
                        } else
                            System.out.println("Aborted.");
                    }
                    case "printlist" -> {
                        tlist.print();
                        alist.print();
                    }
                    default -> System.err.println("ERROR - Invalid action.");
                }
            }
        } catch (IOException e) {
        }
    }
}
