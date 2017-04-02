package org.ligoj.app.plugin.bt.jira.dao.editor;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.bt.jira.editor.UrlEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link UrlEditor}
 */
public class UrlEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testGetValue() {
		Assert.assertEquals("http://www.google.fr", new UrlEditor().getValue(null, "http://www.google.fr"));
	}

	@Test(expected = ValidationJsonException.class)
	public void testGetValueInvalid() {
		new UrlEditor().getValue(new CustomField(), "data");
	}
}
