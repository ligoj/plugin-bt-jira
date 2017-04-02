package org.ligoj.app.plugin.bt.jira.model;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * A custom field value.
 */
@Getter
@Setter
public class CustomFieldValue {

	/**
	 * Issue identifier.
	 */
	private int issue;

	/**
	 * Custom Field identifier.
	 */
	private int customField;

	/**
	 * Optional string representation of this value.
	 */
	private String stringValue;

	/**
	 * Optional numeric representation of this value.
	 */
	private Double numberValue;

	/**
	 * Optional long text representation of this value.
	 */
	private String textValue;

	/**
	 * Optional date representation of this value.
	 */
	private Date dateValue;

}
