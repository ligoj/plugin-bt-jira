package org.ligoj.app.plugin.bt.jira;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Common JIRA for upload test utilities.
 */
public abstract class AbstractJiraUploadTest extends AbstractJiraDataTest {

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeClass
	public static void initializeJiraDataBaseForImport() throws SQLException {
		final DataSource datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/upload/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/upload/jira.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterClass
	public static void cleanJiraDataBaseForImport() throws SQLException {
		final DataSource datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/upload/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

}