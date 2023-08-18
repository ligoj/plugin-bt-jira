/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.in;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test class of {@link JiraImportPluginResource}
 */
class JiraImport2PluginResourceTest extends AbstractJiraImportPluginResourceTest {

	@Test
	void testZUploadWithInsertWithFailAuth() throws Exception {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		httpServer.start();

		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete2.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertEquals(10000, result.getJira().intValue());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertFalse(result.getSynchronizedJira());

		// Greater than the maximal "pcounter"
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		Assertions.assertEquals(5124,
				jdbcTemplate.queryForObject("SELECT pcounter FROM project WHERE ID = ?", Integer.class, 10000).intValue());
	}

	@Test
	void testZUploadWithInsertWithFailCache() throws Exception {
		httpServer.stubFor(get(urlPathEqualTo("/login.jsp")).willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		httpServer.stubFor(post(urlPathEqualTo("/login.jsp"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/")));
		httpServer.stubFor(post(urlPathEqualTo("/secure/admin/WebSudoAuthenticate.jspa")).willReturn(
				aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "/secure/project/ViewProjects.jspa")));
		httpServer.stubFor(get(urlPathEqualTo("/secure/admin/groovy/CannedScriptRunner.jspa"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		httpServer.start();

		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete3.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	void testZUploadWithInsertWithFailReIndex() throws Exception {
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

		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete4.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	void testZUploadWithInsertWithReIndex() throws Exception {
		startOperationalServer();

		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete5.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(7, result.getChanges().intValue());
		Assertions.assertEquals(3, result.getIssues().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertTrue(result.getSynchronizedJira());
	}

	@Test
	void testZUploadWithInsertNoScriptRunner() throws Exception {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		try {
			jdbcTemplate.update("update pluginversion SET pluginkey=? WHERE ID = ?", "N/A", 10170);

			startOperationalServer();

			this.subscription = getSubscription("Jupiter");
			resource.upload(new ClassPathResource("csv/upload/nominal-complete6.csv").getInputStream(), ENCODING, subscription,
					UploadMode.FULL);
			final ImportStatus result = jiraResource.getTask(subscription);
			Assertions.assertEquals(1, result.getChanges().intValue());
			Assertions.assertEquals(30, result.getStep());
			Assertions.assertTrue(result.getCanSynchronizeJira());
			Assertions.assertFalse(result.getScriptRunner());
			Assertions.assertFalse(result.getSynchronizedJira());
		} finally {
			jdbcTemplate.update("update pluginversion SET pluginkey=? WHERE ID = ?", "com.onresolve.jira.groovy.groovyrunner", 10170);
		}
	}

	@Test
	void testZUploadWithInsertWithFailConnect() throws Exception {
		// No started server
		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete7.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertFalse(result.getSynchronizedJira());
	}

	@Test
	void testZUploadWithInsertNoAssociation() throws Exception {
		startOperationalServer();

		this.subscription = getSubscription("Jupiter");
		resource.upload(new ClassPathResource("csv/upload/nominal-complete-no-association.csv").getInputStream(), ENCODING, subscription,
				UploadMode.FULL);
		final ImportStatus result = jiraResource.getTask(subscription);
		Assertions.assertEquals(1, result.getChanges().intValue());
		Assertions.assertEquals(1, result.getIssues().intValue());
		Assertions.assertEquals(30, result.getStep());
		Assertions.assertTrue(result.getCanSynchronizeJira());
		Assertions.assertTrue(result.getScriptRunner());
		Assertions.assertTrue(result.getSynchronizedJira());
	}
}
