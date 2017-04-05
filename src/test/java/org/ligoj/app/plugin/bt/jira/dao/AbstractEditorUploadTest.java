package org.ligoj.app.plugin.bt.jira.dao;

import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;

public abstract class AbstractEditorUploadTest extends AbstractJiraUploadTest {

	public AbstractEditor getEditor(final String key) {
		return JiraDao.MANAGED_TYPE.get(key);
	}
}
