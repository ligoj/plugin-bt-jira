package org.ligoj.app.plugin.bt.jira.in;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import javax.transaction.Transactional;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.JiraPluginResource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link JiraImportPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public abstract class AbstractJiraImportPluginResourceTest extends AbstractJiraUploadTest {

	protected static final String ENCODING = "cp1250";

	protected JiraImportPluginResource resource;

	protected JiraPluginResource jiraResource;

	@BeforeEach
	public void prepareSubscription() {
		this.subscription = getSubscription("MDA");
		resource = new JiraImportPluginResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);

		// Replace the task management to handle the inner transaction
		resource.resource = new JiraPluginResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource.resource);
		jiraResource = resource.resource;
	}

	protected void startOperationalServer() {
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
