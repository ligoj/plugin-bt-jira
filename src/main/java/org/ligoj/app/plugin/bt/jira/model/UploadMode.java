package org.ligoj.app.plugin.bt.jira.model;

/**
 * Upload mode.
 */
public enum UploadMode {

	/**
	 * Validate the input syntax only.
	 */
	SYNTAX,

	/**
	 * Validate the input and JIRA database.
	 */
	VALIDATION,

	/**
	 * Validate the input and return all the amounts of entries that will be created.
	 */
	PREVIEW,

	/**
	 * Validate and insert data.
	 */
	FULL

}
