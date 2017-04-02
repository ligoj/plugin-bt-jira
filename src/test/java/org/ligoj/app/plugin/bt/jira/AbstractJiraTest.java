package org.ligoj.app.plugin.bt.jira;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDriver;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.ligoj.app.plugin.bt.jira.in.JiraImportStatusResource;
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
public abstract class AbstractJiraTest extends AbstractServerTest {

	@Autowired
	protected JiraDao dao;

	@Autowired
	protected BugTrackerConfigurationRepository repository;

	@Autowired
	protected ProjectRepository projectRepository;

	protected static DataSource datasource;

	@Autowired
	protected JiraImportStatusResource importStatusResource;

	@Autowired
	protected IamProvider iamProvider;

	protected int subscription;

	@Before
	public void prepareData() throws IOException {
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
	 * Return the subscription identifier of a project. Assumes there is only
	 * one subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 */
	protected int getSubscription(final String project, final String service) {
		return em.createQuery("SELECT id FROM Subscription s WHERE project.name = ?1 AND node.id LIKE CONCAT(?2,'%')",
				Integer.class).setParameter(1, project).setParameter(2, service).getSingleResult();
	}

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeClass
	public static void initializeJiraDataBase() throws SQLException {
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
	@AfterClass
	public static void cleanJiraDataBase() throws SQLException {
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/base-1/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

}
