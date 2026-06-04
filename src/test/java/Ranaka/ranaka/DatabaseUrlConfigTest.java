package Ranaka.ranaka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class DatabaseUrlConfigTest {

	@Test
	void convertsRenderPostgresUrlToSpringDatasourceProperties() {
		Properties properties = new Properties();

		DatabaseUrlConfig.configureFromEnvironment(Map.of(
				"DATABASE_URL", "postgresql://ranaka_db_user:secret%20pass@dpg-d8goc1rtqb8s73br5590-a:5432/ranaka_db"), properties);

		assertThat(properties.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://dpg-d8goc1rtqb8s73br5590-a:5432/ranaka_db");
		assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("ranaka_db_user");
		assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("secret pass");
	}

	@Test
	void leavesExplicitSpringDatasourceUrlUntouched() {
		Properties properties = new Properties();
		properties.setProperty("spring.datasource.url", "jdbc:postgresql://explicit-host:5432/explicit_db");

		DatabaseUrlConfig.configureFromEnvironment(Map.of(
				"DATABASE_URL", "postgresql://ranaka_db_user:secret@dpg-d8goc1rtqb8s73br5590-a:5432/ranaka_db"), properties);

		assertThat(properties.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://explicit-host:5432/explicit_db");
		assertThat(properties.getProperty("spring.datasource.username")).isNull();
	}
}
