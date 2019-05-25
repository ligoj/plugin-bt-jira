/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.ligoj.bootstrap.core.curl.OnlyRedirectHttpResponseCallback;

/**
 * Jira Sudo response handler.
 */
public class JiraSudoHttpResponseCallback extends OnlyRedirectHttpResponseCallback {

	@Override
	protected boolean acceptResponse(final CloseableHttpResponse response) {
		// Check "X-Atlassian-WebSudo" header value equals to "Has-Authentication"
		return super.acceptResponse(response) && response.getFirstHeader("X-Atlassian-WebSudo") != null
				&& "Has-Authentication".equals(response.getFirstHeader("X-Atlassian-WebSudo").getValue());
	}

	@Override
	protected boolean acceptLocation(final String location) {
		// Always accept location since only HTTP headers are used to validate the response
		return true;
	}

}