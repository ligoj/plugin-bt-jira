/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.in;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.ligoj.app.plugin.bt.jira.JiraBaseResource;
import org.ligoj.app.plugin.bt.jira.JiraCurlProcessor;
import org.ligoj.app.plugin.bt.jira.JiraPluginResource;
import org.ligoj.app.plugin.bt.jira.dao.IssueWithCollections;
import org.ligoj.app.plugin.bt.jira.dao.JiraChangeRow;
import org.ligoj.app.plugin.bt.jira.dao.JiraIssueRow;
import org.ligoj.app.plugin.bt.jira.dao.JiraUpdateDao;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.editor.DateEditor;
import org.ligoj.app.plugin.bt.jira.model.ImportEntry;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.ligoj.app.plugin.bt.jira.model.Workflow;
import org.ligoj.bootstrap.core.csv.CsvForBean;
import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.ligoj.bootstrap.core.curl.CurlRequest;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.bootstrap.core.validation.ValidatorBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JIRA import resource.
 */
@Slf4j
@Path(JiraBaseResource.URL + "/{subscription:\\d+}")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class JiraImportPluginResource extends JiraBaseResource {

	/**
	 * Import context data.
	 */
	protected static class ImportContext {
		private final  Set<String> requiredStatuses = new HashSet<>();
		private final Set<String> requiredPriorities = new HashSet<>();
		private final Set<String> requiredTypes = new HashSet<>();
		private final Set<String> requiredUsers = new HashSet<>();
		private final Set<String> requiredCustomFields = new HashSet<>();
		private final Set<String> requiredResolutions = new HashSet<>();

		/**
		 * Version to create.
		 */
		private final Set<String> completeVersions = new HashSet<>();

		/**
		 * Components to create.
		 */
		private final Set<String> completeComponents = new HashSet<>();

		private Map<Integer, String> statuses;
		private Map<String, Integer> invertedStatuses;
		private Map<String, Integer> priorities;
		private Map<String, Integer> resolutions;
		private Map<String, Integer> types;
		private Map<String, CustomFieldEditor> customFields;
		private Map<String, Integer> existingComponents;
		private Map<String, Integer> existingVersions;
		private Map<Integer, IssueWithCollections> issuesToUpdate;
		private DataSource dataSource;
		private Set<Integer> issues;
		private Map<Integer, List<ImportEntry>> changes;
		private Map<Integer, Workflow> typeToStatusToStep;
		private Map<String, String> parameters;
		private List<ImportEntry> rawEntries;
		private Set<String> newVersionsAsSet;
		private Set<String> newComponentsAsSet;
	}

	private static final String FIELD_ISSUE = "issue";
	private static final String FIELD_STATUS = "status";

	private static final String FIELD_RESOLUTION = "resolution";

	@Autowired
	private CsvForBean csvForBean;

	@Autowired
	protected JiraPluginResource resource;

	@Autowired
	private ValidatorBean validatorBean;

	@Autowired
	protected JiraUpdateDao jiraUpdateDao;

	/**
	 * Prepare an authenticated connection to JIRA
	 *
	 * @param context   Import context.
	 * @param processor The CURL processor for REST calls.
	 * @return <code>true</code> when authenticating as administrator succeed.
	 */
	protected boolean authenticateAdmin(final ImportContext context, final CurlProcessor processor) {
		return super.authenticateAdmin(context.parameters, processor);
	}

	/**
	 * Build status changes of all issues. Issues without changes will not be in this result, and VALUE of this
	 * {@link Map} is never an empty list. KEY is the issueNum.
	 */
	private Map<Integer, List<JiraChangeRow>> buildStatusChanges(final ImportContext context, final ImportStatus result,
			final List<JiraIssueRow> issues) {
		final var statusChanges = new HashMap<Integer, List<JiraChangeRow>>();
		int nbStatusChanges = 0;
		for (final var issue : issues) {
			nbStatusChanges += buildStatusChanges(context, statusChanges, issue);

		}
		result.setStatusChanges(nbStatusChanges);
		return statusChanges;
	}

	/**
	 * Build status changes of one issue.
	 */
	private int buildStatusChanges(final ImportContext context, final Map<Integer, List<JiraChangeRow>> statusChanges,
			final JiraIssueRow issue) {
		var nbStatusChanges = 0;
		final var changes = context.changes.get(issue.getIssueNum());
		final var issueStatusChanges = new ArrayList<JiraChangeRow>();
		var lastChange = changes.getFirst();
		for (final var change : changes) {
			if (change.getStatusId() != lastChange.getStatusId()) {
				// Add the status change
				final JiraChangeRow changeRow = new JiraChangeRow(lastChange.getStatusId(),
						context.statuses.get(lastChange.getStatusId()), change.getStatusId(),
						context.statuses.get(change.getStatusId()), change.getAuthor(), change.getDateValid());
				changeRow.setId(issue.getId());
				issueStatusChanges.add(changeRow);
				lastChange = change;
				nbStatusChanges++;
				statusChanges.put(issue.getIssueNum(), issueStatusChanges);
			}
		}
		return nbStatusChanges;
	}

	/**
	 * Check changes
	 */
	private Map<Integer, List<ImportEntry>> checkChanges(final List<ImportEntry> rawEntries) {
		final var result = new LinkedHashMap<Integer, List<ImportEntry>>();
		for (final var entry : rawEntries) {
			checkChanges(result, entry);
		}
		return result;
	}

	/**
	 * Check changes for a given entry
	 */
	private void checkChanges(final Map<Integer, List<ImportEntry>> issues, final ImportEntry entry) {
		var changes = issues.get(entry.getIssueNum());
		if (changes == null) {
			// First change
			changes = new ArrayList<>();
			issues.put(entry.getIssueNum(), changes);
		} else if (isEquals(changes.getLast(), entry)) {
			// No change means a history issue this the last line is useless
			throw new ValidationJsonException(FIELD_ISSUE, "No change detected detected for issue " + toLog(entry)
					+ " for changes between " + changes.getLast().getDate() + " and " + entry.getDate());
		}
		changes.add(entry);
	}

	/**
	 * Check history and 'pkey' and return issues number.
	 */
	private Set<Integer> checkChronologyAndPkey(final List<ImportEntry> rawEntries, final String pkey) {
		final var chronology = new LinkedHashMap<Integer, Date>();
		final var first = new LinkedHashMap<Integer, ImportEntry>();
		for (final var rawEntry : rawEntries) {
			// Check unique PKEY of issue
			checkPKey(pkey, rawEntry);

			// Get and save the Java Date and check the history
			final var date = checkExcelDate(chronology.get(rawEntry.getIssueNum()), rawEntry);
			rawEntry.setDateValid(date);
			chronology.put(rawEntry.getIssueNum(), date);
			rawEntry.setDateValid(date);

			// Check the resolution/resolution date
			checkResolutionDate(rawEntry);

			// Check the due date
			checkDueDate(first, rawEntry);

			// Register the issue for the first change
			if (!first.containsKey(rawEntry.getIssueNum())) {
				first.put(rawEntry.getIssueNum(), rawEntry);
			}
		}

		return chronology.keySet();
	}

	/**
	 * Check custom fields. During this call, {@link String} custom fields values are replaced with the formatted and
	 * typed ones.
	 */
	private void checkCustomFields(final List<ImportEntry> rawEntries, final Set<String> requiredCustomFields,
			final Map<String, CustomFieldEditor> customFields) {
		final var size = requiredCustomFields.size();
		checkRequired("cf", "custom fields", customFields, requiredCustomFields);
		log.info("Step5 - Type of {} custom fields of {} changes", size, rawEntries.size());
		for (final var rawEntry : rawEntries) {
			for (final var cfEntry : rawEntry.getCf().entrySet()) {
				final CustomFieldEditor customField = customFields.get(cfEntry.getKey());
				cfEntry.setValue(customField.getEditor().getValue(customField, (String) cfEntry.getValue()));
			}
		}
	}

	/**
	 * Check the due date
	 */
	private void checkDueDate(final Map<Integer, ImportEntry> first, final ImportEntry rawEntry) {
		if (rawEntry.getDueDate() != null) {
			rawEntry.setDueDateValid(DateEditor.toDate(rawEntry.getDueDate()));

			// Check the due date against creation
			if (first.containsKey(rawEntry.getIssueNum()) && rawEntry.getDueDateValid().getTime() < first
					.get(rawEntry.getIssueNum()).getDateValid().getTime()) {
				throw new ValidationJsonException("dueDate",
						"Due date must be greater or equals to the creation date for issue " + toLog(rawEntry));
			}
		}

	}

	/**
	 * Parse the date, and check it is after the other one.
	 */
	private Date checkExcelDate(final Date previousDate, final ImportEntry entry) {
		final var date = DateEditor.toDate(entry.getDate());
		if (previousDate != null && date.before(previousDate)) {
			// Chronology issue
			throw new ValidationJsonException("date",
					"Broken history for issue " + toLog(entry) + " " + previousDate + " and " + entry.getDate());
		}
		return date;
	}

	/**
	 * Check JIRA version
	 */
	private String checkJiraVersion(final DataSource dataSource) {
		final var jiraVersion = jiraDao.getJiraVersion(dataSource);
		if (jiraVersion.compareTo("6.0.0") < 0) {
			throw new ValidationJsonException("jira",
					"Required JIRA version is 6.0.0, and the current version is " + jiraVersion);
		}
		return jiraVersion;
	}

	/**
	 * Check the required components we need to create before the changes
	 */
	private void checkNewComponents(final ImportContext context, final ImportStatus result, final int jira) {
		log.info("Get existing components");
		context.existingComponents = MapUtils.invertMap(jiraDao.getComponents(context.dataSource, jira));
		context.newComponentsAsSet = computeDiff(context.existingComponents, context.completeComponents);
		result.setNewComponents(context.newComponentsAsSet.size());
	}

	private void checkNewIssues(final ImportContext context, final ImportStatus result, final int jira) {
		log.info("Get existing issues to update");
		context.issuesToUpdate = jiraDao.getIssues(context.dataSource, jira, result.getMinIssue(), result.getMaxIssue(),
				context.issues);
		result.setNewIssues(context.issues.size() - context.issuesToUpdate.size());
		if (!context.issuesToUpdate.isEmpty()) {
			final var first = context.issuesToUpdate.values().iterator().next();
			throw new ValidationJsonException(FIELD_ISSUE,
					"Updating issues is not yet implemented. " + context.issuesToUpdate.size()
							+ " issues are concerned. First one is issue " + first.getIssue() + " (id=" + first.getId()
							+ ")");
		}
	}

	private void checkNewVersions(final ImportContext context, final ImportStatus result, final int jira) {
		log.info("Get existing versions");
		context.existingVersions = MapUtils.invertMap(jiraDao.getVersions(context.dataSource, jira));
		context.newVersionsAsSet = computeDiff(context.existingVersions, context.completeVersions);
		result.setNewVersions(context.newVersionsAsSet.size());
	}

	/**
	 * Separate the PKEY from the issue number, update the entry, validate the PKEY is the same as the expected one,
	 * otherwise, throws an exception.
	 */
	private void checkPKey(final String pkey, final ImportEntry entry) {
		final var issue = entry.getIssue();
		final var index = issue.indexOf('-');
		if (index == -1) {
			entry.setIssueNum(Integer.parseInt(issue));
		} else {
			entry.setIssueNum(Integer.parseInt(issue.substring(index + 1)));

			// Check the PKEY (subscription and import)
			final var pkeyIssue = issue.substring(0, index);
			if (!pkeyIssue.equals(pkey)) {
				throw new ValidationJsonException(FIELD_ISSUE, "Used issue prefix in import is " + pkeyIssue
						+ ", but associated project is " + pkey + ", are you importing the correct file?");
			}
		}
	}

	/**
	 * Compare and check the required and the actual elements.
	 */
	private void checkRequired(final String field, final String errorString, final Map<String, ?> existing,
			final Set<String> required) {
		final var copy = new HashSet<>(required);
		copy.removeAll(existing.keySet());
		if (!copy.isEmpty()) {
			// Not existing priorities have been found during the import
			throw new ValidationJsonException(field,
					"Some " + errorString + " (" + copy.size() + ") do not exist : " + String.join(",", copy));
		}
	}

	/**
	 * Check the resolution/resolution date
	 */
	private void checkResolutionDate(final ImportEntry rawEntry) {
		if (rawEntry.getResolutionDate() == null) {
			if (rawEntry.getResolution() != null) {
				rawEntry.setResolutionDateValid(rawEntry.getDateValid());
			}
		} else {
			if (rawEntry.getResolution() == null) {
				rawEntry.setResolution("Fixed");
			}
			rawEntry.setResolutionDateValid(DateEditor.toDate(rawEntry.getResolutionDate()));
			if (rawEntry.getResolutionDateValid().getTime() < rawEntry.getDateValid().getTime()) {
				throw new ValidationJsonException("resolutionDate",
						"Resolution date must be greater or equals to the change date for issue " + toLog(rawEntry));
			}
		}
	}

	/**
	 * Check resolution against the status of each issue.
	 */
	private void checkStatusAgainstResolution(final ImportContext context) {
		final var resolvedDate = new HashMap<Integer, Date>();
		for (final var changes : context.changes.values()) {
			for (final ImportEntry change : changes) {
				if (isResolutionStatus(change.getStatus())) {
					completeResolution(change);

					// Update the last known resolution date
					resolvedDate.put(change.getIssueNum(), change.getResolutionDateValid());
				} else if (change.getResolution() != null && !resolvedDate.containsKey(change.getIssueNum())) {
					throw new ValidationJsonException(FIELD_RESOLUTION,
							"Resolution is provided but has never been resolved for issue " + toLog(change));
				}
			}
		}
	}

	/**
	 * Check types against workflow.
	 */
	private void checkStatusAgainstWorkflow(final ImportContext context) {
		final var knowStatus = new HashSet<String>();
		for (final var mapping : context.typeToStatusToStep.values()) {
			knowStatus.addAll(mapping.getStatusToSteps().keySet());
		}
		final var copyOfRequired = new HashSet<>(context.statuses.values());
		copyOfRequired.removeAll(knowStatus);
		if (!copyOfRequired.isEmpty()) {
			throw new ValidationJsonException(FIELD_STATUS,
					"At least one specified status exists but is not managed in the workflow : "
							+ String.join(",", copyOfRequired));
		}
	}

	/**
	 * Check types against workflow
	 */
	private void checkTypesAgainstWorkflow(final ImportContext context) {
		for (final var type : context.requiredTypes) {
			if (!context.typeToStatusToStep.containsKey(context.types.get(type))) {
				throw new ValidationJsonException("type", "Specified type '" + type
						+ "' exists but is not mapped to a workflow and there is no default association");
			}
		}
	}

	/**
	 * Check types against workflow
	 */
	private void checkTypesAgainstWorkflow(final ImportContext context, final ImportStatus result) {
		context.typeToStatusToStep = getTypeToStatusToStep(context.dataSource, result.getJira(), context.statuses);
		if (!context.typeToStatusToStep.containsKey(0)) {
			checkTypesAgainstWorkflow(context);
		}
	}

	/**
	 * Check users
	 */
	private void checkUsers(final DataSource dataSource, final Set<String> requiredUsers) {
		final var existingUsers = jiraDao.getUsers(dataSource, requiredUsers);
		checkRequired("assignee", "assignee/reporters/authors",
				existingUsers.stream().collect(Collectors.toMap(Function.identity(), Function.identity())),
				requiredUsers);
	}

	/**
	 * Clear JIRA Cache.
	 *
	 * @param context   Import context.
	 * @param result    The import result.
	 * @param processor The CURL processor for REST calls.
	 * @return <code>true</code> when JIRA cache failed.
	 * @see "http://localhost:6080/plugins/servlet/scriptrunner/builtin?section=builtin_scripts#"
	 * @see "https://marketplace.atlassian.com/plugins/com.onresolve.jira.groovy.groovyrunner"
	 */
	protected boolean clearJiraCache(final ImportContext context, final ImportStatus result,
			final CurlProcessor processor) {
		if (result.getScriptRunner()) {
			final var requests = new ArrayList<CurlRequest>();
			final var url = context.parameters.get(PARAMETER_URL) + "/secure/admin/groovy/CannedScriptRunner.jspa";
			requests.add(new CurlRequest(HttpMethod.POST, url,
					"cannedScript=com.onresolve.jira.groovy.canned.admin.ClearCaches"
							+ "&cannedScriptArgs_FIELD_WHICH_CACHE=jira"
							+ "&cannedScriptArgs_Hidden_FIELD_WHICH_CACHE=jira"
							+ "&cannedScriptArgs_Hidden_output=Cache+cleared."
							+ "&cannedScript=com.onresolve.jira.groovy.canned.admin.ClearCaches&id="
							+ "&atl_token=B3WY-Y7OK-4J8S-4GH4%7Ca2a4f45ffb53fcf8fbb12453e587949470377ec7%7Clin"
							+ "&RunCanned=Run" + "&webSudoIsPost=true&os_cookie=true",
					"Accept:application/json, text/javascript, */*; q=0.01"));
			return processor.process(requests);
		}
		return false;
	}

	/**
	 * Complete context : required date and data to complete during the import.
	 */
	private void completeContext(final ImportStatus result, final List<ImportEntry> rawEntries,
			final ImportContext context) {
		final var requiredStatuses = context.requiredStatuses;
		final var requiredPriorities = context.requiredPriorities;
		final var requiredTypes = context.requiredTypes;
		final var requiredUsers = context.requiredUsers;
		final var requiredCustomFields = context.requiredCustomFields;
		final var requiredResolutions = context.requiredResolutions;
		final var completeVersions = context.completeVersions;
		final var completeComponents = context.completeComponents;
		var minIssue = rawEntries.getFirst().getIssueNum();
		var maxIssue = rawEntries.getLast().getIssueNum();
		for (final var rawEntry : rawEntries) {
			if (rawEntry.getIssueNum() < minIssue) {
				minIssue = rawEntry.getIssueNum();
			}
			if (rawEntry.getIssueNum() > maxIssue) {
				maxIssue = rawEntry.getIssueNum();
			}

			// Collect the data
			requiredStatuses.add(rawEntry.getStatus());
			requiredPriorities.add(rawEntry.getPriority());
			requiredTypes.add(rawEntry.getType());
			requiredUsers.add(rawEntry.getAssignee());
			requiredUsers.add(rawEntry.getAuthor());
			requiredUsers.add(rawEntry.getReporter());
			if (rawEntry.getCf() == null) {
				rawEntry.setCf(new HashMap<>());
			} else {
				requiredCustomFields.addAll(rawEntry.getCf().keySet());
			}
			if (rawEntry.getResolution() != null) {
				requiredResolutions.add(rawEntry.getResolution());
			}
			rawEntry.setVersionText(getItems(rawEntry.getVersion()));
			completeVersions.addAll(rawEntry.getVersionText());
			rawEntry.setFixedVersionText(getItems(rawEntry.getFixedVersion()));
			completeVersions.addAll(rawEntry.getFixedVersionText());
			rawEntry.setComponentsText(getItems(rawEntry.getComponents()));
			completeComponents.addAll(rawEntry.getComponentsText());
			rawEntry.setLabelsText(getItems(rawEntry.getLabels()));
		}
		result.setMinIssue(minIssue);
		result.setMaxIssue(maxIssue);
	}

	/**
	 * Complete the resolution date and value.
	 */
	private void completeResolution(final ImportEntry change) {
		// Status is resolved, we expect a resolution value
		if (change.getResolution() == null) {
			// No resolution is provided, we set it to "Fixed" as default
			change.setResolution("Fixed");
			change.setResolutionDateValid(change.getDateValid());
		}
	}

	/**
	 * Return item not present in the first parameter.
	 */
	private Set<String> computeDiff(final Map<String, Integer> existing, final Set<String> required) {
		final var set = new HashSet<>(required);
		set.removeAll(existing.keySet());
		return set;
	}

	/**
	 * Compute final state of issue.
	 */
	private List<JiraIssueRow> computeFinalIssueState(final ImportContext context) {
		final var issues = new ArrayList<JiraIssueRow>();
		for (final var changes : context.changes.values()) {
			final var first = changes.getFirst();
			final var last = changes.getLast();
			final JiraIssueRow issueImport = new JiraIssueRow();
			issueImport.setAssignee(last.getAssignee());
			issueImport.setAuthor(first.getAuthor());
			issueImport.setCreated(first.getDateValid());
			issueImport.setDueDate(last.getDueDateValid());
			issueImport.setDescription(last.getDescription());
			issueImport.setPriority(last.getPriorityId());
			issueImport.setReporter(last.getReporter());
			issueImport.setResolution(last.getResolutionId());
			issueImport.setResolutionDate(last.getResolutionDateValid());
			issueImport.setStatus(last.getStatusId());
			issueImport.setStatusText(context.statuses.get(last.getStatusId()));
			issueImport.setSummary(last.getSummary());
			issueImport.setType(last.getTypeId());
			issueImport.setUpdated(last.getDateValid());
			issueImport.setIssueNum(first.getIssueNum());
			issueImport.setComponents(convertTextToId(context.existingComponents, last.getComponentsText()));
			issueImport.setVersions(convertTextToId(context.existingVersions, last.getVersionText()));
			issueImport.setFixedVersions(convertTextToId(context.existingVersions, last.getFixedVersionText()));
			issueImport.setCustomFields(last.getCf());
			issueImport.setLabels(last.getLabelsText());
			issues.add(issueImport);
		}
		return issues;
	}

	/**
	 * Compute labels.
	 */
	private int computeNbLabels(final Map<Integer, List<ImportEntry>> allChanges) {
		int nbLabels = 0;
		for (final var changes : allChanges.values()) {
			nbLabels += changes.getLast().getLabelsText().size();
		}

		return nbLabels;
	}

	/**
	 * Complete the identifiers of text values of required data.
	 */
	private void convertTextToId(final List<ImportEntry> rawEntries, final ImportContext context) {
		for (final var rawEntry : rawEntries) {

			// Collect the data
			rawEntry.setStatusId(context.invertedStatuses.get(rawEntry.getStatus()));
			rawEntry.setPriorityId(context.priorities.get(rawEntry.getPriority()));
			rawEntry.setTypeId(context.types.get(rawEntry.getType()));
			if (rawEntry.getResolution() != null) {
				rawEntry.setResolutionId(context.resolutions.get(rawEntry.getResolution()));
			}
		}
	}

	/**
	 * Convert text to identifier. Return the identifier list, in the same order of given texts.
	 */
	private List<Integer> convertTextToId(final Map<String, Integer> allItems, final Collection<String> itemsAsText) {
		return itemsAsText.stream().map(allItems::get).toList();
	}

	/**
	 * Add and return not blank values from a string split with ',' separator.
	 */
	private Set<String> getItems(final String rawValue) {
		final var result = new HashSet<String>();
		for (final var rawItem : StringUtils.split(StringUtils.trimToEmpty(rawValue), ',')) {
			result.add(StringUtils.trim(rawItem));
		}
		return result;
	}

	/**
	 * Compare 2 entries
	 */
	private boolean isEquals(final ImportEntry lastEntry, final ImportEntry entry) {
		// CHECKSTYLE:OFF
		return isEquals(lastEntry.getAssignee(), entry.getAssignee())
				&& isEquals(lastEntry.getAuthor(), entry.getAuthor())
				&& isEquals(lastEntry.getReporter(), entry.getReporter())
				&& isEquals(lastEntry.getDescription(), entry.getDescription())
				&& isEquals(lastEntry.getFixedVersionText(), entry.getFixedVersionText())
				&& lastEntry.getPriorityId() == entry.getPriorityId()
				&& isEquals(lastEntry.getResolutionId(), entry.getResolutionId())
				&& isEquals(lastEntry.getResolutionDateValid(), entry.getResolutionDateValid())
				&& isEquals(lastEntry.getDueDateValid(), entry.getDueDateValid())
				&& lastEntry.getStatusId() == entry.getStatusId()
				&& isEquals(lastEntry.getSummary(), entry.getSummary()) && isEquals(lastEntry.getCf(), entry.getCf())
				&& isEquals(lastEntry.getLabelsText(), entry.getLabelsText())
				&& isEquals(lastEntry.getComponentsText(), entry.getComponentsText())
				&& lastEntry.getTypeId() == entry.getTypeId()
				&& isEquals(lastEntry.getVersionText(), entry.getVersionText());
		// CHECKSTYLE:ON
	}

	/**
	 * Compare 2 values
	 */
	private boolean isEquals(final Object value1, final Object value2) {
		return value1 == null && value2 == null || value1 != null && value1.equals(value2);
	}

	/**
	 * Indicate the given status is a resolution step.
	 */
	private boolean isResolutionStatus(final String status) {
		return "Resolved".equals(status) || "Closed".equals(status);
	}

	private ImportStatus nextStep(final ImportStatus result) {
		return resource.nextStep(result.getLocked().getId(), t -> t.setStep(t.getStep() + 1));
	}

	private void persistData(final ImportContext context, final ImportStatus result) {

		// Add components and versions
		log.info("Create new components");
		context.existingComponents
				.putAll(jiraUpdateDao.addComponents(context.dataSource, result.getJira(), context.newComponentsAsSet));
		nextStep(result);

		log.info("Create new versions");
		context.existingVersions
				.putAll(jiraUpdateDao.addVersions(context.dataSource, result.getJira(), context.newVersionsAsSet));
		nextStep(result);

		// Add issues, final state
		log.info("Create issues");
		final var issues = computeFinalIssueState(context);
		jiraUpdateDao.addIssues(context.dataSource, result.getJira(), issues, context.typeToStatusToStep);
		nextStep(result);

		// Associate issues to components and (due|fix)versions
		log.info("Associate components to issues");
		jiraUpdateDao.associateComponentsAndVersions(context.dataSource, issues);
		nextStep(result);

		// Associate custom field values
		log.info("Associate custom fields");
		jiraUpdateDao.associateCustomFieldsValues(context.dataSource, issues, context.customFields);
		nextStep(result);

		// Associate labels
		log.info("Associate labels");
		jiraUpdateDao.addLabels(context.dataSource, issues);
		nextStep(result);

		// Add status changes
		log.info("Add status changes history");
		jiraUpdateDao.addChanges(context.dataSource, buildStatusChanges(context, result, issues));
		nextStep(result);

		// Synchronize JIRA
		log.info("Synchronize JIRA cache and index");
		synchronizeJira(context, result);
		nextStep(result);
		// OPT : Build and return a rollback file
	}

	private void prepareCompleteData(final ImportContext context, final ImportStatus result) {
		final var jira = result.getJira();
		checkNewComponents(context, result, jira);
		nextStep(result);
		checkNewVersions(context, result, jira);
		nextStep(result);
		checkNewIssues(context, result, jira);
		nextStep(result);
	}

	/**
	 * Re-index JIRA issues.
	 *
	 * @param context   Import context.
	 * @param result    The import result.
	 * @param processor The CURL processor for REST calls.
	 * @return <code>true</code> when JIRA re-index failed.
	 * @see "http://localhost:6080/secure/admin/IndexProject.jspa?pid=10000"
	 * @see "http://localhost:6080/secure/admin/jira/IndexReIndex.jspa"
	 */
	protected boolean reIndexProject(final ImportContext context, final ImportStatus result,
			final CurlProcessor processor) {
		final var requests = new ArrayList<CurlRequest>();
		final var url = context.parameters.get(PARAMETER_URL) + "/secure/admin/IndexProject.jspa";
		requests.add(new CurlRequest(HttpMethod.GET, url + "?pid=" + result.getJira(), null));
		return processor.process(requests);
	}

	/**
	 * Synchronize JIRA.
	 *
	 * @param context Import context.
	 * @param result  The import result.
	 */
	protected void synchronizeJira(final ImportContext context, final ImportStatus result) {
		if (result.getCanSynchronizeJira()) {
			// Administration account has been provided
			final var processor = new JiraCurlProcessor();
			result.setSynchronizedJira(authenticateAdmin(context, processor)
					&& clearJiraCache(context, result, processor) && reIndexProject(context, result, processor));
			processor.close();
		}
	}

	/**
	 * Return issue log text
	 */
	private String toLog(final ImportEntry entry) {
		if (StringUtils.isNoneBlank(entry.getId())) {
			return entry.getIssue() + "(id=" + entry.getId() + ")";
		}
		return entry.getIssue();
	}

	/**
	 * Import CSV data into a JIRA project
	 *
	 * @param csvInput     the CSV to import.
	 * @param encoding     the encoding of CSV file.
	 * @param subscription the subscription identifier.
	 * @param mode         the upload mode.
	 * @return the import result.
	 * @throws IOException When CSV cannot be read.
	 */
	@POST
	@Path("{mode}/{encoding}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public ImportStatus upload(@Multipart("csv-file") final InputStream csvInput,
			@PathParam("encoding") final String encoding, @PathParam("subscription") final int subscription,
			@PathParam("mode") final UploadMode mode) throws IOException {
		boolean failed = true;
		subscriptionResource.checkVisible(subscription);
		try {

			final var importStatus = resource.startTask(subscription, task -> {

				// Initialize starting information
				task.setStep(1);

				// Reset old values
				task.setChanges(null);
				task.setComponents(null);
				task.setCustomFields(null);
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
				task.setMode(mode);
			});
			uploadPrivate(importStatus, new InputStreamReader(csvInput, encoding));
			failed = false;
			return importStatus;
		} finally {
			// Make sure, whatever the situation, to release the lock
			resource.endTask(subscription, failed);
		}
	}

	/**
	 * Import CSV data into a JIRA project
	 *
	 * @param result   the import result.
	 * @param csvInput the CSV to import.
	 */
	private void uploadPrivate(final ImportStatus result, final Reader csvInput) throws IOException {
		final var context = new ImportContext();
		validateSubscription(context, result);
		validateSyntax(context, result, csvInput);
		if (result.getMode() == UploadMode.SYNTAX) {
			return;
		}
		validateRequiredData(result, context);
		validateWorkflowData(result, context);
		if (result.getMode() == UploadMode.VALIDATION) {
			return;
		}
		prepareCompleteData(context, result);
		if (result.getMode() == UploadMode.PREVIEW) {
			return;
		}
		persistData(context, result);
	}

	private void validateRequiredData(final ImportStatus result, final ImportContext context) {
		// Collect of foreign keys to validate/complete and initialize the
		// business objects
		log.info("Collect required data for the {} issues", context.issues.size());
		final var rawEntries = context.rawEntries;
		completeContext(result, rawEntries, context);
		final var requiredStatuses = context.requiredStatuses;
		final var requiredPriorities = context.requiredPriorities;
		final var requiredTypes = context.requiredTypes;
		final var requiredUsers = context.requiredUsers;
		final var requiredCustomFields = context.requiredCustomFields;
		final var requiredResolutions = context.requiredResolutions;
		result.setPriorities(requiredPriorities.size());
		result.setStatuses(requiredStatuses.size());
		result.setTypes(requiredTypes.size());
		result.setUsers(requiredUsers.size());
		result.setCustomFields(requiredCustomFields.size());
		result.setResolutions(requiredResolutions.size());
		result.setVersions(context.completeVersions.size());
		result.setComponents(context.completeComponents.size());
		nextStep(result);

		// Get and check mandatory links
		log.info("Load and check required data ...");
		log.info("... Statuses");
		context.statuses = jiraDao.getStatuses(context.dataSource, new ArrayList<>(), requiredStatuses);
		context.invertedStatuses = MapUtils.invertMap(context.statuses);
		checkRequired(FIELD_STATUS, "statuses", context.invertedStatuses, requiredStatuses);
		nextStep(result);

		log.info("... Priorities");
		context.priorities = MapUtils.invertMap(jiraDao.getPriorities(context.dataSource));
		checkRequired("priority", "priorities", context.priorities, requiredPriorities);
		nextStep(result);

		log.info("... Resolutions");
		context.resolutions = MapUtils.invertMap(jiraDao.getResolutions(context.dataSource));
		checkRequired(FIELD_RESOLUTION, "resolutions", context.resolutions, requiredResolutions);
		nextStep(result);

		log.info("... Types");
		context.types = MapUtils.invertMap(jiraDao.getTypes(context.dataSource, result.getJira()));
		checkRequired("type", "types", context.types, requiredTypes);
		nextStep(result);

		log.info("... Custom fields definition");
		context.customFields = jiraDao.getCustomFields(context.dataSource, requiredCustomFields, result.getJira());
		checkCustomFields(rawEntries, requiredCustomFields, context.customFields);
		nextStep(result);

		log.info("... Users");
		checkUsers(context.dataSource, requiredUsers);
		nextStep(result);

		// Convert text to identifiers
		log.info("Convert texts to identifiers");
		convertTextToId(rawEntries, context);
		nextStep(result);

		// Compare changes for at least on change each line for each issue
		log.info("Compute changes");
		context.changes = checkChanges(rawEntries);
		nextStep(result);

		// Compute labels
		log.info("Compute labels");
		result.setLabels(computeNbLabels(context.changes));
		nextStep(result);
	}

	/**
	 * Validate subscription and JIRA compatibility.
	 */
	private void validateSubscription(final ImportContext context, final ImportStatus result) {
		// Check the subscription exists and there is no running import
		log.info("Validate subscription settings");
		context.parameters = subscriptionResource.getParameters(result.getLocked().getId());
		context.dataSource = getDataSource(context.parameters);
		final var pkey = context.parameters.get(PARAMETER_PKEY);
		result.setJira(Integer.valueOf(context.parameters.get(PARAMETER_PROJECT)));
		result.setPkey(pkey);
		result.setCanSynchronizeJira(StringUtils.isNotBlank(context.parameters.get(PARAMETER_ADMIN_USER)));
		nextStep(result);

		log.info("Check the JIRA/plugin version");
		result.setJiraVersion(checkJiraVersion(context.dataSource));
		result.setScriptRunner(jiraDao.hasScriptRunnerPlugin(context.dataSource));
		nextStep(result);
	}

	private void validateSyntax(final ImportContext context, final ImportStatus result, final Reader csvInput)
			throws IOException {

		// Read all entries, may need extra memory at this moment ...
		log.info("Read changes to import");
		final var rawEntries = csvForBean.toBean(ImportEntry.class, csvInput);
		result.setChanges(rawEntries.size());
		if (rawEntries.isEmpty()) {
			// No change
			throw new ValidationJsonException("id", "Empty file, no change found");
		}
		nextStep(result);

		log.info("Validate syntax of {} changes", rawEntries.size());
		validatorBean.validateCheck(rawEntries);
		nextStep(result);

		// Collect of foreign keys to validate/complete and initialize the
		// business objects
		log.info("Validate chronology and PKEY constant");
		final var pkey = context.parameters.get(PARAMETER_PKEY);
		context.issues = checkChronologyAndPkey(rawEntries, pkey);
		result.setIssues(context.issues.size());
		result.setIssueFrom(rawEntries.getFirst().getDateValid());
		result.setIssueTo(rawEntries.getLast().getDateValid());
		nextStep(result);
		context.rawEntries = rawEntries;
	}

	private void validateWorkflowData(final ImportStatus result, final ImportContext context) {
		// Compute workflow types
		log.info("Compute workflow types");
		checkTypesAgainstWorkflow(context, result);
		nextStep(result);

		// Compute workflow statuses
		log.info("Compute workflow statuses");
		checkStatusAgainstWorkflow(context);
		nextStep(result);

		// Compute resolution flow
		log.info("Compute resolution flow");
		checkStatusAgainstResolution(context);
		nextStep(result);
	}

}
