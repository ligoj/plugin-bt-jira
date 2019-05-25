/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * A data set change.
 */
@Getter
@Setter
public class CsvChange {

	private int id;
	private String issue;
	private String statusText;
	private String typeText;
	private String priorityText;
	private String resolutionText;
	private Date created;
	private long createdTimestamp;
	private String reporter;
	private String assignee;
	private Date dueDate;
	private long dueDateTimestamp;
	private int type;
	private int priority;
	private int status;
	private int resolution;
	private String slaLivraison;
	private long slaLivraisonMs;
	private Date slaLivraisonStart;
	private long slaLivraisonStartTimestamp;
	private Date slaLivraisonStop;
	private long slaLivraisonStopTimestamp;
	private Date slaRevisedDueDate;
	private long slaRevisedDueDateTimestamp;
	private String slaRevisedDueDateDistance;
	private long slaRevisedDueDateDistanceMs;
	private int nbClosed;
	private int nbOpen;
	private int nbAssigned;
	private int nbInProgress;
	private int nbReopened;
	private int nbResolved;
}
