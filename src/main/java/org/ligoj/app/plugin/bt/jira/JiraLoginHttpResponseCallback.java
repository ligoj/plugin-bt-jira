package org.ligoj.app.plugin.bt.jira;

import org.ligoj.bootstrap.core.curl.OnlyRedirectHttpResponseCallback;

/**
 * Jira login response handler.
 */
public class JiraLoginHttpResponseCallback extends OnlyRedirectHttpResponseCallback {

	@Override
	protected boolean acceptLocation(final String location) {
		return super.acceptLocation(location) && !location.endsWith("login.jsp");
	}
}