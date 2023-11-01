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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class ServerMain {
  public static String word;

  // metodo che genera un numero casuale che corrisponde al numero
  // di una riga del file delle parole e quindi è la parola che sarà estratta.
  private static void generateNewWord() throws IOException {
    int numline = (int) (Math.random() * 30825.0);
    Stream<String> lines = Files.lines(Paths.get("files/words.txt"));
    word = lines.skip(numline).findFirst().get();
    lines.close();
  }

  // metodo che resetta i dati temporanei di tutti i giocatori.
  private static void resetTempData(ArrayList<TemporaryPlayerData> tempDataList) {
    for (int i = 0; i < tempDataList.size(); i++) {
      TemporaryPlayerData item = tempDataList.get(i);
      item.guesses = 12;
      item.word = ServerMain.word;
      item.isGuessed = false;
      tempDataList.set(i, item);
    }
  }

  // metodo per monitorare lo standard input del server,
  // necessario per avviare la procedura di salvataggio dei progressi
  // permanenti sul file json.
  private static String saveStateOnInputDemand(BufferedReader input) throws IOException {
    String result = new String();
    while (input.ready()) {
      result = input.readLine();
    }
    if (result.length() != 9) {
      return null;
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    // setup dei parametri del server.
    int port = 3000;
    int timeoutAccept = 10000;
    int timeoutWord = 300000; // a new word is generated every 5 minutes

    String resultInput = new String();

    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    ServerSocket server = new ServerSocket(port); // connessione request/response.
    server.setSoTimeout(timeoutAccept); // timeout della accept() request/response.

    ExecutorService threadpool = Executors.newCachedThreadPool();
    ArrayList<TemporaryPlayerData> tempDataList = new ArrayList<TemporaryPlayerData>();

    File file = new File("files/users.json");
    ArrayList<Account> userList = new ArrayList<Account>();

    // se ci sono già dei progressi permanenti, li ripristina.
    // Altrimenti la lista rimane vuota.
    if (!file.createNewFile()) {
      JsonReader reader = new JsonReader(new FileReader(file));
      reader.beginArray();
      while (reader.hasNext()) {
        Account account = new Gson().fromJson(reader, Account.class);
        userList.add(account);
      }
      reader.endArray();
      reader.close();
    }

    generateNewWord(); // viene estratta la prima parola.
    long whenWordIsGenerated = System.currentTimeMillis();
    System.out.println("Server is running...");

    while (true) {
      try {
        Socket socket = server.accept(); // connessione request/response
        threadpool.execute(new Player(socket, tempDataList, userList));
        System.out.println("Client connected!");
      } catch (SocketTimeoutException so) {
      } finally {
        // ogni x secondi viene generata
        // una nuova parola e viene inoltrata.
        if (System.currentTimeMillis() - whenWordIsGenerated > timeoutWord) {
          generateNewWord();
          resetTempData(tempDataList);
          System.out.println(word); ////////////////////////////////// per testing
          whenWordIsGenerated = System.currentTimeMillis();
        }
        // controllo dello standard input e salvataggio se richiesto.
        resultInput = saveStateOnInputDemand(input);
        if (resultInput != null && resultInput.contentEquals("savestate")) {
          System.out.println("Saving the server state...");
          String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(userList);
          BufferedWriter printerJSON = new BufferedWriter(new FileWriter(file));
          printerJSON.write(json);
          printerJSON.close();
          System.out.println("Done.");
        }
      }
    }
  }
}