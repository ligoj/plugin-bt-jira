/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.ligoj.app.plugin.bt.jira.model.CustomField;

/**
 * URL editor. Value is checked for string parsing.
 */
public class UrlEditor extends IdentityEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		if (!value.contains("://")) {
			throw newValidationException(customField, value, "A HTTP URL");
		}
		return value;
	}

}
