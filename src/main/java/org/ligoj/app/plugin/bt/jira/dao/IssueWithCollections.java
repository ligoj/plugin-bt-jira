package org.ligoj.app.plugin.bt.jira.dao;

import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * A complete issue
 */
@Getter
@Setter
public class IssueWithCollections extends AbstractPersistable<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Issue key.
	 */
	private int issue;

}
