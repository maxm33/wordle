package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.GsonBuilder;

public class InputReader extends Thread {
    private File file = new File("files/users.json");
    private BufferedReader stdin = Server.stdin;
    private AccountList alist;
    private TemporaryList tlist;
    private BooleanFlag guard;

    public InputReader(AccountList alist, TemporaryList tlist, BooleanFlag guard) {
        this.alist = alist;
        this.tlist = tlist;
        this.guard = guard;
    }

    public void run() {
        String message = new String();
        try {
            while (true) {
                message = stdin.readLine();
                switch (message) {
                    case "savestate":
                        System.out.println("Saving server state...");
                        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                                .toJson(alist);
                        BufferedWriter printerJSON = new BufferedWriter(new FileWriter(file));
                        printerJSON.write(json);
                        printerJSON.close();
                        System.out.println("Done.");
                        break;
                    case "shutdown":
                        System.out.println("Every progress not saved will be lost. Continue? y/n");
                        message = stdin.readLine();
                        if (message.contentEquals("y")) {
                            System.out.println("Shutting down server...");
                            guard.flag = false;
                            stdin.close();
                        } else
                            System.out.println("Aborted.");
                        break;
                    case "printlist":
                        tlist.print();
                        alist.print();
                        break;
                    default:
                        System.err.println("ERROR - Invalid action.");
                        break;
                }
            }
        } catch (IOException e) {
        }
    }
}
