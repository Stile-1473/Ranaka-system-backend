package Ranaka.ranaka;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

/**
 * Normalizes platform-provided PostgreSQL URLs before Spring Boot creates the datasource.
 */
public final class DatabaseUrlConfig {

	private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
	private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
	private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";
	private static final String SPRING_DATASOURCE_URL_ENV = "SPRING_DATASOURCE_URL";
	private static final String SPRING_DATASOURCE_USERNAME_ENV = "SPRING_DATASOURCE_USERNAME";
	private static final String SPRING_DATASOURCE_PASSWORD_ENV = "SPRING_DATASOURCE_PASSWORD";
	private static final String DEFAULT_POSTGRES_PORT = "5432";

	private DatabaseUrlConfig() {
	}

	public static void configureFromEnvironment() {
		configureFromEnvironment(System.getenv(), System.getProperties());
	}

	static void configureFromEnvironment(Map<String, String> environment, Properties properties) {
		String datasourceUrl = firstPresent(environment.get(SPRING_DATASOURCE_URL_ENV), properties.getProperty(SPRING_DATASOURCE_URL));
		if (hasText(datasourceUrl)) {
			configureDatasourceUrl(datasourceUrl, environment, properties);
			return;
		}

		String databaseUrl = firstPresent(environment.get("DATABASE_URL"), environment.get("POSTGRES_URL"),
				environment.get("POSTGRESQL_URL"), environment.get("JDBC_DATABASE_URL"));
		if (hasText(databaseUrl)) {
			configureDatasourceUrl(databaseUrl, environment, properties);
			return;
		}

		DatabaseConnectionProperties renderConnectionProperties = fromRenderDatabaseEnvironment(environment);
		if (renderConnectionProperties != null) {
			applyDatasourceProperties(renderConnectionProperties, environment, properties);
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
			throw new IllegalArgumentException("Unsupported PostgreSQL URL protocol. Expected postgres://, postgresql://, or jdbc:postgresql://.");
		}

		URI uri = URI.create(databaseUrl);
		StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
		if (uri.getPort() > 0) {
			jdbcUrl.append(':').append(uri.getPort());
		}
		jdbcUrl.append(uri.getRawPath());
		if (hasText(uri.getRawQuery())) {
			jdbcUrl.append('?').append(uri.getRawQuery());
		}

		String username = null;
		String password = null;
		if (hasText(uri.getUserInfo())) {
			String[] userInfoParts = uri.getUserInfo().split(":", 2);
			username = userInfoParts[0];
			if (userInfoParts.length > 1) {
				password = userInfoParts[1];
			}
		}

		return new DatabaseConnectionProperties(jdbcUrl.toString(), username, password);
	}

	private static void configureDatasourceUrl(String databaseUrl, Map<String, String> environment, Properties properties) {
		if (!isPostgresUrl(databaseUrl)) {
			return;
		}

		applyDatasourceProperties(fromDatabaseUrl(databaseUrl), environment, properties);
	}

	private static void applyDatasourceProperties(DatabaseConnectionProperties connectionProperties,
			Map<String, String> environment, Properties properties) {
		properties.setProperty(SPRING_DATASOURCE_URL, connectionProperties.jdbcUrl());

		if (hasText(connectionProperties.username()) && !hasText(environment.get(SPRING_DATASOURCE_USERNAME_ENV))
				&& !hasText(properties.getProperty(SPRING_DATASOURCE_USERNAME))) {
			properties.setProperty(SPRING_DATASOURCE_USERNAME, connectionProperties.username());
		}

		if (connectionProperties.password() != null && !hasText(environment.get(SPRING_DATASOURCE_PASSWORD_ENV))
				&& !hasText(properties.getProperty(SPRING_DATASOURCE_PASSWORD))) {
			properties.setProperty(SPRING_DATASOURCE_PASSWORD, connectionProperties.password());
		}
	}

	private static DatabaseConnectionProperties fromRenderDatabaseEnvironment(Map<String, String> environment) {
		String host = environment.get("DB_HOST");
		String databaseName = environment.get("DB_NAME");
		if (!hasText(host) || !hasText(databaseName)) {
			return null;
		}

		String port = firstPresent(environment.get("DB_PORT"), DEFAULT_POSTGRES_PORT);
		String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
		return new DatabaseConnectionProperties(jdbcUrl, environment.get("DB_USERNAME"), environment.get("DB_PASSWORD"));
	}

	private static boolean isPostgresUrl(String databaseUrl) {
		return databaseUrl.startsWith("jdbc:postgresql://") || databaseUrl.startsWith("postgresql://")
				|| databaseUrl.startsWith("postgres://");
	}

	private static String firstPresent(String... values) {
		for (String value : values) {
			if (hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	record DatabaseConnectionProperties(String jdbcUrl, String username, String password) {
	}
}
