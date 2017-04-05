package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.bt.jira.editor.DatePickerEditor;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * test class of {@link DatePickerEditor}
 */
public class DatePickerEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testPostTreatment() {
		Assert.assertEquals(getDate(2015, 8, 9), new DatePickerEditor().postTreatment(getDate(2015, 8, 9, 15, 58, 35)));
	}
}
