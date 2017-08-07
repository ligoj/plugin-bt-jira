package org.ligoj.app.plugin.bt.jira;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.apache.http.HttpStatus;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.iam.Activity;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.bt.jira.dao.ImportStatusRepository;
import org.ligoj.app.resource.node.ParameterValueResource;
import org.ligoj.app.resource.plugin.CurlProcessor;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.core.IDescribableBean;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.sf.ehcache.CacheManager;

/**
 * Test class of {@link JiraPluginResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JiraPluginResourceTest extends AbstractJiraData3Test {

	@Autowired
	private JiraPluginResource resource;

	@Autowired
	private ParameterValueResource pvResource;

	@Autowired
	protected ImportStatusRepository importStatusRepository;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private SubscriptionResource subscriptionResource;

	@Before
	public void prepareSubscription() {
		persistSystemEntities();
		this.subscription = getSubscription("MDA");

		// Clear the cache
		pvResource.getNodeParameters("service:bt:jira:6").remove(JiraBaseResource.PARAMETER_CACHE_VERSION);

		// For coverage
		resource.getKey();
	}

	@Test
	public void getJiraVersion() {
		final DataSource datasource = resource.getDataSource(subscription);
		Assert.assertEquals("4.4.1", dao.getJiraVersion(datasource));
	}

	@Test
	public void deleteTask() throws Exception {
		final long initCount = importStatusRepository.count();
		em.clear();
		resource.deleteTask(subscription);
		em.flush();
		em.clear();
		Assert.assertEquals(initCount - 1, importStatusRepository.count());
		Assert.assertNull(resource.getTask(subscription));
	}

	@Test
	public void validateDataBaseConnectivity() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		final String version = resource.validateDataBaseConnectivity(parameters);
		Assert.assertEquals("4.4.1", version);
	}

	@Test
	public void getVersion() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		final String version = resource.getVersion(parameters);
		Assert.assertEquals("4.4.1", version);
	}

	@Test
	public void checkSubscriptionStatus() throws Exception {
		final SubscriptionStatusWithData checkSubscriptionStatus = resource.checkSubscriptionStatus(null,
				subscriptionResource.getParametersNoCheck(subscription));
		Assert.assertNotNull(checkSubscriptionStatus);
		Assert.assertTrue(checkSubscriptionStatus.getStatus().isUp());
		final JiraProject project = (JiraProject) checkSubscriptionStatus.getData().get("project");
		Assert.assertNotNull(project);
		Assert.assertEquals("MDA", project.getName());
		Assert.assertEquals(1, project.getPriorities().size());
		Assert.assertEquals(2, project.getPriorities().get("Minor").intValue());
		Assert.assertEquals(1, project.getStatuses().size());
		Assert.assertEquals(2, project.getStatuses().get("Open").intValue());
	}

	@Test
	public void checkStatusNoAdmin() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		Assert.assertTrue(resource.checkStatus(null, parameters));
	}

	@Test
	public void checkStatusWithAdmin() throws Exception {
		prepareJiraServer();
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(JiraBaseResource.PARAMETER_URL, "http://localhost:" + MOCK_PORT);
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "one");
		addJdbcParameter(parameters);
		Assert.assertTrue(resource.checkStatus(null, parameters));
	}

	@Test
	public void checkStatusWithAdminFailed() throws Exception {
		prepareJiraServer();
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(JiraBaseResource.PARAMETER_URL, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "one");
		addJdbcParameter(parameters);
		Assert.assertFalse(resource.checkStatus(null, parameters));
	}

	@Test
	public void getLastVersion() throws Exception {
		final String lastVersion = resource.getLastVersion();
		Assert.assertNotNull(lastVersion);
		Assert.assertTrue(new DefaultArtifactVersion(lastVersion).compareTo(new DefaultArtifactVersion("6.3.3")) >= 0);
	}

	@Test
	public void validateDataBaseConnectivityRes() throws Exception {
		final String version = resource.validateDataBaseConnectivity(pvResource.getNodeParameters("service:bt:jira:6"));
		Assert.assertEquals("4.4.1", version);
	}

	@Test(expected = TechnicalException.class)
	public void validateDataBaseConnectivityFailed() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		parameters.put(JiraBaseResource.PARAMETER_JDBC_DRIVER, "org.hsqldb.jdbc.JDBCDriverAny");
		resource.validateDataBaseConnectivity(parameters);
	}

	@Test
	public void validateProject() {
		checkProjectValidation(resource.validateProject(getProjectValidationParameters()));
	}

	@Test
	public void validateProjectRes() {
		final Map<String, String> parameters = pvResource.getNodeParameters("service:bt:jira:6");
		parameters.put(JiraBaseResource.PARAMETER_PKEY, "MDA");
		parameters.put(JiraBaseResource.PARAMETER_PROJECT, "10074");
		checkProjectValidation(resource.validateProject(parameters));
	}

	private void checkProjectValidation(final IDescribableBean<Integer> project) {
		Assert.assertEquals("MDA", project.getName());
		Assert.assertNotNull(project.getId());
		// HSQLDB issue for conversions....
		// Assert.assertEquals(10074, project.getId().intValue());
		Assert.assertEquals("MDA", project.getDescription());
	}

	private Map<String, String> getProjectValidationParameters() {
		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		parameters.put(JiraBaseResource.PARAMETER_PKEY, "MDA");
		parameters.put(JiraBaseResource.PARAMETER_PROJECT, "10074");
		return parameters;
	}

	@Test
	public void findProjectsByNameNotExists() {
		Assert.assertEquals(0, resource.findAllByName("service:bt:jira:any", "10000").size());
	}

	@Test
	public void findProjectsByName() {
		assertGstack(resource.findAllByName("service:bt:jira:6", "10000"));
		assertGstack(resource.findAllByName("service:bt:jira:6", "gStack"));
		assertGstack(resource.findAllByName("service:bt:jira:6", "GSTACK"));
	}

	@Test
	public void getActivitiesNoUser() throws Exception {
		final Collection<String> users = new ArrayList<>();
		final Map<String, Activity> activities = resource.getActivities(subscription, users);
		Assert.assertTrue(activities.isEmpty());
	}

	@Test
	public void getActivities() throws Exception {
		final Collection<String> users = new ArrayList<>();
		users.add("fdaugan");
		users.add("alocquet");
		users.add("any");
		final Map<String, Activity> activities = resource.getActivities(subscription, users);
		Assert.assertEquals(1, activities.size());
		Assert.assertTrue(activities.containsKey("fdaugan"));
		Assert.assertNotNull(activities.get("fdaugan").getLastConnection());
	}

	private void assertGstack(final List<JiraProject> projects) {
		Assert.assertEquals(1, projects.size());
		// HSQLDB issue for conversions....
		Assert.assertEquals(10000, projects.get(0).getId().intValue());
		Assert.assertEquals("GSTACK", projects.get(0).getName());
		Assert.assertEquals("gStack", projects.get(0).getDescription());
	}

	private void addJdbcParameter(final Map<String, String> parameters) {
		parameters.put(JiraBaseResource.PARAMETER_JDBC_URL, "jdbc:hsqldb:mem:dataSource");
		parameters.put(JiraBaseResource.PARAMETER_JDBC_DRIVER, "org.hsqldb.jdbc.JDBCDriver");
		parameters.put(JiraBaseResource.PARAMETER_JDBC_USER, null);
		parameters.put(JiraBaseResource.PARAMETER_JDBC_PASSSWORD, null);
	}

	@Test
	public void validateProjectFailed() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher(JiraBaseResource.PARAMETER_PKEY, "jira-project"));

		final Map<String, String> parameters = new HashMap<>();
		addJdbcParameter(parameters);
		parameters.put(JiraBaseResource.PARAMETER_PKEY, "MDA");
		parameters.put(JiraBaseResource.PARAMETER_PROJECT, "100740");
		resource.validateProject(parameters);
	}

	@Test
	public void validateAdminConnectivity() throws Exception {
		prepareJiraServer();
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(JiraBaseResource.PARAMETER_URL, "http://localhost:" + MOCK_PORT);
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "one");
		parameters.put(JiraBaseResource.PARAMETER_CACHE_VERSION, "4");
		Assert.assertTrue(resource.validateAdminConnectivity(parameters));
	}

	@Test
	public void validateAdminConnectivityRes() throws Exception {
		prepareJiraServer();
		final Map<String, String> parameters = pvResource.getNodeParameters("service:bt:jira:6");
		parameters.put(JiraBaseResource.PARAMETER_URL, "http://localhost:" + MOCK_PORT);
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "one");
		parameters.put(JiraBaseResource.PARAMETER_CACHE_VERSION, "4");

		final JiraPluginResource jiraPluginResource = new JiraPluginResource();
		jiraPluginResource.subscriptionResource = subscriptionResource;
		Assert.assertTrue(jiraPluginResource.validateAdminConnectivity(parameters));
	}

	private void prepareJiraServer() {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		httpServer.stubFor(post(urlPathEqualTo("/login.jsp"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/WebSudoAuthenticate.jspa")).willReturn(
				aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/secure/project/ViewProjects.jspa")
						.withHeader("X-Atlassian-WebSudo", "Has-Authentication")));
		httpServer.start();
	}

	@Test
	public void validateAdminConnectivityFailed() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(JiraBaseResource.PARAMETER_URL, "any:dummy");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "");
		parameters.put(JiraBaseResource.PARAMETER_CACHE_VERSION, "4");
		Assert.assertFalse(resource.validateAdminConnectivity(parameters));
	}

	@Test
	public void validateAdminConnectivityAdminJira3() throws Exception {
		prepareJiraServer();
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(JiraBaseResource.PARAMETER_URL, "http://localhost:" + MOCK_PORT);
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_PASSWORD, "any");
		parameters.put(JiraBaseResource.PARAMETER_ADMIN_USER, "one");
		parameters.put(JiraBaseResource.PARAMETER_CACHE_VERSION, "3");
		Assert.assertTrue(resource.validateAdminConnectivity(parameters));
	}

	@Test(expected = IllegalStateException.class)
	public void validateAdminConnectivityFailedClose() throws Exception {
		Assert.assertFalse(new JiraPluginResource() {

			@Override
			protected boolean authenticateAdmin(final Map<String, String> parameters, final CurlProcessor processor) {
				throw new IllegalStateException();
			}

		}.validateAdminConnectivity(null));
	}

	@Test(expected = CannotGetJdbcConnectionException.class)
	public void linkInvalidDatabase() throws Exception {

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);

		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected(JiraBaseResource.KEY));
		em.persist(subscription);
		final ParameterValue parameterValueEntity = new ParameterValue();
		parameterValueEntity.setParameter(parameterRepository.findOne(JiraBaseResource.PARAMETER_JDBC_URL));
		parameterValueEntity.setData("jdbc:hsqldb:mem:dataSource");
		parameterValueEntity.setSubscription(subscription);
		em.persist(parameterValueEntity);
		final ParameterValue parameterValueEntity2 = new ParameterValue();
		parameterValueEntity2.setParameter(parameterRepository.findOneExpected(JiraBaseResource.PARAMETER_JDBC_DRIVER));
		parameterValueEntity2.setData("org.hsqldb.jdbc.JDBCDriver");
		parameterValueEntity2.setSubscription(subscription);
		em.persist(parameterValueEntity2);
		final ParameterValue parameterValueEntity3 = new ParameterValue();
		parameterValueEntity3.setParameter(parameterRepository.findOneExpected(JiraBaseResource.PARAMETER_JDBC_PASSSWORD));
		parameterValueEntity3.setData("invalid");
		parameterValueEntity3.setSubscription(subscription);
		em.persist(parameterValueEntity3);
		em.flush();
		em.clear();

		resource.link(subscription.getId());
	}

	@Test
	public void linkInvalidAdmin() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher(JiraBaseResource.PARAMETER_ADMIN_USER, "jira-admin"));

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt:jira:6"));
		em.persist(subscription);
		addProjectParameters(subscription, 10074);

		// Set an invalid URL
		em.createQuery("UPDATE ParameterValue v SET v.data = ?3 WHERE v.node.id = ?1 AND v.parameter.id = ?2")
				.setParameter(1, JiraBaseResource.PARAMETER_URL).setParameter(2, "service:bt:jira:6").setParameter(3, "any")
				.executeUpdate();
		em.flush();
		em.clear();
		CacheManager.getInstance().getCache("subscription-parameters").removeAll();
		try {
			resource.link(subscription.getId());
		} finally {
			CacheManager.getInstance().getCache("subscription-parameters").removeAll();
		}
	}

	@Test
	public void linkInvalidProject() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher(JiraBaseResource.PARAMETER_PKEY, "jira-project"));

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt:jira:6"));
		em.persist(subscription);
		addProjectParameters(subscription, 11111);
		em.flush();
		em.clear();
		resource.link(subscription.getId());
	}

	@Test
	public void linkNotAdmin() throws Exception {
		final long initCount = importStatusRepository.count();
		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt:jira:6"));
		em.persist(subscription);
		addProjectParameters(subscription, 10074);
		em.createQuery("DELETE ParameterValue v WHERE v.node.id = ?1 AND v.parameter.id = ?2")
				.setParameter(2, JiraBaseResource.PARAMETER_ADMIN_USER).setParameter(1, "service:bt:jira:6").executeUpdate();
		em.flush();
		em.clear();
		resource.link(subscription.getId());
		em.flush();
		Assert.assertEquals(initCount, importStatusRepository.count());
		Assert.assertNull(resource.getTask(subscription.getId()));
	}

	@Test
	public void link() throws Exception {
		prepareJiraServer();

		final long initCount = importStatusRepository.count();
		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt:jira:6"));
		em.persist(subscription);
		addProjectParameters(subscription, 10074);
		em.flush();
		em.clear();
		resource.link(subscription.getId());
		em.flush();
		Assert.assertEquals(initCount, importStatusRepository.count());
		Assert.assertNull(resource.getTask(subscription.getId()));
	}

	private void addProjectParameters(final Subscription subscription, final int jira) {
		final ParameterValue parameterValueEntity = new ParameterValue();
		parameterValueEntity.setParameter(parameterRepository.findOneExpected(JiraBaseResource.PARAMETER_PKEY));
		parameterValueEntity.setData("MDA");
		parameterValueEntity.setSubscription(subscription);
		em.persist(parameterValueEntity);
		final ParameterValue parameterValueEntity2 = new ParameterValue();
		parameterValueEntity2.setParameter(parameterRepository.findOneExpected(JiraBaseResource.PARAMETER_PROJECT));
		parameterValueEntity2.setData(String.valueOf(jira));
		parameterValueEntity2.setSubscription(subscription);
		em.persist(parameterValueEntity2);
	}

	@Test(expected = IllegalStateException.class)
	public void create() throws Exception {
		resource.create(0);
	}

	@Test
	public void clearLoginFailedNoUser() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
		try {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
			dao.clearLoginFailed(datasource, "any");
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.1", 10075);
		}

		// Untouched counter
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
	}

	@Test
	public void clearLoginFailedOtherUser() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
		try {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
			dao.clearLoginFailed(datasource, "alocquet");
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.1", 10075);
		}

		// Untouched counter
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
	}

	/**
	 * JIRA4 CAPTCHA reset is not supported
	 */
	@Test
	public void clearLoginFailedJira4() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
		dao.clearLoginFailed(datasource, "fdaugan");
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));
	}

	@Test
	public void clearLoginFailed() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assert.assertEquals("1", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));

		try {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
			// Updated counter
			dao.clearLoginFailed(datasource, "fdaugan");
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.1", 10075);
		}
		Assert.assertEquals("0", jdbcTemplate.queryForObject("SELECT attribute_value FROM cwd_user_attributes WHERE ID=212", String.class));

		// Restore the value
		jdbcTemplate.update("UPDATE cwd_user_attributes SET attribute_value=1, lower_attribute_value=1 WHERE ID=212");
	}

	@Test
	public void getInstalledEntities() {
		Assert.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}


	@Override
	protected String getAuthenticationName() {
		return DEFAULT_USER;
	}
}
