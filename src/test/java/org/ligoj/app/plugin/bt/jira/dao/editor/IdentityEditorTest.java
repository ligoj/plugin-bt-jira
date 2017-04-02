package org.ligoj.app.plugin.bt.jira.dao.editor;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.app.plugin.bt.jira.editor.IdentityEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**
 * test class of {@link IdentityEditor}
 */
public class IdentityEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testGetValueFromString() {
		Assert.assertEquals("value", new IdentityEditor().getValue(null, "value"));
	}

	@Test
	public void testGetValueFromDataText() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setTextValue("value");
		Assert.assertEquals("value", new IdentityEditor().getValue(null, value));
	}

	@Test
	public void testGetValueFromDataString() {
		final CustomFieldValue value = new CustomFieldValue();
		value.setStringValue("value");
		Assert.assertEquals("value", new IdentityEditor().getValue(null, value));
	}

	@Test
	public void testManagedTypes() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assert.assertEquals(4.3,
				((Double) AbstractEditor.MANAGED_TYPE.get("com.atlassian.jira.plugin.system.customfieldtypes:float").getValue(customField, "4.3"))
						.doubleValue(),
				0.01);
		Assert.assertEquals("http://any-path",
				AbstractEditor.MANAGED_TYPE.get("com.atlassian.jira.plugin.system.customfieldtypes:url").getValue(customField, "http://any-path"));
		Assert.assertEquals("any",
				AbstractEditor.MANAGED_TYPE.get("com.atlassian.jira.plugin.system.customfieldtypes:textfield").getValue(customField, "any"));
		Assert.assertEquals("any",
				AbstractEditor.MANAGED_TYPE.get("com.atlassian.jira.plugin.system.customfieldtypes:textarea").getValue(customField, "any"));
	}
}
