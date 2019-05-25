/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.editor;

import org.ligoj.app.plugin.bt.jira.model.CustomField;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Custom field data with associated editor.
 */
public class CustomFieldEditor extends CustomField {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Editor for read/write operations.
	 */
	@JsonIgnore
	@Getter
	@Setter
	private AbstractEditor editor;

}
