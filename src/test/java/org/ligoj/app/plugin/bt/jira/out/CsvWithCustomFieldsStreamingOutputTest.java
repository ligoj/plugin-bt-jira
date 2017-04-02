package org.ligoj.app.plugin.bt.jira.out;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.bt.IssueSla;
import org.ligoj.app.plugin.bt.SlaData;
import org.ligoj.app.plugin.bt.jira.JiraSlaComputations;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.bootstrap.core.DescribedBean;
import org.mockito.Mockito;

/**
 * Test class of {@link CsvWithCustomFieldsStreamingOutput}
 */
public class CsvWithCustomFieldsStreamingOutputTest {

	@Test
	public void write() throws IOException {
		// Prepare

		final JiraSlaComputations computations = new JiraSlaComputations();
		computations.setHolidays(new ArrayList<>());
		computations.setJira(50000);
		final DescribedBean<Integer> project = new DescribedBean<>();
		project.setName("P");
		computations.setProject(project);
		final BugTrackerConfiguration btConfiguration = new BugTrackerConfiguration();
		btConfiguration.setSlas(new ArrayList<>());
		computations.setBtConfiguration(btConfiguration);
		computations.setSlaConfigurations(new ArrayList<>());

		// Status
		final HashMap<Integer, String> statusText = new HashMap<>();
		statusText.put(1, "Open");
		computations.setStatusText(statusText);

		// Types
		final HashMap<Integer, String> typeText = new HashMap<>();
		typeText.put(1, "Bug");
		computations.setTypeText(typeText);

		// Components definition
		final HashMap<Integer, String> components = new HashMap<>();
		components.put(1, "C1");
		components.put(2, "C2");

		// Resolutions definition
		final HashMap<Integer, String> resolutionText = new HashMap<>();
		resolutionText.put(1, "Fixed");
		computations.setResolutionText(resolutionText);

		// Priorities definition
		final HashMap<Integer, String> priorityText = new HashMap<>();
		priorityText.put(1, "Critical");
		computations.setPriorityText(priorityText);
		final List<SlaData> data = new ArrayList<>();

		// Add issues
		final Map<Integer, Integer> counters = new HashMap<>();
		final List<IssueSla> issues = new ArrayList<>();
		final IssueSla issue1 = new IssueSla();
		issue1.setAssignee("A11");
		issue1.setAuthor("A12");
		issue1.setCreated(new Date());
		issue1.setPriority(1);
		issue1.setReporter("R1");
		issue1.setResolution(1);
		issue1.setStatus(1);
		issue1.setType(1);
		issue1.setPkey("I1");
		issue1.setId(1000);
		issue1.setStatusCounter(counters);
		issue1.setData(data);
		issues.add(issue1);
		final IssueSla issueNoCustomField = new IssueSla();
		issueNoCustomField.setAssignee("A21");
		issueNoCustomField.setAuthor("A22");
		issueNoCustomField.setCreated(new Date());
		issueNoCustomField.setPriority(1);
		issueNoCustomField.setReporter("R2");
		issueNoCustomField.setResolution(1);
		issueNoCustomField.setStatus(1);
		issueNoCustomField.setType(1);
		issueNoCustomField.setPkey("I2");
		issueNoCustomField.setId(1002);
		issueNoCustomField.setStatusCounter(counters);
		issueNoCustomField.setData(data);
		issues.add(issueNoCustomField);

		final IssueSla issueOutOfRange = new IssueSla();
		issueOutOfRange.setAssignee("A11");
		issueOutOfRange.setAuthor("A12");
		issueOutOfRange.setCreated(new Date());
		issueOutOfRange.setPriority(1);
		issueOutOfRange.setReporter("R1");
		issueOutOfRange.setResolution(1);
		issueOutOfRange.setStatus(1);
		issueOutOfRange.setType(1);
		issueOutOfRange.setPkey("I3");
		issueOutOfRange.setId(5000);
		issueOutOfRange.setStatusCounter(counters);
		issueOutOfRange.setData(data);
		issues.add(issueOutOfRange);

		computations.setIssues(issues);

		// Components associations
		final Map<Integer, Collection<Integer>> componentAssociations = new HashMap<>();
		componentAssociations.put(1000, Collections.singleton(1));
		componentAssociations.put(1002, Arrays.asList(1, 2));

		// Custom fields association
		final List<CustomFieldValue> customFieldValues = new ArrayList<>();
		final CustomFieldValue customfieldValue0 = new CustomFieldValue();
		customfieldValue0.setIssue(0);
		customfieldValue0.setCustomField(0);
		customFieldValues.add(customfieldValue0);
		final CustomFieldValue customfieldValue1 = new CustomFieldValue();
		customfieldValue1.setIssue(1000);
		customfieldValue1.setCustomField(0);
		customFieldValues.add(customfieldValue1);
		final CustomFieldValue customfieldValue2 = new CustomFieldValue();
		customfieldValue2.setIssue(1000);
		customfieldValue2.setCustomField(1);
		customFieldValues.add(customfieldValue2);
		final CustomFieldValue customfieldValue2b = new CustomFieldValue();
		customfieldValue2b.setIssue(1000);
		customfieldValue2b.setCustomField(3);
		customFieldValues.add(customfieldValue2b);
		final CustomFieldValue customfieldValue2c = new CustomFieldValue();
		customfieldValue2c.setIssue(1000);
		customfieldValue2c.setCustomField(3);
		customFieldValues.add(customfieldValue2c);
		final CustomFieldValue customfieldValue2NoValue = new CustomFieldValue();
		customfieldValue2NoValue.setIssue(1000);
		customfieldValue2NoValue.setCustomField(4);
		customFieldValues.add(customfieldValue2NoValue);
		final CustomFieldValue customfieldValue2NoCf = new CustomFieldValue();
		customfieldValue2NoCf.setIssue(1000);
		customfieldValue2NoCf.setCustomField(6000);
		customFieldValues.add(customfieldValue2NoValue);
		final CustomFieldValue customfieldValueNoIssue = new CustomFieldValue();
		customfieldValueNoIssue.setIssue(1005);
		customfieldValueNoIssue.setCustomField(6000);
		customFieldValues.add(customfieldValueNoIssue);

		// Custom field definition
		final Map<Integer, CustomFieldEditor> customFieldEditors = new HashMap<>();
		final AbstractEditor editor = Mockito.mock(AbstractEditor.class);

		final CustomFieldEditor customFieldEditor1 = new CustomFieldEditor();
		customFieldEditor1.setEditor(editor);
		customFieldEditor1.setName("CF1");
		customFieldEditor1.setId(1);
		customFieldEditors.put(1, customFieldEditor1);

		final CustomFieldEditor customFieldEditorNotUsed = new CustomFieldEditor();
		customFieldEditorNotUsed.setEditor(editor);
		customFieldEditorNotUsed.setName("CF2");
		customFieldEditorNotUsed.setId(2);
		customFieldEditors.put(2, customFieldEditorNotUsed);

		final CustomFieldEditor customFieldEditorMultiple = new CustomFieldEditor();
		customFieldEditorMultiple.setEditor(editor);
		customFieldEditorMultiple.setName("CF3");
		customFieldEditorMultiple.setId(3);
		customFieldEditors.put(3, customFieldEditorMultiple);

		final CustomFieldEditor customFieldEditorNoValue = new CustomFieldEditor();
		customFieldEditorNoValue.setEditor(editor);
		customFieldEditorNoValue.setName("CF4");
		customFieldEditorNoValue.setId(4);
		customFieldEditors.put(4, customFieldEditorNoValue);
		Mockito.when(editor.getValue(customFieldEditor1, customfieldValue2)).thenReturn("V2");
		Mockito.when(editor.getValue(customFieldEditorMultiple, customfieldValue2b)).thenReturn("V2b");
		Mockito.when(editor.getValue(customFieldEditorMultiple, customfieldValue2c)).thenReturn("V2c");

		// Subtasks
		final Map<Integer, Integer> subtasks = new HashMap<>();
		subtasks.put(1000, 1001);

		// Call
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		new CsvWithCustomFieldsStreamingOutput(computations, customFieldValues, customFieldEditors, componentAssociations, components, subtasks)
				.write(out);
		final List<String> lines = IOUtils.readLines(new ByteArrayInputStream(out.toByteArray()), StandardCharsets.UTF_8);

		// Check
		Assert.assertEquals(4, lines.size());
		Assert.assertEquals("id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText"
				+ ";created;createdTimestamp;reporter;assignee;dueDate;dueDateTimestamp"
				+ ";timeSpent(s);timeEstimate(s);timeEstimateInit(s);parent;components;CF1;CF2;CF3;CF4;#Open", lines.get(0));
		Assert.assertTrue(lines.get(1).startsWith("1000;I1;1;OPEN;1;Bug;1;Critical;1;Fixed;20"));
		Assert.assertTrue(lines.get(1).endsWith(";R1;A11;;;;;;1001;C1;\"V2\";;\"V2b,V2c\";#INVALID,#INVALID;0"));
		Assert.assertTrue(lines.get(2).startsWith("1002;I2;1;OPEN;1;Bug;1;Critical;1;Fixed;20"));
		Assert.assertTrue(lines.get(2).endsWith(";R2;A21;;;;;;;C1,C2;;;;;0"));
	}
}
