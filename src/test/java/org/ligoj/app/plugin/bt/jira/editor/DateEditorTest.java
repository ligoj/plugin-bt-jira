package org.ligoj.app.plugin.bt.jira.editor;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;

/**
 * test class of {@link DateEditor}
 */
public class DateEditorTest extends AbstractEditorTest {

	private static final String D_20140521_1545 = "2014-05-21 15:45";
	private static final String D_20140521_154556 = D_20140521_1545 + ":56";

	@Test
	public void testCustomColumn() {
		Assert.assertEquals("DATEVALUE", new DateEditor().getCustomColumn());
	}

	@Test
	public void testGetValueInvalid() {
		MatcherUtil.expectValidationException(thrown, "cf$NAME", "Invalid value 'invalid'. Expected : A valid date");
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		new DateEditor().getValue(customField, "invalid");
	}

	@Test
	public void testPostTreatment() {
		final Date newDate = new Date();
		Assert.assertEquals(newDate, new DateEditor().postTreatment(newDate));
	}

	@Test
	public void testGetValueFromString() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 56), new DateEditor().getValue(customField, "2014-05-21 15:45:56"));
	}

	@Test
	public void testGetValueFromData() {
		final CustomField customField = new CustomField();
		final CustomFieldValue value = new CustomFieldValue();
		final Date date = getDate(2014, 5, 21, 15, 45, 56);
		value.setDateValue(date);
		Assert.assertEquals(date, new DateEditor().getValue(customField, value));
	}

	@Test
	public void testToDate() {
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("2014-05-21 15:45:56"));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 00), DateEditor.toDate(D_20140521_1545));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate(D_20140521_154556));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 00), DateEditor.toDate("2014/05/21 15:45"));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21/05/2014 15:45:56"));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21/05/2014 15:45"));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 0), DateEditor.toDate("21.5.2014 15:45"));
		Assert.assertEquals(getDate(2014, 5, 21, 15, 45, 56), DateEditor.toDate("21.5.2014 15:45:56"));

		Assert.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014-05-21"));
		Assert.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("2014/05/21"));
		Assert.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21/05/2014"));
		Assert.assertEquals(getDate(2014, 5, 21, 0, 0, 0), DateEditor.toDate("21.5.2014"));

		Assert.assertEquals(getDate(2010, 07, 04, 07, 12, 30), DateEditor.toDate("40363.300347222"));
		Assert.assertEquals(getDate(2010, 07, 04, 07, 12, 30), DateEditor.toDate("40363,300347222"));
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
		Assert.assertEquals(date, getEditor(key).getValue(new CustomField(), D_20140521_154556));
	}
}
