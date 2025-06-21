package org.example.jsonprotocol;

import org.example.PlayerDTO;
import com.google.gson.annotations.SerializedName;

public class Request {
    private RequestType type;
    // Câmpuri posibile ale cererii (numai unul-două sunt folosite în funcție de tip):
    private PlayerDTO player;
    //private String password;
    private Integer row;
    private Integer col;

    // Constructori statici în JsonProtocolUtils vor popula aceste câmpuri corespunzător

    public RequestType getType() { return type; }
    public PlayerDTO getPlayer() { return player; }
    //public String getPassword() { return password; }
    public Integer getRow() { return row; }
    public Integer getCol() { return col; }

    public void setType(RequestType type) { this.type = type; }
    public void setPlayer(PlayerDTO player) { this.player = player; }
    //public void setPassword(String password) { this.password = password; }
    public void setRow(Integer row) { this.row = row; }
    public void setCol(Integer col) { this.col = col; }
}
