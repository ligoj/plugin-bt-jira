package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link FloatEditor}
 */
public class FloatEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testGetValueFromStringDot() {
		Assertions.assertEquals(1.0, new FloatEditor().getValue(null, "1.0"));
	}

	@Test
	public void testGetValueFromStringComma() {
		Assertions.assertEquals(1.0, new FloatEditor().getValue(null, "1,0"));
	}

	@Test
	public void testGetValueFromStringInvalid() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			new FloatEditor().getValue(new CustomField(), "a");
		}), "cf$null", "Invalid value 'a'. Expected : A decimal value");
	}

	@Test
	public void testGetValueFromDataText() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setNumberValue(1.0);
		Assertions.assertEquals(1.0, new FloatEditor().getValue(null, value));
	}
}
