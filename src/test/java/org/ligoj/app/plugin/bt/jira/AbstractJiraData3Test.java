package org.ligoj.app.plugin.bt.jira;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Common JIRA test utilities with more data required to validate and get some statistics..
 */
public abstract class AbstractJiraData3Test extends AbstractJiraDataTest {

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeClass
	public static void initializeJiraDataBase3() throws SQLException {
		final Connection connection = datasource.getConnection();
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-3/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-3/jira.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterClass
	public static void cleanJiraDataBase3() throws SQLException {
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-3/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}
}