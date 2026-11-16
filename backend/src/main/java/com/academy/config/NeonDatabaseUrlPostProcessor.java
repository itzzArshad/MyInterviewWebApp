package com.academy.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets the app use Neon's connection strings exactly as Neon provides them
 * ({@code neon deploy} / {@code neon env pull} write them to .env.local; on Render they are env vars):
 *
 * <ul>
 *   <li>{@code DATABASE_URL} (pooled, {@code -pooler} host) → the app's DataSource (HikariCP)</li>
 *   <li>{@code DATABASE_URL_UNPOOLED} (direct) → Flyway migrations, which need a session-level connection.
 *       If absent, the direct URL is derived from the pooled one.</li>
 * </ul>
 *
 * Values already in JDBC form ({@code jdbc:postgresql://…} with DATABASE_USERNAME / DATABASE_PASSWORD) are left alone.
 * Runs after config files are loaded, and its values take precedence over them.
 */
public class NeonDatabaseUrlPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "neonDatabaseUrls";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String pooledRaw = env.getProperty("DATABASE_URL");
        if (!NeonDatabaseUrl.isLibpqUrl(pooledRaw)) return;

        NeonDatabaseUrl app = NeonDatabaseUrl.parse(pooledRaw);
        String directRaw = env.getProperty("DATABASE_URL_UNPOOLED");
        NeonDatabaseUrl migrations = NeonDatabaseUrl.isLibpqUrl(directRaw) ? NeonDatabaseUrl.parse(directRaw) : app.direct();

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", app.jdbcUrl());
        putIfPresent(props, "spring.datasource.username", app.username());
        putIfPresent(props, "spring.datasource.password", app.password());
        props.put("spring.flyway.url", migrations.jdbcUrl());
        putIfPresent(props, "spring.flyway.user", migrations.username());
        putIfPresent(props, "spring.flyway.password", migrations.password());
        env.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
    }

    private static void putIfPresent(Map<String, Object> props, String key, String value) {
        if (value != null) props.put(key, value);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // after ConfigDataEnvironmentPostProcessor has loaded .env.local imports
    }
}
