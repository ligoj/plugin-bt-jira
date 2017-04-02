package org.ligoj.app.plugin.bt.jira.out;

import java.io.IOException;
import java.io.Writer;
import java.text.Format;

import org.ligoj.app.plugin.bt.jira.JiraSimpleExport;
import org.ligoj.app.plugin.bt.jira.dao.JiraIssueRow;
import org.ligoj.app.plugin.bt.model.IssueDetails;

/**
 * CSV output writer from Jira issues data. No SLA data.
 */
public class CsvSimpleOutput extends AbstractCsvOutput {

	protected final JiraSimpleExport export;

	/**
	 * Constructor for database offline data.
	 * 
	 * @param export
	 *            SLA computations with issues.
	 */
	public CsvSimpleOutput(final JiraSimpleExport export) {
		super(export.getPriorityText(), export.getResolutionText(), export.getStatusText(), export.getTypeText());
		this.export = export;
	}

	/**
	 * Write CSV header. Ends with new line.
	 * 
	 * @param writer
	 *            Target output.
	 */
	@Override
	protected void writeHeaders(final Writer writer) throws IOException {
		writeNonSlaHeaders(writer);

		// Summary
		writer.write(";summary");
		writer.write('\n');
	}

	@Override
	protected void writeData(final Writer writer, final Format df, final Format idf) throws IOException {
		for (final JiraIssueRow issue : export.getIssues()) {
			// Write static data
			writeIssueData(issue, writer, df, idf);
			writer.write('\n');
		}
	}

	@Override
	protected void writeIssueData(final IssueDetails issue, final Writer writer, final Format df, final Format idf) throws IOException {
		super.writeIssueData(issue, writer, df, idf);

		// Write the escaped summary
		writer.write(';');
		writer.append('\"');
		writer.append(((JiraIssueRow) issue).getSummary().replace("\"", "\"\""));
		writer.append('\"');
	}

}
