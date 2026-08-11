/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.jira;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		final var response = mock(ClassicHttpResponse.class);
		when(response.getCode()).thenReturn(200);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302() {
		final var response = mock(ClassicHttpResponse.class);
		when(response.getCode()).thenReturn(302);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302WithHeader() {
		final var response = mock(ClassicHttpResponse.class);
		when(response.getCode()).thenReturn(302);
		final var header = mock(Header.class);
		when(header.getValue()).thenReturn("any");
		when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assertions.assertFalse(jiraSudoHttpResponseCallback.acceptResponse(response));
	}

	@Test
	void acceptResponse302WithCorrectHeader() {
		final var response = mock(ClassicHttpResponse.class);
		when(response.getCode()).thenReturn(302);
		final var header = mock(Header.class);
		when(header.getValue()).thenReturn("Has-Authentication");
		when(response.getFirstHeader("X-Atlassian-WebSudo")).thenReturn(header);
		Assertions.assertTrue(jiraSudoHttpResponseCallback.acceptResponse(response));
	}
}
