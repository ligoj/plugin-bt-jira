package org.ligoj.app.plugin.bt.jira.out;

import java.io.IOException;
import java.io.Writer;
import java.text.Format;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.bt.IssueSla;
import org.ligoj.app.plugin.bt.jira.JiraSlaComputations;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.app.plugin.bt.model.IssueDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV output writer from Jira issues data and custom field values.
 */
@Slf4j
public class CsvWithCustomFieldsStreamingOutput extends CsvStreamingOutput {

	/**
	 * All custom field values ordered by issue and custom field.
	 */
	private final List<CustomFieldValue> customFieldValues;

	/**
	 * Custom field configurations, ordered by identifier.
	 */
	private final Map<Integer, CustomFieldEditor> customFields;

	/**
	 * Custom field values cursor.
	 */
	private int issueAndCustomFieldsCursor = 0;

	/**
	 * All association between issues and components. Key is the issue, Value are the associated components.
	 */
	private final Map<Integer, Collection<Integer>> componentAssociations;

	/**
	 * All components associated to this project, ordered by name.
	 */
	private final Map<Integer, String> components;

	/**
	 * Subtask relationship. Key is the identifier of the subtask. Value is the parent.
	 */
	private final Map<Integer, Integer> subTasks;

	/**
	 * Constructor for database offline data.
	 * 
	 * @param slaComputations
	 *            SLA computations with issues.
	 * @param customFieldValues
	 *            All custom field values ordered by issue and custom field.
	 * @param customFields
	 *            Custom field configurations, ordered by identifier.
	 * @param components
	 *            All components associated to this project, ordered by name.
	 * @param componentAssociations
	 *            All association between issues and components. Key is the issue, Value are the associated components.
	 * @param subTasks
	 *            Subtask relationship. Key is the identifier of the subtask. Value is the parent.
	 */
	public CsvWithCustomFieldsStreamingOutput(final JiraSlaComputations slaComputations, final List<CustomFieldValue> customFieldValues,
			final Map<Integer, CustomFieldEditor> customFields, final Map<Integer, Collection<Integer>> componentAssociations,
			final Map<Integer, String> components, final Map<Integer, Integer> subTasks) {
		super(slaComputations);
		this.customFieldValues = customFieldValues;
		this.customFields = customFields;
		this.componentAssociations = componentAssociations;
		this.components = components;
		this.subTasks = subTasks;

	}

	@Override
	protected void writeNonSlaHeaders(final Writer writer) throws IOException {
		// Write static headers
		super.writeNonSlaHeaders(writer);

		// Worklog data
		writer.write(";timeSpent(s);timeEstimate(s);timeEstimateInit(s)");

		// Write optional parent
		writer.write(";parent");

		// Write unique components header
		writer.write(";components");

		// Write custom fields headers
		for (final CustomField customField : customFields.values()) {
			writer.append(';');
			writer.write(customField.getName());
		}
	}

	/**
	 * Complete the standard data with temporal data (not SLA), sub task, and components
	 */
	@Override
	protected void writeIssueData(final IssueDetails issue, final Writer writer, final Format df, final Format idf) throws IOException {
		// Write static data
		super.writeIssueData(issue, writer, df, idf);

		// Time spent
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull((Object) issue.getTimeSpent(), "")));

		// Time estimate
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull((Object) issue.getTimeEstimate(), "")));

		// Time initial estimate
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull((Object) issue.getTimeEstimateInit(), "")));

		// Optional parent
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull(subTasks.get(issue.getId()), "")));

		// Custom non fixed fields
		writeCustomData((IssueSla) issue, writer, df);
	}

	/**
	 * Custom non fixed fields
	 */
	private void writeCustomData(final IssueSla issue, final Writer writer, final Format df) throws IOException {

		// Write components data
		writeComponentsData(issue, writer);

		// Write custom fields data
		writeCustomFieldsData(issue, writer, df);
	}

	/**
	 * Write all components attached to given issue.
	 */
	private void writeComponentsData(final IssueSla issue, final Writer writer) throws IOException {
		// Write the component values with ',' as separator
		writer.append(';');
		writer.write(
				CollectionUtils.emptyIfNull(componentAssociations.get(issue.getId())).stream().map(components::get).collect(Collectors.joining(",")));
	}

	/**
	 * Write all custom field values attached to given issue.
	 */
	private void writeCustomFieldsData(final IssueSla issue, final Writer writer, final Format df) throws IOException {
		final int issueId = issue.getId();

		// Write the custom field values, starting from the position of the cursor
		for (final CustomFieldEditor customField : customFields.values()) {
			writer.append(';');
			writeCustomFieldData(issueId, customField, writer, df);

		}
	}

	/**
	 * Write all custom field values attached to given issue and custom field. Multi-valued custom fields use ',' as
	 * separator.
	 */
	private void writeCustomFieldData(final int issue, final CustomFieldEditor customField, final Writer writer, final Format df) throws IOException {
		final int customFieldId = customField.getId();

		// Write the custom field data
		boolean first = true;
		moveToIssueAndCustomField(issue, customFieldId);
		boolean stringData = false;
		while (issueAndCustomFieldsCursor < customFieldValues.size() && customFieldValues.get(issueAndCustomFieldsCursor).getIssue() == issue
				&& customFieldValues.get(issueAndCustomFieldsCursor).getCustomField() == customFieldId) {
			// Custom field value has been found
			final CustomFieldValue customFieldValue = customFieldValues.get(issueAndCustomFieldsCursor);
			if (!first) {
				// Multi-valued custom field, append the value
				writer.append(',');
			}
			final Object value = customField.getEditor().getValue(customField, customFieldValue);
			if (value == null) {
				// Broken reference has been found, report it
				writer.append("#INVALID");
				log.warn("Broken reference for project '{}'[{}], issue [{}], custom field '{}'[{}], value '{}'",
						slaComputations.getProject().getName(), slaComputations.getJira(), issue, customField.getName(), customField.getId(),
						customFieldValue.getStringValue());
			} else if (value instanceof Date) {
				// Simple date, no escape
				writer.append(df.format(value));
			} else if (value instanceof Number) {
				// Simple number, no escape
				writer.append(StringUtils.removeEnd(value.toString().replace('.', ','), ",0"));
			} else {
				if (!stringData) {
					// Add the cell protection
					writer.append('\"');
					stringData = true;
				}
				// Write the value escaping the protection chars
				writer.append(value.toString().replace("\"", "\"\"").replace(';', ','));
			}
			issueAndCustomFieldsCursor++;
			first = false;
		}

		// Close the cell protection
		if (stringData) {
			writer.append('\"');
		}
	}

	/**
	 * Move the cursor to the next matching issue and custom field
	 */
	private void moveToIssueAndCustomField(final int issue, final int customFieldId) {
		while (issueAndCustomFieldsCursor < customFieldValues.size() && customFieldValues.get(issueAndCustomFieldsCursor).getIssue() < issue) {
			issueAndCustomFieldsCursor++;
		}
		while (issueAndCustomFieldsCursor < customFieldValues.size() && customFieldValues.get(issueAndCustomFieldsCursor).getIssue() == issue
				&& customFieldValues.get(issueAndCustomFieldsCursor).getCustomField() < customFieldId) {
			issueAndCustomFieldsCursor++;
		}
	}

}
