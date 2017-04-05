package org.ligoj.app.plugin.bt.jira.dao.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.dao.JiraDao;
import org.ligoj.app.plugin.bt.jira.editor.IdEditor;
import org.ligoj.app.plugin.bt.jira.editor.MultipleIdEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomField;

/**
 * test class of {@link MultipleIdEditor}
 */
public class MultipleIdEditorTest extends AbstractJiraUploadTest {

	@Test
	public void testCustomColumn() {
		Assert.assertEquals("STRINGVALUE", new MultipleIdEditor().getCustomColumn());
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
		new MultipleIdEditor().getValue(customField, "keyA,invalid");
	}

	@Test
	public void testGetValue() {
		final CustomField customField = new CustomField();
		final Map<String, Integer> values = new LinkedHashMap<>();
		values.put("keyA", 1);
		values.put("keyB", 2);
		values.put("keyC", 3);
		customField.setInvertValues(values);
		@SuppressWarnings("unchecked")
		final List<Integer> ids = new ArrayList<>((Collection<Integer>) new MultipleIdEditor().getValue(customField, "keyA, keyC, ,"));
		Assert.assertEquals(2, ids.size());
		Assert.assertEquals(1, ids.get(0).intValue());
		Assert.assertEquals(3, ids.get(1).intValue());
	}

	@Test
	public void testManagedTypesMultiCheckboxes() {
		assertMulti("com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes");
	}

	@Test
	public void testManagedTypesMultiSelect() {
		assertMulti("com.atlassian.jira.plugin.system.customfieldtypes:multiselect");
	}

	private void assertMulti(final String key) {
		final CustomField customField = new CustomField();
		customField.setName("Motif suspension");
		customField.setId(10056);
		new IdEditor().populateValues(datasource, customField, 10074);

		@SuppressWarnings("unchecked")
		final List<Integer> ids = new ArrayList<>(
				(Collection<Integer>) JiraDao.MANAGED_TYPE.get(key).getValue(customField, "Décalage planning , Demande révisée"));
		Assert.assertEquals(2, ids.size());
		Assert.assertEquals(10048, ids.get(0).intValue());
		Assert.assertEquals(10049, ids.get(1).intValue());
	}
}
