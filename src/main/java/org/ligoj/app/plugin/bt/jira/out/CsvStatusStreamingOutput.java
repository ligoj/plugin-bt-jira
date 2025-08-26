/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.io.IOException;
import java.io.Writer;
import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.plugin.bt.jira.dao.JiraChangeItem;

/**
 * CSV status history output writer from Jira issues data.
 */
public class CsvStatusStreamingOutput extends AbstractCsvOutput {

	protected final List<JiraChangeItem> changes;

	/**
	 * Constructor for database offline data.
	 * 
	 * @param changes
	 *            Status changes.
	 * @param statusText
	 *            Status mapping, identifier to text.
	 */
	public CsvStatusStreamingOutput(final List<JiraChangeItem> changes, final Map<Integer, String> statusText) {
		super(null, null, statusText, null);
		this.changes = changes;
	}

	@Override
	protected void writeHeaders(final Writer writer) throws IOException {
		writer.write("issueid;key;author;from;to;fromText;toText;date;dateTimestamp\n");
	}

	@Override
	protected void writeData(final Writer writer, final Format df, final Format idf) throws IOException {
		final Map<Integer, String> idToKey = new HashMap<>();
		for (final JiraChangeItem change : changes) {
			// Complete mapping
			if (change.getPkey() != null) {
				idToKey.put(change.getId(), change.getPkey());
			}

			// Write static data
			writeData(change, idToKey.get(change.getId()), writer, df, idf);
			writer.write('\n');
		}
	}

	/**
	 * Write issue data
	 */
	private void writeData(final JiraChangeItem change, final String key, final Writer writer, final Format df, final Format idf) throws IOException {
		// Write static data
		writer.write(change.getId().toString());
		writer.write(';');
		writer.write(key);
		writer.write(';');
		writer.write(ObjectUtils.getIfNull(change.getAuthor(), change.getReporter()));
		writer.write(';');
		writer.write(ObjectUtils.getIfNull(change.getFromStatus(), "").toString());
		writer.write(';');
		writer.write(String.valueOf(change.getToStatus()));
		writer.write(';');
		writer.write(idf.format(ObjectUtils.getIfNull(statusText.get(change.getFromStatus()), "")));
		writer.write(';');
		writer.write(idf.format(statusText.get(change.getToStatus())));
		writer.write(';');
		writer.write(df.format(change.getCreated()));
		writer.write(';');
		writer.write(String.valueOf(change.getCreated().getTime()));
	}
}
