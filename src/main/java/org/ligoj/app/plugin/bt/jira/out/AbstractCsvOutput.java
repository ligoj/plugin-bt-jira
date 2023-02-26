/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.Format;
import java.util.Date;
import java.util.Map;

import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.ligoj.app.plugin.bt.model.IssueDetails;
import org.ligoj.app.resource.NormalizeFormat;

/**
 * CSV output writer from Jira issues data. No SLA data.
 */
public abstract class AbstractCsvOutput implements StreamingOutput {

	protected final Map<Integer, String> resolutionText;
	protected final Map<Integer, String> statusText;
	protected final Map<Integer, String> typeText;
	protected final Map<Integer, String> priorityText;

	/**
	 *
	 * @param priorityText   The priority mapping: identifier to name.
	 * @param resolutionText The resolution mapping: identifier to name.
	 * @param statusText     The status mapping: identifier to name.
	 * @param typeText       The issue type mapping: identifier to name.
	 */
	protected AbstractCsvOutput(final Map<Integer, String> priorityText, final Map<Integer, String> resolutionText,
			final Map<Integer, String> statusText, final Map<Integer, String> typeText) {
		this.resolutionText = resolutionText;
		this.statusText = statusText;
		this.typeText = typeText;
		this.priorityText = priorityText;
	}

	@Override
	public void write(final OutputStream output) throws IOException {
		final Writer writer = new BufferedWriter(new OutputStreamWriter(output, "cp1252"));
		final FastDateFormat df = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss");
		final Format idf = new NormalizeFormat();

		// Write headers
		writeHeaders(writer);

		// Write data
		writeData(writer, df, idf);
		writer.flush();
	}

	/**
	 * Basic headers.
	 *
	 * @param writer Target output.
	 * @throws IOException When data could not be written.
	 */
	protected void writeNonSlaHeaders(final Writer writer) throws IOException {
		// Write static headers
		writer.write("id;issue;status;statusText;type;typeText;priority;priorityText;resolution;resolutionText");

		// Create/Update data
		writer.write(";created;createdTimestamp;reporter;assignee;dueDate;dueDateTimestamp");
	}

	/**
	 * Write CSV header. Ends with new line.
	 *
	 * @param writer Target output.
	 * @throws IOException When data could not be written.
	 */
	protected abstract void writeHeaders(Writer writer) throws IOException;

	/**
	 * Write issues data. Ends with new line.
	 *
	 * @param writer Target output.
	 * @param df     The {@link Format} used to write the date when not <code>null</code>.
	 * @param idf    The identifier format.
	 * @throws IOException When data could not be written.
	 */
	protected abstract void writeData(Writer writer, Format df, Format idf) throws IOException;

	/**
	 * Write issue data
	 *
	 * @param issue  The issue to write.
	 * @param writer Target output.
	 * @param df     The {@link Format} used to write the date when not <code>null</code>.
	 * @param idf    The identifier format.
	 * @throws IOException When data could not be written.
	 */
	protected void writeIssueData(final IssueDetails issue, final Writer writer, final Format df, final Format idf)
			throws IOException {
		// Write static data
		writer.write(issue.getId().toString());
		writer.write(';');
		writer.write(issue.getPkey());

		// Status
		writer.write(';');
		writer.write(String.valueOf(issue.getStatus()));
		writer.write(';');
		writer.write(idf.format(statusText.get(issue.getStatus())));

		// Type
		writer.write(';');
		writer.write(String.valueOf(issue.getType()));
		writer.write(';');
		writer.write(typeText.get(issue.getType()));

		// Priority
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull(issue.getPriority(), "")));
		writer.write(';');
		writer.write(ObjectUtils.defaultIfNull(priorityText.get(issue.getPriority()), ""));

		// Resolution
		writer.write(';');
		writer.write(String.valueOf(ObjectUtils.defaultIfNull(issue.getResolution(), "")));
		writer.write(';');
		writer.write(ObjectUtils.defaultIfNull(resolutionText.get(issue.getResolution()), ""));

		// Creation
		writeDate(writer, df, issue.getCreated());

		// Reporter
		writer.write(';');
		writer.write(issue.getReporter());

		// Assignee
		writer.write(';');
		writer.write(StringUtils.trimToEmpty(issue.getAssignee()));

		// Due date
		writeDate(writer, df, issue.getDueDate());
	}

	/**
	 * Write a date using the given format and in millisecond format, so 2 strings are added in the CSV output.
	 * <code>null</code> management is performed there. Empty {@link String} is written with <code>null</code> date.
	 *
	 * @param writer The target output.
	 * @param df     The {@link Format} used to write the date when not <code>null</code>.
	 * @param date   The {@link Date} to write.
	 * @throws IOException When data could not be written.
	 */
	protected void writeDate(final Writer writer, final Format df, final Date date) throws IOException {
		if (date == null) {
			// null date
			writer.write(";;");
		} else {
			writer.write(';');
			writer.write(df.format(date));
			writer.write(';');
			writer.write(String.valueOf(date.getTime()));
		}
	}

	/**
	 * Write a duration using the "HH:mm:ss" format and in millisecond format, so 2 strings are added in the CSV output.
	 * <code>null</code> management is performed there. Empty {@link String} is written with <code>null</code> duration.
	 *
	 * @param writer   The target output.
	 * @param duration The duration to write, in milliseconds.
	 * @throws IOException When data could not be written.
	 */
	protected void writeDuration(final Writer writer, final Long duration) throws IOException {
		if (duration == null) {
			// null duration
			writer.write(";;");
		} else {
			writer.write(';');
			if (duration < 0) {
				// Add the sign before the HMS value
				writer.write('-');
			}
			writer.write(DurationFormatUtils.formatDuration(Math.abs(duration), "HH:mm:ss"));
			writer.write(';');
			writer.write(String.valueOf(duration));
		}
	}
}
