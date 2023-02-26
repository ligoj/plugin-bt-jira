/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.io.IOException;
import java.io.Writer;
import java.text.Format;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.plugin.bt.IssueSla;
import org.ligoj.app.plugin.bt.SlaConfiguration;
import org.ligoj.app.plugin.bt.SlaData;
import org.ligoj.app.plugin.bt.jira.JiraSlaComputations;
import org.springframework.data.jpa.domain.AbstractPersistable;

/**
 * CSV output writer from bug tracker issues data.
 */
public class CsvStreamingOutput extends AbstractCsvOutput {

	protected final JiraSlaComputations slaComputations;

	/**
	 * Constructor for database offline data.
	 *
	 * @param slaComputations
	 *            SLA computations with issues.
	 */
	public CsvStreamingOutput(final JiraSlaComputations slaComputations) {
		super(slaComputations.getPriorityText(), slaComputations.getResolutionText(), slaComputations.getStatusText(),
				slaComputations.getTypeText());
		this.slaComputations = slaComputations;

		// Sort the computation by identifier instead of creation date
		slaComputations.setIssues(slaComputations.getIssues().stream().sorted(Comparator.comparing(AbstractPersistable::getId)).toList());
	}

	@Override
	protected void writeHeaders(final Writer writer) throws IOException {
		writeNonSlaHeaders(writer);

		// Write SLA headers
		writeSlaHeaders(writer);

		// Write Status counter headers
		writeCountersHeaders(writer);

		writer.write('\n');
	}

	/**
	 * Write headers
	 *
	 * @param writer
	 *            Target output.
	 * @throws IOException When data could not be written.
	 */
	protected void writeCountersHeaders(final Writer writer) throws IOException {
		// Iterate over statuses of all issues to compute used statuses
		for (final String status : slaComputations.getStatusText().values()) {
			writer.write(";#");
			writer.write(status);
		}
	}

	/**
	 * Write headers
	 *
	 * @param writer
	 *            Target output.
	 * @throws IOException When data could not be written.
	 */
	protected void writeSlaHeaders(final Writer writer) throws IOException {
		for (final SlaConfiguration sla : slaComputations.getSlaConfigurations()) {
			writeSlaHeader(writer, sla, "(h:m:s)");
			writeSlaHeader(writer, sla, "(ms)");
			writeSlaHeader(writer, sla, "(Start)");
			writeSlaHeader(writer, sla, "(Start timestamp)");
			writeSlaHeader(writer, sla, "(Stop)");
			writeSlaHeader(writer, sla, "(Stop timestamp)");
			writeSlaHeader(writer, sla, "(Revised Due Date)");
			writeSlaHeader(writer, sla, "(Revised Due Date timestamp)");
			writeSlaHeader(writer, sla, "(Revised Due Date distance h:m:s)");
			writeSlaHeader(writer, sla, "(Revised Due Date distance ms)");
		}
	}

	/**
	 * Write a SLA header.
	 *
	 * @param writer
	 *            Target output.
	 * @param sla
	 *            The SLA configuration to write.
	 * @param suffix
	 *            The SLA suffix of each header.
	 * @throws IOException When data could not be written.
	 */
	protected void writeSlaHeader(final Writer writer, final SlaConfiguration sla, final String suffix)
			throws IOException {
		writer.write(";[SLA] ");
		writer.write(sla.getName());
		writer.write(suffix);
	}

	@Override
	protected void writeData(final Writer writer, final Format df, final Format idf) throws IOException {
		for (final IssueSla issue : slaComputations.getIssues()) {
			// Write standard data
			writeIssueData(issue, writer, df, idf);

			// Write specific data
			writeSlaData(issue, writer, df);
			writer.write('\n');
		}
	}

	/**
	 * Write data of one issue : SLA and counters.
	 *
	 * @param issue
	 *            The issue to write.
	 * @param writer
	 *            Target output.
	 * @param df
	 *            The {@link Format} used to write the date when not <code>null</code>.
	 * @throws IOException When data could not be written.
	 */
	protected void writeSlaData(final IssueSla issue, final Writer writer, final Format df) throws IOException {

		// Write SLA duration
		writeSlaDurationData(issue, writer, df);

		// Write SLA counters
		writeSlaCounters(issue, writer);
	}

	/**
	 * Write status counters
	 *
	 * @param issue
	 *            The issue to write.
	 * @param writer
	 *            Target output.
	 * @throws IOException When data could not be written.
	 */
	protected void writeSlaCounters(final IssueSla issue, final Writer writer) throws IOException {
		final Map<Integer, Integer> counter = issue.getStatusCounter();
		for (final Integer status : slaComputations.getStatusText().keySet()) {
			writer.write(';');

			// Write counters
			writer.write(ObjectUtils.defaultIfNull(counter.get(status), "0").toString());
		}
	}

	/**
	 * Write SLA durations
	 *
	 * @param issue
	 *            The issue to write.
	 * @param writer
	 *            Target output.
	 * @param df
	 *            The {@link Format} used to write the date when not <code>null</code>.
	 * @throws IOException When data could not be written.
	 */
	protected void writeSlaDurationData(final IssueSla issue, final Writer writer, final Format df) throws IOException {
		for (final SlaData data : issue.getData()) {

			// Write the duration
			if (data == null) {
				// No duration, SLA cannot be applied to this issue
				writer.write(";;;;;;;;;;");
			} else {
				// Write formated and raw values of duration
				writeDuration(writer, data.getDuration());
				writeDate(writer, df, data.getStart());
				writeDate(writer, df, data.getStop());
				writeDate(writer, df, data.getRevisedDueDate());
				writeDuration(writer, data.getRevisedDueDateDistance());
			}
		}
	}

}
