package org.ligoj.app.plugin.bt.jira.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorUploadTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link IdEditor} and {@link AbstractEditor}
 */
public class IdEditorTest extends AbstractEditorUploadTest {

	@Test
	public void testCustomColumn() {
		Assert.assertEquals("STRINGVALUE", new IdEditor().getCustomColumn());
	}

	@Test
	public void testGetValueInvalid() {
		MatcherUtil.expectValidationException(thrown, "cf$NAME", "Invalid value 'invalid'. Expected : keyA,keyB");
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("keyA", 1);
		values.put("keyB", 2);
		customField.setInvertValues(values);
		customField.setName("NAME");
		new IdEditor().getValue(customField, "invalid");
	}

	@Test
	public void testGetValue() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("key", 1);
		customField.setInvertValues(values);
		Assert.assertEquals(1, ((Integer) new IdEditor().getValue(customField, "key")).intValue());
	}

	@Test
	public void testGetValuebyId() {
		final CustomField customField = new CustomField();
		final CustomFieldValue value = new CustomFieldValue();
		value.setStringValue("4");
		final Map<Integer, String> values = new LinkedHashMap<>();
		values.put(4, "val");
		customField.setValues(values);
		Assert.assertEquals("val", new IdEditor().getValue(customField, value));
	}

	@Test
	public void testPopulateValues() {
		final CustomField customField = new CustomField();
		customField.setName("Motif suspension");
		customField.setId(10056);
		final IdEditor editor = new IdEditor();
		editor.populateValues(datasource, customField, 10074);
		Assert.assertEquals(3, customField.getValues().size());
		Assert.assertEquals("Décalage planning", customField.getValues().get(10048));
		Assert.assertEquals("Demande révisée", customField.getValues().get(10049));
		Assert.assertEquals("Autre (à préciser)", customField.getValues().get(10050));
		Assert.assertEquals(3, customField.getValues().size());
		Assert.assertEquals(10048, customField.getInvertValues().get("Décalage planning").intValue());
		Assert.assertEquals(10049, customField.getInvertValues().get("Demande révisée").intValue());
		Assert.assertEquals(10050, customField.getInvertValues().get("Autre (à préciser)").intValue());
	}

	@Test
	public void testGetMap() {
		final Map<Integer, String> items = AbstractEditor.getMap(datasource,
				"SELECT ID AS id, customvalue AS pname, SEQUENCE FROM customfieldoption WHERE CUSTOMFIELD = ? ORDER BY SEQUENCE", 10056);
		Assert.assertEquals(3, items.size());
		Assert.assertEquals("Décalage planning", items.get(10048));
		Assert.assertEquals("Demande révisée", items.get(10049));
		Assert.assertEquals("Autre (à préciser)", items.get(10050));
	}

	@Test
	public void testGetInvertedMap() {
		final Map<String, Integer> items = AbstractEditor.getInvertedMap(datasource,
				"SELECT ID AS id, customvalue AS pname, SEQUENCE FROM customfieldoption WHERE CUSTOMFIELD = ? ORDER BY SEQUENCE", 10056);
		Assert.assertEquals(3, items.size());
		Assert.assertEquals(10048, items.get("Décalage planning").intValue());
		Assert.assertEquals(10049, items.get("Demande révisée").intValue());
		Assert.assertEquals(10050, items.get("Autre (à préciser)").intValue());
	}

	@Test
	public void testManagedTypesUrl() {
		MatcherUtil.expectValidationException(thrown, "cf$NAME", "Invalid value 'invalid'. Expected : A HTTP URL");
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		Assert.assertEquals(4.3,
				((Double) getEditor("com.atlassian.jira.plugin.system.customfieldtypes:url").getValue(customField, "invalid"))
						.doubleValue(),
				0.01);
	}

	@Test
	public void testManagedTypesRadio() {
		assertSelect("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons");
	}

	@Test
	public void testManagedTypesRadioNullContext() {
		final CustomField customField = new CustomField();
		customField.setName("Origine");
		customField.setId(10108);

		// Provides project where the context is not valid for this custom field
		new IdEditor().populateValues(datasource, customField, 1);
		getEditor("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons").getValue(customField, "APP1");
	}

	@Test(expected = ValidationJsonException.class)
	public void testManagedTypesRadioFailContext() {
		final CustomField customField = new CustomField();
		customField.setName("Origine");
		customField.setId(10108);

		// Provides project where the context is not valid for this custom field
		new IdEditor().populateValues(datasource, customField, 1);
		getEditor("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons").getValue(customField, "APP");
	}

	@Test
	public void testManagedTypesRadioExactContext() {
		final CustomField customField = new CustomField();
		customField.setName("Origine");
		customField.setId(10108);

		// Provides project where the context is not valid for this custom field
		new IdEditor().populateValues(datasource, customField, 10074);
		getEditor("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons").getValue(customField, "APP");
	}

	/**
	 * This a special custom field, not yet managed string to id operations.
	 */
	@Test(expected = ValidationJsonException.class)
	public void testNotManagedFromString() {
		final CustomField customField = new CustomField();
		customField.setName("cf-cascading");
		customField.setId(19003);
		new IdEditor().populateValues(datasource, customField, 10074);
		getEditor("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect").getValue(customField, "any");
	}

	@Test
	public void testNotManagedFromStringButFromId() {
		final CustomField customField = new CustomField();
		customField.setName("cf-cascading");
		customField.setId(19003);
		final CustomFieldValue value = new CustomFieldValue();
		value.setStringValue("10204");
		final AbstractEditor editor = getEditor("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect");
		editor.populateValues(datasource, customField, 10074);
		Assert.assertEquals("CValue", editor.getValue(customField, value));
	}

	@Test
	public void testManagedTypesSelect() {
		assertSelect("com.atlassian.jira.plugin.system.customfieldtypes:select");
	}

	private void assertSelect(final String key) {
		final CustomField customField = new CustomField();
		customField.setName("Motif suspension");
		customField.setId(10056);
		customField.setFieldType(key);
		new IdEditor().populateValues(datasource, customField, 10074);
		Assert.assertEquals(10048, ((Integer) new IdEditor().getValue(customField, "Décalage planning")).intValue());
	}

}
