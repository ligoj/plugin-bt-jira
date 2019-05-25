/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.app.plugin.bt.jira.model.ImportEntry;
import org.ligoj.bootstrap.core.template.DecimalDateProcessor;
import org.ligoj.bootstrap.core.template.ParseDateProcessor;
import org.ligoj.bootstrap.core.template.Processor;

/**
 * Date editor.
 */
public class DateEditor extends AbstractEditor {

	/**
	 * Supported date formats
	 */
	private static final Map<Pattern, Processor<String>> PATTERN_TO_FORMAT = new LinkedHashMap<>();

	static {
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_FR + ImportEntry.SECONDS_PATTERN), new ParseDateProcessor("dd/MM/yyyy HH:mm:ss"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_FR), new ParseDateProcessor("dd/MM/yyyy HH:mm"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_FR), new ParseDateProcessor("dd/MM/yyyy"));

		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_ISO8601 + ImportEntry.SECONDS_PATTERN), new ParseDateProcessor("yyyy-MM-dd HH:mm:ss"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_ISO8601), new ParseDateProcessor("yyyy-MM-dd HH:mm"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_ISO8601), new ParseDateProcessor("yyyy-MM-dd"));

		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_ISO8601B + ImportEntry.SECONDS_PATTERN), new ParseDateProcessor("yyyy/MM/dd HH:mm:ss"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_ISO8601B), new ParseDateProcessor("yyyy/MM/dd HH:mm"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_ISO8601B), new ParseDateProcessor("yyyy/MM/dd"));

		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_EN + ImportEntry.SECONDS_PATTERN), new ParseDateProcessor("dd.MM.yyyy HH:mm:ss"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_TIME_EN), new ParseDateProcessor("dd.MM.yyyy HH:mm"));
		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_EN), new ParseDateProcessor("dd.MM.yyyy"));

		PATTERN_TO_FORMAT.put(Pattern.compile(ImportEntry.DATE_DECIMAL), new DecimalDateProcessor());
	}

	@Override
	public String getCustomColumn() {
		return "DATEVALUE";
	}

	@Override
	public Object getValue(final CustomField customField, final CustomFieldValue value) {
		return value.getDateValue();
	}

	@Override
	public Date getValue(final CustomField customField, final String value) {
		final Date date = toDate(value);
		if (date == null) {
			throw newValidationException(customField, value, "A valid date");
		}
		return postTreatment(date);
	}

	/**
	 * Post-treatment of date.
	 * 
	 * @param date
	 *            the original date.
	 * @return the manipulated date.
	 */
	public Date postTreatment(final Date date) {
		return date;
	}

	/**
	 * Parse the date using ordered patterns {@link #PATTERN_TO_FORMAT}.
	 * 
	 * @param rawDate
	 *            the date as string.
	 * @return the date object of <code>null</code>.O
	 */
	public static Date toDate(final String rawDate) {
		for (final Entry<Pattern, Processor<String>> pattern : PATTERN_TO_FORMAT.entrySet()) {
			if (pattern.getKey().matcher(rawDate).find()) {
				return (Date) pattern.getValue().getValue(rawDate);
			}
		}
		return null;
	}
}
