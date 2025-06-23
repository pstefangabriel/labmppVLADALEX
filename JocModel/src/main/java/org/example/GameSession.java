package org.example;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GameSession implements Serializable {
    boolean[][] holes;
    int nextRow;
    int currentPoints;
    Instant startTime;
    List<Move> moves;
    GameSession(boolean[][] holes) {
        this.holes = holes;
        this.nextRow = 1;
        this.currentPoints = 0;
        this.startTime = Instant.now();
        this.moves = new ArrayList<>();
    }
}