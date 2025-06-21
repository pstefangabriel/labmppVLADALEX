package org.example;

public interface IGameObserver {
    // Notifică clientul că trebuie actualizat clasamentul (ex. a fost adăugat un joc nou finalizat)
    void scoreboardUpdated() throws GameException;
}
