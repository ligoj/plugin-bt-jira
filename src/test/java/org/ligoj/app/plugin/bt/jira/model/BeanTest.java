/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.model;

import org.junit.jupiter.api.Test;

/**
 * Simple test of API beans.
 */
public class BeanTest {

	@Test
	public void testUploadMode() {
		UploadMode.valueOf(UploadMode.values()[UploadMode.FULL.ordinal()].name());
	}
}
