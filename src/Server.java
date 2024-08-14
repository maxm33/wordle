package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

class BooleanFlag {
  public boolean flag = true;
}

public class Server {
  public static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
  public static volatile String word;

  // method to generate a new random word
  private static void generateWord(String dictionary) throws IOException {
    Stream<String> counting_lines = Files.lines(Paths.get(dictionary));
    int numlines = (int) counting_lines.count();
    counting_lines.close();
    int selectedLine = (int) (Math.random() * (numlines + 1));
    Stream<String> selecting_lines = Files.lines(Paths.get(dictionary));
    word = selecting_lines.skip(selectedLine).findFirst().get();
    selecting_lines.close();
    System.out.println("#DEV Solution: " + word); ////////////////////// testing
  }

  public static void main(String[] args) throws Exception {
    // loading properties
    FileReader config = new FileReader("files/.config");
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
    File file = new File("files/database.json");
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
    BooleanFlag guard = new BooleanFlag();
    InputReader inputReader = new InputReader(accountList, tempList, guard);
    inputReader.start();

    // to manage the connection with threads in pool
    ArrayList<Socket> socketList = new ArrayList<Socket>();

    System.out.println("Server is running...");
    while (guard.flag) {
      try {
        Socket socket = server.accept(); // waiting new players to connect
        socketList.add(socket);
        threadpool.execute(new GameManager(socket, prop, tempList, accountList));
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
    stdin.close();
    for (Socket x : socketList)
      x.close();
    server.close();
    threadpool.shutdown();
    if (!threadpool.awaitTermination(10, TimeUnit.SECONDS))
      threadpool.shutdownNow();
    System.out.println("Server is offline.");
  }
}