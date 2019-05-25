/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.dao;

import org.ligoj.app.plugin.bt.jira.AbstractJiraUploadTest;
import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;

/**
 * Editor base test class.
 */
public abstract class AbstractEditorUploadTest extends AbstractJiraUploadTest {

	protected AbstractEditor getEditor(final String key) {
		return JiraDao.MANAGED_TYPE.get(key);
	}
}
