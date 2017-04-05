package org.ligoj.app.plugin.bt.jira.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ligoj.app.iam.Activity;
import org.ligoj.app.plugin.bt.jira.JiraProject;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.editor.DateEditor;
import org.ligoj.app.plugin.bt.jira.editor.DatePickerEditor;
import org.ligoj.app.plugin.bt.jira.editor.FailsafeEditor;
import org.ligoj.app.plugin.bt.jira.editor.FloatEditor;
import org.ligoj.app.plugin.bt.jira.editor.IdEditor;
import org.ligoj.app.plugin.bt.jira.editor.IdentityEditor;
import org.ligoj.app.plugin.bt.jira.editor.MultipleIdEditor;
import org.ligoj.app.plugin.bt.jira.editor.UrlEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.app.plugin.bt.model.ChangeItem;
import org.ligoj.app.plugin.bt.model.IssueDetails;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * DAO for JIRA services.
 * 
 * @see "https://confluence.atlassian.com/display/JIRA041/Example+SQL+queries+for+JIRA"
 */
@Component
public class JiraDao {

	private static final String STATUS_OPEN = "1";

	/**
	 * Fail safe editor, non blocking export.
	 */
	public static final AbstractEditor FAILSAFE_TYPE = new FailsafeEditor();

	/**
	 * Well known types, but not yet implemented :
	 * <ul>
	 * <li>"com.atlassian.jira.plugin.system.customfieldtypes:grouppicker"</li>
	 * <li>"com.atlassian.jira.plugin.system.customfieldtypes:userpicker"</li>
	 * <li>"com.atlassian.jira.plugin.system.customfieldtypes:version"</li>
	 * <li>"com.atlassian.jira.plugin.system.customfieldtypes:multiversion"</li>
	 * </ul>
	 * Well known types, and fully implemented
	 */
	protected static final Map<String, AbstractEditor> MANAGED_TYPE = new HashMap<>();
	static {
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:textarea", new IdentityEditor() {
			@Override
			public String getCustomColumn() {
				return "TEXTVALUE";
			}

		});
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:url", new UrlEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:datepicker", new DatePickerEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:float", new FloatEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:textfield", new IdentityEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:datetime", new DateEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes", new MultipleIdEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:select", new IdEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons", new IdEditor());
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:multiselect", new MultipleIdEditor());

		// Only id -> string support
		MANAGED_TYPE.put("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect", new IdEditor() {
			@Override
			public Object getValue(final CustomField customField, final String value) {
				throw new ValidationJsonException("cf$" + customField.getName(),
						"Custom field '" + customField.getName() + "' has a not yet managed type '" + customField.getFieldType() + "'");
			}

		});

	}

	/**
	 * Return all status changes of issues of given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param pkey
	 *            the project 'pkey'.
	 * @param resultType
	 *            the bean type to build in result list.
	 * @param timing
	 *            When <code>true</code> time spent data is fetched.
	 * @param summary
	 *            When <code>true</code> Summary is fetched.
	 * @return status changes of all issues of given project.
	 */
	public <T> List<T> getChanges(final DataSource dataSource, final int jira, final String pkey, final Class<T> resultType, final boolean timing,
			final boolean summary) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<T> rowMapper = new BeanPropertyRowMapper<>(resultType);

		// First, get all created issues (first change)
		final List<T> issues;
		final String sqlPart = ", RESOLUTION AS resolution, PRIORITY AS priority, issuestatus AS status, ASSIGNEE AS assignee,"
				+ " REPORTER AS reporter, issuetype AS type, ? AS toStatus, DUEDATE AS dueDate, created"
				+ (timing ? ", TIMESPENT AS timeSpent, TIMEESTIMATE AS timeEstimate, TIMEORIGINALESTIMATE AS timeEstimateInit" : "")
				+ (summary ? ", SUMMARY AS summary" : "") + "  FROM jiraissue WHERE PROJECT = ?";
		if (getJiraVersion(dataSource).compareTo("6.0.0") < 0) {
			// JIRA 4-5 implementation, use "pkey"
			issues = jdbcTemplate.query("SELECT ID AS id, pkey AS pkey" + sqlPart, rowMapper, STATUS_OPEN, jira);
		} else {
			// JIRA 6+, "pkey" is no more available in the 'jiraissue' table
			issues = jdbcTemplate.query("SELECT ID AS id, CONCAT(?, issuenum) AS pkey" + sqlPart, rowMapper, pkey + "-", STATUS_OPEN, jira);
		}
		return issues;
	}

	/**
	 * Return all status changes of issues of given project and last known issue values : due date, resolution,
	 * reporter,... in type {@link IssueDetails}.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param pkey
	 *            the project 'pkey'.
	 * @param authoring
	 *            When <code>true</code> authors are fetched for changes.
	 * @param timing
	 *            When <code>true</code> time spent data is fetched.
	 * @return status changes of all issues of given project.
	 */
	public List<ChangeItem> getChanges(final DataSource dataSource, final int jira, final String pkey, final boolean authoring,
			final boolean timing) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<ChangeItem> rowMapper = new BeanPropertyRowMapper<>(ChangeItem.class);

		// First, get all created issues (first change)
		final List<ChangeItem> changes = getChanges(dataSource, jira, pkey, ChangeItem.class, timing, false);

		// Then add all status changes
		changes.addAll(jdbcTemplate.query(
				"SELECT i.ID AS id, cgi.OLDVALUE AS fromStatus, cgi.NEWVALUE AS toStatus, cg.CREATED AS created"
						+ (authoring ? ", cg.AUTHOR as author" : "")
						+ " FROM changeitem cgi INNER JOIN changegroup AS cg ON (cgi.groupid = cg.ID) INNER JOIN jiraissue AS i ON (cg.issueid = i.ID)"
						+ " WHERE cgi.FIELD = ? AND cgi.OLDVALUE IS NOT NULL AND cgi.NEWVALUE IS NOT NULL AND cg.CREATED IS NOT NULL AND i.PROJECT = ?",
				rowMapper, "status", jira));

		/*
		 * Then sort the result by "created" date. The previous SQL query did not used since order had to be applied to
		 * the whole collection. In addition, the result set of the previous query "should already been ordered since
		 * the natural order in this table is chronological.
		 */
		changes.sort(Comparator.comparing(IssueDetails::getCreated));
		return changes;
	}

	/**
	 * Return all custom fields values attached to an issue of a project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return all custom fields attached to an issue of a project ordered by issue.
	 */
	public List<CustomFieldValue> getCustomFieldValues(final DataSource dataSource, final int jira) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<CustomFieldValue> rowMapper = new BeanPropertyRowMapper<>(CustomFieldValue.class);
		return jdbcTemplate.query(
				"SELECT cv.STRINGVALUE AS stringValue,cv.NUMBERVALUE AS numberValue,cv.TEXTVALUE AS textValue,cv.DATEVALUE AS dateValue, cv.CUSTOMFIELD AS customField, cv.ISSUE AS issue"
						+ " FROM customfieldvalue cv INNER JOIN jiraissue i ON (i.ID = cv.ISSUE AND i.PROJECT = ?) ORDER BY cv.ISSUE,cv.CUSTOMFIELD",
				rowMapper, jira);
	}

	/**
	 * Return project definitions where name or PKEY contains a string.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param nameIdOrKey
	 *            the JIRA project name, PKEY or identifier.
	 * @return the project definitions.
	 */
	public List<JiraProject> findProjectsByName(final DataSource dataSource, final String nameIdOrKey) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<JiraProject> rowMapper = new BeanPropertyRowMapper<>(JiraProject.class);
		return jdbcTemplate.query(
				"SELECT pkey AS name, ID AS id, pname AS description FROM project"
						+ " WHERE UPPER(pkey) LIKE CONCAT(CONCAT('%',UPPER(?)),'%') OR  UPPER(pname) LIKE CONCAT(CONCAT('%',UPPER(?)),'%') OR ID = ? ORDER BY pname",
				rowMapper, nameIdOrKey, nameIdOrKey, NumberUtils.toInt(nameIdOrKey));
	}

	/**
	 * Get the project's name by its PKEY and its identifier including :
	 * <ul>
	 * <li>Counts for each provided status</li>
	 * <li>Counts for each provided priorities</li>
	 * </ul>
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return the project's name or <code>null</code>.
	 */
	public JiraProject getProject(final DataSource dataSource, final int jira) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<Integer, String> priorities = getPriorities(dataSource);
		final Map<Integer, String> statuses = getStatuses(dataSource);
		final StringBuilder query = new StringBuilder(100 * (1 + priorities.size() + statuses.size())); // NOPMD

		// Fetch basic projetc definition
		query.append("SELECT pkey, ID, pname"); // NOPMD - capacity well defined

		// Fetch priorities count
		priorities.forEach((id, name) -> query.append(',').append(newSelectCount("PRIORITY", id, "p")));

		// Fetch statuses count
		statuses.forEach((id, name) -> query.append(',').append(newSelectCount("issuestatus", id, "s")));

		// Execute the query for maximal 1 project
		query.append(" FROM project WHERE ID = ? GROUP BY ID");
		final Object[] dummyProjet = new Object[priorities.size() + statuses.size() + 1];
		Arrays.fill(dummyProjet, jira);
		return jdbcTemplate.query(query.toString(), (rs, rowNum) -> {
			final JiraProject project = new JiraProject();
			project.setName(rs.getString("pkey"));
			project.setId(rs.getInt("ID"));
			project.setDescription(rs.getString("pname"));

			// Add priorities count
			project.setPriorities(toMapCount(priorities, rs, "p"));

			// Add statuses count
			project.setStatuses(toMapCount(statuses, rs, "s"));
			return project;
		}, dummyProjet).stream().findFirst().orElse(null);
	}

	/**
	 * Return a count on table <code>jiraissue</code> for unresolved issues and column matching to the given identifier.
	 * 
	 * @param column
	 *            Column to be filtered.
	 * @param id
	 *            Filtered value against the column.
	 * @param prefixAlias
	 *            The prefix used for alias. Alias would be <code>prefixAlias+id</code>.
	 * @return a count on table <code>jiraissue</code> for unresolved issues and column matching to the given identifier.
	 */
	private String newSelectCount(final String column, final Integer id, final String prefixAlias) {
		return "(SELECT COUNT(ID) FROM jiraissue WHERE " + column + "=" + id + " AND PROJECT=? AND RESOLUTION IS NULL) AS " + prefixAlias + id;
	}

	/**
	 * Return a map where an entry is
	 * <code>entry.key=reverseMap.value</code> and <code>entry.value=rs[resulsetPrefix+reverseMap.key]</code> if
	 * <code>entry.value</code> is superior than 0.
	 * 
	 * @param reverseMap
	 *            the set of results to read from the {@link ResultSet}. K of this map is used to extract the count.
	 * @param rs
	 *            the current {@link ResultSet}
	 * @param resulsetPrefix
	 *            Prefix of column name of result set.
	 * @return K is the value of given reverse map. V is the non zero result column named with the following form :
	 *         prefix+K of given reverse map.
	 */
	private Map<String, Integer> toMapCount(final Map<Integer, String> reverseMap, final ResultSet rs, final String resulsetPrefix)
			throws SQLException {
		final Map<String, Integer> result = new LinkedHashMap<>();
		for (final Entry<Integer, String> entry : reverseMap.entrySet()) {
			// Add only non zero values
			final int count = rs.getInt(resulsetPrefix + entry.getKey());
			if (count > 0) {
				result.put(entry.getValue(), count);
			}
		}
		return result;
	}

	/**
	 * Return the JIRA version.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @return JIRA version.
	 */
	public String getJiraVersion(final DataSource dataSource) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate.queryForObject("SELECT pluginversion FROM pluginversion WHERE pluginkey = ?", String.class, "com.atlassian.jira.ext.rpc");
	}

	/**
	 * Return all priorities : identifier and text
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @return all priorities labels.
	 */
	public Map<Integer, String> getPriorities(final DataSource dataSource) {
		return AbstractEditor.getMap(dataSource, "SELECT p.ID AS id, p.pname AS pname FROM priority p ORDER BY id");
	}

	/**
	 * Return all resolutions : identifier and text
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @return all resolutions labels.
	 */
	public Map<Integer, String> getResolutions(final DataSource dataSource) {
		return AbstractEditor.getMap(dataSource, "SELECT p.ID AS id, p.pname AS pname FROM resolution p ORDER BY SEQUENCE");
	}

	/**
	 * Return all types associated to given project : identifier and text
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return all types labels. Key is the identifier.
	 */
	public Map<Integer, String> getTypes(final DataSource dataSource, final int jira) {
		return AbstractEditor.getMap(dataSource,
				"SELECT ID AS id, pname FROM issuetype AS i, (SELECT OPTIONID FROM optionconfiguration AS o,"
						+ "(SELECT FIELDCONFIGSCHEME FROM configurationcontext WHERE customfield = ? AND "
						+ "(PROJECT = ? OR PROJECT IS NULL AND NOT EXISTS (SELECT FIELDCONFIGSCHEME FROM configurationcontext WHERE customfield = ? AND PROJECT = ?))"
						+ ") AS f WHERE o.FIELDCONFIG IN (f.FIELDCONFIGSCHEME)) AS t WHERE i.ID IN (t.OPTIONID) ORDER BY id",
				"issuetype", jira, "issuetype", jira);
	}

	/**
	 * Return all statuses.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @return all existing statuses.
	 */
	public Map<Integer, String> getStatuses(final DataSource dataSource) {
		return AbstractEditor.getMap(dataSource, "SELECT ID AS id, pname AS pname FROM issuestatus ORDER BY pname");
	}

	/**
	 * Return all statuses identifiers and names matching either to the given name, either to the given identifier.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param statuses
	 *            the expected status identifiers.
	 * @param statusesAsText
	 *            the expected status labels.
	 * @return all statuses referenced in the given project.
	 */
	public Map<Integer, String> getStatuses(final DataSource dataSource, final Collection<Integer> statuses,
			final Collection<String> statusesAsText) {
		final Collection<Object> parameters = new ArrayList<>();
		parameters.addAll(statusesAsText);
		parameters.addAll(statuses);
		return AbstractEditor.getMap(dataSource, "SELECT DISTINCT(s.ID) AS id, s.pname AS pname FROM issuestatus AS s WHERE s.pname IN ("
				+ newIn(statusesAsText) + ") OR s.ID IN (" + newIn(statuses) + ") ORDER BY pname", parameters.toArray());
	}

	/**
	 * Return the "IN" SQL query parameters like : ?,?,?
	 */
	protected String newIn(final Collection<?> items) {
		if (items.isEmpty()) {
			return "null";
		}
		return StringUtils.repeat("?", ",", items.size());
	}

	/**
	 * Return all custom fields matching to the given names
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param customFields
	 *            the expected custom fields names.
	 * @param project
	 *            Jira project identifier. Required to filter custom field against contexts.
	 * @return all custom field configurations referenced in the given project and matching the required names.
	 */
	public Map<String, CustomFieldEditor> getCustomFields(final DataSource dataSource, final Set<String> customFields, final int project) {
		if (customFields.isEmpty()) {
			// No custom field, we save useless queries
			return new HashMap<>();
		}

		// Get map as list
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<CustomFieldEditor> rowMapper = new BeanPropertyRowMapper<>(CustomFieldEditor.class);
		final List<CustomFieldEditor> resultList = jdbcTemplate
				.query("SELECT cf.ID AS id, TRIM(cf.cfname) AS name, cf.DESCRIPTION AS description, cf.CUSTOMFIELDTYPEKEY AS fieldType FROM customfield AS cf WHERE TRIM(cf.cfname) IN ("
						+ newIn(customFields) + ")", rowMapper, customFields.toArray());

		// Also add the translated items
		final List<CustomFieldEditor> resultListTranslated = jdbcTemplate.query(
				"SELECT cf.ID AS id, TRIM(cf.cfname) AS originalName, TRIM(ps.propertyvalue) AS name, cf.cfname AS originalName, cf.DESCRIPTION AS description, "
						+ "cf.CUSTOMFIELDTYPEKEY AS fieldType "
						+ "FROM customfield AS cf INNER JOIN propertyentry AS pe ON pe.ENTITY_ID = cf.ID INNER JOIN propertystring AS ps ON pe.ID = ps.ID "
						+ "WHERE pe.ENTITY_NAME=? AND PROPERTY_KEY LIKE ? AND TRIM(ps.propertyvalue) IN (" + newIn(customFields) + ")",
				rowMapper, ArrayUtils.addAll(new String[] { "CustomField", "%FR" }, customFields.toArray()));

		// Make a Map of valid values for single/multi select values field
		final Map<String, CustomFieldEditor> result = new HashMap<>();
		addListToMap(dataSource, resultList, result, project);
		addListToMap(dataSource, resultListTranslated, result, project);
		return result;
	}

	/**
	 * Return ordered custom fields by the given identifiers
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param customFields
	 *            the expected custom fields identifiers.
	 * @param project
	 *            Jira project identifier. Required to filter custom field agains contexts.
	 * @return ordered custom fields by their identifier.
	 */
	public Map<Integer, CustomFieldEditor> getCustomFieldsById(final DataSource dataSource, final Set<Integer> customFields, final int project) {
		if (customFields.isEmpty()) {
			// No custom field, we save an useless query
			return new HashMap<>();
		}

		// Get map as list
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final RowMapper<CustomFieldEditor> rowMapper = new BeanPropertyRowMapper<>(CustomFieldEditor.class);
		final List<CustomFieldEditor> resultList = jdbcTemplate
				.query("SELECT ID AS id, TRIM(cfname) AS name, DESCRIPTION AS description, CUSTOMFIELDTYPEKEY AS fieldType FROM customfield WHERE ID IN ("
						+ newIn(customFields) + ") ORDER BY id", rowMapper, customFields.toArray());

		// Make a Map of valid values for single/multi select values field
		final Map<Integer, CustomFieldEditor> result = new LinkedHashMap<>();
		addListToMapIdentifier(dataSource, resultList, result, project);
		return result;
	}

	/**
	 * Populate the custom field using the editor.
	 */
	private void updateCustomFieldEditor(final DataSource dataSource, final CustomFieldEditor customField, final int project,
			final AbstractEditor editor) {

		// Get values of configuration
		customField.setEditor(editor);
		editor.populateValues(dataSource, customField, project);
	}

	/**
	 * Return the custom field editor of given custom field.
	 */
	private void buildEditor(final DataSource dataSource, final CustomFieldEditor customField, final int project) {
		// Get editor for this custom field
		updateCustomFieldEditor(dataSource, customField, project,
				ObjectUtils.defaultIfNull(MANAGED_TYPE.get(customField.getFieldType()), FAILSAFE_TYPE));
	}

	/**
	 * Add custom field list to the result map. Key is the identifier.
	 */
	private void addListToMapIdentifier(final DataSource dataSource, final List<CustomFieldEditor> resultList,
			final Map<Integer, CustomFieldEditor> result, final int project) {
		for (final CustomFieldEditor customField : resultList) {

			// Get editor for this custom field
			buildEditor(dataSource, customField, project);
			result.put(customField.getId(), customField);
		}
	}

	/**
	 * Add custom field list to the result map. Key is the name.
	 */
	private void addListToMap(final DataSource dataSource, final List<CustomFieldEditor> resultList, final Map<String, CustomFieldEditor> result,
			final int project) {
		for (final CustomFieldEditor customField : resultList) {
			if (result.containsKey(customField.getName()) && !result.get(customField.getName()).getId().equals(customField.getId())) {
				// Duplicate custom field names
				throw new ValidationJsonException("cf$" + customField.getName(), "There are several custom fields named '" + customField.getName()
						+ "', ambiguous identifier : " + customField.getId() + ", " + result.get(customField.getName()).getId());
			}

			// Get editor for this custom field
			buildEditor(dataSource, customField, project);
			result.put(customField.getName(), customField);
		}
	}

	/**
	 * Return a set of required users having been found in the database.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param requiredUsers
	 *            The required users.
	 * @return the existing users.
	 */
	public List<String> getUsers(final DataSource dataSource, final Set<String> requiredUsers) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate.queryForList("SELECT user_name FROM cwd_user WHERE user_name IN (" + newIn(requiredUsers) + ")", String.class,
				requiredUsers.toArray());
	}

	/**
	 * Return existing components for the given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return existing components for the given project. Key is the identifier.
	 */
	public Map<Integer, String> getComponents(final DataSource dataSource, final int jira) {
		return AbstractEditor.getMap(dataSource, "SELECT c.ID AS id, c.cname AS pname FROM component AS c WHERE c.PROJECT = ?", jira);
	}

	/**
	 * Return existing versions for the given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return existing versions for the given project.
	 */
	public Map<Integer, String> getVersions(final DataSource dataSource, final int jira) {
		return AbstractEditor.getMap(dataSource, "SELECT v.ID AS id, v.vname AS pname FROM projectversion AS v WHERE v.PROJECT = ?", jira);
	}

	/**
	 * Return existing issues identifier for the given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param pkey
	 *            the project 'pkey'.
	 * @return existing labels for the given project.
	 */
	public List<JiraIssueRow> getIssues(final DataSource dataSource, final int jira, final String pkey) {
		return getChanges(dataSource, jira, pkey, JiraIssueRow.class, false, true);
	}

	/**
	 * Return existing issues identifier for the given project. Min and max bounds are there to reduce the amount of
	 * issues to get with this query.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @param minIssue
	 *            the minimal issue number.
	 * @param maxIssue
	 *            the maximal issue number.
	 * @param importIssues
	 *            the issues to import (INSERT or UPDATE ?).
	 * @return existing labels for the given project.
	 */
	public Map<Integer, IssueWithCollections> getIssues(final DataSource dataSource, final int jira, final int minIssue, final int maxIssue,
			final Set<Integer> importIssues) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Set<Integer> existing = new HashSet<>(jdbcTemplate.queryForList(
				"SELECT issuenum FROM jiraissue WHERE PROJECT = ? AND issuenum >= ? AND issuenum <= ?", Integer.class, jira, minIssue, maxIssue));
		final Collection<Integer> updatingIssues = CollectionUtils.intersection(existing, importIssues);
		final Map<Integer, IssueWithCollections> result = new LinkedHashMap<>();
		if (!updatingIssues.isEmpty()) {
			final RowMapper<IssueWithCollections> rowMapper = new BeanPropertyRowMapper<>(IssueWithCollections.class);
			final List<IssueWithCollections> issues = jdbcTemplate.query("SELECT i.ID AS id, i.issuenum AS issue"
					+ " FROM jiraissue AS i WHERE i.PROJECT = ? AND i.issuenum IN (" + newIn(updatingIssues) + ") ORDER BY i.issuenum", rowMapper,
					ArrayUtils.addAll(new Object[] { jira }, updatingIssues.toArray()));
			for (final IssueWithCollections issue : issues) {
				result.put(issue.getId(), issue);
			}
		}
		return result;
	}

	/**
	 * Return workflow definition.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param workflow
	 *            the workflow's name.
	 * @return The workflow XML description.
	 */
	public String getWorflow(final DataSource dataSource, final String workflow) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final List<String> workflows = jdbcTemplate.queryForList("SELECT DESCRIPTOR FROM jiraworkflows WHERE workflowname=?", String.class, workflow);
		if (workflows.isEmpty()) {

			// Implicit 'jira' workflow case
			return null;
		}
		return workflows.get(0);
	}

	/**
	 * Return all components attached to an issue of a project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return all custom fields attached to an issue of a project ordered by issue. Key is the issue.
	 */
	public Map<Integer, Collection<Integer>> getComponentsAssociation(final DataSource dataSource, final int jira) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<Integer, Collection<Integer>> result = new HashMap<>();

		jdbcTemplate.query(
				"SELECT n.SOURCE_NODE_ID AS issue, n.SINK_NODE_ID AS c"
						+ " FROM nodeassociation AS n INNER JOIN jiraissue AS i ON (i.ID = n.SOURCE_NODE_ID AND i.PROJECT = ?)"
						+ " WHERE n.SOURCE_NODE_ENTITY=? AND n.SINK_NODE_ENTITY=? AND n.ASSOCIATION_TYPE=?",
				(RowCallbackHandler) rs -> result.computeIfAbsent(rs.getInt("issue"), k -> new ArrayList<>()).add(rs.getInt("c")), jira, "Issue",
				"Component", "IssueComponent");
		return result;
	}

	/**
	 * Return valid types with associated workflow for the given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param jira
	 *            the JIRA project identifier.
	 * @return a {@link Map} were KEY is the type's identifier, and VALUE is the workflow's name.
	 */
	public Map<Integer, String> getTypesToWorkflow(final DataSource dataSource, final int jira) {
		final Map<Integer, String> result = AbstractEditor.getMap(dataSource,
				"SELECT issuetype AS id, WORKFLOW AS pname FROM workflowschemeentity WHERE"
						+ " SCHEME = (SELECT SINK_NODE_ID FROM nodeassociation WHERE SOURCE_NODE_ID=? AND SOURCE_NODE_ENTITY=? AND SINK_NODE_ENTITY=? AND ASSOCIATION_TYPE=?)",
				jira, "Project", "WorkflowScheme", "ProjectScheme");
		if (result.isEmpty()) {
			// Default workflow scheme 'jira' is used.
			return AbstractEditor.getMap(dataSource, "SELECT id, ? AS pname FROM issuetype", "jira");
		}
		return result;
	}

	/**
	 * Indicate the installation of "script runner" plug-in.
	 * 
	 * @see "https://jamieechlin.atlassian.net/wiki/display/GRV/Script+Runner"
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @return <code>true</code> if Script Runner plug-in is installed.
	 */
	public boolean hasScriptRunnerPlugin(final DataSource dataSource) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pluginversion p WHERE  p.PLUGINKEY = ?", Integer.class,
				"com.onresolve.jira.groovy.groovyrunner") == 1;
	}

	/**
	 * Query JIRA database to collect activities of given users. For now, only the last success connection is
	 * registered.
	 * 
	 * @param dataSource The JIRA datasource to query the user activities.
	 * @param users
	 *            the users to query.
	 * @return activities.
	 */
	public Map<String, Activity> getActivities(final DataSource dataSource, final Collection<String> users) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<String, Activity> result = new HashMap<>();
		if (!users.isEmpty()) {
			jdbcTemplate.query("SELECT u.lower_user_name AS login, attribute_value AS value FROM cwd_user AS u"
					+ " INNER JOIN cwd_user_attributes AS ua ON ua.user_id = u.ID WHERE u.lower_user_name IN (" + newIn(users)
					+ ") AND attribute_name=?;", rs -> {
						final Activity activity = new Activity();
						activity.setLastConnection(new Date(Long.valueOf(rs.getString("value"))));
						result.put(rs.getString("login"), activity);
					}, ArrayUtils.add(users.toArray(), "lastAuthenticated"));
		}
		return result;
	}

	/**
	 * Return all parent relationships of tickets of given project.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param project
	 *            Jira project identifier. Required to filter custom field against contexts.
	 * @return all parent relationships of tickets of given project. Key is the subtask, value is the parent issue.
	 */
	public Map<Integer, Integer> getSubTasks(final DataSource dataSource, final int project) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<Integer, Integer> result = new HashMap<>();
		jdbcTemplate.query(
				"SELECT l.SOURCE AS parent,l.DESTINATION AS subTask FROM issuelink AS l"
						+ " INNER JOIN issuelinktype AS t ON (l.LINKTYPE = t.ID AND t.LINKNAME=?)"
						+ " INNER JOIN jiraissue AS i ON (i.ID = l.SOURCE AND i.PROJECT = ?);",
				(RowCallbackHandler) rs -> result.put(rs.getInt("subTask"), rs.getInt("parent")), "jira_subtask_link", project);
		return result;
	}

	/**
	 * Reset the login failed to bypass CAPTCHA guard.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param login
	 *            The user login to reset.
	 */
	public void clearLoginFailed(final DataSource dataSource, final String login) {
		// Reset the CAPTCHA only for Jira 6+
		if (getJiraVersion(dataSource).compareTo("6.0.0") >= 0) {
			final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			final List<Integer> ids = jdbcTemplate.query("SELECT ID FROM cwd_user WHERE lower_user_name = ?", (rs, n) -> rs.getInt("ID"),
					login.toLowerCase(Locale.ENGLISH));
			if (!ids.isEmpty()) {
				jdbcTemplate.update("UPDATE cwd_user_attributes SET attribute_value=0, lower_attribute_value=0 WHERE attribute_name=? AND user_id=?",
						"login.currentFailedCount", ids.get(0));
			}
		}
	}

}
