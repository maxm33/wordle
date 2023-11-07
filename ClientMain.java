import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ClientMain {
    // metodo per richiedere username e password durante register e login.
    private static String[] askCredentials(BufferedReader input) throws IOException {
        String[] credentials = new String[2];
        while (true) {
            System.out.println("Enter your username:");
            credentials[0] = input.readLine();
            if (credentials[0] == null || credentials[0].isBlank())
                continue;
            break;
        }
        while (true) {
            System.out.println("Enter your password:");
            credentials[1] = input.readLine();
            if (credentials[1] == null || credentials[1].isBlank())
                continue;
            break;
        }
        return credentials;
    }

    // metodo che verifica se la parola inserita è presente nel file delle parole.
    private static boolean checkVocabulary(String word) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("files/words.txt"));
        String line = reader.readLine();
        while (line != null) {
            if (line.contentEquals(word)) {
                reader.close();
                return true;
            }
            line = reader.readLine();
        }
        reader.close();
        return false;
    }

    public static void main(String[] args) throws IOException, UnknownHostException {
        // setup dei parametri del client.
        int port = 3000, portMulticast = 3002, TTL = 255, timeoutReceive = 1000;

        int guesses = 0;
        boolean isLogged = false, isGuessed = false;
        String[] credentials = new String[2];
        String username = new String(), line = new String(), response = new String();
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in)); // stdin.

        // la connessione request/response.
        Socket socket = new Socket("localhost", port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // un loop in cui il client aspetta un messaggio
        // da parte dell'utente sullo standard input. Alla ricezione,
        // manda la relativa richiesta al server, che agisce di conseguenza.
        // Termina quando il client farà login con successo.
        System.out.println("\nType the action you want to do:\n- register\n- login\n- exit\n");
        while (!isLogged) {
            response = input.readLine();
            switch (response) {
                case "register":
                    credentials = askCredentials(input);
                    out.println("register," + credentials[0] + "," + credentials[1]);
                    response = in.readLine();
                    System.out.println(response);
                    break;
                case "login":
                    credentials = askCredentials(input);
                    out.println("login," + credentials[0] + "," + credentials[1]);
                    response = in.readLine();
                    System.out.println(response);
                    response = in.readLine();
                    if (response.contentEquals("true")) {
                        isLogged = true;
                        username = credentials[0];
                    }
                    break;
                case "exit":
                    out.println("exit, , ");
                    return;
                default:
                    System.out.println("ERROR - Invalid action. Please try again.");
                    break;
            }
        }
        // il client si unisce al gruppo multicast perchè
        // ha fatto login con successo.
        InetAddress multiAddr = InetAddress.getByName("239.255.255.255");
        MulticastSocket multiSocket = new MulticastSocket(portMulticast);
        multiSocket.setSoTimeout(timeoutReceive);
        multiSocket.setTimeToLive(TTL);
        multiSocket.joinGroup(multiAddr);
        // struttura dati per lo storage delle notifiche ricevute.
        ArrayList<String> notificationList = new ArrayList<String>();

        // il loop dove il client effettua tutte le richieste durante il gioco,
        // il client invia messaggi con un certo format al server. Alcune richieste
        // sono fatte esplicitamente dal giocatore, mentre altre sono fatte dal client
        // per ricevere certi dati.
        while (isLogged) {
            // ripristina le info del giocatore sulla partita corrente, qualora ci fossero.
            out.println("info," + username);
            line = in.readLine();
            String[] info = line.split(",");
            guesses = Integer.parseInt(info[0]);
            isGuessed = Boolean.parseBoolean(info[1]);

            if (guesses == 0 || isGuessed == true) {
                System.out.println("You can't try anymore for now, press 'enter' to refresh...");
                line = input.readLine();
                continue;
            }

            System.out.printf(
                    "\n************************\nHi %s!\nThis is what you can do:\n1) sendWord <word> (to try guessing the word)\n2) skip (counts as a loss and you'll have to wait for a new word)\n3) sendStats (to get your account stats)\n4) showSharings (to show notifications about other players' results)\n5) logout (to exit the session)\n\nGuesses remaining: %s\n************************\n\n",
                    username, guesses);

            // loop del gioco, viene eseguito finchè il giocatore non ha indovinato,
            // ha ancora tentativi o non fa il logout.
            while (guesses > 0 && isLogged && !isGuessed) {
                out.println("info," + username);
                line = in.readLine();
                info = line.split(",");
                guesses = Integer.parseInt(info[0]);
                isGuessed = Boolean.parseBoolean(info[1]);
                // per x secondi riceve tutte le notifiche disponibili,
                // le conserva e rimane in ascolto.
                try {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    multiSocket.receive(packet);
                    String message = new String(packet.getData(), StandardCharsets.UTF_8);
                    notificationList.add(message);
                } catch (SocketTimeoutException so) {
                }
                // controllo del stdin.
                while (input.ready()) {
                    line = input.readLine();
                    String[] parts = line.split(" ");
                    switch (parts[0]) {
                        // i controlli eseguiti quando viene fatto
                        // un tentativo di indovinare la parola.
                        case "sendWord":
                            try {
                                if (parts[1].length() != 10) {
                                    System.out.println("ERROR - Word is not 10 characters.");
                                    continue;
                                }
                                if (!checkVocabulary(parts[1])) {
                                    System.out.println("ERROR - Word is not in vocabulary.");
                                    continue;
                                }
                                guesses--;
                                // decrementa i guesses lato server e invia la parola.
                                out.println("guess," + parts[1]);
                                System.out.printf("\n");
                                for (int i = 0; i < 10; i++)
                                    System.out.printf("%s  ", parts[1].charAt(i));
                                System.out.printf("\n");
                                line = in.readLine();
                                System.out.printf("%s\n\n", line);
                                line = in.readLine();

                                if (line.contentEquals("true")) {
                                    isGuessed = true;
                                    out.println("won," + Integer.toString(12 - guesses));
                                    System.out.println("\nYOU HAVE WON!! Do you want to share it with everybody? y/n");
                                    response = input.readLine();
                                    if (response.contentEquals("y"))
                                        out.println(
                                                "share," + username + "," + parts[1] + ","
                                                        + Integer.toString(12 - guesses)); // richiesta di share.
                                    else
                                        System.out.println("Alright then...keep your secrets...");
                                }
                                // se i tentativi rimanenti sono 0 e la parola
                                // non è stata indovinata, è una sconfitta e viene
                                // eseguita la stessa procedura dello skip nel lato server.
                                else if (guesses > 0)
                                    System.out.printf("Guesses remaining: %s\n\n", guesses);
                                else if (guesses == 0 && !isGuessed)
                                    out.println("lost");
                            } catch (ArrayIndexOutOfBoundsException exception) {
                                System.out.println("ERROR - Invalid format. Please try again.");
                                continue;
                            }
                            break;
                        // richiesta di logout, invia al server e
                        // viene eseguita la relativa funzione di logout.
                        case "logout":
                            out.println("logout");
                            response = in.readLine();
                            System.out.println(response);
                            response = in.readLine();
                            if (response.contentEquals("true"))
                                isLogged = false;
                            break;
                        // richiesta delle statistiche relative al giocatore,
                        // invia al server viene eseguita la relativa funzione.
                        case "sendStats":
                            out.println("sendStats");
                            response = in.readLine();
                            String[] stats = response.split(",");
                            float percentage = 0;
                            if (Float.parseFloat(stats[1]) != 0)
                                percentage = (Float.parseFloat(stats[2]) / Float.parseFloat(stats[1])) * 100;
                            System.out.printf(
                                    "\nusername: %s\n# of matches: %s\n# of wins: %s\nwin percentage: %.1f\ncurrent win streak: %s\nmax win streak: %s\naverage tries per win: %.1f\n\n",
                                    stats[0], stats[1], stats[2], percentage, stats[3], stats[4],
                                    Float.parseFloat(stats[5]));
                            break;
                        // printa tutte le notifiche presenti nella lista
                        // e le rimuove perchè sono già state visualizzate.
                        case "showSharings":
                            System.out.println("Retrieving notifications, please wait...\n");
                            for (int i = 0; i < notificationList.size(); i++) {
                                System.out.println(notificationList.get(i));
                                notificationList.remove(i);
                            }
                            System.out.println("Done.\n");
                            break;
                        // se un giocatore non vuole più provare ad indovinare la parola,
                        // può skipparla. Se ce ne sono altre nel buffer, verranno scartate
                        // tranne l'ultima, quindi il gioco viene aggiornato alla parola
                        // corrente. Se il giocatore skippa la parola corrente, deve aspettare
                        // la generazione della prossima parola. Uno skip azzera i tentativi
                        // rimanenti sulla parola, quindi conta come una sconfitta.
                        case "skip":
                            System.out.println("Word skipped.");
                            out.println("lost");
                            isGuessed = true;
                            continue;
                        default:
                            System.out.println("ERROR - Invalid action. Please try again.");
                            continue;
                    }
                }
            }
        }
        input.close();
        socket.close();
        multiSocket.close();
    }
}