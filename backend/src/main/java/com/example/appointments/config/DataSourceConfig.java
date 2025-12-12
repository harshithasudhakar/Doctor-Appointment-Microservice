package com.example.appointments.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration for the 'postgres' profile to make Render Blueprints easier.
 * It constructs a JDBC URL from DATABASE_URL (postgres://...), or falls back to SPRING_DATASOURCE_URL if provided.
 * Username/password are taken from DB_USER/DB_PASSWORD or SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD.
 */
@Configuration
@Profile("postgres")
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        String jdbcUrl = env.getProperty("SPRING_DATASOURCE_URL");
        String databaseUrl = env.getProperty("DATABASE_URL");
        String user = firstNonBlank(
                env.getProperty("SPRING_DATASOURCE_USERNAME"),
                env.getProperty("DB_USER"),
                env.getProperty("DATABASE_USER"),
                env.getProperty("POSTGRES_USER")
        );
        String password = firstNonBlank(
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
            throw new IllegalStateException("No datasource URL configured. Provide SPRING_DATASOURCE_URL or DATABASE_URL.");
        }
        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("No datasource credentials configured. Provide DB_USER/DB_PASSWORD or SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD.");
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
