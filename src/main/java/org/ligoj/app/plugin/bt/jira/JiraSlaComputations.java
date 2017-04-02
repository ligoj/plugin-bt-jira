package org.ligoj.app.plugin.bt.jira;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;
import javax.sql.DataSource;

import org.ligoj.app.plugin.bt.SlaComputations;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.bootstrap.core.DescribedBean;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Computations of all SLA of a JIRA project.
 */
@Getter
@Setter
public class JiraSlaComputations extends SlaComputations {

	/**
	 * JIRA internal identifier.
	 */
	private int jira;

	/**
	 * Project name.
	 */
	private DescribedBean<Integer> project;

	@Transient
	@JsonIgnore
	private BugTrackerConfiguration btConfiguration;

	@Transient
	@JsonIgnore
	private List<Date> holidays;

	@Transient
	@JsonIgnore
	private List<Sla> slas;

	@Transient
	@JsonIgnore
	private Map<Integer, String> statusText;

	@Transient
	@JsonIgnore
	private Map<Integer, String> priorityText;

	@Transient
	@JsonIgnore
	private Map<Integer, String> resolutionText;

	@Transient
	@JsonIgnore
	private Map<Integer, String> typeText;

	@Transient
	@JsonIgnore
	private DataSource dataSource;
}
