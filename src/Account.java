package src;

public class Account {
    public String username, password;
    public int currentWinStreak = 0, maxWinStreak = 0, numberOfMatches = 0, numberOfWins = 0;
    public float averageTries = 0;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }
}