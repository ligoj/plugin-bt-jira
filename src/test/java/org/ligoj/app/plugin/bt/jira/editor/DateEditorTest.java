/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

import java.util.Date;

/**
 * test class of {@link DateEditor}
 */
class DateEditorTest extends AbstractEditorTest {

	private static final String D_20140521_1545 = "2014-05-21 15:45";
	private static final String D_20140521_154556 = D_20140521_1545 + ":56";

	@Test
	void customColumn() {
		Assertions.assertEquals("DATEVALUE", new DateEditor().getCustomColumn());
	}

	@Test
	void getValueInvalid() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		final var editor = new DateEditor();
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> editor.getValue(customField, "invalid")), "cf$NAME", "Invalid value 'invalid'. Expected : A valid date");
	}

	@Test
	void postTreatment() {
		final Date newDate = new Date();
		Assertions.assertEquals(newDate, new DateEditor().postTreatment(newDate));
	}

	@Test
	void getValueFromString() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), new DateEditor().getValue(customField, "2014-05-21 15:45:56"));
	}

	@Test
	void getValueFromData() {
		final CustomField customField = new CustomField();
		final CustomFieldValue value = new CustomFieldValue();
		final Date date = getDate(2014, 5, 21, 15, 45, 56);
		value.setDateValue(date);
		Assertions.assertEquals(date, new DateEditor().getValue(customField, value));
	}

	@Test
	void toDate() {
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("2014-05-21 15:45:56"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate(D_20140521_1545));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate(D_20140521_154556));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("2014/05/21 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21/05/2014 15:45:56"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21/05/2014 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21.5.2014 15:45"));
		Assertions.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21.5.2014 15:45:56"));

		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014-05-21"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014/05/21"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21/05/2014"));
		Assertions.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21.5.2014"));

		Assertions.assertEquals(getDate(2010, 7, 4, 7, 12, 30), DateEditor.toDate("40363.300347222"));
		Assertions.assertEquals(getDate(2010, 7, 4, 7, 12, 30), DateEditor.toDate("40363,300347222"));
	}

	@Test
	void managedTypesDateTime() {
		assertDate(getDate(2014, 5, 21, 15, 45, 56), "com.atlassian.jira.plugin.system.customfieldtypes:datetime");
	}

	@Test
	void managedTypesDatePicker() {
		assertDate(getDate(2014, 5, 21, 0, 0, 0), "com.atlassian.jira.plugin.system.customfieldtypes:datepicker");
	}

	private void assertDate(final Date date, final String key) {
		final var value = getEditor(key).getValue(new CustomField(), D_20140521_154556);
		Assertions.assertEquals(date, value);
	}
}
