package org.example;

import jakarta.persistence.*;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "holes")
public class Hole implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "game_id")
    @JsonIgnore
    private Game game;

    @Column(name = "row_index")
    private int row;
    @Column(name = "col_index")
    private int col;

    public Hole() {}
    public Hole(Game game, int row, int col) {
        this.game = game;
        this.row = row;
        this.col = col;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    public void setGame(Game game) { this.game = game; }
    public void setRow(int row) { this.row = row; }
    public void setCol(int col) { this.col = col; }

    @Override
    public String toString() {
        return "Hole{row=" + row + ", col=" + col + "}";
    }
}
