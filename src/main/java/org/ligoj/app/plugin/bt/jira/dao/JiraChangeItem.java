package org.ligoj.app.plugin.bt.jira.dao;

import org.ligoj.app.plugin.bt.model.ChangeItem;

import lombok.Getter;
import lombok.Setter;

/**
 * A issue change for Jira
 */
@Getter
@Setter
public class JiraChangeItem extends ChangeItem {

	/**
	 * Change author
	 */
	private String author;

}
