package com.academy;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Every test Spring context starts from an empty schema: drop everything, then re-run all migrations.
 * This also proves the Flyway scripts work on real PostgreSQL from scratch.
 */
@Configuration
@Profile("test")
public class CleanDatabaseConfig {

    @Bean
    FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
