/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link JiraLoginHttpResponseCallback}
 */
public class JiraLoginHttpResponseCallbackTest {

	@Test
	public void test() {
		Assertions.assertFalse(new JiraLoginHttpResponseCallback().acceptLocation(null));
		Assertions.assertFalse(new JiraLoginHttpResponseCallback().acceptLocation("/login.jsp"));
		Assertions.assertTrue(new JiraLoginHttpResponseCallback().acceptLocation("/"));
	}
}
