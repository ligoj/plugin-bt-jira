package org.ligoj.app.plugin.bt.jira;

import java.util.List;
import java.util.Map;

import org.ligoj.app.plugin.bt.jira.dao.JiraIssueRow;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Jira simple export data
 */
@Getter
@Setter
public class JiraSimpleExport {

	@JsonIgnore
	private Map<Integer, String> statusText;
	@JsonIgnore
	private Map<Integer, String> priorityText;
	@JsonIgnore
	private Map<Integer, String> resolutionText;
	@JsonIgnore
	private Map<Integer, String> typeText;
	private List<JiraIssueRow> issues;

}
