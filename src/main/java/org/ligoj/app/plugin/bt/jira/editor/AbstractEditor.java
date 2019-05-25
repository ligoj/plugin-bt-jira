/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * Custom field editor converting {@link String} to the expected value of a custom field.
 */
public abstract class AbstractEditor {

	/**
	 * Return a map with K is result identifier, and V is the string value. Assumes there is a 'id' and a 'pname'
	 * column. Name is trimmed.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param query
	 *            The SELECT query to execute.
	 * @param parameters
	 *            the optional, ordered parameters of query.
	 * @return a {@link Map} where KEY is the 'id' column (number), and the VALUE is the 'pname' column (string).
	 */
	public static Map<Integer, String> getMap(final DataSource dataSource, final String query, final Object... parameters) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<Integer, String> result = new LinkedHashMap<>();
		jdbcTemplate.query(query, (RowCallbackHandler) rs -> result.put(rs.getInt("id"), StringUtils.trimToEmpty(rs.getString("pname"))), parameters);
		return result;

	}

	/**
	 * Return a map with V is result identifier, and K is the string value. Assumes there is a 'id' and a 'pname'
	 * column. Name is trimmed.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param query
	 *            The SELECT query to execute.
	 * @param parameters
	 *            the optional, ordered parameters of query.
	 * @return a {@link Map} where KEY is the 'pname' column (string), and the VALUE is the 'id' column (number).
	 */
	public static Map<String, Integer> getInvertedMap(final DataSource dataSource, final String query, final Object... parameters) {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		final Map<String, Integer> result = new LinkedHashMap<>();
		jdbcTemplate.query(query, (RowCallbackHandler) rs -> result.put(StringUtils.trimToEmpty(rs.getString("pname")), rs.getInt("id")), parameters);
		return result;
	}

	/**
	 * The 'customfieldvalue' column : STRINGVALUE, TEXTVALUE, DATEVALUE, NUMBERVALUE.
	 * 
	 * @see "SELECT * FROM `customfieldvalue` WHERE CUSTOMFIELD = 10200"
	 * @return the column where the custom field's value will be stored.
	 */
	public String getCustomColumn() {
		return "STRINGVALUE";
	}

	/**
	 * Validate the value of custom field.
	 * 
	 * @param customField
	 *            the custom field definition.
	 * @param value
	 *            the {@link String} value to set to the custom field.
	 * @return the parsed value.
	 */
	public abstract Object getValue(CustomField customField, String value);

	/**
	 * Return the value corresponding to the given custom field row.
	 * 
	 * @param customField
	 *            the custom field definition.
	 * @param value
	 *            the {@link CustomFieldValue} value from the database.
	 * @return the right value from the {@link CustomFieldValue}.
	 */
	public abstract Object getValue(CustomField customField, CustomFieldValue value);

	/**
	 * Return a new {@link ValidationJsonException} corresponding to an out of bound text value.
	 * 
	 * @param customField
	 *            the custom field definition.
	 * @param value
	 *            the {@link String} to match.
	 * @param expected
	 *            the valid value(s)
	 * @return a new {@link ValidationJsonException}
	 */
	protected ValidationJsonException newValidationException(final INamableBean<?> customField, final String value, final String expected) {
		return new ValidationJsonException("cf$" + customField.getName(), "Invalid value '" + value + "'. Expected : " + expected);
	}

	/**
	 * Fill the {@link CustomField} valid values.
	 * 
	 * @param dataSource
	 *            The data source of JIRA database.
	 * @param customField
	 *            the custom field definition.
	 * @param project
	 *            Jira project identifier.
	 */
	public void populateValues(final DataSource dataSource, final CustomField customField, final int project) {
		// Not valid as default
	}

}
