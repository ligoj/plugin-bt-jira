/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.dao;

import org.ligoj.app.plugin.bt.jira.editor.AbstractEditor;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
/**
 * Editor base test.
 */
public abstract class AbstractEditorTest extends AbstractDataGeneratorTest {

	protected AbstractEditor getEditor(final String key) {
		return JiraDao.MANAGED_TYPE.get(key);
	}
}
