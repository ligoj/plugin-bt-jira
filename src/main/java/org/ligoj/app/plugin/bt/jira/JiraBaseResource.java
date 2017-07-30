package org.ligoj.app.plugin.bt.jira;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.HttpMethod;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.bt.BugTrackerResource;
import org.ligoj.app.plugin.bt.IdentifierHelper;
import org.ligoj.app.plugin.bt.jira.dao.JiraDao;
import org.ligoj.app.plugin.bt.jira.model.Workflow;
import org.ligoj.app.plugin.bt.model.ChangeItem;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.app.resource.node.NodeResource;
import org.ligoj.app.resource.node.ParameterValueResource;
import org.ligoj.app.resource.plugin.CurlProcessor;
import org.ligoj.app.resource.plugin.CurlRequest;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Basic JIRA business features
 */
@Slf4j
public class JiraBaseResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = BugTrackerResource.SERVICE_URL + "/jira";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Database JDBC URL key
	 */
	public static final String PARAMETER_JDBC_URL = KEY + ":jdbc-url";

	/**
	 * Database JDBC Driver
	 */
	public static final String PARAMETER_JDBC_DRIVER = KEY + ":jdbc-driver";

	/**
	 * Database user name
	 */
	public static final String PARAMETER_JDBC_USER = KEY + ":jdbc-user";

	/**
	 * Database password
	 */
	public static final String PARAMETER_JDBC_PASSSWORD = KEY + ":jdbc-password";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * JIRA internal identifier.
	 */
	public static final String PARAMETER_PROJECT = KEY + ":project";

	/**
	 * JIRA external string identifier, AKA pkey.
	 */
	public static final String PARAMETER_PKEY = KEY + ":pkey";

	/**
	 * JIRA user name able to perform index.
	 */
	public static final String PARAMETER_ADMIN_USER = KEY + ":user";

	/**
	 * JIRA user password able to perform index.
	 */
	public static final String PARAMETER_ADMIN_PASSWORD = KEY + ":password";

	/**
	 * Parameter corresponding to the associated version. Not yet saved in
	 * database, only in-memory to save request during status checks.
	 */
	protected static final String PARAMETER_CACHE_VERSION = KEY + ":version";

	/**
	 * Workflow XML description pattern to match step-status identifiers.
	 */
	private static final Pattern STEP_PATTERN = Pattern.compile("<step id=\"(\\d+)\" name=\"([^\"]+)\">");
	private static final Pattern STATUS_PATTERN = Pattern.compile("<meta name=\"jira.status.id\">(\\d+)</meta>");

	/**
	 * Default 'jira' step to status mapping.
	 */
	private static final Workflow JIRA_WORKFLOW = new Workflow();

	static {
		final Map<String, INamableBean<Integer>> mapping = new HashMap<>();
		mapping.put("Open", new NamedBean<>(1, "Open"));
		mapping.put("In Progress", new NamedBean<>(3, "In Progress"));
		mapping.put("Resolved", new NamedBean<>(4, "Resolved"));
		mapping.put("mapping", new NamedBean<>(5, "Reopened"));
		mapping.put("Closed", new NamedBean<>(6, "Closed"));
		JIRA_WORKFLOW.setStatusToSteps(mapping);
		JIRA_WORKFLOW.setName("jira");
	}

	/**
	 * Entity manager.
	 */
	@PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "pu")
	protected EntityManager em;

	@Autowired
	protected SubscriptionResource subscriptionResource;

	@Autowired
	protected JiraDao jiraDao;

	@Autowired
	protected IdentifierHelper identifierHelper;

	@Autowired
	protected NodeResource nodeResource;

	@Autowired
	protected ParameterValueResource pvResource;

	/**
	 * Return the data source of JIRA database server.
	 * 
	 * @param subscription
	 *            The subscription used to retrieved the database parameters to
	 *            build the datasource.
	 * @return the data source of JIRA database server.
	 */
	public DataSource getDataSource(final int subscription) {
		return getDataSource(subscriptionResource.getParametersNoCheck(subscription));
	}

	/**
	 * Return the data source of JIRA database server.
	 * 
	 * @param parameters
	 *            the subscription parameters containing at least the data
	 *            source configuration.
	 * @return the data source of JIRA database server.
	 */
	protected DataSource getDataSource(final Map<String, String> parameters) {
		try {
			return new SimpleDriverDataSource(
					(Driver) Class.forName(
							StringUtils.defaultIfBlank(parameters.get(PARAMETER_JDBC_DRIVER), "com.mysql.cj.jdbc.Driver"))
							.newInstance(),
					StringUtils.defaultIfBlank(parameters.get(PARAMETER_JDBC_URL),
							"jdbc:mysql://localhost:3306/jira6?useColumnNamesInFindColumn=true&useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true&maxReconnects=3"),
					parameters.get(PARAMETER_JDBC_USER), parameters.get(PARAMETER_JDBC_PASSSWORD));
		} catch (final Exception e) {
			log.error("Database connection issue for JIRA", e);
			throw new TechnicalException("Database connection issue for JIRA", e);
		}
	}

	/**
	 * Validate the database connectivity.
	 * 
	 * @param parameters
	 *            the JDBC parameters.
	 * @return the detected JIRA version.
	 */
	protected String validateDataBaseConnectivity(final Map<String, String> parameters) {
		if (parameters.containsKey(PARAMETER_ADMIN_USER)) {
			// Clear the login failed status to clear the CAPTCHA status
			jiraDao.clearLoginFailed(getDataSource(parameters), parameters.get(PARAMETER_ADMIN_USER));
		}
		return getVersion(parameters);
	}

	/**
	 * Return the version of Jira from the database.
	 * 
	 * @param parameters
	 *            The parameters required to connect to the JIRA database.
	 * @return The version of Jira from the database.
	 */
	@Transactional(value = TxType.SUPPORTS)
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public String getVersion(final Map<String, String> parameters) {

		// Get previously resolved version if available
		String version = parameters.get(PARAMETER_CACHE_VERSION);
		if (version == null) {
			// Require a SQL query
			version = jiraDao.getJiraVersion(getDataSource(parameters));
			parameters.put(PARAMETER_CACHE_VERSION, version);
		}
		return version;
	}

	/**
	 * Validate the administration connectivity.
	 * 
	 * @param parameters
	 *            the administration parameters.
	 * @return <code>true</code> when administration connection succeed.
	 */
	protected boolean validateAdminConnectivity(final Map<String, String> parameters) {
		final CurlProcessor processor = new JiraCurlProcessor();
		try {
			return authenticateAdmin(parameters, processor);
		} finally {
			processor.close();
		}
	}

	/**
	 * Validate the project configuration.
	 * 
	 * @param parameters
	 *            the project parameters.
	 * @return project description.
	 */
	protected JiraProject validateProject(final Map<String, String> parameters) {

		// Get the project if it exists and with some statistics
		final JiraProject project = jiraDao.getProject(getDataSource(parameters),
				Integer.parseInt(ObjectUtils.defaultIfNull(parameters.get(PARAMETER_PROJECT), "0")));
		if (project == null) {
			// Invalid couple PKEY and id
			throw new ValidationJsonException(PARAMETER_PKEY, "jira-project",
					parameters.get(PARAMETER_PKEY) + "/" + parameters.get(PARAMETER_PROJECT));
		}

		return project;
	}

	/**
	 * Prepare an authenticated connection to JIRA
	 */
	protected boolean authenticateAdmin(final Map<String, String> parameters, final CurlProcessor processor) {
		final String user = parameters.get(PARAMETER_ADMIN_USER);
		final String password = StringUtils.trimToEmpty(parameters.get(PARAMETER_ADMIN_PASSWORD));
		final String baseUrl = parameters.get(PARAMETER_URL);
		final String url = StringUtils.appendIfMissing(baseUrl, "/") + "login.jsp";
		final List<CurlRequest> requests = new ArrayList<>();
		requests.add(new CurlRequest(HttpMethod.GET, url, null));
		requests.add(
				new CurlRequest(HttpMethod.POST, url,
						"os_username=" + user + "&os_password=" + password
								+ "&os_destination=&atl_token=&login=Connexion",
						JiraCurlProcessor.LOGIN_CALLBACK,
						"Accept:text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));

		// Sudoing is only required for JIRA 4+
		if ("4".compareTo(getVersion(parameters)) <= 0) {
			requests.add(new CurlRequest(HttpMethod.POST,
					StringUtils.appendIfMissing(baseUrl, "/") + "secure/admin/WebSudoAuthenticate.jspa",
					"webSudoIsPost=false&os_cookie=true&authenticate=Confirm&webSudoPassword=" + password,
					JiraCurlProcessor.SUDO_CALLBACK,
					"Accept:text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
		}
		return processor.process(requests);
	}

	/**
	 * Update the status, priorities, resolutions and types text of given SLA
	 * and retrieve all status texts involved of issues of given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param slas
	 *            the SLA list containing involved status.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param changes
	 *            Current changes. Used to computed required statuses.
	 * @return a {@link Map} where KEY is the status identifier and the VALUE is
	 *         the status name, upper case and un-localized.
	 */
	protected Map<Integer, String> updateIndentifierFromText(final DataSource dataSource, final List<Sla> slas,
			final int jira, final List<ChangeItem> changes) {
		// Get all available priorities & types for given project
		final Map<Integer, String> types = jiraDao.getTypes(dataSource, jira);
		final Map<Integer, String> priorities = jiraDao.getPriorities(dataSource);
		final Map<Integer, String> resolutions = jiraDao.getResolutions(dataSource);

		// Gather the involved statuses
		final Set<String> slaStatus = new HashSet<>();
		for (final Sla sla : slas) {
			// Add involved statuses of this SLA : start, stop, pause
			slaStatus.addAll(identifierHelper.asList(sla.getPause()));
			slaStatus.addAll(identifierHelper.asList(sla.getStart()));
			slaStatus.addAll(identifierHelper.asList(sla.getStop()));
		}

		// Compute the identifiers from the texts
		log.info("Get relevant text of {} statuses", slaStatus.size());
		final Map<Integer, String> allStatus = jiraDao.getStatuses(dataSource, getInvolvedStatuses(changes), slaStatus);
		for (final Sla sla : slas) {
			// Augment start/end/ignored status having a similar name
			sla.setStartAsSet(identifierHelper.toIdentifiers(sla.getStart(), allStatus));
			sla.setStopAsSet(identifierHelper.toIdentifiers(sla.getStop(), allStatus));
			sla.setPausedAsSet(identifierHelper.toIdentifiers(sla.getPause(), allStatus));

			// Update the priorities, resolutions & types
			sla.setResolutionsAsSet(identifierHelper.toIdentifiers(sla.getResolutions(), resolutions));
			sla.setPrioritiesAsSet(identifierHelper.toIdentifiers(sla.getPriorities(), priorities));
			sla.setTypesAsSet(identifierHelper.toIdentifiers(sla.getTypes(), types));
		}
		return allStatus;
	}

	/**
	 * Return unique involved statues in the given changes.
	 */
	protected Collection<Integer> getInvolvedStatuses(final List<? extends ChangeItem> changes) {
		final Collection<Integer> statuses = new HashSet<>();
		for (final ChangeItem change : changes) {
			statuses.add(change.getFromStatus());
			statuses.add(change.getToStatus());
			statuses.add(change.getStatus());
		}
		return statuses;
	}

	/**
	 * Return workflow steps of given workflow's name and managing default
	 * 'jira' workflow. KEY is the status, VALUE is the corresponding workflow's
	 * step
	 */
	private Workflow getWorkflow(final String name, final String workflowXml, final Map<Integer, String> statuses) {
		if (workflowXml == null) {
			// Default worflow
			return JIRA_WORKFLOW;
		}
		final Map<String, INamableBean<Integer>> statusToSteps = getWorkflowSteps(workflowXml, statuses);
		final Workflow result = new Workflow();
		result.setName(name);
		result.setStatusToSteps(statusToSteps);
		return result;
	}

	/**
	 * Return type to status to step mapping for all types valid for given
	 * project. KEY is the type, VALUE is the workflow mapping status and steps.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param statuses
	 *            the worldwide available statuses.
	 */
	protected Map<Integer, Workflow> getTypeToStatusToStep(final DataSource dataSource, final int jira,
			final Map<Integer, String> statuses) {
		final Map<Integer, String> typeToWorkflow = jiraDao.getTypesToWorkflow(dataSource, jira);
		final Map<String, Workflow> workflows = new HashMap<>();
		final Map<Integer, Workflow> mapping = new HashMap<>();
		for (final Map.Entry<Integer, String> entry : typeToWorkflow.entrySet()) {
			final String workflow = entry.getValue();
			final Integer type = entry.getKey();
			if (!workflows.containsKey(workflow)) {
				// Not yet parsed workflow
				final String workflowXml = jiraDao.getWorflow(dataSource, workflow);
				workflows.put(workflow, getWorkflow(workflow, workflowXml, statuses));
			}
			mapping.put(type, workflows.get(workflow));
		}
		return mapping;
	}

	/**
	 * Return workflow steps of given workflow's name. KEY is the status, VALUE
	 * is the corresponding workflow's step
	 */
	private Map<String, INamableBean<Integer>> getWorkflowSteps(final String workflowXml,
			final Map<Integer, String> statuses) {
		final Map<String, INamableBean<Integer>> workflowSteps = new HashMap<>();
		final String[] lines = StringUtils.replace(StringUtils.replace(workflowXml, "\\\"", "\""), "\\n", "\n").split("\\r?\\n");
		NamedBean<Integer> currentStep = null;
		for(final String line : lines) {
			if (line.startsWith("<step id=")) {
				final Matcher matcher = STEP_PATTERN.matcher(line);
				matcher.find();
				currentStep = new NamedBean<>(Integer.valueOf(matcher.group(1)), matcher.group(2));
			} else if (line.startsWith("<meta name=\"jira.status.id\">")) {
				final Matcher matcher = STATUS_PATTERN.matcher(line);
				matcher.find();
				workflowSteps.put(statuses.get(Integer.valueOf(matcher.group(1))), currentStep);
				currentStep = null;
			}
		}

		// Remove not used status by the current import
		workflowSteps.remove(null);

		return workflowSteps;
	}

}
