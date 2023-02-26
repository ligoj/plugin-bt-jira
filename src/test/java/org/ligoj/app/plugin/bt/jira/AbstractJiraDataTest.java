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
 * Common JIRA test utilities.
 */
public abstract class AbstractJiraDataTest extends AbstractJiraTest {

	/**
	 * Initialize database with 'MDA' JIRA project.
	 */
	@BeforeAll
	static void initializeJiraDataBase2() throws SQLException {
		try (final var connection = datasource.getConnection()) {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-2/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira.sql"), StandardCharsets.UTF_8));
		}
	}

	/**
	 * Clean database with 'MDA' JIRA project.
	 */
	@AfterAll
	static void cleanJiraDataBase2() throws SQLException {
		try (final var connection = datasource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("sql/base-2/jira-drop.sql"), StandardCharsets.UTF_8));
		}
	}

}
