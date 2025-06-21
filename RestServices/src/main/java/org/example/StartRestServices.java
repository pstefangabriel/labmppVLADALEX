package org.example;

import org.example.jdbc.GameHibernateRepository;
import org.example.jdbc.PlayerHibernateRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

@SpringBootApplication(scanBasePackages = {"org.example"})
public class StartRestServices {
    public static void main(String[] args) {
        SpringApplication.run(StartRestServices.class, args);
    }

    @Bean
    public GameRepo gameRepo() {
        return new GameHibernateRepository();
    }

    @Bean
    public PlayerRepo playerRepo() {
        return new PlayerHibernateRepository();
    }
}
