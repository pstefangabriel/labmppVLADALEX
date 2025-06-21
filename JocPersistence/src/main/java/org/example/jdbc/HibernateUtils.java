package org.example.jdbc;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.example.Player;
import org.example.Game;
import org.example.Move;
import org.example.Hole;

public class HibernateUtils {
    private static SessionFactory sessionFactory;

    // Inițializează SessionFactory cu entitățile noastre adnotate
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = new Configuration()
                    .addAnnotatedClass(Player.class)
                    .addAnnotatedClass(Game.class)
                    .addAnnotatedClass(Move.class)
                    .addAnnotatedClass(Hole.class)
                    .buildSessionFactory();
        }
        return sessionFactory;
    }

    // Închide fabrica de sesiuni (la oprirea aplicatiei)
    public static void close() {
        if (sessionFactory != null) sessionFactory.close();
    }

    // Metodă utilitară: execută acțiuni într-o tranzacție Hibernate
    public static void inTransaction(TransactionalAction action) {
        Session session = getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            action.execute(session);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    // Interfață funcțională internă pentru lambda-uri tranzacționale
    @FunctionalInterface
    public interface TransactionalAction {
        void execute(Session session);
    }
}
