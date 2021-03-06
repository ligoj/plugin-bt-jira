/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira;

import java.util.Map;

import org.ligoj.bootstrap.core.DescribedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * JIRA project description with unresolved issues counters for each priority.
 */
@Getter
@Setter
public class JiraProject extends DescribedBean<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Count for each statuses. K is the status name. V is the
	 */
	private Map<String, Integer> statuses;
	private Map<String, Integer> priorities;
}
