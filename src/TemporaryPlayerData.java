package src;

// Questo oggetto rappresenta i dati temporanei che servono
// a gestire le variabili di una partita e vengono resettati
// ad ogni cambio di parola o al riavvio del server.
public class TemporaryPlayerData {
    public String username, word;
    public int guesses;
    public boolean isLogged, isGuessed;

    public TemporaryPlayerData(String username) {
        this.username = username;
        this.guesses = 12;
        this.isLogged = true;
        this.isGuessed = false;
        this.word = ServerMain.word;
    }
}