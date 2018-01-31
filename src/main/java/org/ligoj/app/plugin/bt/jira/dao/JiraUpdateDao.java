package org.ligoj.app.plugin.bt.jira.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.Workflow;
import org.ligoj.bootstrap.core.INamableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * DAO for JIRA update/delete/create services.
 * 
 * @see "https://confluence.atlassian.com/display/JIRA041/Example+SQL+queries+for+JIRA"
 */
@Component
@Slf4j
public class JiraUpdateDao {

	/**
	 * Jira Node names for associations
	 */
	private static final String COMPONENT_NODE = "Component";
	private static final String VERSION_NODE = "Version";
	private static final String ISSUE_NODE = "Issue";

	@Autowired
	private JiraDao jiraDao;

	/**
	 * Add the given components to the given project
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param components
	 *            the component names to add
	 * @return the {@link Map} where value is the created component identifier.
	 */
	public Map<String, Integer> addComponents(final DataSource dataSource, final int jira, final Collection<String> components) {
		final Map<String, Integer> result = new HashMap<>();
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		int nextId = prepareForNextId(dataSource, COMPONENT_NODE, components.size());
		for (final String component : components) {
			jdbcTemplate.update("INSERT INTO component (ID,PROJECT,cname) values(?,?,?)", nextId, jira, component);
			result.put(component, nextId);
			nextId++;
		}
		return result;
	}

