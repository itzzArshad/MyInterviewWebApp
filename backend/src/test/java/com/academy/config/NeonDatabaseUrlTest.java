package com.academy.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NeonDatabaseUrlTest {

    // Fake values in Neon's format — not a real database.
    static final String POOLED = "\"postgresql://app_user:p%40ss@ep-cool-name-123-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require\"";
    static final String DIRECT = "postgresql://app_user:p%40ss@ep-cool-name-123.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";

    @Test
    void convertsPooledUrlForJdbcAndPgBouncer() {
        NeonDatabaseUrl u = NeonDatabaseUrl.parse(POOLED);
        assertThat(u.pooled()).isTrue();
        assertThat(u.username()).isEqualTo("app_user");
        assertThat(u.password()).isEqualTo("p@ss");                    // percent-decoded
        assertThat(u.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-cool-name-123-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb"
                        + "?sslmode=require&channelBinding=require&prepareThreshold=0")
                .doesNotContain("p@ss").doesNotContain("app_user");
        assertThat(u.toString()).doesNotContain("p@ss");
    }

    @Test
    void derivesDirectUrlFromPooled() {
        NeonDatabaseUrl d = NeonDatabaseUrl.parse(POOLED).direct();
        assertThat(d.pooled()).isFalse();
        assertThat(d.jdbcUrl()).isEqualTo(NeonDatabaseUrl.parse(DIRECT).jdbcUrl())
                .doesNotContain("-pooler").doesNotContain("prepareThreshold");
    }

    @Test
    void postProcessorWiresDataSourceAndFlyway() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("test", Map.of(
                "DATABASE_URL", POOLED, "DATABASE_URL_UNPOOLED", DIRECT,
                "spring.datasource.url", "jdbc:postgresql://localhost/academy")));
        new NeonDatabaseUrlPostProcessor().postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).contains("-pooler").contains("prepareThreshold=0");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("app_user");
        assertThat(env.getProperty("spring.flyway.url")).doesNotContain("-pooler");
        assertThat(env.getProperty("spring.flyway.password")).isEqualTo("p@ss");
    }

    @Test
    void leavesJdbcStyleConfigurationAlone() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new MapPropertySource("test", Map.of("DATABASE_URL", "jdbc:postgresql://h/db")));
        new NeonDatabaseUrlPostProcessor().postProcessEnvironment(env, null);
        assertThat(env.getPropertySources().contains(NeonDatabaseUrlPostProcessor.SOURCE_NAME)).isFalse();
    }
}
