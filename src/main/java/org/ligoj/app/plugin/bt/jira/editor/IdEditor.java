/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;

/**
 * Return the identifier corresponding to the given text.
 */
public class IdEditor extends AbstractEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		final Integer id = customField.getInvertValues().get(value);
		if (id == null) {
			throw newValidationException(customField, value, StringUtils.join(customField.getInvertValues().keySet(), ','));
		}
		return id;
	}

	@Override
	public Object getValue(final CustomField customField, final CustomFieldValue value) {
		return customField.getValues().get(Integer.valueOf(value.getStringValue()));
	}

	@Override
	public void populateValues(final DataSource dataSource, final CustomField customField, final int project) {
		// Get the valid values labels
		customField.setValues(getMap(dataSource, "SELECT co.ID AS id, co.customvalue AS pname, co.SEQUENCE"
				+ " FROM customfieldoption AS co, configurationcontext AS cc"
				+ " WHERE co.CUSTOMFIELDCONFIG = cc.FIELDCONFIGSCHEME AND co.CUSTOMFIELD = ? AND (PROJECT = ? OR (PROJECT IS NULL"
				+ "  AND NOT EXISTS(SELECT ID FROM configurationcontext WHERE PROJECT = ? AND customfield = ?))) ORDER BY co.SEQUENCE",
				customField.getId(), project, project, "customfield_" + customField.getId()));

		// Also save the inverted values
		customField.setInvertValues(new TreeMap<>(MapUtils.invertMap(customField.getValues())));
	}
}
