package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Player implements Runnable {
    private ArrayList<Account> userList; // lista dei dati permanenti condivisa tra i thread.
    private ArrayList<TemporaryPlayerData> tempDataList; // lista dei dati temporanei condivisa tra i thread.
    private String username; // viene salvato lo username con cui viene fatto il login.
    private boolean isLogged; // per impedire ad un altro client di fare login con lo stesso username.
    private BufferedReader in;
    private PrintWriter out;

    public Player(Socket socket, ArrayList<TemporaryPlayerData> tempDataList, ArrayList<Account> userList)
            throws IOException {
        this.tempDataList = tempDataList;
        this.userList = userList;
        this.isLogged = false;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public boolean validateUser(String username, String password) {
        for (int i = 0; i < this.userList.size(); i++) {
            Account item = this.userList.get(i);
            if (item.username.contentEquals(username) && item.password.contentEquals(password))
                return true;
        }
        return false;
    }

    // metodo che restituisce l'indice dello user cercato nella lista dei dati
    // permanenti, o -1 se non c'è.
    public int getUserInPerm(String username) {
        for (int i = 0; i < this.userList.size(); i++) {
            if (this.userList.get(i).username.contentEquals(username))
                return i;
        }
        return -1;
    }

    // metodo che restituisce l'indice dello user cercato nella lista dei dati
    // temporanei, o -1 se non c'è.
    public int getUserInTemp(String username) {
        for (int i = 0; i < this.tempDataList.size(); i++) {
            if (this.tempDataList.get(i).username.contentEquals(username))
                return i;
        }
        return -1;
    }

    // metodo che aggiunge un utente, se nuovo, nella lista dati permanenti.
    public synchronized boolean register(String username, String password) throws IOException {
        int index = getUserInPerm(username);
        if (index != -1) {
            out.println("ERROR - this username is registered already. Please log in.");
            return false;
        } else {
            Account newAccount = new Account(username, password);
            this.userList.add(newAccount);
            out.println("OK - user successfully registered! Please log in.");
            return true;
        }
    }

    // metodo per il login del giocatore.
    public synchronized boolean login(String username, String password) throws IOException {
        if (validateUser(username, password)) {
            int index = getUserInTemp(username);
            if (index != -1) {
                TemporaryPlayerData item = this.tempDataList.get(index);
                if (item.isLogged) {
                    out.println("ERROR - user is already logged.");
                    return false;
                }
                item.isLogged = true;
                this.tempDataList.set(index, item);
                this.username = username;
                this.isLogged = true;
                out.println("OK - login successful!");
                return true;
            }
            TemporaryPlayerData newPlayer = new TemporaryPlayerData(username);
            this.tempDataList.add(newPlayer);
            this.username = username;
            this.isLogged = true;
            out.println("OK - login successful!");
            return true;
        } else {
            out.println("ERROR - invalid username/password. Please try again.");
            return false;
        }
    }

    // metodo per il logout del giocatore.
    public synchronized boolean logout() throws IOException {
        int index = getUserInTemp(this.username);
        if (index != -1) {
            TemporaryPlayerData item = this.tempDataList.get(index);
            item.isLogged = false;
            this.tempDataList.set(index, item);
            this.isLogged = false;
            out.println("OK - logout successful. Bye!");
            return true;
        } else {
            out.println("ERROR - user is not logged in.");
            return false;
        }
    }

    // metodo per decrementare i guess rimanenti del giocatore ad ogni suo
    // tentativo.
    public synchronized void decrementGuesses() {
        int index = getUserInTemp(this.username);
        if (index != -1) {
            TemporaryPlayerData item = this.tempDataList.get(index);
            item.guesses--;
            this.tempDataList.set(index, item);
        }
    }

    // metodo che invia al client info sulla partita corrente per il giocatore
    // richiesto.
    public void getGuessesAndGuessed() throws IOException {
        int index = getUserInTemp(this.username);
        if (index != -1) {
            TemporaryPlayerData item = this.tempDataList.get(index);
            out.println(Integer.toString(item.guesses) + "," + Boolean.toString(item.isGuessed));
        }
    }

    // metodo che aggiorna le statistiche (nella lista dei dati permanenti)
    // quando il giocatore vince. Aggiorna dei dati anche nella lista temporanea.
    public synchronized void updateStatsOnWin(int tries) {
        int index = getUserInPerm(this.username);
        if (index != -1) {
            Account item = this.userList.get(index);
            item.currentWinStreak++;
            item.numberOfMatches++;
            if (item.currentWinStreak >= item.maxWinStreak) {
                item.maxWinStreak = item.currentWinStreak;
            }
            item.averageTries = ((item.averageTries * item.numberOfWins) + tries) /
                    (item.numberOfWins + 1);
            item.numberOfWins++;
            this.userList.set(index, item);
        }
        index = getUserInTemp(this.username);
        if (index != -1) {
            TemporaryPlayerData item = this.tempDataList.get(index);
            item.isGuessed = true;
            this.tempDataList.set(index, item);
        }
    }

    // metodo che aggiorna le statistiche (nella lista dei dati permanenti)
    // quando il giocatore perde o skippa la parola. Aggiorna dei dati anche nella
    // lista temporanea.
    public synchronized void updateStatsOnLoss() {
        int index = getUserInPerm(this.username);
        if (index != -1) {
            Account item = this.userList.get(index);
            item.numberOfMatches++;
            item.currentWinStreak = 0;
            this.userList.set(index, item);
        }
        index = getUserInTemp(this.username);
        if (index != -1) {
            TemporaryPlayerData item = this.tempDataList.get(index);
            item.guesses = 0;
            this.tempDataList.set(index, item);
        }
    }

    // metodo che passa al client le statistiche del giocatore.
    public void getAccountStats() throws IOException {
        int index = getUserInPerm(this.username);
        if (index != -1) {
            Account item = this.userList.get(index);
            out.println(
                    item.username +
                            "," +
                            item.numberOfMatches +
                            "," +
                            item.numberOfWins +
                            "," +
                            item.currentWinStreak +
                            "," +
                            item.maxWinStreak +
                            "," +
                            item.averageTries);
        }
    }

    // printa le liste dei dati permanenti e temporanei, per testing.
    public void printList() throws IOException {
        System.out.println("*****************************************");
        for (int i = 0; i < this.userList.size(); i++) {
            Account item = this.userList.get(i);
            System.out.printf(
                    "username: %s - matches: %s - wins: %s - curWS: %s - maxWS: %s - avgtries: %s\n",
                    item.username,
                    item.numberOfMatches,
                    item.numberOfWins,
                    item.currentWinStreak,
                    item.maxWinStreak,
                    item.averageTries);
        }
        System.out.println("-----------------------------------------");
        for (int i = 0; i < this.tempDataList.size(); i++) {
            TemporaryPlayerData item = this.tempDataList.get(i);
            System.out.printf(
                    "username: %s - isLogged: %s - guesses: %s - word: %s - isGuessed: %s\n",
                    item.username,
                    item.isLogged,
                    item.guesses,
                    item.word,
                    item.isGuessed);
        }
    }

    public void run() {
        String message = new String();
        // un loop in cui il server aspetta un messaggio
        // con un format predefinito dal client e agisce di conseguenza.
        // Termina quando il client farà login con successo.
        try {
            while (!this.isLogged) {
                message = in.readLine();
                String[] data = message.split(",");
                switch (data[0]) {
                    case "register":
                        register(data[1], data[2]);
                        printList();
                        break;
                    case "login":
                        if (login(data[1], data[2])) {
                            out.println("true");
                            this.isLogged = true;
                        } else
                            out.println("false");
                        printList();
                        break;
                    case "exit":
                        System.out.println("Client disconnected.");
                        return;
                    default:
                        out.println("Invalid command.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client forced to disconnect.");
            return;
        }

        try {
            // setup delle impostazioni per il multicast.
            int portMulticast = 3002, TTL = 255;

            // il server si unisce al gruppo multicast.
            InetAddress multiAddr = InetAddress.getByName("239.255.255.255");
            MulticastSocket multiSocket = new MulticastSocket(portMulticast);
            multiSocket.setTimeToLive(TTL);
            multiSocket.joinGroup(multiAddr);

            // il loop che gestisce tutte le richieste fatte dal client durante il gioco,
            // anche qui il server sta in ascolto e riceve un messaggio con un certo format
            // e agisce in base alla richiesta. Alcune richieste sono fatte esplicitamente
            // dal giocatore, mentre altre sono fatte implicitamente dal client per ricevere
            // certi dati.
            while (this.isLogged) {
                message = in.readLine();
                String[] data = message.split(",");
                switch (data[0]) {
                    case "logout":
                        if (logout())
                            out.println("true");
                        else
                            out.println("false");
                        printList();
                        break;
                    case "sendStats":
                        getAccountStats();
                        break;
                    case "share":
                        String notification = data[1] + " has guessed the word '" + data[2] + "' with " + data[3]
                                + " attempts.";
                        DatagramPacket dp = new DatagramPacket(notification.getBytes(), notification.length(),
                                multiAddr,
                                portMulticast);
                        multiSocket.send(dp);
                        break;
                    case "guess":
                        decrementGuesses();
                        String word = ServerMain.word, hint = new String();
                        for (int i = 0; i < 10; i++) {
                            if (word.charAt(i) == data[1].charAt(i)) {
                                hint = hint + "+  ";
                            } else if (word.indexOf(data[1].charAt(i)) != -1) {
                                hint = hint + "?  ";
                            } else {
                                hint = hint + "X  ";
                            }
                        }
                        out.println(hint);
                        if (word.contentEquals(data[1]))
                            out.println("true");
                        else
                            out.println("false");
                        printList();
                        break;
                    case "info":
                        getGuessesAndGuessed();
                        break;
                    case "won":
                        updateStatsOnWin(Integer.parseInt(data[1]));
                        printList();
                        break;
                    case "lost":
                        updateStatsOnLoss();
                        printList();
                        break;
                }
            }
        } catch (IOException e) {
            // se un client è crashato per qualche motivo, il server fa
            // logout al posto suo.
            System.out.println("Client forced to disconnect.");
            try {
                logout();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}