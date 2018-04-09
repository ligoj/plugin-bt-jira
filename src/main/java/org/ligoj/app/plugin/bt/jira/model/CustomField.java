package org.ligoj.app.plugin.bt.jira.model;

import java.util.Map;

import org.ligoj.bootstrap.core.model.AbstractDescribedEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * A custom field.
 */
@Getter
@Setter
public class CustomField extends AbstractDescribedEntity<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	// CUSTOMFIELDTYPEKEY
	private String fieldType;

	/**
	 * Valid and ordered values.
	 */
	private Map<Integer, String> values;

	/**
	 * Valid and ordered values (by value). Inverse of "values"
	 */
	private Map<String, Integer> invertValues;
}
