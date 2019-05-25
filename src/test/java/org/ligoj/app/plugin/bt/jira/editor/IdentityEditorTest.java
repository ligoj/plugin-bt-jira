/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;

/**
 * test class of {@link IdentityEditor}
 */
class IdentityEditorTest extends AbstractEditorTest {

	@Test
	void testGetValueFromString() {
		Assertions.assertEquals("value", new IdentityEditor().getValue(null, "value"));
	}

	@Test
	void testGetValueFromDataText() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setTextValue("value");
		Assertions.assertEquals("value", new IdentityEditor().getValue(null, value));
	}

	@Test
	void testGetValueFromDataString() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setStringValue("value");
		Assertions.assertEquals("value", new IdentityEditor().getValue(null, value));
	}

	@Test
	void testManagedTypes() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assertions.assertEquals(4.3,
				((Double) getEditor("com.atlassian.jira.plugin.system.customfieldtypes:float").getValue(customField, "4.3"))
						.doubleValue(),
				0.01);
		Assertions.assertEquals("http://any-path",
				getEditor("com.atlassian.jira.plugin.system.customfieldtypes:url").getValue(customField, "http://any-path"));
		Assertions.assertEquals("any",
				getEditor("com.atlassian.jira.plugin.system.customfieldtypes:textfield").getValue(customField, "any"));
		Assertions.assertEquals("any",
				getEditor("com.atlassian.jira.plugin.system.customfieldtypes:textarea").getValue(customField, "any"));
	}
}
