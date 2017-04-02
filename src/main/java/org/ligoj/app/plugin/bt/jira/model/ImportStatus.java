package org.ligoj.app.plugin.bt.jira.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Import status. Is deleted only with the project.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_BT_IMPORT_STATUS")
public class ImportStatus extends AbstractPersistable<Integer> {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	@NotNull
	@JsonIgnore
	@JoinColumn(name = "subscription")
	private Subscription subscription;

	private int step;

	private Date start;

	/**
	 * Current status. <code>true</code> means failed.
	 */
	private boolean failed;

	/**
	 * Null while not finished
	 */
	private Date end;

	/**
	 * User proceeding the import.
	 */
	private String author;

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

	@Transient
	@JsonIgnore
	private Set<String> newComponentsAsSet;

	@Transient
	@JsonIgnore
	private Set<String> newVersionsAsSet;

	@Transient
	@JsonIgnore
	private List<ImportEntry> rawEntries;
}
