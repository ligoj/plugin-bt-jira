/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.bt.jira.dao.AbstractEditorUploadTest;
import org.ligoj.app.plugin.bt.jira.model.CustomField;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

/**
 * test class of {@link MultipleIdEditor}
 */
class MultipleIdEditorTest extends AbstractEditorUploadTest {

	@Test
	void testCustomColumn() {
		Assertions.assertEquals("STRINGVALUE", new MultipleIdEditor().getCustomColumn());
	}

	@Test
	void testGetValueInvalid() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("keyA", 1);
		values.put("keyB", 2);
		customField.setInvertValues(values);
		customField.setName("NAME");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			new MultipleIdEditor().getValue(customField, "keyA,invalid");
		}), "cf$NAME", "Invalid value 'invalid'. Expected : keyA,keyB");
	}

	@Test
	void testGetValue() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("keyA", 1);
		values.put("keyB", 2);
		values.put("keyC", 3);
		customField.setInvertValues(values);
		@SuppressWarnings("unchecked")
		final List<Integer> ids = new ArrayList<>(
				(Collection<Integer>) new MultipleIdEditor().getValue(customField, "keyA, keyC, ,"));
		Assertions.assertEquals(2, ids.size());
		Assertions.assertEquals(1, ids.get(0).intValue());
		Assertions.assertEquals(3, ids.get(1).intValue());
	}

	@Test
	void testManagedTypesMultiCheckboxes() {
		assertMulti("com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes");
	}

	@Test
	void testManagedTypesMultiSelect() {
		assertMulti("com.atlassian.jira.plugin.system.customfieldtypes:multiselect");
	}

	private void assertMulti(final String key) {
		final CustomField customField = new CustomField();
		customField.setName("Motif suspension");
		customField.setId(10056);
		new IdEditor().populateValues(datasource, customField, 10074);

		@SuppressWarnings("unchecked")
		final List<Integer> ids = new ArrayList<>(
				(Collection<Integer>) getEditor(key).getValue(customField, "Décalage planning , Demande révisée"));
		Assertions.assertEquals(2, ids.size());
		Assertions.assertEquals(10048, ids.get(0).intValue());
		Assertions.assertEquals(10049, ids.get(1).intValue());
	}
}
