package org.ligoj.app.plugin.bt.jira;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link JiraLoginHttpResponseCallback}
 */
public class JiraLoginHttpResponseCallbackTest {

	@Test
	public void test() {
		Assert.assertFalse(new JiraLoginHttpResponseCallback().acceptLocation(null));
		Assert.assertFalse(new JiraLoginHttpResponseCallback().acceptLocation("/login.jsp"));
		Assert.assertTrue(new JiraLoginHttpResponseCallback().acceptLocation("/"));
	}
}
