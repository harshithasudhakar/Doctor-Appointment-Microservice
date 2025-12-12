package com.example.appointments.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration for the 'postgres' profile to make Render Blueprints easier.
 * It constructs a JDBC URL from DATABASE_URL (postgres://...) or reads spring.datasource.url/SPRING_DATASOURCE_URL.
 * Username/password are taken from spring.datasource.username/SPRING_DATASOURCE_USERNAME or DB_USER/DB_PASSWORD.
 */
@Configuration
@Profile("postgres")
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        // Read both Spring-style keys and env-style keys to be compatible in tests and production
        String jdbcUrl = firstNonBlank(
                env.getProperty("spring.datasource.url"),
                env.getProperty("SPRING_DATASOURCE_URL")
        );

        String databaseUrl = firstNonBlank(
                env.getProperty("DATABASE_URL"),
                env.getProperty("database.url")
        );

        String user = firstNonBlank(
                env.getProperty("spring.datasource.username"),
                env.getProperty("SPRING_DATASOURCE_USERNAME"),
                env.getProperty("DB_USER"),
                env.getProperty("DATABASE_USER"),
                env.getProperty("POSTGRES_USER")
        );

        String password = firstNonBlank(
                env.getProperty("spring.datasource.password"),
                env.getProperty("SPRING_DATASOURCE_PASSWORD"),
                env.getProperty("DB_PASSWORD"),
                env.getProperty("DATABASE_PASSWORD"),
                env.getProperty("POSTGRES_PASSWORD")
        );

        if ((jdbcUrl == null || jdbcUrl.isBlank()) && databaseUrl != null && !databaseUrl.isBlank()) {
            // Transform postgres://host:port/db to jdbc:postgresql://host:port/db
            jdbcUrl = databaseUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
        }

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("No datasource URL configured. Provide spring.datasource.url or DATABASE_URL.");
        }
        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("No datasource credentials configured. Provide spring.datasource.username/password or DB_USER/DB_PASSWORD.");
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(user);
        ds.setPassword(password);
        return ds;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
