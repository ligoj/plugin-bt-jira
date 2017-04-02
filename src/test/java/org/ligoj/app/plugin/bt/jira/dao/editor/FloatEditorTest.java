package org.ligoj.app.plugin.bt.jira.dao.editor;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.bt.jira.editor.FloatEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link FloatEditor}
 */
public class FloatEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testGetValueFromStringDot() {
		Assert.assertEquals(1.0, new FloatEditor().getValue(null, "1.0"));
	}

	@Test
	public void testGetValueFromStringComma() {
		Assert.assertEquals(1.0, new FloatEditor().getValue(null, "1,0"));
	}

	@Test(expected = ValidationJsonException.class)
	public void testGetValueFromStringInvalid() {
		new FloatEditor().getValue(new CustomField(), "a");
	}

	@Test
	public void testGetValueFromDataText() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setNumberValue(1.0);
		Assert.assertEquals(1.0, new FloatEditor().getValue(null, value));
	}
}
