package org.ligoj.app.plugin.bt.jira.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.ligoj.app.model.AbstractLongTaskSubscription;

import lombok.Getter;
import lombok.Setter;

/**
 * Import status. Is deleted only with the project.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_BT_IMPORT_STATUS", uniqueConstraints=@UniqueConstraint(columnNames="locked"))
public class ImportStatus extends AbstractLongTaskSubscription {

	private int step;

	private UploadMode mode;
	private Integer jira;
	private String pkey;
	private Integer issues;
	private Integer changes;
	private Integer customFields;
	private Integer types;
	private Integer resolutions;
	private Integer priorities;
	private Integer versions;
	private Integer statuses;
	private Integer labels;
	private Integer components;
	private Date issueFrom;
	private Date issueTo;
	private Integer users;
	private Integer minIssue;
	private Integer maxIssue;
	private Integer newIssues;
	private Integer newComponents;
	private Integer newVersions;
	private Boolean scriptRunner;
	private String jiraVersion;
	private Integer statusChanges;
	private Boolean canSynchronizeJira;
	private Boolean synchronizedJira;

}
