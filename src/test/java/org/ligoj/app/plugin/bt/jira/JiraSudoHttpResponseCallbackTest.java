package org.ligoj.app.plugin.bt.jira;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class of {@link JiraSudoHttpResponseCallback}
 */
public class JiraSudoHttpResponseCallbackTest {
	private JiraSudoHttpResponseCallback jiraSudoHttpResponseCallback = new JiraSudoHttpResponseCallback();

	@Test
	public void acceptLocation() {
		Assert.assertTrue(jiraSudoHttpResponseCallback.acceptLocation(null));
		Assert.assertTrue(jiraSudoHttpResponseCallback.acceptLocation("/login.jsp"));
	}

	@Test
	public void acceptResponse200() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(200);
		Assert.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	public void acceptResponse302() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(302);
		Assert.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	public void acceptResponse302WithHeader() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(302);
		final Header header = Mockito.mock(Header.class);
		Mockito.when(header.getValue()).thenReturn("any");
		Mockito.when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assert.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	public void acceptResponse302WithCorrectHeader() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(302);
		final Header header = Mockito.mock(Header.class);
		Mockito.when(header.getValue()).thenReturn("Has-Authentication");
		Mockito.when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assert.assertTrue(jiraSudoHttpResponseCallback.acceptResponse(response));
	}
}
