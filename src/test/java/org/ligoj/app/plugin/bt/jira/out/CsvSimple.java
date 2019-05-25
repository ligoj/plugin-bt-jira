/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import lombok.Getter;
import lombok.Setter;

/**
 * Add only summary.
 */
@Getter
@Setter
public class CsvSimple extends CsvChange {

	private String summary;
}
