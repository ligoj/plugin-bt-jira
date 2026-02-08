/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.in;

import jakarta.transaction.Transactional;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.JiraPluginResource;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Test class of {@link JiraImportPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
abstract class AbstractJiraImportPluginResourceTest extends AbstractJiraUploadTest {

	protected static final String ENCODING = "cp1250";

	protected JiraImportPluginResource resource;

	protected JiraPluginResource jiraResource;

	@BeforeEach
	void prepareSubscription() {
		this.subscription = getSubscription("MDA");
		resource = new JiraImportPluginResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);

		// Replace the task management to handle the inner transaction
		jiraResource = new JiraPluginResource() {
			public  ImportStatus startTask(final Integer lockedId, final Consumer<ImportStatus> initializer) {
				return super.startTaskInternal(lockedId, initializer);
			}

			public ImportStatus nextStep(final Integer lockedId, final Consumer<ImportStatus> stepper) {
				return super.nextStepInternal(lockedId, stepper);
			}
			public ImportStatus endTask(final Integer lockedId, final boolean failed) {
				return endTaskInternal(lockedId, failed, t -> {
					// Nothing to do by default
				});
			}

		};
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(jiraResource);
		resource.resource = jiraResource;
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
