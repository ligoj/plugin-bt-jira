package org.ligoj.app.plugin.bt.jira.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
		Assertions.assertEquals("STRINGVALUE", new IdEditor().getCustomColumn());
	}

	@Test
	public void testGetValueInvalid() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("keyA", 1);
		values.put("keyB", 2);
		customField.setInvertValues(values);
		customField.setName("NAME");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			new IdEditor().getValue(customField, "invalid");
		}), "cf$NAME", "Invalid value 'invalid'. Expected : keyA,keyB");
	}

	@Test
	public void testGetValue() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("key", 1);
		customField.setInvertValues(values);
		Assertions.assertEquals(1, ((Integer) new IdEditor().getValue(customField, "key")).intValue());
	}

	@Test
	public void testGetValuebyId() {
		final CustomField customField = new CustomField();
		final CustomFieldValue value = new CustomFieldValue();
		value.setStringValue("4");
		final Map<Integer, String> values = new LinkedHashMap<>();
		values.put(4, "val");
		customField.setValues(values);
		Assertions.assertEquals("val", new IdEditor().getValue(customField, value));
	}

	@Test
	public void testPopulateValues() {
		final CustomField customField = new CustomField();
		customField.setName("Motif suspension");
		customField.setId(10056);
		final IdEditor editor = new IdEditor();
		editor.populateValues(datasource, customField, 10074);
		Assertions.assertEquals(3, customField.getValues().size());
		Assertions.assertEquals("Décalage planning", customField.getValues().get(10048));
		Assertions.assertEquals("Demande révisée", customField.getValues().get(10049));
		Assertions.assertEquals("Autre (à préciser)", customField.getValues().get(10050));
		Assertions.assertEquals(3, customField.getValues().size());
		Assertions.assertEquals(10048, customField.getInvertValues().get("Décalage planning").intValue());
		Assertions.assertEquals(10049, customField.getInvertValues().get("Demande révisée").intValue());
		Assertions.assertEquals(10050, customField.getInvertValues().get("Autre (à préciser)").intValue());
	}

	@Test
	public void testGetMap() {
		final Map<Integer, String> items = AbstractEditor.getMap(datasource,
				"SELECT ID AS id, customvalue AS pname, SEQUENCE FROM customfieldoption WHERE CUSTOMFIELD = ? ORDER BY SEQUENCE", 10056);
		Assertions.assertEquals(3, items.size());
		Assertions.assertEquals("Décalage planning", items.get(10048));
		Assertions.assertEquals("Demande révisée", items.get(10049));
		Assertions.assertEquals("Autre (à préciser)", items.get(10050));
	}

	@Test
	public void testGetInvertedMap() {
		final Map<String, Integer> items = AbstractEditor.getInvertedMap(datasource,
				"SELECT ID AS id, customvalue AS pname, SEQUENCE FROM customfieldoption WHERE CUSTOMFIELD = ? ORDER BY SEQUENCE", 10056);
		Assertions.assertEquals(3, items.size());
		Assertions.assertEquals(10048, items.get("Décalage planning").intValue());
		Assertions.assertEquals(10049, items.get("Demande révisée").intValue());
		Assertions.assertEquals(10050, items.get("Autre (à préciser)").intValue());
	}

	@Test
	public void testManagedTypesUrl() {
		final CustomField customField = new CustomField();
		customField.setName("NAME");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			getEditor("com.atlassian.jira.plugin.system.customfieldtypes:url").getValue(customField, "invalid");
		}), "cf$NAME", "Invalid value 'invalid'. Expected : A HTTP URL");
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

	@Test
	public void testManagedTypesRadioFailContext() {
		final CustomField customField = new CustomField();
		customField.setName("Origine");
		customField.setId(10108);

		// Provides project where the context is not valid for this custom field
		new IdEditor().populateValues(datasource, customField, 1);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			getEditor("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons").getValue(customField, "APP");
		}), "cf$Origine", "Invalid value 'APP'. Expected : APP1,INC1");
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
	@Test
	public void testNotManagedFromString() {
		final CustomField customField = new CustomField();
		customField.setName("cf-cascading");
		customField.setId(19003);
		new IdEditor().populateValues(datasource, customField, 10074);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			getEditor("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect").getValue(customField, "any");
		}), "cf$cf-cascading", "Custom field 'cf-cascading' has a not yet managed type 'null'");
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
		Assertions.assertEquals("CValue", editor.getValue(customField, value));
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
		Assertions.assertEquals(10048, ((Integer) new IdEditor().getValue(customField, "Décalage planning")).intValue());
	}

}
