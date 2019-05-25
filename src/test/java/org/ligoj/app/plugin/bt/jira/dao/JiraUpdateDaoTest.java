/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.dao;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * {@link JiraUpdateDao} test class.
 */
public class JiraUpdateDaoTest {

	private static DataSource datasource;

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeAll
	public static void initializeJiraDataBaseForImport() throws SQLException {
		datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-1/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-2/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/upload/jira-create.sql"), StandardCharsets.UTF_8));

			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-1/jira.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/upload/jira.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterAll
	public static void cleanJiraDataBaseForImport() throws SQLException {
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-1/jira-drop.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira-drop.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/upload/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	@Test
	public void testPrepareForNextIdConcurrent() {
		final JiraUpdateDao dao = new JiraUpdateDao() {

			private boolean mock = true;

			@Override
			protected int getCurrentSequence(final String sequenceName, final JdbcOperations jdbcTemplate) {
				if (mock) {
					mock = false;
					return -10000;
				}
				return super.getCurrentSequence(sequenceName, jdbcTemplate);
			}

		};
		Assertions.assertEquals(10100, dao.prepareForNextId(datasource, "ChangeGroup", 2000));
	}

}
