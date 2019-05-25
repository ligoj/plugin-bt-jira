/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * test class of {@link DatePickerEditor}
 */
class DatePickerEditorTest extends AbstractDataGeneratorTest {

	@Test
	void testPostTreatment() {
		Assertions.assertEquals(getDate(2015, 8, 9),
				new DatePickerEditor().postTreatment(getDate(2015, 8, 9, 15, 58, 35)));
	}
}
