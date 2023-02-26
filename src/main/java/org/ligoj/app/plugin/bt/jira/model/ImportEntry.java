/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import lombok.Getter;
import lombok.Setter;

/**
 * Row import data with basic validation.
 */
@Setter
@Getter
public class ImportEntry {

	/**
	 * Pattern for accepted second.
	 */
	public static final String SECONDS_PATTERN = ":\\d\\d";

	/**
	 * Pattern for accepted hour/minute.
	 */
	public static final String HM_PATTERN = "\\s+\\d\\d:\\d\\d";

	/**
	 * DD/MM/YYYY
	 */
	public static final String DATE_FR = "\\d\\d/\\d\\d/\\d\\d\\d\\d";

	/**
	 * YYYY-MM-DD
	 */
	public static final String DATE_ISO8601 = "\\d\\d\\d\\d-\\d\\d-\\d\\d";

	/**
	 * YYYY/MM/DD
	 */
	public static final String DATE_ISO8601B = "\\d\\d\\d\\d/\\d\\d/\\d\\d";

	/**
	 * D.M.YYYY
	 */
	public static final String DATE_EN = "\\d\\d?\\.\\d\\d?\\.\\d\\d\\d\\d";

	/**
	 * DD/MM/YYYY HH:mm
	 */
	public static final String DATE_TIME_FR = DATE_FR + HM_PATTERN;

	/**
	 * YYYY-MM-DD HH:mm
	 */
	public static final String DATE_TIME_ISO8601 = DATE_ISO8601 + HM_PATTERN;

	/**
	 * YYYY/MM/DD HH:mm
	 */
	public static final String DATE_TIME_ISO8601B = DATE_ISO8601B + HM_PATTERN;

	/**
	 * D.M.YYYY HH:mm
	 */
	public static final String DATE_TIME_EN = DATE_EN + HM_PATTERN;

	/**
	 * 1523,4568
	 */
	public static final String DATE_DECIMAL = "\\d+([.,]\\d+)";

	/**
	 * Global date pattern
	 */
	public static final String DATE_PATTERN = "((" + DATE_FR + "|" + DATE_ISO8601 + "|" + DATE_ISO8601B + "|" + DATE_EN + ")(" + HM_PATTERN + "("
			+ SECONDS_PATTERN + ")?)?|" + DATE_DECIMAL + ")";

	private String id;

	@NotBlank
	@Pattern(regexp = "(\\w+-)?[1-9]\\d*")
	private String issue;
	private int issueNum;

	@NotBlank
	@Length(min = 2)
	private String status;
	private int statusId;

	@NotBlank
	@Length(min = 2, max = 250)
	private String summary;

	@Length(min = 2)
	private String description;

	@Length(min = 2)
	private String resolution;
	private Integer resolutionId;

	@Pattern(regexp = DATE_PATTERN)
	private String resolutionDate;
	private Date resolutionDateValid;

	@Pattern(regexp = DATE_PATTERN)
	private String dueDate;
	private Date dueDateValid;

	@NotBlank
	@Length(min = 1)
	private String type;
	private int typeId;

	@NotBlank
	@Length(min = 1)
	private String priority;
	private int priorityId;

	@Pattern(regexp = "[^,\\s]+(\\s*,\\s*[^,\\s]+)*")
	private String labels;
	private Set<String> labelsText;

	@Pattern(regexp = "[^,\\s]+(\\s*,\\s*[^,\\s]+)*")
	private String components;
	private Set<String> componentsText;

	@Pattern(regexp = "[^,\\s]+(\\s*,\\s*[^,\\s]+)*")
	private String fixedVersion;
	private Set<String> fixedVersionText;

	@Pattern(regexp = "[^,\\s]+(\\s*,\\s*[^,\\s]+)*")
	private String version;
	private Set<String> versionText;

	@NotBlank
	@Pattern(regexp = DATE_PATTERN)
	private String date;
	private Date dateValid;

	@NotBlank
	@Pattern(regexp = "[^\\s]{3,20}")
	private String assignee;

	@NotBlank
	@Pattern(regexp = "[^\\s]{3,20}")
	private String reporter;

	private Map<String, Object> cf;

	/**
	 * Author of change.
	 */
	@NotBlank
	@Pattern(regexp = "[^\\s]{3,20}")
	private String author;
}
