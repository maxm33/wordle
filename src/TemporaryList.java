package src;

import java.util.ArrayList;

class TemporaryData {
    public String username, word = Server.word;
    public boolean isLogged = true, isGuessed = false;
    public int guesses;

    public TemporaryData(String username, int guessLimit) {
        this.username = username;
        this.guesses = guessLimit;
    }
}

public class TemporaryList extends ArrayList<TemporaryData> {

    public int getIndex(String username) {
        for (int i = 0; i < this.size(); i++)
            if (this.get(i).username.contentEquals(username))
                return i;
        return -1;
    }

    public int getGuesses(String username) {
        for (TemporaryData x : this)
            if (x.username.contentEquals(username))
                return x.guesses;
        return -1;
    }

    public synchronized void decrementGuesses(String username) {
        int index = getIndex(username);
        if (index != -1)
            this.get(index).guesses--;
    }

    public boolean hasGuessed(String username) {
        for (TemporaryData x : this)
            if (x.username.contentEquals(username))
                return x.isGuessed;
        return false;
    }

    public synchronized void hasWon(String username) {
        int index = getIndex(username);
        if (index != -1)
            this.get(index).isGuessed = true;
    }

    public synchronized void hasLost(String username) {
        int index = getIndex(username);
        if (index != -1)
            this.get(index).guesses = 0;
    }

    public synchronized void reset(int guessLimit) {
        this.replaceAll((x) -> new TemporaryData(x.username, guessLimit));
    }

    public void print() {
        System.out.println("-----------------------------------------");
        for (TemporaryData x : this)
            System.out.printf(
                    "username: %s - isLogged: %s - isGuessed: %s - guesses: %s - word: %s\n",
                    x.username,
                    x.isLogged,
                    x.isGuessed,
                    x.guesses,
                    x.word);
        System.out.println("-----------------------------------------");
    }
}