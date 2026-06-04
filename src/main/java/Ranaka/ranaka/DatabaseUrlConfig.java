package Ranaka.ranaka;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Normalizes platform-provided database URLs before Spring Boot creates the datasource.
 */
public final class DatabaseUrlConfig {

	private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
	private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
	private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";

	private DatabaseUrlConfig() {
	}

	public static void configureFromEnvironment() {
		configureFromEnvironment(System.getenv(), System.getProperties());
	}

	static void configureFromEnvironment(Map<String, String> environment, Properties properties) {
		if (hasText(environment.get("SPRING_DATASOURCE_URL")) || hasText(properties.getProperty(SPRING_DATASOURCE_URL))) {
			return;
		}

		String databaseUrl = firstPresent(environment, "DATABASE_URL", "POSTGRES_URL", "POSTGRESQL_URL", "JDBC_DATABASE_URL");
		if (!hasText(databaseUrl)) {
			return;
		}

		DatabaseConnectionProperties connectionProperties = fromDatabaseUrl(databaseUrl);
		properties.setProperty(SPRING_DATASOURCE_URL, connectionProperties.jdbcUrl());

		if (hasText(connectionProperties.username()) && !hasText(environment.get("SPRING_DATASOURCE_USERNAME"))
				&& !hasText(properties.getProperty(SPRING_DATASOURCE_USERNAME))) {
			properties.setProperty(SPRING_DATASOURCE_USERNAME, connectionProperties.username());
		}

		if (connectionProperties.password() != null && !hasText(environment.get("SPRING_DATASOURCE_PASSWORD"))
				&& !hasText(properties.getProperty(SPRING_DATASOURCE_PASSWORD))) {
			properties.setProperty(SPRING_DATASOURCE_PASSWORD, connectionProperties.password());
		}
	}

	static DatabaseConnectionProperties fromDatabaseUrl(String databaseUrl) {
		if (databaseUrl.startsWith("jdbc:postgresql://")) {
			return new DatabaseConnectionProperties(databaseUrl, null, null);
		}

		if (databaseUrl.startsWith("postgres://")) {
			databaseUrl = "postgresql://" + databaseUrl.substring("postgres://".length());
		}

		if (!databaseUrl.startsWith("postgresql://")) {
			throw new IllegalArgumentException("Unsupported DATABASE_URL protocol. Expected postgres://, postgresql://, or jdbc:postgresql://.");
		}

		URI uri = URI.create(databaseUrl);
		StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
		if (uri.getPort() > 0) {
			jdbcUrl.append(':').append(uri.getPort());
		}
		jdbcUrl.append(uri.getPath());
		if (hasText(uri.getQuery())) {
			jdbcUrl.append('?').append(uri.getQuery());
		}

		String username = null;
		String password = null;
		if (hasText(uri.getUserInfo())) {
			String[] userInfoParts = uri.getUserInfo().split(":", 2);
			username = urlDecode(userInfoParts[0]);
			if (userInfoParts.length > 1) {
				password = urlDecode(userInfoParts[1]);
			}
		}

		return new DatabaseConnectionProperties(jdbcUrl.toString(), username, password);
	}

	private static String firstPresent(Map<String, String> environment, String... names) {
		for (String name : names) {
			String value = environment.get(name);
			if (hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private static String urlDecode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	record DatabaseConnectionProperties(String jdbcUrl, String username, String password) {
	}
}
