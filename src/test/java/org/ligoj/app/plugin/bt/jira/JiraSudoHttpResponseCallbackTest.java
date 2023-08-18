/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test class of {@link JiraSudoHttpResponseCallback}
 */
class JiraSudoHttpResponseCallbackTest {
	private final JiraSudoHttpResponseCallback jiraSudoHttpResponseCallback = new JiraSudoHttpResponseCallback();

	@Test
	void acceptLocation() {
		Assertions.assertTrue(jiraSudoHttpResponseCallback.acceptLocation(null));
		Assertions.assertTrue(jiraSudoHttpResponseCallback.acceptLocation("/login.jsp"));
	}

	@Test
	void acceptResponse200() {
		final var response = Mockito.mock(ClassicHttpResponse.class);
		Mockito.when(response.getCode()).thenReturn(200);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302() {
		final var response = Mockito.mock(ClassicHttpResponse.class);
		Mockito.when(response.getCode()).thenReturn(302);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302WithHeader() {
		final var response = Mockito.mock(ClassicHttpResponse.class);
		Mockito.when(response.getCode()).thenReturn(302);
		final var header = Mockito.mock(Header.class);
		Mockito.when(header.getValue()).thenReturn("any");
		Mockito.when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302WithCorrectHeader() {
		final var response = Mockito.mock(ClassicHttpResponse.class);
		Mockito.when(response.getCode()).thenReturn(302);
		final var header = Mockito.mock(Header.class);
		Mockito.when(header.getValue()).thenReturn("Has-Authentication");
		Mockito.when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assertions.assertTrue(jiraSudoHttpResponseCallback.acceptResponse(response));
	}
}
