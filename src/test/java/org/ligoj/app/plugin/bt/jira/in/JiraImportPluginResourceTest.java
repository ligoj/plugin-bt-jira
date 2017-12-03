package org.ligoj.app.plugin.bt.jira.in;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Map;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

import org.apache.http.HttpStatus;
import org.hsqldb.lib.StringInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.JiraPluginResource;
import org.ligoj.app.plugin.bt.jira.dao.ImportStatusRepository;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.app.plugin.bt.jira.in.JiraImportPluginResource.ImportContext;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.ligoj.app.resource.plugin.CurlProcessor;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link JiraImportPluginResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JiraImportPluginResourceTest extends AbstractJiraUploadTest {

	private static final String ENCODING = "cp1250";

	private JiraImportPluginResource resource;

	private JiraPluginResource jiraResource;

	@Autowired
	private ImportStatusRepository repository;

	@Before
	public void prepareSubscription() {
		this.subscription = getSubscription("MDA");
		resource = new JiraImportPluginResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);

		// Replace the task management to handle the inner transaction
		resource.resource = new JiraPluginResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource.resource);
		jiraResource = resource.resource;
	}

	@Test
	public void testUploadEmptyFile() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", "Empty file, no change found"));
		resource.upload(new StringInputStream("id;"), ENCODING, subscription, UploadMode.VALIDATION);
	}

	@Test
	public void testUploadInvalidSubscription() throws Exception {
		thrown.expect(JpaObjectRetrievalFailureException.class);
		resource.upload(new StringInputStream("id;"), ENCODING, -1, UploadMode.VALIDATION);
	}

	@Test
	public void testUploadNoChange() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("issue",
				"No change detected detected for issue 2(id=2) for changes between 01/03/2014 12:01 and 01/03/2014 12:01"));
		resource.upload(new ClassPathResource("csv/upload/nochange.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
	}

	@Test
	public void testUploadBrokenHistory() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("date",
				"Broken history for issue 2(id=2) Sat Mar 01 12:01:00 CET 2014 and 1.3.2014 12:00:59"));
		resource.upload(new ClassPathResource("csv/upload/broken-history.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testSynchronizeJiraCantSynchronize() throws Exception {
		final ImportContext context = new ImportContext();
		final ImportStatus result = new ImportStatus();
		result.setCanSynchronizeJira(false);
		final JiraImportPluginResource resource = Mockito.mock(JiraImportPluginResource.class);
		Mockito.doCallRealMethod().when(resource).synchronizeJira(ArgumentMatchers.same(context), ArgumentMatchers.same(result));
		resource.synchronizeJira(context, result);
		Mockito.verify(resource, Mockito.never()).authenticateAdmin(ArgumentMatchers.same(context),
				ArgumentMatchers.any(CurlProcessor.class));
		Assert.assertNull(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraNoAuth() throws Exception {
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
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraNoScriptRunner() throws Exception {
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
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJiraReindexFailed() throws Exception {
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
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testSynchronizeJira() throws Exception {
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
		Assert.assertTrue(result.getSynchronizedJira());
	}

	@Test
	public void testUploadTranslatedAmbiguousCf() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$cf-conflictB",
				"There are several custom fields named 'cf-conflictB', ambiguous identifier : 10001, 10003"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-ambiguous.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadTranslatedNotAmbiguousCfSameName() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-not-really-ambiguous.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadNotManagedCfType() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$SLA_PEC",
				"Custom field 'SLA_PEC' has a not yet managed type 'com.valiantys.jira.plugins.vertygo.jira-vertygosla-plugin:sla.be.cf'"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-not-managed-type.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadTranslatedCf() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-translated.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(0, result.getComponents().intValue());
		Assert.assertEquals(1, result.getCustomFields().intValue());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(2, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(1, result.getPriorities().intValue());
		Assert.assertEquals(0, result.getResolutions().intValue());
		Assert.assertEquals(1, result.getStatuses().intValue());
		Assert.assertEquals(1, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(0, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(0, result.getLabels().intValue());

		Assert.assertEquals(1, result.getNewIssues().intValue());
		Assert.assertEquals(0, result.getNewComponents().intValue());
		Assert.assertEquals(0, result.getNewVersions().intValue());
	}

	@Test
	public void testUploadInvalidVersion() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("jira", "Required JIRA version is 6.0.0, and the current version is 4.4.2"));
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.2", 10075);
			resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
		}
	}

	@Test
	public void testUploadInvalidPkey() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("issue",
				"Used issue prefix in import is SIOP, but associated project is MDA, are you importing the correct file?"));
		resource.upload(new ClassPathResource("csv/upload/invalid-pkey.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
	}

	@Test
	public void testUploadUpdate() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("issue",
				"Updating issues is not yet implemented. 2 issues are concerned. First one is issue 1 (id=13402)"));
		resource.upload(new ClassPathResource("csv/upload/invalid-update.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadMissingResolutionForResolved() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("resolution", "Resolution is provided but has never been resolved for issue 2(id=2)"));
		resource.upload(new ClassPathResource("csv/upload/invalid-resolution-resolved.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidResolutionDate() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("resolutionDate",
				"Resolution date must be greater or equals to the change date for issue 2(id=1)"));
		resource.upload(new ClassPathResource("csv/upload/invalid-resolution-date.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidResolutionDateNoId() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("resolutionDate",
				"Resolution date must be greater or equals to the change date for issue 2"));
		resource.upload(new ClassPathResource("csv/upload/invalid-resolution-date-no-id.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidDueDate() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(
				MatcherUtil.validationMatcher("dueDate", "Due date must be greater or equals to the creation date for issue 2(id=4)"));
		resource.upload(new ClassPathResource("csv/upload/invalid-duedate.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidInput() throws Exception {
		thrown.expect(ConstraintViolationException.class);
		resource.upload(new ClassPathResource("csv/upload/invalid-input.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidCfSelectValue() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$Motif suspension",
				"Invalid value 'Demande'. Expected : Autre (à préciser),Demande révisée,Décalage planning"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-select.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidCfDateValue() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$Date démarrage prévue", "Invalid value 'Value'. Expected : A valid date"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-date.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidCfDatePickerValue() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$Date de livraison", "Invalid value 'VALUE'. Expected : A valid date"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-datepicker.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidCfFloatValue() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("cf$Délai levée réserves (jrs)", "Invalid value 'A'. Expected : A decimal value"));
		resource.upload(new ClassPathResource("csv/upload/invalid-cf-float.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidStatus() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("status", "Some statuses (1) do not exist : VALUE"));
		resource.upload(new ClassPathResource("csv/upload/invalid-status.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidWorkflowStatus() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("status",
				"At least one specified status exists but is not managed in the workflow : Assigned"));
		resource.upload(new ClassPathResource("csv/upload/invalid-workflow-status.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidType() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("type", "Some types (1) do not exist : VALUE"));
		resource.upload(new ClassPathResource("csv/upload/invalid-type.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidWorkflowType() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("type",
				"Specified type 'Bug' exists but is not mapped to a workflow and there is no default association"));

		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			// Delete the default workflow mapping, where type = '0'
			jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 1, 10272);
			resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
					UploadMode.PREVIEW);
		} finally {
			jdbcTemplate.update("update workflowschemeentity SET SCHEME=? WHERE ID = ?", 10025, 10272);
		}
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
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertFalse(result.isFailed());
		Assert.assertEquals(22, result.getStep());
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
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertFalse(result.isFailed());
		Assert.assertEquals(22, result.getStep());
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
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertFalse(result.isFailed());
		Assert.assertEquals(22, result.getStep());
	}

	@Test
	public void testUploadInvalidPriority() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("priority", "Some priorities (1) do not exist : VALUE"));
		resource.upload(new ClassPathResource("csv/upload/invalid-priority.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test
	public void testUploadInvalidUser() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("assignee", "Some assignee/reporters/authors (1) do not exist : VALUE"));
		resource.upload(new ClassPathResource("csv/upload/invalid-assignee.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
	}

	@Test(expected = BusinessException.class)
	public void testUploadConcurrent() throws Exception {
		em.createQuery("UPDATE ImportStatus i SET i.end = NULL WHERE i.locked.id  = ?1").setParameter(1, subscription)
				.executeUpdate();
		em.flush();
		em.clear();
		resource.upload(null, ENCODING, subscription, UploadMode.PREVIEW);
	}

	@Test
	public void testUploadFailed() throws Exception {
		try {
			resource.upload(new StringInputStream("id;"), ENCODING, subscription, UploadMode.VALIDATION);
			Assert.fail();
		} catch (final ValidationJsonException vje) {
			em.flush();
			em.clear();
			final ImportStatus result = jiraResource.getTask(subscription);
			Assert.assertNotNull(result.getEnd());
			Assert.assertTrue(result.isFailed());
			Assert.assertEquals(3, result.getStep());
		}
	}

	@Test
	public void testUploadSingleChange() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/single-change.csv").getInputStream(), ENCODING, subscription, UploadMode.PREVIEW);
		em.clear();
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(46, result.getChanges().intValue());
		Assert.assertEquals(getAuthenticationName(), result.getAuthor());
		Assert.assertEquals(2, result.getComponents().intValue());
		Assert.assertEquals(22, result.getStep());
		Assert.assertFalse(result.isFailed());
		Assert.assertNotNull(result.getStart());
		Assert.assertNotNull(result.getEnd());
		Assert.assertEquals(9, result.getCustomFields().intValue());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(2, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(2, result.getPriorities().intValue());
		Assert.assertEquals(2, result.getResolutions().intValue());
		Assert.assertEquals(2, result.getStatuses().intValue());
		Assert.assertEquals(2, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(2, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 04, 15, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(2, result.getLabels().intValue());

		Assert.assertEquals(1, result.getNewIssues().intValue());
		Assert.assertEquals(0, result.getNewComponents().intValue());
		Assert.assertEquals(0, result.getNewVersions().intValue());
	}

	@Test
	public void testUploadSimpleSyntax() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription, UploadMode.SYNTAX);
		em.flush();
		em.clear();
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertNull(result.getComponents());
		Assert.assertNull(result.getCustomFields());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertNull(result.getMaxIssue());
		Assert.assertNull(result.getMinIssue());
		Assert.assertNull(result.getPriorities());
		Assert.assertNull(result.getResolutions());
		Assert.assertNull(result.getStatuses());
		Assert.assertNull(result.getTypes());
		Assert.assertNull(result.getUsers());
		Assert.assertNull(result.getVersions());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.SYNTAX, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assert.assertNull(result.getLabels());

		Assert.assertNull(result.getNewIssues());
		Assert.assertNull(result.getNewComponents());
		Assert.assertNull(result.getNewVersions());
	}

	@Test
	public void testUploadSimpleValidation() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
				UploadMode.VALIDATION);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(0, result.getComponents().intValue());
		Assert.assertEquals(0, result.getCustomFields().intValue());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(2, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(1, result.getPriorities().intValue());
		Assert.assertEquals(0, result.getResolutions().intValue());
		Assert.assertEquals(1, result.getStatuses().intValue());
		Assert.assertEquals(1, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(0, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.VALIDATION, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(0, result.getLabels().intValue());

		Assert.assertNull(result.getNewIssues());
		Assert.assertNull(result.getNewComponents());
		Assert.assertNull(result.getNewVersions());
	}

	@Test
	public void testUploadSimple() throws Exception {
		resource.upload(new ClassPathResource("csv/upload/nominal-simple.csv").getInputStream(), ENCODING, subscription,
				UploadMode.PREVIEW);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals("6.0.1", result.getJiraVersion());
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(0, result.getComponents().intValue());
		Assert.assertEquals(0, result.getCustomFields().intValue());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(2, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(1, result.getPriorities().intValue());
		Assert.assertEquals(0, result.getResolutions().intValue());
		Assert.assertEquals(1, result.getStatuses().intValue());
		Assert.assertEquals(1, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(0, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(0, result.getLabels().intValue());

		Assert.assertEquals(1, result.getNewIssues().intValue());
		Assert.assertEquals(0, result.getNewComponents().intValue());
		Assert.assertEquals(0, result.getNewVersions().intValue());
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
		Assert.assertEquals(3, result.getChanges().intValue());
		Assert.assertEquals(4, result.getComponents().intValue());
		Assert.assertEquals(9, result.getCustomFields().intValue());
		Assert.assertEquals(2, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(9, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(2, result.getPriorities().intValue());
		Assert.assertEquals(1, result.getResolutions().intValue());
		Assert.assertEquals(2, result.getStatuses().intValue());
		Assert.assertEquals(2, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(4, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(UploadMode.PREVIEW, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(getDate(2014, 03, 02, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(2, result.getLabels().intValue());

		Assert.assertEquals(2, result.getNewIssues().intValue());
		Assert.assertEquals(2, result.getNewComponents().intValue());
		Assert.assertEquals(2, result.getNewVersions().intValue());
	}

	@Test
	public void testZUploadWithInsert() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		final int pcounter = jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10074);

		resource.upload(new ClassPathResource("csv/upload/nominal-complete.csv").getInputStream(), ENCODING, subscription, UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(42, result.getChanges().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertEquals(4, result.getComponents().intValue());
		Assert.assertEquals(9, result.getCustomFields().intValue());
		Assert.assertEquals(3, result.getIssues().intValue());
		Assert.assertEquals(10074, result.getJira().intValue());
		Assert.assertEquals(6, result.getMaxIssue().intValue());
		Assert.assertEquals(2, result.getMinIssue().intValue());
		Assert.assertEquals(2, result.getPriorities().intValue());
		Assert.assertEquals(1, result.getResolutions().intValue());
		Assert.assertEquals(5, result.getStatuses().intValue());
		Assert.assertEquals(2, result.getTypes().intValue());
		Assert.assertEquals(2, result.getUsers().intValue());
		Assert.assertEquals(5, result.getVersions().intValue());
		Assert.assertEquals(getDate(2014, 03, 01, 12, 01, 00), result.getIssueFrom());
		Assert.assertEquals(getDate(2014, 04, 11, 12, 01, 00), result.getIssueTo());
		Assert.assertEquals(UploadMode.FULL, result.getMode());
		Assert.assertEquals("MDA", result.getPkey());
		Assert.assertEquals(6, result.getLabels().intValue());

		Assert.assertEquals(3, result.getNewIssues().intValue());
		Assert.assertEquals(2, result.getNewComponents().intValue());
		Assert.assertEquals(3, result.getNewVersions().intValue());
		Assert.assertFalse(result.isFailed());

		Assert.assertEquals(13, result.getStatusChanges().intValue());
		Assert.assertFalse(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertNull(result.getSynchronizedJira());

		// Lesser than the maximal "pcounter"
		Assert.assertEquals(pcounter,
				jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10074).intValue());

		// Check sequences
		final Map<String, Integer> sequences = AbstractEditor.getInvertedMap(datasource,
				"SELECT SEQ_NAME AS pname, SEQ_ID AS id FROM SEQUENCE_VALUE_ITEM");
		Assert.assertEquals(10200, sequences.get("ChangeGroup").intValue());
		Assert.assertEquals(10200, sequences.get("ChangeItem").intValue());
		Assert.assertEquals(10300, sequences.get("Component").intValue());
		Assert.assertEquals(10300, sequences.get("Issue").intValue());
		Assert.assertEquals(10100, sequences.get("Label").intValue());
		Assert.assertEquals(10300, sequences.get("OSCurrentStep").intValue());
		Assert.assertEquals(10300, sequences.get("OSWorkflowEntry").intValue());
		Assert.assertEquals(10300, sequences.get("Version").intValue());

		final int workflowId = jdbcTemplate
				.queryForObject("SELECT WORKFLOW_ID FROM jiraissue WHERE issuenum=? AND project=? AND issuestatus=? AND priority=?"
						+ " AND RESOLUTION=? AND RESOLUTIONDATE IS NOT NULL AND issuetype=? AND DESCRIPTION=? AND SUMMARY=? AND REPORTER=?"
						+ " AND ASSIGNEE=?", Integer.class, 3, 10074, 6, 3, 1, 1, "DESCRIPTION-34", "SUMMARY-34", "alocquet", "fdaugan");
		Assert.assertEquals(10201, workflowId);

		final String workflow = jdbcTemplate.queryForObject("SELECT NAME FROM OS_WFENTRY WHERE ID=? AND STATE=?", String.class, workflowId,
				1);
		Assert.assertEquals("CSN", workflow);

		final String status = jdbcTemplate.queryForObject("SELECT STATUS FROM OS_CURRENTSTEP WHERE ENTRY_ID=? AND STEP_ID=?", String.class,
				workflowId, 6);
		Assert.assertEquals("Closed", status);

	}

	@Test
	public void testZUploadWithInsertWithFailAuth() throws Exception {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		httpServer.start();

		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete2.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertEquals(10000, result.getJira().intValue());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertFalse(result.getSynchronizedJira());

		// Greater than the maximal "pcounter"
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assert.assertEquals(5124,
				jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10000).intValue());
	}

	@Test
	public void testZUploadWithInsertWithFailCache() throws Exception {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		httpServer.stubFor(post(urlPathEqualTo("/login.jsp"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/WebSudoAuthenticate.jspa")).willReturn(
				aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/secure/project/ViewProjects.jspa")));
		httpServer.stubFor(get(urlPathEqualTo("/secure/admin/groovy/CannedScriptRunner.jspa"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		httpServer.start();

		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete3.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testZUploadWithInsertWithFailReIndex() throws Exception {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		httpServer.stubFor(post(urlPathEqualTo("/login.jsp"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/WebSudoAuthenticate.jspa")).willReturn(
				aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/secure/project/ViewProjects.jspa")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/groovy/CannedScriptRunner.jspa"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_NO_CONTENT)));
		httpServer.stubFor(
				get(urlPathEqualTo("/secure/admin/IndexProject.jspa")).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		httpServer.start();

		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete4.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testZUploadWithInsertWithReIndex() throws Exception {
		startOperationalServer();

		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete5.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(7, result.getChanges().intValue());
		Assert.assertEquals(3, result.getIssues().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertTrue(result.getSynchronizedJira());
	}

	@Test
	public void testZUploadWithInsertNoScriptRunner() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			jdbcTemplate.update("update pluginversion SET pluginkey=? WHERE ID = ?", "N/A", 10170);

			startOperationalServer();

			this.subscription = getSubscription("gStack");
			resource.upload(new ClassPathResource("csv/upload/nominal-complete6.csv").getInputStream(), ENCODING, subscription,
					UploadMode.FULL);
			final ImportStatus result = jiraResource.getTask(subscription);
			Assert.assertEquals(1, result.getChanges().intValue());
			Assert.assertEquals(30, result.getStep());
			Assert.assertTrue(result.getCanSynchronizeJira());
			Assert.assertFalse(result.getScriptRunner());
			Assert.assertFalse(result.getSynchronizedJira());
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginkey=? WHERE ID = ?", "com.onresolve.jira.groovy.groovyrunner", 10170);
		}
	}

	@Test
	public void testZUploadWithInsertWithFailConnect() throws Exception {
		// No started server
		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete7.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertFalse(result.getSynchronizedJira());
	}

	@Test
	public void testZUploadWithInsertNoAssociation() throws Exception {
		startOperationalServer();

		this.subscription = getSubscription("gStack");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete-no-association.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assert.assertEquals(1, result.getChanges().intValue());
		Assert.assertEquals(1, result.getIssues().intValue());
		Assert.assertEquals(30, result.getStep());
		Assert.assertTrue(result.getCanSynchronizeJira());
		Assert.assertTrue(result.getScriptRunner());
		Assert.assertTrue(result.getSynchronizedJira());
	}

	private void startOperationalServer() {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		httpServer.stubFor(post(urlPathEqualTo("/login.jsp"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/WebSudoAuthenticate.jspa")).willReturn(
				aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/secure/project/ViewProjects.jspa")
						.withHeader("X-Atlassian-WebSudo", "Has-Authentication")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/groovy/CannedScriptRunner.jspa"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_NO_CONTENT)));
		httpServer.stubFor(
				get(urlPathEqualTo("/secure/admin/IndexProject.jspa")).willReturn(aResponse().withStatus(HttpStatus.SC_NO_CONTENT)));
		httpServer.start();
	}

}
