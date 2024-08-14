package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

public class Client {
    private static BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
    private static BufferedReader fromServer;
    private static PrintWriter toServer;

    // method to request username/password during register/login
    private static String askCredential(String str) throws IOException {
        String credential = new String();
        while (credential == null || credential.isBlank()) {
            System.out.println("Enter your " + str + ":");
            credential = userInput.readLine();
        }
        return credential;
    }

    // method to check if word is in word file
    private static boolean checkWord(String word, String dictionary) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(dictionary));
        String line = reader.readLine();
        while (line != null) {
            if (line.contentEquals(word)) {
                reader.close();
                return true;
            } else
                line = reader.readLine();
        }
        reader.close();
        return false;
    }

    public static void main(String[] args) throws IOException, UnknownHostException {
        // loading properties
        FileReader config = new FileReader("files/.config");
        Properties prop = new Properties();
        prop.load(config);
        // setup client params
        int port = Integer.parseInt(prop.getProperty("port"));
        int portMulticast = Integer.parseInt(prop.getProperty("port_multicast"));
        int TTL = Integer.parseInt(prop.getProperty("time_to_live"));
        int timeoutReceive = Integer.parseInt(prop.getProperty("receive_timeout"));
        int guessLimit = Integer.parseInt(prop.getProperty("guessLimit"));
        String address = prop.getProperty("address");
        String addressMulticast = prop.getProperty("address_multicast");
        String dictionary = prop.getProperty("dictionary");

        boolean isLogged = false;
        String username = new String(), response, password;

        Socket socket = new Socket(address, port); // request/response connection
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        toServer = new PrintWriter(socket.getOutputStream(), true);

        // authentication menu
        System.out.println("\nType the action you want to do:\n- register\n- login\n- exit\n");
        while (!isLogged) {
            response = userInput.readLine();
            switch (response) {
                case "register":
                    username = askCredential("username");
                    password = askCredential("password");
                    toServer.println("register," + username + "," + password);
                    response = fromServer.readLine();
                    System.out.println(response);
                    break;
                case "login":
                    username = askCredential("username");
                    password = askCredential("password");
                    toServer.println("login," + username + "," + password);
                    response = fromServer.readLine();
                    System.out.println(response);
                    isLogged = Boolean.parseBoolean(fromServer.readLine());
                    break;
                case "exit":
                    toServer.println("exit, , ");
                    return;
                default:
                    System.err.println("ERROR - Invalid action.");
                    break;
            }
        }
        // entering multicast group
        InetAddress multiAddr = InetAddress.getByName(addressMulticast);
        MulticastSocket multiSocket = new MulticastSocket(portMulticast);
        multiSocket.setSoTimeout(timeoutReceive);
        multiSocket.setTimeToLive(TTL);
        multiSocket.joinGroup(multiAddr);

        ArrayList<String> notificationList = new ArrayList<String>();

        // starting the notification receiver
        NotificationReceiver receiver = new NotificationReceiver(notificationList, multiSocket);
        receiver.start();

        int guesses;
        String line;
        boolean isGuessed = false;

        while (isLogged) {
            // restoring previous game, if any
            toServer.println("info," + username);
            line = fromServer.readLine();
            String[] info = line.split(",");
            guesses = Integer.parseInt(info[0]);
            isGuessed = Boolean.parseBoolean(info[1]);

            if (guesses == 0 || isGuessed == true) {
                System.out.println("You can't try anymore for now, press 'enter' to refresh...");
                userInput.readLine();
                continue;
            }

            // game menu
            System.out.printf(
                    "\n************************\nHi %s!\nThis is what you can do:\n- guess <word>\n- skip (counts as a loss)\n- stats\n- sharings (to show notifications)\n- logout\n\nGuesses remaining: %s\n************************\n\n",
                    username, guesses);

            while (guesses > 0 && isLogged && !isGuessed) {
                line = userInput.readLine();
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "guess":
                        try {
                            if (!checkWord(parts[1], dictionary)) {
                                System.err.println("ERROR - Word is not in vocabulary.");
                                continue;
                            }
                            guesses--;
                            toServer.println("guess," + parts[1]);
                            System.out.printf("\n");
                            for (int i = 0; i < parts[1].length(); i++)
                                System.out.printf("%s  ", parts[1].charAt(i));
                            System.out.printf("\n");
                            line = fromServer.readLine();
                            System.out.printf("%s\n\n", line);
                            isGuessed = Boolean.parseBoolean(fromServer.readLine());

                            if (isGuessed) {
                                toServer.println("won," + Integer.toString(guessLimit - guesses));
                                System.out.println("\nYOU HAVE WON!! Do you want to share it with everybody? y/n");
                                response = userInput.readLine();
                                if (response.contentEquals("y")) {
                                    toServer.println(
                                            "share," + username + "," + parts[1] + ","
                                                    + Integer.toString(guessLimit - guesses));
                                    System.out.println("Notification sent.");
                                } else
                                    System.out.println("Alright then...keep your secrets...");
                            } else if (guesses > 0)
                                System.out.printf("Guesses remaining: %s\n\n", guesses);
                            else if (guesses == 0 && !isGuessed)
                                toServer.println("lost");
                        } catch (ArrayIndexOutOfBoundsException exception) {
                            System.err.println("ERROR - Invalid format.");
                            continue;
                        }
                        break;
                    case "skip":
                        System.out.println("Word skipped.");
                        toServer.println("lost");
                        guesses = 0;
                        continue;
                    case "stats":
                        toServer.println("stats");
                        response = fromServer.readLine();
                        String[] stats = response.split(",");
                        float percentage = 0;
                        if (Float.parseFloat(stats[1]) != 0)
                            percentage = (Float.parseFloat(stats[2]) / Float.parseFloat(stats[1])) * 100;
                        percentage *= 100;
                        percentage = Math.round(percentage);
                        percentage /= 100;
                        System.out.printf(
                                "\nusername: %s\n# of matches: %s\n# of wins: %s\nwin percentage: %s\ncurrent win streak: %s\nmax win streak: %s\naverage tries per win: %s\n\n",
                                stats[0], stats[1], stats[2], percentage, stats[3], stats[4],
                                stats[5]);
                        break;
                    case "sharings":
                        System.out.println("Retrieving notifications...");
                        for (int i = 0; i < notificationList.size(); i++) {
                            System.out.println(notificationList.get(i));
                            notificationList.remove(i);
                        }
                        System.out.println("Done.\n");
                        break;
                    case "logout":
                        toServer.println("logout");
                        response = fromServer.readLine();
                        System.out.println(response);
                        isLogged = Boolean.parseBoolean(fromServer.readLine());
                        break;
                    default:
                        System.err.println("ERROR - Invalid action.");
                        continue;
                }
            }
        }
        receiver.stop_1();
        userInput.close();
        socket.close();
        multiSocket.close();
    }
}