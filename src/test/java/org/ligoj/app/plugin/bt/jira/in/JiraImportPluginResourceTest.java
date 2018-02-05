package org.ligoj.app.plugin.bt.jira.in;

import javax.validation.ConstraintViolationException;

import org.hsqldb.lib.StringInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.plugin.bt.jira.in.JiraImportPluginResource.ImportContext;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.ligoj.app.resource.plugin.CurlProcessor;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;

/**
 * Test class of {@link JiraImportPluginResource}
 */
public class JiraImportPluginResourceTest extends AbstractJiraImportPluginResourceTest {

	@Test
	public void testUploadEmptyFile() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new StringInputStream("id;"), ENCODING, subscription, UploadMode.VALIDATION);
		}), "id", "Empty file, no change found");
	}

	@Test
	public void testUploadInvalidSubscription() {
		Assertions.assertThrows(JpaObjectRetrievalFailureException.class, () -> {
			resource.upload(new StringInputStream("id;"), ENCODING, -1, UploadMode.VALIDATION);
		});
	}

	@Test
	public void testUploadNoChange() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/nochange.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
		}), "issue", "No change detected detected for issue 2(id=2) for changes between 01/03/2014 12:01 and 01/03/2014 12:01");
	}

	@Test
	public void testUploadBrokenHistory() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/broken-history.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "date", "Broken history for issue 2(id=2) Sat Mar 01 12:01:00 CET 2014 and 1.3.2014 12:00:59");
	}

	@Test
	public void testSynchronizeJiraCantSynchronize() {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(false);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		resource.synchronizeJira(context, result);
		Mockito.verify(resource, Mockito.never()).authenticateAdmin(ArgumentMatchers.same(context),
				ArgumentMatchers.any(CurlProcessor.class));
		Assertions.assertNull(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraNoAuth() {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(true);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		resource.synchronizeJira(context, result);
		Mockito.verify(resource, Mockito.times(1)).authenticateAdmin(ArgumentMatchers.same(context),
				ArgumentMatchers.any(CurlProcessor.class));
		Mockito.verify(resource, Mockito.never()).clearJiraCache(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class));
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraNoScriptRunner() {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(true);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		Mockito.when(resource.authenticateAdmin(ArgumentMatchers.same(context), ArgumentMatchers.any(CurlProcessor.class)))
				.thenReturn(true);
		resource.synchronizeJira(context, result);
		Mockito.verify(resource, Mockito.times(1)).authenticateAdmin(ArgumentMatchers.same(context),
				ArgumentMatchers.any(CurlProcessor.class));
		Mockito.verify(resource, Mockito.times(1)).clearJiraCache(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class));
		Mockito.verify(resource, Mockito.never()).reIndexProject(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class));
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraReindexFailed() {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(true);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		Mockito.when(resource.authenticateAdmin(ArgumentMatchers.same(context), ArgumentMatchers.any(CurlProcessor.class)))
				.thenReturn(true);
		Mockito.when(resource.clearJiraCache(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class))).thenReturn(true);
		resource.synchronizeJira(context, result);
		Mockito.verify(resource, Mockito.times(1)).authenticateAdmin(ArgumentMatchers.same(context),
				ArgumentMatchers.any(CurlProcessor.class));
		Mockito.verify(resource, Mockito.times(1)).clearJiraCache(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class));
		Mockito.verify(resource, Mockito.times(1)).reIndexProject(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class));
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJira() {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(true);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		Mockito.when(resource.authenticateAdmin(ArgumentMatchers.same(context), ArgumentMatchers.any(CurlProcessor.class)))
				.thenReturn(true);
		Mockito.when(resource.clearJiraCache(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class))).thenReturn(true);
		Mockito.when(resource.reIndexProject(ArgumentMatchers.same(context), ArgumentMatchers.same(result),
				ArgumentMatchers.any(CurlProcessor.class))).thenReturn(true);
		resource.synchronizeJira(context, result);
		Assertions.assertTrue(result.getSynchronizedJira());
	}

	@Test
	public void testUploadTranslatedAmbiguousCf() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-ambiguous.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$cf-conflictB", "There are several custom fields named 'cf-conflictB', ambiguous identifier : 10001, 10003");
	}

	@Test
	public void testUploadTranslatedNotAmbiguousCfSameName() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-not-really-ambiguous.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadNotManagedCfType() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-not-managed-type.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$SLA_PEC",
				"Custom field 'SLA_PEC' has a not yet managed type 'com.valiantys.jira.plugins.vertygo.jira-vertygosla-plugin:sla.be.cf'");
	}

	@Test
	public void testUploadTranslatedCf() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-translated.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(0, result.getComponents().intValue());
		Assertions.assertEquals(1, result.getCustomFields().intValue());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(2, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(1, result.getPriorities().intValue());
		Assertions.assertEquals(0, result.getResolutions().intValue());
		Assertions.assertEquals(1, result.getStatuses().intValue());
		Assertions.assertEquals(1, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(0, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(0, result.getLabels().intValue());

		Assertions.assertEquals(1, result.getNewIssues().intValue());
		Assertions.assertEquals(0, result.getNewComponents().intValue());
		Assertions.assertEquals(0, result.getNewVersions().intValue());
	}

	@Test
	public void testUploadInvalidVersion() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			try {
				jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.2", 10075);
				resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
						UploadMode.PREVIEW);
			} finally {
				jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
			}
		}), "jira", "Required JIRA version is 6.0.0, and the current version is 4.4.2");
	}

	@Test
	public void testUploadInvalidPkey() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-pkey.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "issue", "Used issue prefix in import is SIOP, but associated project is MDA, are you importing the correct file?");
	}

	@Test
	public void testUploadUpdate() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-update.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "issue", "Updating issues is not yet implemented. 2 issues are concerned. First one is issue 1 (id=13402)");
	}

	@Test
	public void testUploadMissingResolutionForResolved() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-resolution-resolved.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "resolution", "Resolution is provided but has never been resolved for issue 2(id=2)");
	}

	@Test
	public void testUploadInvalidResolutionDate() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-resolution-date.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "resolutionDate", "Resolution date must be greater or equals to the change date for issue 2(id=1)");
	}

	@Test
	public void testUploadInvalidResolutionDateNoId() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-resolution-date-no-id.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "resolutionDate", "Resolution date must be greater or equals to the change date for issue 2");
	}

	@Test
	public void testUploadInvalidDueDate() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-duedate.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "dueDate", "Due date must be greater or equals to the creation date for issue 2(id=4)");
	}

	@Test
	public void testUploadInvalidInput() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ConstraintViolationException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-input.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "type", "NotBlank");
	}

	@Test
	public void testUploadInvalidCfSelectValue() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-select.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$Motif suspension", "Invalid value 'Demande'. Expected : Autre (à préciser),Demande révisée,Décalage planning");
	}

	@Test
	public void testUploadInvalidCfDateValue() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-date.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$Date démarrage prévue", "Invalid value 'Value'. Expected : A valid date");
	}

	@Test
	public void testUploadInvalidCfDatePickerValue() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-datepicker.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$Date de livraison", "Invalid value 'VALUE'. Expected : A valid date");
	}

	@Test
	public void testUploadInvalidCfFloatValue() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-cf-float.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "cf$Délai levée réserves (jrs)", "Invalid value 'A'. Expected : A decimal value");
	}

	@Test
	public void testUploadInvalidStatus() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-status.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "status", "Some statuses (1) do not exist : VALUE");
	}

	@Test
	public void testUploadInvalidWorkflowStatus() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-workflow-status.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "status", "At least one specified status exists but is not managed in the workflow : Assigned");
	}

	@Test
	public void testUploadInvalidType() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-type.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "type", "Some types (1) do not exist : VALUE");
	}

	@Test
	public void testUploadInvalidWorkflowType() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			try {
				// Delete the default workflow mapping, where type = '0'
				jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 1, 10272);
				resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
						UploadMode.PREVIEW);
			} finally {
				jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 10025, 10272);
			}
		}), "type", "Specified type 'Bug' exists but is not mapped to a workflow and there is no default association");
	}

	@Test
	public void testUploadDefaultWorkflowType() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			// Delete the default workflow mapping, where type = '0'
			jdbcTemplate.update("update workflowschemeentity SET issuetype=? WHERE ID = ?", 1, 10302);
			jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 1, 10272);
			resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		} finally {
			jdbcTemplate.update("update workflowschemeentity SET issuetype=? WHERE ID = ?", 6, 10302);
			jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 10025, 10272);
		}

		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertFalse(result.isFailed());
		Assertions.assertEquals(22, result.getStep());
	}

	@Test
	public void testUploadDefaultWorkflowScheme() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			// Delete the default project/workflow mapping, where type = '0'
			jdbcTemplate.update(
					"update nodeassociation SET SOURCE_NODE_ID=? WHERE SOURCE_NODE_ID=? AND SOURCE_NODE_ENTITY=? AND SINK_NODE_ID=? AND SINK_NODE_ENTITY=? AND ASSOCIATION_TYPE=?",
					1, 10074, "Project", 10025, "WorkflowScheme", "ProjectScheme");
			resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		} finally {
			jdbcTemplate.update(
					"update nodeassociation SET SOURCE_NODE_ID=? WHERE SOURCE_NODE_ID=? AND SOURCE_NODE_ENTITY=? AND SINK_NODE_ID=? AND SINK_NODE_ENTITY=? AND ASSOCIATION_TYPE=?",
					10074, 1, "Project", 10025, "WorkflowScheme", "ProjectScheme");
		}

		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertFalse(result.isFailed());
		Assertions.assertEquals(22, result.getStep());
	}

	@Test
	public void testUploadDefaultWorkflow() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			// Delete the default project/workflow mapping, where type = '0'
			jdbcTemplate.update("update workflowschemeentity SET WORKFLOW=? WHERE ID=?", "jira", 10302);
			resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		} finally {
			jdbcTemplate.update("update workflowschemeentity SET WORKFLOW=? WHERE ID=?", "CSN", 10302);
		}

		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertFalse(result.isFailed());
		Assertions.assertEquals(22, result.getStep());
	}

	@Test
	public void testUploadInvalidPriority() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-priority.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "priority", "Some priorities (1) do not exist : VALUE");
	}

	@Test
	public void testUploadInvalidUser() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new ClassPathResource("csv/upload/invalid-assignee.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		}), "assignee", "Some assignee/reporters/authors (1) do not exist : VALUE");
	}

	@Test
	public void testUploadConcurrent() {
		em.createQuery("UPDATE ImportStatus i SET i.end = NULL WHERE i.locked.id  = ?1").setParameter(1, subscription).executeUpdate();
		em.flush();
		em.clear();
		Assertions.assertEquals(Assertions.assertThrows(BusinessException.class, () -> {
			resource.upload(null, ENCODING, subscription, UploadMode.PREVIEW);
		}).getMessage(), "concurrent-task");
	}

	@Test
	public void testUploadFailed() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.upload(new StringInputStream("id;"), ENCODING, subscription, UploadMode.VALIDATION);
		}), "id", "Empty file, no change found");
		em.flush();
		em.clear();
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertNotNull(result.getEnd());
		Assertions.assertTrue(result.isFailed());
		Assertions.assertEquals(3, result.getStep());
	}

	@Test
	public void testUploadSingleChange() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/single-change.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
		em.clear();
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(46, result.getChanges().intValue());
		Assertions.assertEquals(getAuthenticationName(), result.getAuthor());
		Assertions.assertEquals(2, result.getComponents().intValue());
		Assertions.assertEquals(22, result.getStep());
		Assertions.assertFalse(result.isFailed());
		Assertions.assertNotNull(result.getStart());
		Assertions.assertNotNull(result.getEnd());
		Assertions.assertEquals(9, result.getCustomFields().intValue());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(2, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(2, result.getPriorities().intValue());
		Assertions.assertEquals(2, result.getResolutions().intValue());
		Assertions.assertEquals(2, result.getStatuses().intValue());
		Assertions.assertEquals(2, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(2, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 04, 15, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(2, result.getLabels().intValue());

		Assertions.assertEquals(1, result.getNewIssues().intValue());
		Assertions.assertEquals(0, result.getNewComponents().intValue());
		Assertions.assertEquals(0, result.getNewVersions().intValue());
	}

	@Test
	public void testUploadSimpleSyntax() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription, UploadMode.SYNTAX);
		em.flush();
		em.clear();
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertNull(result.getComponents());
		Assertions.assertNull(result.getCustomFields());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertNull(result.getMaxIssue());
		Assertions.assertNull(result.getMinIssue());
		Assertions.assertNull(result.getPriorities());
		Assertions.assertNull(result.getResolutions());
		Assertions.assertNull(result.getStatuses());
		Assertions.assertNull(result.getTypes());
		Assertions.assertNull(result.getUsers());
		Assertions.assertNull(result.getVersions());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.SYNTAX, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assertions.assertNull(result.getLabels());

		Assertions.assertNull(result.getNewIssues());
		Assertions.assertNull(result.getNewComponents());
		Assertions.assertNull(result.getNewVersions());
	}

	@Test
	public void testUploadSimpleValidation() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
				UploadMode.VALIDATION);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(0, result.getComponents().intValue());
		Assertions.assertEquals(0, result.getCustomFields().intValue());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(2, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(1, result.getPriorities().intValue());
		Assertions.assertEquals(0, result.getResolutions().intValue());
		Assertions.assertEquals(1, result.getStatuses().intValue());
		Assertions.assertEquals(1, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(0, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.VALIDATION, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(0, result.getLabels().intValue());

		Assertions.assertNull(result.getNewIssues());
		Assertions.assertNull(result.getNewComponents());
		Assertions.assertNull(result.getNewVersions());
	}

	@Test
	public void testUploadSimple() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals("6.0.1", result.getJiraVersion());
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(0, result.getComponents().intValue());
		Assertions.assertEquals(0, result.getCustomFields().intValue());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(2, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(1, result.getPriorities().intValue());
		Assertions.assertEquals(0, result.getResolutions().intValue());
		Assertions.assertEquals(1, result.getStatuses().intValue());
		Assertions.assertEquals(1, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(0, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(0, result.getLabels().intValue());

		Assertions.assertEquals(1, result.getNewIssues().intValue());
		Assertions.assertEquals(0, result.getNewComponents().intValue());
		Assertions.assertEquals(0, result.getNewVersions().intValue());
	}

	@Test
	public void testUploadSimpleNoimportStatus() throws Exception {
		repository.deleteAll();
		em.flush();
		testUploadSimple();
	}

	@Test
	public void testUploadPreview() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(3, result.getChanges().intValue());
		Assertions.assertEquals(4, result.getComponents().intValue());
		Assertions.assertEquals(9, result.getCustomFields().intValue());
		Assertions.assertEquals(2, result.getIssues().intValue());
		Assertions.assertEquals(10074, result.getJira().intValue());
		Assertions.assertEquals(9, result.getMaxIssue().intValue());
		Assertions.assertEquals(2, result.getMinIssue().intValue());
		Assertions.assertEquals(2, result.getPriorities().intValue());
		Assertions.assertEquals(1, result.getResolutions().intValue());
		Assertions.assertEquals(2, result.getStatuses().intValue());
		Assertions.assertEquals(2, result.getTypes().intValue());
		Assertions.assertEquals(2, result.getUsers().intValue());
		Assertions.assertEquals(4, result.getVersions().intValue());
		Assertions.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assertions.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assertions.assertEquals("MDA", result.getPkey());
		Assertions.assertEquals(getDate(2014, 03, 02, 12, 01, 00), result.getIssueTo());
		Assertions.assertEquals(2, result.getLabels().intValue());

		Assertions.assertEquals(2, result.getNewIssues().intValue());
		Assertions.assertEquals(2, result.getNewComponents().intValue());
		Assertions.assertEquals(2, result.getNewVersions().intValue());
	}

}
