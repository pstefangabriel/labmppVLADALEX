package org.example;

public interface PlayerRepo {
    Player findByName(String name);
    Player save(Player player);
    Player find(Long id);  // Added: find player by unique ID
}
