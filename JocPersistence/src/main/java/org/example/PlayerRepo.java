package org.example;

public interface PlayerRepo {
    Player findByName(String name);
    Player save(Player player);
}
