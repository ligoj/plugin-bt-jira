package org.ligoj.app.plugin.bt.jira;

import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.ligoj.bootstrap.core.curl.CurlRequest;
import org.ligoj.bootstrap.core.curl.HttpResponseCallback;

/**
 * JIRA Curl processor.
 */
public class JiraCurlProcessor extends CurlProcessor {

	/**
	 * Special callback for JIRA login check.
	 */
	public static final HttpResponseCallback LOGIN_CALLBACK = new JiraLoginHttpResponseCallback();

	/**
	 * Special callback for JIRA Sudo check.
	 */
	public static final HttpResponseCallback SUDO_CALLBACK = new JiraSudoHttpResponseCallback();

	@Override
	protected boolean process(final CurlRequest request) {
		// Add headers for SSO
		request.getHeaders().put("X-Atlassian-Token", "nocheck");
		return super.process(request);
	}

}
