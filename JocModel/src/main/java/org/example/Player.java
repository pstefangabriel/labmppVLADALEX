package org.example;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "players")
public class Player implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "alias", unique = true, nullable = false)
    private String name;  // aliasul jucătorului

    public Player() {}  // constructor fără param. necesar pentru JPA

    public Player(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Player{" + "id=" + id + ", name='" + name + "'}";
    }
}
