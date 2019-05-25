/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link UrlEditor}
 */
public class UrlEditorTest extends AbstractDataGeneratorTest {

	@Test
	public void testGetValue() {
		Assertions.assertEquals("http://www.google.fr", new UrlEditor().getValue(null, "http://www.google.fr"));
	}

	@Test
	public void testGetValueInvalid() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			new UrlEditor().getValue(new CustomField(), "data");
		}), "cf$null", "Invalid value 'data'. Expected : A HTTP URL");
	}
}
