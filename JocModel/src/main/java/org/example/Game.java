package org.example;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "games")
public class Game implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    @JsonIgnore  // în serializarea JSON (la serviciul REST) vom afișa doar aliasul, nu întregul obiect Player
    private Player player;

    @Column(name = "points")
    private int points;

    @Column(name = "duration_seconds")
    private int duration;  // durata jocului în secunde

    // Lista mutărilor efectuate de jucător în acest joc (pozițiile alese pe tablă)
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Move> moves = new ArrayList<>();

    // Lista gropilor generate în acest joc
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Hole> holes = new ArrayList<>();

    public Game() {}
    public Game(Player player, int points, int duration) {
        this.player = player;
        this.points = points;
        this.duration = duration;
    }

    // Metodă ajutătoare pentru a adăuga o mutare în listă
    public void addMove(Move move) {
        moves.add(move);
        move.setGame(this);
    }
    public void addHole(Hole hole) {
        holes.add(hole);
        hole.setGame(this);
    }

    // Getteri
    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public int getPoints() { return points; }
    public int getDuration() { return duration; }
    public List<Move> getMoves() { return moves; }
    public List<Hole> getHoles() { return holes; }

    public void setPlayer(Player player) { this.player = player; }
    public void setPoints(int points) { this.points = points; }
    public void setDuration(int duration) { this.duration = duration; }

    // Proprietate derivată pentru alias-ul jucătorului, folosită la serializarea JSON (REST)
    @Transient
    @JsonProperty("playerAlias")
    public String getPlayerAlias() {
        return player != null ? player.getName() : null;
    }

    @Override
    public String toString() {
        return "Game{" + "id=" + id + ", player=" + player.getName() +
                ", points=" + points + ", duration=" + duration + "s}";
    }
}
