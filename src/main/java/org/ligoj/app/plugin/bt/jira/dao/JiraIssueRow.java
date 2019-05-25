/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.dao;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.ligoj.app.plugin.bt.model.IssueDetails;

import lombok.Getter;
import lombok.Setter;

/**
 * Object to insert in "jiraissue"
 */
@Getter
@Setter
public class JiraIssueRow extends IssueDetails {

	private Date resolutionDate;
	private String summary;
	private String description;
	private Date updated;
	private String statusText;
	private int issueNum;
	private String author;

	private Collection<Integer> components;
	private Collection<Integer> versions;
	private Collection<Integer> fixedVersions;
	private Map<String, Object> customFields;
	private Collection<String> labels;

}
