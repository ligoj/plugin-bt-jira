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
 * Common JIRA test utilities.
 */
public abstract class AbstractJiraDataTest extends AbstractJiraTest {

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeClass
	public static void initializeJiraDataBase2() throws SQLException {
		final Connection connection = datasource.getConnection();
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-2/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterClass
	public static void cleanJiraDataBase2() throws SQLException {
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

}