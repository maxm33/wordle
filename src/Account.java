package src;

// Questo oggetto rappresenta i dati permanenti dei giocatori,
// che devono essere salvati e ripristinati al riavvio del server.
public class Account {
    public String username, password;
    public int currentWinStreak, maxWinStreak, numberOfMatches, numberOfWins;
    public float averageTries;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
        this.currentWinStreak = 0;
        this.maxWinStreak = 0;
        this.numberOfMatches = 0;
        this.numberOfWins = 0;
        this.averageTries = 0;
    }
}