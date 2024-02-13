package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class ServerMain {
  private static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
  public static String word;

  // method to generate a new random word
  private static void generateWord(String dictionary) throws IOException {
    Stream<String> counting_lines = Files.lines(Paths.get(dictionary));
    int numlines = (int) counting_lines.count();
    counting_lines.close();
    int selectedLine = (int) (Math.random() * (numlines + 1));
    Stream<String> selecting_lines = Files.lines(Paths.get(dictionary));
    word = selecting_lines.skip(selectedLine).findFirst().get();
    selecting_lines.close();
    System.out.println(word); ////////////////////////////////// testing
  }

  @SuppressWarnings("resource")
  public static void main(String[] args) throws Exception {
    // loading properties
    FileReader config = new FileReader("files/config.config");
    Properties prop = new Properties();
    prop.load(config);
    // setup server params
    int port = Integer.parseInt(prop.getProperty("port"));
    int timeoutAccept = Integer.parseInt(prop.getProperty("accept_timeout"));
    int timeoutWord = Integer.parseInt(prop.getProperty("word_timeout")); // a new word is generated every 5 minutes
    int guessLimit = Integer.parseInt(prop.getProperty("guessLimit"));
    String dictionary = prop.getProperty("dictionary");

    ServerSocket server = new ServerSocket(port); // request/response connection
    server.setSoTimeout(timeoutAccept); // timeout accept()

    ExecutorService threadpool = Executors.newCachedThreadPool();

    TemporaryList tempList = new TemporaryList();
    AccountList accountList = new AccountList();
    // restore all accounts data, if any
    File file = new File("files/users.json");
    if (!file.createNewFile()) {
      JsonReader reader = new JsonReader(new FileReader(file));
      reader.beginArray();
      while (reader.hasNext()) {
        Account account = new Gson().fromJson(reader, Account.class);
        accountList.add(account);
      }
      reader.endArray();
      reader.close();
    }

    generateWord(dictionary); // first word is generated
    long whenWordIsGenerated = System.currentTimeMillis();

    // starting the user input reader
    Thread inputReader = new Thread(new Runnable() {
      @Override
      public void run() {
        String message = new String();
        try {
          while ((message = stdin.readLine()) != null) {
            switch (message) {
              case "savestate":
                System.out.println("Saving server state...");
                String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                    .toJson(accountList);
                BufferedWriter printerJSON = new BufferedWriter(new FileWriter(file));
                printerJSON.write(json);
                printerJSON.close();
                System.out.println("Done.");
                break;
              default:
                System.out.println("ERROR - Invalid action.");
                break;
            }
          }
        } catch (IOException e) {
        }
      }
    });
    inputReader.start();

    System.out.println("Server is running...");
    while (true) {
      try {
        Socket socket = server.accept(); // waiting new players to connect
        threadpool.execute(new Player(socket, prop, tempList, accountList));
        System.out.println("Client connected!");
      } catch (SocketTimeoutException so) {
      } finally {
        if (System.currentTimeMillis() - whenWordIsGenerated > timeoutWord) { // every 5 mins
          generateWord(dictionary);
          tempList.reset(guessLimit);
          whenWordIsGenerated = System.currentTimeMillis();
        }
      }
    }
  }
}