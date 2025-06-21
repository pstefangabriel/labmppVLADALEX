package org.example.jdbc;

import org.example.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.List;

public class GameHibernateRepository implements GameRepo {
    private final SessionFactory sf = HibernateUtils.getSessionFactory();

    @Override
    public Game add(Game game) {
        // Salvează jocul (și cascadează salvarea mutărilor și gropilor atașate)
        HibernateUtils.inTransaction(session -> {
            session.persist(game);
        });
        return game;
    }

    @Override
    public Game find(Long id) {
        try (Session session = sf.openSession()) {
            return session.find(Game.class, id);
        }
    }

    @Override
    public List<Game> findByPlayer(Player player) {
        try (Session session = sf.openSession()) {
            // selectează toate jocurile jucătorului dat, cu mutările și gropile încărcate (FetchType.EAGER)
            String hql = "from Game g where g.player = :player order by g.id";
            return session.createQuery(hql, Game.class)
                    .setParameter("player", player)
                    .getResultList();
        }
    }

    @Override
    public List<Game> findAllByScore() {
        try (Session session = sf.openSession()) {
            // toate jocurile ordonate descendent după puncte, iar la egalitate de puncte ascendent după durată
            String hql = "from Game g order by g.points DESC, g.duration ASC";
            return session.createQuery(hql, Game.class).getResultList();
        }
    }

    @Override
    public long getRankForGame(Game game) {
        try (Session session = sf.openSession()) {
            // Numără câte jocuri au scor mai mare, sau același scor dar durată mai mică (mai rapid) decât jocul dat
            Long betterCount = session.createQuery(
                            "select count(g) from Game g where g.points > :p or (g.points = :p and g.duration < :d)",
                            Long.class)
                    .setParameter("p", game.getPoints())
                    .setParameter("d", game.getDuration())
                    .uniqueResult();
            long rank = (betterCount != null ? betterCount : 0) + 1;
            return rank;
        }
    }

    @Override
    public void addMove(Long gameId, int row, int col) {
        // Adaugă o nouă mutare (pozitie propusă) pentru jocul cu id-ul specificat
        HibernateUtils.inTransaction(session -> {
            Game game = session.find(Game.class, gameId);
            if (game == null) {
                throw new RuntimeException("Game not found");
            }
            Move move = new Move(game, row, col);
            session.persist(move);
            // *Notă:* Nu modificăm punctajul sau rezultatul jocului, deoarece adăugarea prin REST
            // este considerată doar o înregistrare suplimentară (jocul fiind deja finalizat).
        });
    }
}
