package org.example.jdbc;

import org.example.Player;
import org.example.PlayerRepo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

public class PlayerHibernateRepository implements PlayerRepo {
    private final SessionFactory sf = HibernateUtils.getSessionFactory();

    @Override
    public Player findByName(String name) {
        try (Session session = sf.openSession()) {
            Query<Player> q = session.createQuery(
                    "from Player p where p.name = :name", Player.class);
            q.setParameter("name", name);
            return q.uniqueResult();  // poate întoarce null dacă nu există
        }
    }

    @Override
    public Player save(Player player) {
        // Salvează (sau actualizează) un jucător în baza de date
        HibernateUtils.inTransaction(session -> {
            session.persist(player);
        });
        return player;
    }
}
