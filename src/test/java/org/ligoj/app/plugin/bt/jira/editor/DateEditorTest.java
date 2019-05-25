/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link DateEditor}
 */
public class DateEditorTest extends AbstractEditorTest {

	private static final String D_20140521_1545 = "2014-05-21 15:45";
	private static final String D_20140521_154556 = D_20140521_1545 + ":56";

	@Test
	public void testCustomColumn() {
		Assertions.assertEquals("DATEVALUE", new DateEditor().getCustomColumn());
	}

	@Test
	public void testGetValueInvalid() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			new DateEditor().getValue(customField, "invalid");
		}), "cf$NAME", "Invalid value 'invalid'. Expected : A valid date");
	}

	@Test
	public void testPostTreatment() {
		final Date newDate = new Date();
		Assertions.assertEquals(newDate, new DateEditor().postTreatment(newDate));
	}

	@Test
	public void testGetValueFromString() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), new DateEditor().getValue(customField, "2014-05-21 15:45:56"));
	}

	@Test
	public void testGetValueFromData() {
		final CustomField customField = new CustomField();
		final CustomFieldValue value = new CustomFieldValue();
		final Date date = getDate(2014, 5, 21, 15, 45, 56);
		value.setDateValue(date);
		Assertions.assertEquals(date, new DateEditor().getValue(customField, value));
	}

	@Test
	public void testToDate() {
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("2014-05-21 15:45:56"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 00), DateEditor.toDate(D_20140521_1545));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate(D_20140521_154556));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 00), DateEditor.toDate("2014/05/21 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21/05/2014 15:45:56"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21/05/2014 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21.5.2014 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21.5.2014 15:45:56"));

		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014-05-21"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014/05/21"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21/05/2014"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21.5.2014"));

		Assertions.assertEquals(getDate(2010, 07, 04, 07, 12, 30), DateEditor.toDate("40363.300347222"));
		Assertions.assertEquals(getDate(2010, 07, 04, 07, 12, 30), DateEditor.toDate("40363,300347222"));
	}

	@Test
	public void testManagedTypesDateTime() {
		assertDate(getDate(2014, 05, 21, 15, 45, 56), "com.atlassian.jira.plugin.system.customfieldtypes:datetime");
	}

	@Test
	public void testManagedTypesDatePicker() {
		assertDate(getDate(2014, 05, 21, 0, 0, 0), "com.atlassian.jira.plugin.system.customfieldtypes:datepicker");
	}

	private void assertDate(final Date date, final String key) {
		Assertions.assertEquals(date, getEditor(key).getValue(new CustomField(), D_20140521_154556));
	}
}
