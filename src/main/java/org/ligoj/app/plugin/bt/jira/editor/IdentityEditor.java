/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;

/**
 * A valid value, whatever the content. Return the original value.
 */
public class IdentityEditor extends AbstractEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		return value;
	}
	
	@Override
	public Object getValue(final CustomField customField, final CustomFieldValue value) {
		return ObjectUtils.getIfNull(value.getStringValue(), value.getTextValue());
	}


}
