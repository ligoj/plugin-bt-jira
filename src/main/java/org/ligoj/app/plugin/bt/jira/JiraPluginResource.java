package org.ligoj.app.plugin.bt.jira;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.Activity;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.plugin.bt.BugTrackerServicePlugin;
import org.ligoj.app.plugin.bt.dao.SlaRepository;
import org.ligoj.app.plugin.bt.jira.dao.ImportStatusRepository;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.ligoj.app.plugin.bt.jira.model.Workflow;
import org.ligoj.app.resource.ActivitiesProvider;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.app.resource.plugin.VersionUtils;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;

/**
 * JIRA issues resource.
 */
@Path(JiraBaseResource.URL)
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class JiraPluginResource extends JiraBaseResource
		implements BugTrackerServicePlugin, ToolPlugin, ActivitiesProvider, LongTaskRunner<ImportStatus, ImportStatusRepository> {

	@Autowired
	@Getter
	private ImportStatusRepository taskRepository;

	@Getter
	@Autowired
	protected SubscriptionRepository subscriptionRepository;

	@Autowired
	protected NodeRepository nodeRepository;

	@Autowired
	protected SecurityHelper securityHelper;

	@Autowired
	protected SlaRepository slaRepository;

	@Autowired
	protected VersionUtils versionUtils;

	@Override
	public void link(final int subscription) throws Exception {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);

		// Validate the project identifier an PKEY
		validateDataBaseConnectivity(parameters);
		validateProject(parameters);
		if (parameters.containsKey(PARAMETER_ADMIN_USER) && !validateAdminConnectivity(parameters)) {
			// Invalid administration configuration
			throw new ValidationJsonException(PARAMETER_ADMIN_USER, "jira-admin", parameters.get(PARAMETER_ADMIN_USER));
		}
	}

	/**
	 * Validate the administration connectivity.
	 * 
	 * @param node
	 *            the node to be tested with given parameters.
	 * @param criteria
	 *            the search criteria.
	 * @return project name.
	 */
	@GET
	@Path("{node:\\w+:.*}/{criteria}")
	@Consumes(MediaType.APPLICATION_JSON)
	public List<JiraProject> findAllByName(@PathParam("node") final String node, @PathParam("criteria") final String criteria) {
		// Check the node exists
		if (nodeRepository.findOneVisible(node, securityHelper.getLogin()) == null) {
			return Collections.emptyList();
		}

		return jiraDao.findProjectsByName(getDataSource(pvResource.getNodeParameters(node)), criteria);
	}

	@Override
	@Transactional(value = TxType.SUPPORTS)
	public String getKey() {
		return JiraBaseResource.KEY;
	}

	@Override
	public Set<String> getStatuses(final int subscription) throws IOException {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final int jira = Integer.parseInt(parameters.get(JiraBaseResource.PARAMETER_PROJECT));
		final DataSource dataSource = getDataSource(parameters);

		// Get all available statuses
		final Map<Integer, String> items = jiraDao.getStatuses(dataSource);

		// Filter these status to get only the involved ones
		final Map<Integer, Workflow> typeToStatusToStep = getTypeToStatusToStep(dataSource, jira, items);

		// Extract the filtered status from the involved workflows
		final Set<String> result = new TreeSet<>();
		for (final Workflow workflow : typeToStatusToStep.values()) {
			result.addAll(workflow.getStatusToSteps().keySet());
		}
		return result;
	}

	@Override
	public Set<String> getTypes(final int subscription) {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final int jira = Integer.parseInt(parameters.get(JiraBaseResource.PARAMETER_PROJECT));
		final DataSource dataSource = getDataSource(parameters);

		// Get types of given project
		return new TreeSet<>(jiraDao.getTypes(dataSource, jira).values());
	}

	@Override
	public Set<String> getPriorities(final int subscription) {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final DataSource dataSource = getDataSource(parameters);

		// Get all priorities
		return new TreeSet<>(jiraDao.getPriorities(dataSource).values());
	}

	@Override
	public Set<String> getResolutions(final int subscription) {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final DataSource dataSource = getDataSource(parameters);

		// Get all priorities
		return new TreeSet<>(jiraDao.getResolutions(dataSource).values());
	}

	@Override
	@Transactional(value = TxType.SUPPORTS)
	public String getLastVersion() throws Exception {
		// Get the download json from the default repository
		return versionUtils.getLatestReleasedVersionName("https://jira.atlassian.com", "JRA");
	}

	@Override
	public boolean checkStatus(final Map<String, String> parameters) throws Exception {
		// Status is UP <=> Database is UP and Administration access is UP (if
		// defined)
		final String version = validateDataBaseConnectivity(parameters);
		parameters.put(PARAMETER_PKEY, version);
		return !parameters.containsKey(PARAMETER_ADMIN_USER) || super.validateAdminConnectivity(parameters);
	}

	@Override
	public SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters) throws Exception {
		final SubscriptionStatusWithData nodeStatusWithData = new SubscriptionStatusWithData();
		nodeStatusWithData.put("project", validateProject(parameters));
		return nodeStatusWithData;
	}

	@Override
	public Map<String, Activity> getActivities(final int subscription, final Collection<String> users) throws Exception {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final DataSource dataSource = getDataSource(parameters);
		return jiraDao.getActivities(dataSource, users);
	}

	@Override
	public void create(final int subscription) {
		throw new IllegalStateException("Not yet implemented");
	}

	@Override
	public List<Class<?>> getInstalledEntities() {
		return Arrays.asList(Node.class, Parameter.class);
	}

	@Override
	public void nextStepInternal(final ImportStatus task) {
		task.setStep(task.getStep() + 1);
	}

	@Override
	public void resetTask(final ImportStatus task) {

		// Initialize starting information
		task.setStep(1);

		// Rest old values
		task.setChanges(null);
		task.setFailed(false);
		task.setComponents(null);
		task.setCustomFields(null);
		task.setEnd(null);
		task.setIssueFrom(null);
		task.setIssues(null);
		task.setJira(null);
		task.setJiraVersion(null);
		task.setLabels(null);
		task.setMaxIssue(null);
		task.setMinIssue(null);
		task.setNewIssues(null);
		task.setNewVersions(null);
		task.setNewComponents(null);
		task.setPkey(null);
		task.setPriorities(null);
		task.setResolutions(null);
		task.setStatuses(null);
		task.setIssueTo(null);
		task.setTypes(null);
		task.setUsers(null);
		task.setVersions(null);
		task.setScriptRunner(null);
		task.setStatusChanges(null);
		task.setSynchronizedJira(null);
		task.setCanSynchronizeJira(null);
		task.setMode(UploadMode.VALIDATION);
	}

	@Override
	public Supplier<ImportStatus> newTask() {
		return ImportStatus::new;
	}
}
