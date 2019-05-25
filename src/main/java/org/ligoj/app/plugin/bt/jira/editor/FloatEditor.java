/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;

/**
 * Decimal editor.
 */
public class FloatEditor extends AbstractEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		try {
			return Double.valueOf(value.replace(',', '.'));
		} catch (final NumberFormatException nfe) {
			throw newValidationException(customField, value, "A decimal value"); // NOPMD
		}
	}

	@Override
	public Object getValue(final CustomField customField, final CustomFieldValue value) {
		return value.getNumberValue();
	}

}
