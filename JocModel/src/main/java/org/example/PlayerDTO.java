package org.example;

import java.io.Serializable;

// DTO for Player entity – used in JSON communication (contains no sensitive fields)
public class PlayerDTO implements Serializable {
    private Long id;
    private String name;

    public PlayerDTO() {}
    public PlayerDTO(String name) { this.name = name; }
    public PlayerDTO(Long id, String name) { this.id = id; this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