	/**
	 * Add the given versions to the given project
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param versions
	 *            the version names to add
	 * @return the {@link Map} where value is the created version identifier.
	 */
	public Map<String, Integer> addVersions(final DataSource dataSource, final int jira, final Collection<String> versions) {
		final Map<String, Integer> result = new HashMap<>();
		final JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);
		int nextId = prepareForNextId(dataSource, VERSION_NODE, versions.size());
		int nextSequence = getNextVersionSequence(jira, jdbcTemplate);
		for (final String version : versions) {
			jdbcTemplate.update("INSERT INTO projectversion (ID,PROJECT,vname,SEQUENCE) values(?,?,?,?)", nextId, jira, version,
					nextSequence);
			result.put(version, nextId);
			nextId++;
			nextSequence++;
		}
		return result;
	}

	/**
	 * Return the next sequence of versions of given project.
	 */
	private int getNextVersionSequence(final int jira, final JdbcOperations jdbcTemplate) {
		final Long sequence = jdbcTemplate.queryForObject("SELECT MAX(SEQUENCE) FROM projectversion WHERE PROJECT = ?", Long.class, jira);
		final int nextSequence;
		if (sequence == null) {
			// First version
			nextSequence = 1;
		} else {
			nextSequence = sequence.intValue() + 1;
		}
		return nextSequence;
	}

	/**
	 * Update the sequence identifiers for a given sequence name.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param sequenceName
	 *            the sequence name, like "Version" or "Component"
	 * @param amount
	 *            the reserved amount.
	 * @return the starting reserved index.
	 */
	public int prepareForNextId(final DataSource dataSource, final String sequenceName, final int amount) {

		// Check the need to reserve a pool
		if (amount == 0) {
			return 0;
		}

		int updated;
		int currentSequence;
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		do {
			currentSequence = getCurrentSequence(sequenceName, jdbcTemplate);

			// Set the next sequence
			final int nextSequence = (currentSequence / 100) * 100 + ((int) Math.signum(currentSequence % 100)) * 100
					+ (amount / 100 + (int) Math.signum(amount % 100)) * 100;
			updated = jdbcTemplate.update("UPDATE SEQUENCE_VALUE_ITEM SET SEQ_ID = ? WHERE SEQ_NAME = ? AND SEQ_ID = ?", nextSequence,
					sequenceName, currentSequence);
		} while (updated == 0);

		return currentSequence;

	}

	/**
	 * Update the "pcounter" value for a given sequence project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param issues
	 *            the issues.
	 */
	private void reserveProjectCounter(final DataSource dataSource, final int jira, final List<JiraIssueRow> issues) {

		// Compute the maximal "issuenum" value
		int max = 0;
		for (final JiraIssueRow issueRow : issues) {
			if (issueRow.getIssueNum() > max) {
				max = issueRow.getIssueNum();
			}
		}

		// Update pcounter with the maximal issue number value
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("UPDATE project SET pcounter = ? WHERE ID = ? AND pcounter <= ?", max + 1, jira, max);
	}

	/**
	 * Return current sequence after updating it to reserve it if not defined.
	 * 
	 * @param sequenceName
	 *            The sequence name.
	 * @param jdbcTemplate
	 *            The current JDBC template.
	 * @return The current sequence value.
	 */
	protected int getCurrentSequence(final String sequenceName, final JdbcOperations jdbcTemplate) {
		final Long currentSequence;
		final List<Long> sequence = jdbcTemplate.queryForList("SELECT SEQ_ID FROM SEQUENCE_VALUE_ITEM WHERE SEQ_NAME = ?", Long.class,
				sequenceName);
		if (sequence.isEmpty()) {
			// New sequence, start from an arbitrary sequence to be sure JIRA
			// does not fill it
			currentSequence = 10000L;
			jdbcTemplate.update("INSERT INTO SEQUENCE_VALUE_ITEM (SEQ_NAME,SEQ_ID) values(?,?)", sequenceName, currentSequence);
		} else {
			currentSequence = sequence.get(0);
		}
		return currentSequence.intValue();
	}

	/**
	 * Create labels for given issues.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param issues
	 *            The issues to add. The ID property of each inserted issue is
	 *            also updated in these objects.
	 */
	public void addLabels(final DataSource dataSource, final List<JiraIssueRow> issues) {
		// Compute required amount of labels
		int amount = 0;
		for (final JiraIssueRow issue : issues) {
			amount += issue.getLabels().size();
		}

		// Persist the labels
		if (amount > 0) {
			final JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);
			int nextId = prepareForNextId(dataSource, "Label", amount);
			for (final JiraIssueRow issue : issues) {
				nextId = addLabels(jdbcTemplate, nextId, issue);
			}
		}
	}

	/**
	 * Add labels to given issue.
	 */
	private int addLabels(final JdbcOperations jdbcTemplate, final int nextId, final JiraIssueRow issue) {
		int nextIdl = nextId;
		// Remove previous labels
		jdbcTemplate.update("DELETE FROM label WHERE ISSUE = ? AND LABEL NOT IN (" + jiraDao.newIn(issue.getLabels()) + ")",
				ArrayUtils.addAll(new Object[] { issue.getId() }, issue.getLabels().toArray()));

		// Add missing labels
		for (final String label : issue.getLabels()) {
			jdbcTemplate.update("INSERT INTO label(ID,ISSUE,LABEL) values(?,?,?)", nextIdl, issue.getId(), label);
			nextIdl++;
		}
		return nextIdl;
	}

	/**
	 * Indicate the installation and activation of "script runner" plug-in.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param issues
	 *            The issues to add. The ID property of each inserted issue is
	 *            also updated in these objects.
	 * @param workflowStepMapping
	 *            the {@link Map} linking type identifier to a {@link Map}
	 *            linking status name to steps.
	 */
	public void addIssues(final DataSource dataSource, final int jira, final List<JiraIssueRow> issues,
			final Map<Integer, Workflow> workflowStepMapping) {
		final JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);
		int nextId = prepareForNextId(dataSource, ISSUE_NODE, issues.size());
		reserveProjectCounter(dataSource, jira, issues);
		int nextCurrentStepId = prepareForNextId(dataSource, "OSCurrentStep", issues.size());
		int nextWfEntryId = prepareForNextId(dataSource, "OSWorkflowEntry", issues.size());
		int counter = 0;
		for (final JiraIssueRow issueRow : issues) {
			issueRow.setId(nextId);
			log.info("Inserting issue {}-{}({}) {}/{}", issueRow.getPkey(), issueRow.getIssueNum(), issueRow.getId(), counter,
					issues.size());
			Workflow workflow = workflowStepMapping.get(issueRow.getType());
			if (workflow == null) {
				workflow = workflowStepMapping.get(0);
			}

			addIssue(jira, jdbcTemplate, nextId, nextCurrentStepId, nextWfEntryId, issueRow, workflow);
			nextId++;
			nextWfEntryId++;
			nextCurrentStepId++;
			counter++;
		}
	}

	/**
	 * Add an issue, workflow entry, corresponding step of current status and
	 * link author to this issue (user activity dashboard)
	 */
	private void addIssue(final int jira, final JdbcOperations jdbcTemplate, final int nextId, final int nextCurrentStepId,
			final int nextWfEntryId, final JiraIssueRow issueRow, final Workflow workflow) {
		final INamableBean<Integer> workflowStep = workflow.getStatusToSteps().get(issueRow.getStatusText());

		// Insert workflow activity
		jdbcTemplate.update("INSERT INTO OS_WFENTRY (ID,NAME,STATE) values(?,?,?)", nextWfEntryId, workflow.getName(), 1);
		jdbcTemplate.update("INSERT INTO OS_CURRENTSTEP (ID,ENTRY_ID,STEP_ID,ACTION_ID,START_DATE,STATUS) values(?,?,?,?,?,?)",
				nextCurrentStepId, nextWfEntryId, workflowStep.getId(), 0, issueRow.getCreated(), workflowStep.getName());

		// Insert issue
		jdbcTemplate.update("INSERT INTO jiraissue"
				+ " (ID,issuenum,WATCHES,VOTES,PROJECT,REPORTER,ASSIGNEE,CREATOR,issuetype,SUMMARY,DESCRIPTION,PRIORITY,RESOLUTION,RESOLUTIONDATE,"
				+ "issuestatus,CREATED,UPDATED,WORKFLOW_ID,DUEDATE) values(?,?,1,0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", nextId,
				issueRow.getIssueNum(), jira, issueRow.getReporter(), issueRow.getAssignee(), issueRow.getAuthor(), issueRow.getType(),
				issueRow.getSummary(), issueRow.getDescription(), issueRow.getPriority(), issueRow.getResolution(),
				issueRow.getResolutionDate(), issueRow.getStatus(), issueRow.getCreated(), issueRow.getUpdated(), nextWfEntryId,
				issueRow.getDueDate());

		// Add user relation
		jdbcTemplate.update(
				"INSERT INTO userassociation (SOURCE_NAME,SINK_NODE_ID,SINK_NODE_ENTITY,ASSOCIATION_TYPE,CREATED) values(?,?,?,?,?)",
				issueRow.getAuthor(), nextId, ISSUE_NODE, "WatchIssue", issueRow.getUpdated());
	}

	/**
	 * Associate issue to components and versions. All related entries must
	 * exist.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param issues
	 *            The issues to add.
	 */
	public void associateComponentsAndVersions(final DataSource dataSource, final List<JiraIssueRow> issues) {
		final JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);
		for (final JiraIssueRow issueRow : issues) {
			final int issueId = issueRow.getId();
			associatedItems(jdbcTemplate, issueId, issueRow.getComponents(), COMPONENT_NODE, "IssueComponent");
			associatedItems(jdbcTemplate, issueId, issueRow.getVersions(), VERSION_NODE, "IssueVersion");
			associatedItems(jdbcTemplate, issueId, issueRow.getFixedVersions(), VERSION_NODE, "IssueFixVersion");
		}
	}

	/**
	 * Associate an issue to a set of items;
	 */
	private void associatedItems(final JdbcOperations jdbcTemplate, final int issueId, final Collection<Integer> items,
			final String nodetype, final String associationType) {
		for (final Integer item : items) {
			jdbcTemplate
					.update("INSERT INTO nodeassociation (SOURCE_NODE_ID,SOURCE_NODE_ENTITY,SINK_NODE_ID,SINK_NODE_ENTITY,ASSOCIATION_TYPE)"
							+ " values(?,?,?,?,?)", issueId, ISSUE_NODE, item, nodetype, associationType);
		}
	}

	/**
	 * Associate issue to custom values. Custom component must exist.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param issues
	 *            The issues to add.
	 * @param customFields
	 *            Existing custom field definitions.
	 */
	public void associateCustomFieldsValues(final DataSource dataSource, final List<JiraIssueRow> issues,
			final Map<String, CustomFieldEditor> customFields) {
		final JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);

		// Compute total amount of custom values to create
		int nextId = prepareForNextId(dataSource, "CustomFieldValue", countCustomFieldValues(issues));
		for (final JiraIssueRow issueRow : issues) {
			nextId = associateCutomFieldValues(customFields, jdbcTemplate, nextId, issueRow);
		}
	}

	/**
	 * Count amount of custom field values of given issues.
	 */
	private int countCustomFieldValues(final List<JiraIssueRow> issues) {
		int customFieldValues = 0;
		for (final JiraIssueRow issueRow : issues) {
			for (final Object cfValue : issueRow.getCustomFields().values()) {
				if (cfValue instanceof Collection) {
					// Multi-values
					customFieldValues += ((Collection<?>) cfValue).size();
				} else {
					// Single value
					customFieldValues++;
				}
			}
		}
		return customFieldValues;
	}

	/**
	 * Associate custom field values to a given issue.
	 */
	@SuppressWarnings("unchecked")
	private int associateCutomFieldValues(final Map<String, CustomFieldEditor> customFields, final JdbcOperations jdbcTemplate,
			final int nextId, final JiraIssueRow issueRow) {
		int nextIdl = nextId;
		for (final Entry<String, Object> entry : issueRow.getCustomFields().entrySet()) {
			final String cfName = entry.getKey();
			final CustomField customField = customFields.get(cfName);
			final int cfId = customField.getId();
			final Object cfValue = entry.getValue();

			// Determine the right 'customfieldvalue' column
			final String column = JiraDao.MANAGED_TYPE.get(customField.getFieldType()).getCustomColumn();
			final Collection<Object> values;
			if (cfValue instanceof Collection) {
				// Multi-values
				values = (Collection<Object>) cfValue;
			} else {
				// Single value
				values = new ArrayList<>(1);
				values.add(cfValue);
			}
			nextIdl = associateCustomFieldValue(jdbcTemplate, issueRow.getId(), nextIdl, cfId, column, values);

		}
		return nextIdl;
	}

	/**
	 * Associate a single custom field values to a given issue.
	 */
	private int associateCustomFieldValue(final JdbcOperations jdbcTemplate, final int issueId, final int nextId, final int cfId,
			final String column, final Iterable<Object> values) {
		int nextIdl = nextId;
		// Persist single/multiple values
		for (final Object value : values) {
			jdbcTemplate.update("INSERT INTO customfieldvalue (ID,ISSUE,CUSTOMFIELD," + column + ") values(?,?,?,?)", nextIdl, issueId,
					cfId, value);
			nextIdl++;
		}
		return nextIdl;
	}

	/**
	 * Return valid types with associated Worflow for the given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param buildStatusChanges
	 *            the status changes.
	 */
	public void addChanges(final DataSource dataSource, final Map<Integer, List<JiraChangeRow>> buildStatusChanges) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		// Compute total change items to add
		int totalChanges = 0;
		for (final List<JiraChangeRow> changes : buildStatusChanges.values()) {
			totalChanges += changes.size();
		}

		int nextChangeItemId = prepareForNextId(dataSource, "ChangeItem", totalChanges);
		int nextChangeGroupId = prepareForNextId(dataSource, "ChangeGroup", totalChanges);
		for (final Map.Entry<Integer, List<JiraChangeRow>> changes : buildStatusChanges.entrySet()) {
			for (final JiraChangeRow change : changes.getValue()) {

				// Add 'changegroup'
				jdbcTemplate.update("INSERT INTO changegroup (ID,issueid,AUTHOR,CREATED) values(?,?,?,?)", nextChangeGroupId,
						change.getId(), change.getAuthor(), change.getDate());

				// Add 'changeitem'
				jdbcTemplate.update(
						"INSERT INTO changeitem (ID,groupId,FIELDTYPE,FIELD,OLDVALUE,OLDSTRING,NEWVALUE,NEWSTRING) values(?,?,?,?,?,?,?,?)",
						nextChangeItemId, nextChangeGroupId, "jira", "status", change.getFromStatus(), change.getFromStatusText(),
						change.getToStatus(), change.getToStatusText());

				nextChangeItemId++;
				nextChangeGroupId++;
			}
		}
	}
}
