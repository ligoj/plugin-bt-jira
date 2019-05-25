/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.hsqldb.jdbc.JDBCDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.plugin.bt.IssueSla;
import org.ligoj.app.plugin.bt.SlaComputations;
import org.ligoj.app.plugin.bt.SlaConfiguration;
import org.ligoj.app.plugin.bt.SlaData;
import org.ligoj.app.plugin.bt.dao.SlaRepository;
import org.ligoj.app.plugin.bt.jira.AbstractJiraDataTest;
import org.ligoj.app.plugin.bt.jira.JiraSlaComputations;
import org.ligoj.app.plugin.bt.jira.dao.JiraDao;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.csv.CsvBeanReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link JiraExportPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class JiraExportPluginResourceTest extends AbstractJiraDataTest {

	@Autowired
	private JiraExportPluginResource resource;

	@Autowired
	private SlaRepository slaRepository;

	@Test
	public void getSlaComputationsNoChangeJira6() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "6.0.1", 10075);
			getSlaComputationsNoChange();
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginversion=? WHERE ID = ?", "4.4.2", 10075);
		}
	}

	@Test
	public void getSlaComputationsNoChange() {
		final int subscription = getSubscription("gStack");
		final SlaComputations computations = resource.getSlaComputations(subscription, false);
		Assertions.assertNotNull(computations);
		final List<IssueSla> issues = computations.getIssues();
		Assertions.assertEquals(0, issues.size());

		final List<SlaConfiguration> slaConfigurations = computations.getSlaConfigurations();
		Assertions.assertEquals(0, slaConfigurations.size());
	}

	@Test
	public void getCustomFieldsById() {
		final Map<Integer, CustomFieldEditor> customFieldsById = SpringUtils.getBean(JiraDao.class).getCustomFieldsById(null,
				new HashSet<>(), 0);
		Assertions.assertEquals(0, customFieldsById.size());
	}

	@Test
	public void getSlaComputationsCsv() throws Exception {
		final int subscription = getSubscription("MDA");
		final StreamingOutput csv = (StreamingOutput) resource.getSlaComputationsCsv(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
		final BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "cp1252"));
		final String header = inputStreamReader.readLine();
		Assertions.assertEquals(
				"id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText;created;createdTimestamp;reporter;assignee"
						+ ";dueDate;dueDateTimestamp;[SLA] Livraison(h:m:s);[SLA] Livraison(ms)"
						+ ";[SLA] Livraison(Start);[SLA] Livraison(Start timestamp);[SLA] Livraison(Stop);[SLA] Livraison(Stop timestamp)"
						+ ";[SLA] Livraison(Revised Due Date);[SLA] Livraison(Revised Due Date timestamp)"
						+ ";[SLA] Livraison(Revised Due Date distance h:m:s);[SLA] Livraison(Revised Due Date distance ms)"
						+ ";#Assigned;#Closed;#In Progress;#Open;#Reopened;#Resolved",
				header);
		final CsvBeanReader<CsvChange> beanReader = new CsvBeanReader<>(inputStreamReader, CsvChange.class, "id", "issue", "status",
				"statusText", "type", "typeText", "priority", "priorityText", "resolution", "resolutionText", "created", "createdTimestamp",
				"reporter", "assignee", "dueDate", "dueDateTimestamp", "slaLivraison", "slaLivraisonMs", "slaLivraisonStart",
				"slaLivraisonStartTimestamp", "slaLivraisonStop", "slaLivraisonStopTimestamp", "slaRevisedDueDate",
				"slaRevisedDueDateTimestamp", "slaRevisedDueDateDistance", "slaRevisedDueDateDistanceMs", "nbAssigned", "nbClosed",
				"nbInProgress", "nbOpen", "nbReopened", "nbResolved");
		CsvChange issue = beanReader.read();
		boolean issue174 = true;
		int count = 0;
		while (issue != null) {
			if (count == 0) {
				Assertions.assertEquals(11432, issue.getId());
				Assertions.assertEquals("MDA-1", issue.getIssue());
				Assertions.assertEquals("fdaugan", issue.getReporter());
				Assertions.assertEquals("fdaugan", issue.getAssignee());
				Assertions.assertEquals(1, issue.getType());
				Assertions.assertEquals("Bug", issue.getTypeText());
				Assertions.assertEquals(3, issue.getPriority());
				Assertions.assertEquals("Major", issue.getPriorityText());
				Assertions.assertEquals(6, issue.getStatus());
				Assertions.assertEquals("CLOSED", issue.getStatusText());
				Assertions.assertEquals(1, issue.getResolution());
				Assertions.assertEquals("Fixed", issue.getResolutionText());
				Assertions.assertEquals(getDate(2009, 03, 23, 15, 26, 43), issue.getCreated());
				Assertions.assertEquals(1237818403000L, issue.getCreatedTimestamp());
				Assertions.assertEquals(getDate(2009, 03, 24, 0, 0, 0), issue.getDueDate());
				Assertions.assertEquals(1237849200000L, issue.getDueDateTimestamp());
				Assertions.assertEquals("4946:33:17", issue.getSlaLivraison());
				Assertions.assertEquals(17807597000L, issue.getSlaLivraisonMs());
				Assertions.assertEquals(getDate(2009, 03, 24, 0, 0, 0), issue.getSlaRevisedDueDate());
				Assertions.assertEquals(1237849200000L, issue.getSlaRevisedDueDateTimestamp());
				Assertions.assertEquals(1, issue.getNbClosed());
				Assertions.assertEquals(1, issue.getNbOpen());
				Assertions.assertEquals(1, issue.getNbResolved());
				Assertions.assertEquals(0, issue.getNbReopened());
				Assertions.assertEquals(0, issue.getNbInProgress());
				Assertions.assertEquals(0, issue.getNbAssigned());
			}
			if (issue.getIssue().equals("MDA-174")) {
				issue174 = true;
				Assertions.assertEquals(14825, issue.getId());
				Assertions.assertEquals("MDA-174", issue.getIssue());
				Assertions.assertEquals("rfumery", issue.getReporter());
				Assertions.assertEquals("fdaugan", issue.getAssignee());
				Assertions.assertEquals(2, issue.getType());
				Assertions.assertEquals("New Feature", issue.getTypeText());
				Assertions.assertEquals(3, issue.getPriority());
				Assertions.assertEquals("Major", issue.getPriorityText());
				Assertions.assertEquals(1, issue.getResolution());
				Assertions.assertEquals("Fixed", issue.getResolutionText());
				Assertions.assertEquals(6, issue.getStatus());
				Assertions.assertEquals("CLOSED", issue.getStatusText());
				Assertions.assertEquals(getDate(2009, 11, 30, 14, 59, 11), issue.getCreated());
				Assertions.assertEquals(1259589551000L, issue.getCreatedTimestamp());
				Assertions.assertEquals(getDate(2009, 12, 05, 0, 0, 0), issue.getDueDate()); // Due
																								// date
																								// is
																								// a
																								// Saturday
																								// ->
																								// Monday
				Assertions.assertEquals(1259967600000L, issue.getDueDateTimestamp());
				Assertions.assertEquals("28:37:25", issue.getSlaLivraison());
				Assertions.assertEquals(103045000, issue.getSlaLivraisonMs());
				Assertions.assertEquals(getDate(2009, 12, 07, 9, 0, 4), issue.getSlaRevisedDueDate()); // Paused
																										// 4
																										// seconds,
																										// starting
																										// from
																										// 9:00:00
				Assertions.assertEquals(1260172804000L, issue.getSlaRevisedDueDateTimestamp());
				Assertions.assertEquals(1, issue.getNbClosed());
				Assertions.assertEquals(1, issue.getNbOpen());
				Assertions.assertEquals(1, issue.getNbResolved());
				Assertions.assertEquals(0, issue.getNbReopened());
				Assertions.assertEquals(0, issue.getNbInProgress());
				Assertions.assertEquals(0, issue.getNbAssigned());
			}
			count++;
			issue = beanReader.read();
		}
		Assertions.assertTrue(issue174);
		Assertions.assertEquals(197, count);
	}

	@Test
	public void getSimpleCsv() throws Exception {
		final int subscription = getSubscription("MDA");
		final StreamingOutput csv = (StreamingOutput) resource.getSimpleCsv(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
		final BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "cp1252"));
		final String header = inputStreamReader.readLine();
		Assertions.assertEquals("id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText;"
				+ "created;createdTimestamp;reporter;assignee;dueDate;dueDateTimestamp;summary", header);
		final CsvBeanReader<CsvSimple> beanReader = new CsvBeanReader<>(inputStreamReader, CsvSimple.class, "id", "issue", "status",
				"statusText", "type", "typeText", "priority", "priorityText", "resolution", "resolutionText", "created", "createdTimestamp",
				"reporter", "assignee", "dueDate", "dueDateTimestamp", "summary");
		final CsvSimple issue = beanReader.read();
		Assertions.assertEquals(11432, issue.getId());
		Assertions.assertEquals("MDA-1", issue.getIssue());
		Assertions.assertEquals("fdaugan", issue.getReporter());
		Assertions.assertEquals("fdaugan", issue.getAssignee());
		Assertions.assertEquals(1, issue.getType());
		Assertions.assertEquals("Bug", issue.getTypeText());
		Assertions.assertEquals(3, issue.getPriority());
		Assertions.assertEquals("Major", issue.getPriorityText());
		Assertions.assertEquals(6, issue.getStatus());
		Assertions.assertEquals("CLOSED", issue.getStatusText());
		Assertions.assertEquals(1, issue.getResolution());
		Assertions.assertEquals("Fixed", issue.getResolutionText());
		Assertions.assertEquals(getDate(2009, 03, 23, 15, 26, 43), issue.getCreated());
		Assertions.assertEquals(1237818403000L, issue.getCreatedTimestamp());
		Assertions.assertEquals("Mise à disposition d un update site pour RSM 7.0.0.4", issue.getSummary());
	}

	@Test
	public void getSlaComputationsCsvWithCustomFields() throws Exception {
		final int subscription = getSubscription("MDA");
		final StreamingOutput csv = (StreamingOutput) resource.getSlaComputationsCsvWithCustomFields(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
		final BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "cp1252"));
		final String header = inputStreamReader.readLine();
		Assertions.assertEquals(
				"id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText;created;createdTimestamp;reporter;assignee"
						+ ";dueDate;dueDateTimestamp;timeSpent(s);timeEstimate(s);timeEstimateInit(s);parent;components;N° DFE;Délai levée réserves (jrs)"
						+ ";Processus;cf-group;Date de livraison;Région;[SLA] Livraison(h:m:s);[SLA] Livraison(ms)"
						+ ";[SLA] Livraison(Start);[SLA] Livraison(Start timestamp);[SLA] Livraison(Stop);[SLA] Livraison(Stop timestamp)"
						+ ";[SLA] Livraison(Revised Due Date);[SLA] Livraison(Revised Due Date timestamp)"
						+ ";[SLA] Livraison(Revised Due Date distance h:m:s);[SLA] Livraison(Revised Due Date distance ms)"
						+ ";#Assigned;#Closed;#In Progress;#Open;#Reopened;#Resolved",
				header);
		String lastLine = inputStreamReader.readLine();
		Assertions.assertTrue(lastLine.startsWith(
				"11432;MDA-1;6;CLOSED;1;Bug;3;Major;1;Fixed;2009/03/23 15:26:43;1237818403000;fdaugan;fdaugan;2009/03/24 00:00:00;1237849200000;12951;11951;10951;15114;;;;;;;;"));
		for (int i = 8; i-- > 0;) {
			lastLine = inputStreamReader.readLine();
		}
		Assertions.assertTrue(lastLine.startsWith(
				"12706;MDA-41;1;OPEN;2;New Feature;4;Minor;;;2009/07/09 08:45:33;1247121933000;xsintive;fdaugan;;;;;;;Javascript,Java;\"V1.0\";1,25;\"Value\";;2014/01/30 16:57:00;\"E,A\";"));
		Assertions.assertTrue(lastLine.endsWith(";0;1;1;2;1;0"));
	}

	@Test
	public void getStatusHistory() throws Exception {
		final int subscription = getSubscription("MDA");
		final StreamingOutput csv = (StreamingOutput) resource.getStatusHistory(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
		final BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "cp1252"));
		final String header = inputStreamReader.readLine();
		Assertions.assertEquals("issueid;key;author;from;to;fromText;toText;date;dateTimestamp", header);
		String lastLine = inputStreamReader.readLine();
		Assertions.assertEquals("11432;MDA-1;fdaugan;;1;;OPEN;2009/03/23 15:26:43;1237818403000", lastLine);
		lastLine = inputStreamReader.readLine();
		Assertions.assertEquals("11437;MDA-4;xsintive;;1;;OPEN;2009/03/23 16:23:31;1237821811000", lastLine);
		lastLine = inputStreamReader.readLine();
		lastLine = inputStreamReader.readLine();
		lastLine = inputStreamReader.readLine();
		lastLine = inputStreamReader.readLine();
		Assertions.assertEquals("11535;MDA-8;challer;;1;;OPEN;2009/04/01 14:20:29;1238588429000", lastLine);
		lastLine = inputStreamReader.readLine();
		lastLine = inputStreamReader.readLine();
		lastLine = inputStreamReader.readLine();
		Assertions.assertEquals("11535;MDA-8;fdaugan;1;10024;OPEN;ASSIGNED;2009/04/09 09:45:16;1239263116000", lastLine);
		lastLine = inputStreamReader.readLine();
		Assertions.assertEquals("11535;MDA-8;fdaugan;10024;3;ASSIGNED;IN PROGRESS;2009/04/09 09:45:30;1239263130000", lastLine);
	}

	@Test
	public void getSlaComputationsCsvFiltered() throws Exception {
		final int subscription = getSubscription("MDA");
		slaRepository.findBySubscription(subscription).get(0).setTypes("Bug,New Feature");
		slaRepository.findBySubscription(subscription).get(0).setPriorities("Blocker,Critical");
		slaRepository.findBySubscription(subscription).get(0).setResolutions("Fixed,Won't Fix");

		final StreamingOutput csv = (StreamingOutput) resource.getSlaComputationsCsv(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
		final BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "cp1252"));
		final String header = inputStreamReader.readLine();
		Assertions.assertEquals(
				"id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText;created;createdTimestamp;reporter;assignee"
						+ ";dueDate;dueDateTimestamp;[SLA] Livraison(h:m:s);[SLA] Livraison(ms);[SLA] Livraison(Start);[SLA] Livraison(Start timestamp)"
						+ ";[SLA] Livraison(Stop);[SLA] Livraison(Stop timestamp);[SLA] Livraison(Revised Due Date);[SLA] Livraison(Revised Due Date timestamp)"
						+ ";[SLA] Livraison(Revised Due Date distance h:m:s);[SLA] Livraison(Revised Due Date distance ms)"
						+ ";#Assigned;#Closed;#In Progress;#Open;#Reopened;#Resolved",
				header);
		final CsvBeanReader<CsvChange> beanReader = new CsvBeanReader<>(inputStreamReader, CsvChange.class, "id", "issue", "status",
				"statusText", "type", "typeText", "priority", "priorityText", "resolution", "resolutionText", "created", "createdTimestamp",
				"reporter", "assignee", "dueDate", "dueDateTimestamp", "slaLivraison", "slaLivraisonMs", "slaLivraisonStart",
				"slaLivraisonStartTimestamp", "slaLivraisonStop", "slaLivraisonStopTimestamp", "slaRevisedDueDate",
				"slaRevisedDueDateTimestamp", "slaRevisedDueDateDistance", "slaRevisedDueDateDistanceMs", "nbAssigned", "nbClosed",
				"nbInProgress", "nbOpen", "nbReopened", "nbResolved");
		CsvChange issue = beanReader.read();
		boolean issue174 = true;
		int count = 0;
		while (issue != null) {
			if (count == 0) {
				Assertions.assertEquals(11432, issue.getId());
				Assertions.assertEquals("MDA-1", issue.getIssue());
				Assertions.assertEquals("fdaugan", issue.getReporter());
				Assertions.assertEquals("fdaugan", issue.getAssignee());
				Assertions.assertEquals(1, issue.getType());
				Assertions.assertEquals("Bug", issue.getTypeText());
				Assertions.assertEquals(3, issue.getPriority());
				Assertions.assertEquals("Major", issue.getPriorityText());
				Assertions.assertEquals(6, issue.getStatus());
				Assertions.assertEquals("CLOSED", issue.getStatusText());
				Assertions.assertEquals(1, issue.getResolution());
				Assertions.assertEquals("Fixed", issue.getResolutionText());
				Assertions.assertEquals(getDate(2009, 03, 23, 15, 26, 43), issue.getCreated());
				Assertions.assertEquals(1237818403000L, issue.getCreatedTimestamp());
				Assertions.assertNull(issue.getSlaLivraison());
				Assertions.assertEquals(0, issue.getSlaLivraisonMs());
				Assertions.assertEquals(1, issue.getNbClosed());
				Assertions.assertEquals(1, issue.getNbOpen());
				Assertions.assertEquals(1, issue.getNbResolved());
				Assertions.assertEquals(0, issue.getNbReopened());
				Assertions.assertEquals(0, issue.getNbInProgress());
				Assertions.assertEquals(0, issue.getNbAssigned());
			}
			if (issue.getIssue().equals("MDA-174")) {
				issue174 = true;
				Assertions.assertEquals(14825, issue.getId());
				Assertions.assertEquals("MDA-174", issue.getIssue());
				Assertions.assertEquals("rfumery", issue.getReporter());
				Assertions.assertEquals("fdaugan", issue.getAssignee());
				Assertions.assertEquals(2, issue.getType());
				Assertions.assertEquals("New Feature", issue.getTypeText());
				Assertions.assertEquals(3, issue.getPriority());
				Assertions.assertEquals("Major", issue.getPriorityText());
				Assertions.assertEquals(1, issue.getResolution());
				Assertions.assertEquals("Fixed", issue.getResolutionText());
				Assertions.assertEquals(6, issue.getStatus());
				Assertions.assertEquals("CLOSED", issue.getStatusText());
				Assertions.assertEquals(getDate(2009, 11, 30, 14, 59, 11), issue.getCreated());
				Assertions.assertEquals(1259589551000L, issue.getCreatedTimestamp());
				Assertions.assertNull(issue.getSlaLivraison());
				Assertions.assertEquals(0, issue.getSlaLivraisonMs());
				Assertions.assertEquals(1, issue.getNbClosed());
				Assertions.assertEquals(1, issue.getNbOpen());
				Assertions.assertEquals(1, issue.getNbResolved());
			}
			count++;
			issue = beanReader.read();
		}
		Assertions.assertTrue(issue174);
		Assertions.assertEquals(197, count);
	}

	@Test
	public void getSlaComputationsXls() throws Exception {
		final int subscription = getSubscription("MDA");
		final StreamingOutput csv = (StreamingOutput) resource.getSlaComputationsXls(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
	}

	@Test
	public void getSlaComputationsXlsNoMatchSla() throws Exception {
		final int subscription = getSubscription("MDA");
		repository.findBySubscriptionFetch(subscription).getSlas().get(0).setPriorities("TRIVIAL");
		em.flush();
		em.clear();
		final StreamingOutput csv = (StreamingOutput) resource.getSlaComputationsXls(subscription, "file1").getEntity();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		csv.write(out);
	}

	@Test
	public void getSlaComputationsXlsBropenPipe() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			((StreamingOutput) resource.getSlaComputationsXls(getSubscription("MDA"), "file1").getEntity()).write(null);
		});
	}

	@Test
	public void getSlaConfigurations() {
		final int subscription = getSubscription("MDA");
		final JiraSlaComputations computations = resource.getSlaComputations(subscription, true);
		Assertions.assertNotNull(computations);
		Assertions.assertEquals(10074, computations.getJira());
		Assertions.assertEquals("MDA", computations.getProject().getName());
		Assertions.assertEquals("MDA", computations.getProject().getName());
		final List<IssueSla> issues = computations.getIssues();
		Assertions.assertEquals(197, issues.size());

		final IssueSla issueSla = issues.get(0);
		Assertions.assertEquals(11432, issueSla.getId().intValue());
		Assertions.assertEquals("MDA-1", issueSla.getPkey());
		Assertions.assertEquals("fdaugan", issueSla.getReporter());
		Assertions.assertEquals("fdaugan", issueSla.getAssignee());
		Assertions.assertEquals(1, issueSla.getType());
		Assertions.assertEquals(3, issueSla.getPriority().intValue());
		Assertions.assertEquals(6, issueSla.getStatus());
		Assertions.assertEquals(getDate(2009, 03, 23, 15, 26, 43), issueSla.getCreated());
		Assertions.assertEquals(1, issueSla.getData().size());

		// Timing
		Assertions.assertEquals(12951, issueSla.getTimeSpent().intValue());
		Assertions.assertEquals(11951, issueSla.getTimeEstimate().intValue());
		Assertions.assertEquals(10951, issueSla.getTimeEstimateInit().intValue());

		// 2009,03,23,15,26,43 ~> 2011-08-04 10:37:00
		Assertions.assertEquals(17807597000L, issueSla.getData().get(0).getDuration());

		final List<SlaConfiguration> slaConfigurations = computations.getSlaConfigurations();
		Assertions.assertEquals(1, slaConfigurations.size());
		final SlaConfiguration slaConfiguration = slaConfigurations.get(0);
		Assertions.assertNotNull(slaConfiguration.getId());
		Assertions.assertEquals("OPEN", slaConfiguration.getStart().get(0));
		Assertions.assertEquals("CLOSED", slaConfiguration.getStop().get(0));
		Assertions.assertEquals("RESOLVED", slaConfiguration.getPause().get(0));

		for (final IssueSla issueSlaNotEnded : issues) {
			if (issueSlaNotEnded.getPkey().equals("MDA-174")) {
				Assertions.assertEquals(14825, issueSlaNotEnded.getId().intValue());
				Assertions.assertEquals("MDA-174", issueSlaNotEnded.getPkey());
				Assertions.assertEquals(2, issueSlaNotEnded.getType());
				Assertions.assertEquals(3, issueSlaNotEnded.getPriority().intValue());
				Assertions.assertEquals(6, issueSlaNotEnded.getStatus());
				Assertions.assertEquals(getDate(2009, 11, 30, 14, 59, 11), issueSlaNotEnded.getCreated());
				Assertions.assertEquals(1, issueSlaNotEnded.getData().size());

				// Timing
				Assertions.assertNull(issueSlaNotEnded.getTimeSpent());
				Assertions.assertNull(issueSlaNotEnded.getTimeEstimate());
				Assertions.assertNull(issueSlaNotEnded.getTimeEstimateInit());

				// Global : 2009,11,30,14,59,11 -> 2009-12-04 10:36:40 =
				// 49+3*60*60+3*8*60*60+60*60+36*60+40 = 103049000
				// SLA : 103049000 - (Resolved time) = 103049000 - (2009-12-04
				// 10:36:40 - 2009-12-04 10:36:36) =
				Assertions.assertEquals(103045000, issueSlaNotEnded.getData().get(0).getDuration());
				break;
			}
		}
	}

	@Test
	public void getSlaComputationsUnknownSubscription() {
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.getSlaComputations(0, false);
		});

	}

	@Test
	public void toStyleProcessorNoThreshold() {
		final Deque<Object> contextData = new ArrayDeque<>();
		contextData.add(1); // Index
		final SlaData data = new SlaData();
		data.setDuration(10L); // Duration;
		contextData.add(data);
		final JiraSlaComputations slaComputations = new JiraSlaComputations();
		final SlaConfiguration slaConfiguration0 = new SlaConfiguration();
		slaConfiguration0.setThreshold(1);
		final SlaConfiguration slaConfiguration1 = new SlaConfiguration();
		slaConfiguration1.setThreshold(0);
		slaComputations.setSlaConfigurations(Arrays.asList(slaConfiguration0, slaConfiguration1));
		Assertions.assertEquals("normal", resource.toStyleProcessor(slaComputations, "normal", "invalid").getValue(contextData));

	}

	@Test
	public void toStyleProcessorNoDuration() {
		final Deque<Object> contextData = new LinkedList<>();
		contextData.add(1); // Index
		contextData.add(null); // Duration
		final JiraSlaComputations slaComputations = new JiraSlaComputations();
		final SlaConfiguration slaConfiguration0 = new SlaConfiguration();
		slaConfiguration0.setThreshold(1);
		final SlaConfiguration slaConfiguration1 = new SlaConfiguration();
		slaConfiguration1.setThreshold(1);
		slaComputations.setSlaConfigurations(Arrays.asList(slaConfiguration0, slaConfiguration1));
		Assertions.assertEquals("normal", resource.toStyleProcessor(slaComputations, "normal", "invalid").getValue(contextData));

	}

	@Test
	public void toStyleProcessor() {
		final Deque<Object> contextData = new ArrayDeque<>();
		contextData.add(1); // Index
		final SlaData data = new SlaData();
		data.setDuration(10L); // Duration;
		contextData.add(data);
		final JiraSlaComputations slaComputations = new JiraSlaComputations();
		final SlaConfiguration slaConfiguration0 = new SlaConfiguration();
		slaConfiguration0.setThreshold(1);
		final SlaConfiguration slaConfiguration1 = new SlaConfiguration();
		slaConfiguration1.setThreshold(12);
		slaComputations.setSlaConfigurations(Arrays.asList(slaConfiguration0, slaConfiguration1));
		Assertions.assertEquals("normal", resource.toStyleProcessor(slaComputations, "normal", "invalid").getValue(contextData));
	}

	@Test
	public void toStyleProcessorDistance() {
		final Deque<Object> contextData = new ArrayDeque<>();
		final SlaData data = new SlaData();
		data.setRevisedDueDateDistance(0L); // Perfect distance
		contextData.add(data);
		Assertions.assertEquals("normal", resource.toStyleProcessorDistance("normal", "invalid").getValue(contextData));
	}

	@Test
	public void toStyleProcessorDistanceUndefined() {
		final Deque<Object> contextData = new ArrayDeque<>();
		contextData.add(new SlaData()); // No defined distance
		Assertions.assertEquals("normal", resource.toStyleProcessorDistance("normal", "invalid").getValue(contextData));
	}

	@Test
	public void toStyleProcessorDistanceAdvance() {
		final Deque<Object> contextData = new ArrayDeque<>();
		final SlaData data = new SlaData();
		data.setRevisedDueDateDistance(1L); // Positive distance, advance
		contextData.add(data);
		Assertions.assertEquals("normal", resource.toStyleProcessorDistance("normal", "invalid").getValue(contextData));
	}

	@Test
	public void toStyleProcessorDistanceLate() {
		final Deque<Object> contextData = new ArrayDeque<>();
		final SlaData data = new SlaData();
		data.setRevisedDueDateDistance(-1L); // Negative distance, late
		contextData.add(data);
		Assertions.assertEquals("invalid", resource.toStyleProcessorDistance("normal", "invalid").getValue(contextData));
	}

	@Test
	public void toStyleProcessorOverThreshold() {
		final Deque<Object> contextData = new ArrayDeque<>();
		contextData.add(1); // Index
		final SlaData data = new SlaData();
		data.setDuration(13L); // Duration;
		contextData.add(data);
		final JiraSlaComputations slaComputations = new JiraSlaComputations();
		final SlaConfiguration slaConfiguration0 = new SlaConfiguration();
		slaConfiguration0.setThreshold(14);
		final SlaConfiguration slaConfiguration1 = new SlaConfiguration();
		slaConfiguration1.setThreshold(12);
		slaComputations.setSlaConfigurations(Arrays.asList(slaConfiguration0, slaConfiguration1));
		Assertions.assertEquals("invalid", resource.toStyleProcessor(slaComputations, "normal", "invalid").getValue(contextData));

	}

	/**
	 * Initialize data base with 'MDA' JIRA project.
	 */
	@BeforeAll
	public static void initializeJiraDataBaseForSla() throws SQLException {
		final DataSource datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();
		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/sla/jira-create.sql"), StandardCharsets.UTF_8));
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/sla/jira.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}

	/**
	 * Clean data base with 'MDA' JIRA project.
	 */
	@AfterAll
	public static void cleanJiraDataBaseForSla() throws SQLException {
		final DataSource datasource = new SimpleDriverDataSource(new JDBCDriver(), "jdbc:hsqldb:mem:dataSource", null, null);
		final Connection connection = datasource.getConnection();

		try {
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ClassPathResource("sql/sla/jira-drop.sql"), StandardCharsets.UTF_8));
		} finally {
			connection.close();
		}
	}
}
