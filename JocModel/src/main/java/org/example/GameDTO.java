package org.example;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;

// DTO pentru entitatea Game - folosit în comunicarea JSON a clasamentului și a rezultatului final al unui joc
public class GameDTO implements Serializable {
    private Long id;
    private String playerAlias;
    private int points;
    private int duration;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer rank;  // poziția în clasament (prezentă doar la final de joc pentru jucătorul curent)

    public GameDTO() {}
    public GameDTO(Long id, String playerAlias, int points, int duration, Integer rank) {
        this.id = id;
        this.playerAlias = playerAlias;
        this.points = points;
        this.duration = duration;
        this.rank = rank;
    }

    // Getteri și setteri
    public Long getId() { return id; }
    public String getPlayerAlias() { return playerAlias; }
    public int getPoints() { return points; }
    public int getDuration() { return duration; }
    public Integer getRank() { return rank; }

    public void setId(Long id) { this.id = id; }
    public void setPlayerAlias(String alias) { this.playerAlias = alias; }
    public void setPoints(int points) { this.points = points; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setRank(Integer rank) { this.rank = rank; }
}
