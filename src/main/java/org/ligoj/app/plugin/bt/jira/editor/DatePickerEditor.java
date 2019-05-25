/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**
 * Date picker editor.
 */
public class DatePickerEditor extends DateEditor {

	@Override
	public Date postTreatment(final Date date) {
		return DateUtils.truncate(date, java.util.Calendar.DATE);
	}
}
