/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ligoj.app.AbstractServerTest;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.bt.BugTrackerResource;
import org.ligoj.app.plugin.bt.dao.BugTrackerConfigurationRepository;
import org.ligoj.app.plugin.bt.jira.dao.JiraDao;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.Calendar;
import org.ligoj.app.plugin.bt.model.Holiday;
import org.ligoj.app.plugin.bt.model.Sla;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Common JPA test utilities.
 */
abstract class AbstractJiraTest extends AbstractServerTest {

	@Autowired
	protected JiraDao dao;

	@Autowired
	protected BugTrackerConfigurationRepository repository;

	@Autowired
	protected ProjectRepository projectRepository;

	protected static DataSource datasource;

	@Autowired
	protected IamProvider iamProvider;

	protected int subscription;

	@BeforeEach
	void prepareData() throws IOException {
		// Only with Spring context
		if (csvForJpa != null) {
			persistEntities("csv",
					new Class[] { Calendar.class, Holiday.class, Node.class, Parameter.class, Project.class,
							Subscription.class, ParameterValue.class, BugTrackerConfiguration.class,
							BusinessHours.class, Sla.class, ImportStatus.class },
					StandardCharsets.UTF_8.name());
		}
	}

	/**
	 * Return the subscription identifier of a project. Assumes there is only one subscription for a service.
	 * 
	 * @param project The project key name.
	 * @return The first subscription identifier associated to the {@link BugTrackerResource#SERVICE_KEY} service for
	 *         the related project.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeAll
	static void initializeJiraDataBase() throws SQLException {
		datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-1/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-1/jira.sql"), StandardCharsets.UTF_8));
			jdbcTemplate.queryForList("SELECT * FROM pluginversion WHERE ID = 10075");
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterAll
	static void cleanJiraDataBase() throws SQLException {
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-1/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

}
