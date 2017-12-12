package org.ligoj.app.plugin.bt.jira.dao;

import java.util.Date;

import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JIRA change to insert into changeitem table.
 */
@Getter
@AllArgsConstructor
public class JiraChangeRow extends AbstractPersistable<Integer> {

	/**
	 * Status from.
	 */
	private int fromStatus;

	/**
	 * Status from as String.
	 */
	private String fromStatusText;

	/**
	 * Status to
	 */
	private int toStatus;

	/**
	 * Status to as String.
	 */
	private String toStatusText;

	/**
	 * Author of change.
	 */
	private String author;

	/**
	 * Date of change.
	 */
	private Date date;

}
