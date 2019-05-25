/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.in;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test class of {@link JiraImportPluginResource}
 */
public class JiraImport3PluginResourceTest extends AbstractJiraImportPluginResourceTest {

	@Test
	public void testZUploadWithInsert() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		final int pcounter = jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10074);

		resource.upload(new ClassPathResource("csv/upload/nominal-complete.csv").getInputStream(), ENCODING, subscription, UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(42, result.getChanges().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertEquals(4, result.getComponents().intValue());
		Assertions.assertEquals(9, result.getCustomFields().intValue());
		Assertions.assertEquals(3, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(6, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(2, result.getPriorities().intValue());
		Assertions.assertEquals(1, result.getResolutions().intValue());
		Assertions.assertEquals(5, result.getStatuses().intValue());
		Assertions.assertEquals(2, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(5, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(getDate(2014, 04, 11, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(UploadMode.FULL, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(6, result.getLabels().intValue());

		Assertions.assertEquals(3, result.getNewIssues().intValue());
		Assertions.assertEquals(2, result.getNewComponents().intValue());
		Assertions.assertEquals(3, result.getNewVersions().intValue());
		Assertions.assertFalse(result.isFailed());

		Assertions.assertEquals(13, result.getStatusChanges().intValue());
		Assertions.assertFalse(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertNull(result.getSynchronizedJira());

		// Lesser than the maximal "pcounter"
		Assertions.assertEquals(pcounter,
				jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10074).intValue());

		// Check sequences
		final Map<String, Integer> sequences = AbstractEditor.getInvertedMap(datasource,
				"SELECT SEQ_NAME AS pname, SEQ_ID AS id FROM SEQUENCE_VALUE_ITEM");
		Assertions.assertEquals(10200, sequences.get("ChangeGroup").intValue());
		Assertions.assertEquals(10200, sequences.get("ChangeItem").intValue());
		Assertions.assertEquals(10300, sequences.get("Component").intValue());
		Assertions.assertEquals(10300, sequences.get("Issue").intValue());
		Assertions.assertEquals(10100, sequences.get("Label").intValue());
		Assertions.assertEquals(10300, sequences.get("OSCurrentStep").intValue());
		Assertions.assertEquals(10300, sequences.get("OSWorkflowEntry").intValue());
		Assertions.assertEquals(10300, sequences.get("Version").intValue());

		final int workflowId = jdbcTemplate
				.queryForObject("SELECT WORKFLOW_ID FROM jiraissue WHERE issuenum=? AND project=? AND issuestatus=? AND priority=?"
						+ " AND RESOLUTION=? AND RESOLUTIONDATE IS NOT NULL AND issuetype=? AND DESCRIPTION=? AND SUMMARY=? AND REPORTER=?"
						+ " AND ASSIGNEE=?", Integer.class, 3, 10074, 6, 3, 1, 1, "DESCRIPTION-34", "SUMMARY-34", "alocquet", "fdaugan");
		Assertions.assertEquals(10201, workflowId);

		final String workflow = jdbcTemplate.queryForObject("SELECT NAME FROM OS_WFENTRY WHERE ID=? AND STATE=?", String.class, workflowId,
				1);
		Assertions.assertEquals("CSN", workflow);

		final String status = jdbcTemplate.queryForObject("SELECT STATUS FROM OS_CURRENTSTEP WHERE ENTRY_ID=? AND STEP_ID=?", String.class,
				workflowId, 6);
		Assertions.assertEquals("Closed", status);

	}

}
