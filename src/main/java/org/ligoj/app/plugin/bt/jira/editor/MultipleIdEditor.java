package org.ligoj.app.plugin.bt.jira.editor;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.plugin.bt.jira.model.CustomField;

/**
 * Return the identifiers corresponding to the given text to split.
 */
public class MultipleIdEditor extends IdEditor {

	@Override
	public Object getValue(final CustomField customField, final String value) {
		return Arrays.stream(StringUtils.split(value, ',')).map(StringUtils::trimToNull).filter(Objects::nonNull)
				.map(v -> (Integer) super.getValue(customField, v)).collect(Collectors.toSet());
	}
}
