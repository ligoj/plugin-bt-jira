package org.ligoj.app.plugin.bt.jira.editor;

import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * Editor used for unmanaged types.
 */
public class FailsafeEditor extends AbstractEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		// No edition available
		throw new ValidationJsonException("cf$" + customField.getName(), "Custom field '" + customField.getName() + "' has a not yet managed type '"
				+ customField.getFieldType() + "'");
	}

	@Override
	public Object getValue(final CustomField customField, final CustomFieldValue value) {
		// Not managed type, so null
		return null;
	}

}
