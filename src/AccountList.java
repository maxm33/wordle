package src;

import java.util.ArrayList;

class Account {
    public int currentWinStreak = 0, maxWinStreak = 0, numberOfMatches = 0, numberOfWins = 0;
    public float averageTries = 0;
    public String username, password;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

public class AccountList extends ArrayList<Account> {

    public boolean isRegistered(String username, String password) {
        for (Account x : this)
            if (x.username.contentEquals(username) && x.password.contentEquals(password))
                return true;
        return false;
    }

    public int getIndex(String username) {
        for (int i = 0; i < this.size(); i++)
            if (this.get(i).username.contentEquals(username))
                return i;
        return -1;
    }

    public Account getAccount(String username) {
        for (Account x : this)
            if (x.username.contentEquals(username))
                return x;
        return null;
    }

    public synchronized void hasWon(String username, int tries) {
        int index = getIndex(username);
        if (index != -1) {
            Account x = this.get(index);
            x.currentWinStreak++;
            x.numberOfMatches++;
            if (x.currentWinStreak >= x.maxWinStreak)
                x.maxWinStreak = x.currentWinStreak;
            if (x.numberOfWins != 0) {
                x.averageTries = ((x.averageTries * x.numberOfWins) + tries) / (x.numberOfWins + 1);
                x.averageTries *= 100;
                x.averageTries = Math.round(x.averageTries);
                x.averageTries /= 100;
            } else
                x.averageTries = tries;
            x.numberOfWins++;
        }
    }

    public synchronized void hasLost(String username, int tries) {
        int index = getIndex(username);
        if (index != -1) {
            Account x = this.get(index);
            x.numberOfMatches++;
            x.currentWinStreak = 0;
            if (x.numberOfWins != 0) {
                x.averageTries = ((x.averageTries * x.numberOfWins) + tries) / (x.numberOfWins);
                x.averageTries *= 100;
                x.averageTries = Math.round(x.averageTries);
                x.averageTries /= 100;
            } else
                x.averageTries = tries;
        }
    }

    public void print() {
        System.out.println("*****************************************");
        for (Account x : this)
            System.out.printf(
                    "username: %s - matches: %s - wins: %s - curWS: %s - maxWS: %s - avgtries: %s\n",
                    x.username,
                    x.numberOfMatches,
                    x.numberOfWins,
                    x.currentWinStreak,
                    x.maxWinStreak,
                    x.averageTries);
        System.out.println("*****************************************");
    }
}