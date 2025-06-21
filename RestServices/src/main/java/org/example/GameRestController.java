package org.example;

import org.example.Game;
import org.example.Player;
import org.example.jdbc.GameHibernateRepository;
import org.example.jdbc.PlayerHibernateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/games")
public class GameRestController {
    @Autowired private GameRepo gameRepo;
    @Autowired private PlayerRepo playerRepo;

    // Service 1: listă jocuri finalizate pentru un anumit jucător
    @GetMapping("/player/{alias}")
    public ResponseEntity<List<Game>> getGamesByPlayer(@PathVariable String alias) {
        Player player = playerRepo.findByName(alias);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        List<Game> games = gameRepo.findByPlayer(player);
        return ResponseEntity.ok(games);
    }

    // Service 2: adăugare o nouă mutare propusă pentru un joc dat
    @PutMapping("/{gameId}")
    public ResponseEntity<Game> addMoveToGame(@PathVariable Long gameId, @RequestBody Map<String, Integer> payload) {
        // payload-ul JSON se așteaptă de forma {"row": X, "col": Y}
        Integer row = payload.get("row");
        Integer col = payload.get("col");
        if (row == null || col == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            gameRepo.addMove(gameId, row, col);
        } catch(Exception e) {
            return ResponseEntity.notFound().build();
        }
        Game updatedGame = gameRepo.find(gameId);
        if (updatedGame == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedGame);
    }
}
