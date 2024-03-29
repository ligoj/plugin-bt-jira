/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Common JIRA test utilities with more data required to validate and get some statistics..
 */
public abstract class AbstractJiraData3Test extends AbstractJiraDataTest {

	/**
	 * Initialize database with 'MDA' JIRA project.
	 * @throws SQLException When SQL execution failed.
	 */
	@BeforeAll
	public static void initializeJiraDataBase3() throws SQLException {
		try(final var connection = datasource.getConnection()) {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-3/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-3/jira.sql"), StandardCharsets.UTF_8));
		}
	}

	/**
	 * Clean database with 'MDA' JIRA project.
	 * @throws SQLException When SQL execution failed.
	 */
	@AfterAll
	public static void cleanJiraDataBase3() throws SQLException {
		try(final var connection = datasource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-3/jira-drop.sql"), StandardCharsets.UTF_8));
		}
	}
}
