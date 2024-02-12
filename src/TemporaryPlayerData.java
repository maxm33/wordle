package src;

public class TemporaryPlayerData {
    public String username, word = ServerMain.word;
    public int guesses;
    public boolean isLogged = true, isGuessed = false;

    public TemporaryPlayerData(String username, int guessLimit) {
        this.username = username;
        this.guesses = guessLimit;
    }
}