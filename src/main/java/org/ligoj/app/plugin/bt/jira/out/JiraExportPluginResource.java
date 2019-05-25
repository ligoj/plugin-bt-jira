/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.bt.jira.out;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.ligoj.app.plugin.bt.BugTrackerResource;
import org.ligoj.app.plugin.bt.IssueSla;
import org.ligoj.app.plugin.bt.SlaComputations;
import org.ligoj.app.plugin.bt.SlaConfiguration;
import org.ligoj.app.plugin.bt.SlaData;
import org.ligoj.app.plugin.bt.SlaProcessor;
import org.ligoj.app.plugin.bt.dao.BugTrackerConfigurationRepository;
import org.ligoj.app.plugin.bt.dao.HolidayRepository;
import org.ligoj.app.plugin.bt.dao.SlaRepository;
import org.ligoj.app.plugin.bt.jira.JiraBaseResource;
import org.ligoj.app.plugin.bt.jira.JiraSimpleExport;
import org.ligoj.app.plugin.bt.jira.JiraSlaComputations;
import org.ligoj.app.plugin.bt.jira.dao.JiraChangeItem;
import org.ligoj.app.plugin.bt.jira.dao.JiraIssueRow;
import org.ligoj.app.plugin.bt.jira.editor.CustomFieldEditor;
import org.ligoj.app.plugin.bt.jira.model.CustomFieldValue;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.ChangeItem;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.app.resource.NormalizeFormat;
import org.ligoj.app.resource.plugin.AbstractToolPluginResource;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.template.BeanProcessor;
import org.ligoj.bootstrap.core.template.FormatProcessor;
import org.ligoj.bootstrap.core.template.MapProcessor;
import org.ligoj.bootstrap.core.template.Processor;
import org.ligoj.bootstrap.core.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * JIRA export issues resource.
 */
@Slf4j
@Path(JiraBaseResource.URL + "/{subscription:\\d+}")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class JiraExportPluginResource extends JiraBaseResource {

	private static final String START_WORKFLOW = "start";

	/**
	 * Simple processor returning the time from a date.
	 */
	private static final Processor<?> TIME_PROCESSOR = new Processor<Date>() {
		@Override
		public Object getValue(final Date context) {
			return context.getTime();
		}
	};

	@Autowired
	private HolidayRepository holidayRepository;

	@Autowired
	private SlaProcessor slaProcessor;

	@Autowired
	protected SlaRepository slaRepository;

	@Autowired
	protected BugTrackerResource resource;

	@Autowired
	protected BugTrackerConfigurationRepository bugTrackerConfigurationRepository;

	/**
	 * Return a simple export data without any computation.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @return the SLA configuration
	 */
	protected JiraSimpleExport getSimpleData(final int subscription) {

		// Find the project corresponding to the given JIRA project
		final long start = System.currentTimeMillis();
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final int jira = Integer.parseInt(parameters.get(JiraBaseResource.PARAMETER_PROJECT));
		final String pkey = parameters.get(JiraBaseResource.PARAMETER_PKEY);
		final DataSource dataSource = getDataSource(parameters);

		// Get issues of this project
		log.info("Get issues of {}({})", pkey, jira);
		final List<JiraIssueRow> issues = jiraDao.getIssues(dataSource, jira, pkey);
		log.info("Retrieved changes : {}", issues.size());

		final Map<Integer, String> statusText = jiraDao.getStatuses(dataSource);
		final Map<Integer, String> priorityText = jiraDao.getPriorities(dataSource);
		final Map<Integer, String> resolutionText = jiraDao.getResolutions(dataSource);
		final Map<Integer, String> typeText = jiraDao.getTypes(dataSource, jira);
		log.info("Fetch done of {} for {} issues, {} status, {} priorities, {} types, {} resolutions", subscription,
				issues.size(), statusText.size(), priorityText.size(), typeText.size(), resolutionText.size());

		final JiraSimpleExport jiraComputations = new JiraSimpleExport();
		jiraComputations.setIssues(issues);
		jiraComputations.setStatusText(statusText);
		jiraComputations.setPriorityText(priorityText);
		jiraComputations.setResolutionText(resolutionText);
		jiraComputations.setTypeText(typeText);
		log.info("End of simple export, took {}",
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
		return jiraComputations;
	}

	/**
	 * Return SLA computations.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param timing
	 *            When <code>true</code> time spent data is fetched.
	 * @return the SLA configuration
	 */
	protected JiraSlaComputations getSlaComputations(final int subscription, final boolean timing) {

		// Find the project corresponding to the given JIRA project
		final long start = System.currentTimeMillis();
		log.info("Get configuration of " + subscription);
		final BugTrackerConfiguration btConfiguration = bugTrackerConfigurationRepository
				.findBySubscriptionFetch(subscription);
		if (btConfiguration == null) {
			throw new EntityNotFoundException(String.valueOf(subscription));
		}

		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final int jira = Integer.parseInt(parameters.get(JiraBaseResource.PARAMETER_PROJECT));
		final String pkey = parameters.get(JiraBaseResource.PARAMETER_PKEY);
		final DataSource dataSource = getDataSource(parameters);

		// Get changes
		log.info("Get changes of {}({})", pkey, jira);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final List<ChangeItem> changes = (List) jiraDao.getChanges(dataSource, jira, pkey, false, timing);
		log.info("Retrieved changes : {}", changes.size());

		// Get SLA configuration
		final List<Sla> slas = slaRepository.findBySubscription(subscription);
		// Get attributes of issues
		log.info("Get relevant status, priorities, resolutions, components and types of project {} subscription {}",
				pkey, subscription);
		final Map<Integer, String> statusText = updateIndentifierFromText(dataSource, slas, jira, changes);
		final Map<Integer, String> priorityText = jiraDao.getPriorities(dataSource);
		final Map<Integer, String> resolutionText = jiraDao.getResolutions(dataSource);
		final Map<Integer, String> typeText = jiraDao.getTypes(dataSource, jira);
		log.info("Fetch done of {} for {} changes, {} status, {} priorities, {} types, {} resolutions", subscription,
				changes.size(), statusText.size(), priorityText.size(), typeText.size(), resolutionText.size());

		// Get relevant holidays and project configuration to compute SLA
		log.info("Compute SLA of {}", subscription);
		final List<BusinessHours> businessHours = btConfiguration.getBusinessHours();
		final List<Date> holidays;
		if (changes.isEmpty()) {
			holidays = new ArrayList<>();
		} else {
			holidays = holidayRepository.getHolidays(subscription, changes.get(0).getCreated(), new Date());
		}
		final SlaComputations computations = slaProcessor.process(businessHours, changes, holidays, slas);
		final JiraSlaComputations jiraComputations = new JiraSlaComputations();
		jiraComputations.setDataSource(dataSource);
		jiraComputations.setSlas(slas);
		jiraComputations.setIssues(computations.getIssues());
		jiraComputations.setSlaConfigurations(computations.getSlaConfigurations());
		jiraComputations.setJira(jira);
		jiraComputations.setProject(DescribedBean.clone(btConfiguration.getSubscription().getProject()));
		jiraComputations.setBtConfiguration(btConfiguration);
		jiraComputations.setHolidays(holidays);
		jiraComputations.setStatusText(statusText);
		jiraComputations.setPriorityText(priorityText);
		jiraComputations.setResolutionText(resolutionText);
		jiraComputations.setTypeText(typeText);
		log.info("End of SLA computation, took {}",
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
		return jiraComputations;
	}

	/**
	 * Return SLA computations as CSV input stream.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param file
	 *            The user file name to use in download response.
	 * @return the stream ready to be read during the serialization.
	 */
	@GET
	@Path("{file:.*-short.csv}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSlaComputationsCsv(@PathParam("subscription") final int subscription,
			@PathParam("file") final String file) {
		log.info("SLA report requested by '{}' for subscription '{}'",
				SecurityContextHolder.getContext().getAuthentication().getName(), subscription);
		return AbstractToolPluginResource
				.download(new CsvStreamingOutput(getSlaComputations(subscription, false)), file).build();
	}

	/**
	 * Return simple data as CSV input stream. There is no specific computation.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param file
	 *            The user file name to use in download response.
	 * @return the stream ready to be read during the serialization.
	 */
	@GET
	@Path("{file:.*-simple.csv}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSimpleCsv(@PathParam("subscription") final int subscription,
			@PathParam("file") final String file) {
		log.info("Standard report requested by '{}' for subscription '{}'",
				SecurityContextHolder.getContext().getAuthentication().getName(), subscription);
		return AbstractToolPluginResource.download(new CsvSimpleOutput(getSimpleData(subscription)), file).build();
	}

	/**
	 * Return SLA computations and custom field data as CSV input stream.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param file
	 *            The user file name to use in download response.
	 * @return the stream ready to be read during the serialization.
	 */
	@GET
	@Path("{file:.*-full.csv}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSlaComputationsCsvWithCustomFields(@PathParam("subscription") final int subscription,
			@PathParam("file") final String file) {
		log.info("SLA+ report requested by '{}' for subscription '{}'",
				SecurityContextHolder.getContext().getAuthentication().getName(), subscription);
		final long start = System.currentTimeMillis();
		final JiraSlaComputations slaComputations = getSlaComputations(subscription, true);
		final DataSource dataSource = slaComputations.getDataSource();

		// Components
		final Map<Integer, Collection<Integer>> componentAssociations = jiraDao.getComponentsAssociation(dataSource,
				slaComputations.getJira());
		log.info("Retrieved components associations : {}", componentAssociations.size());
		final Map<Integer, String> components = jiraDao.getComponents(dataSource, slaComputations.getJira());
		log.info("Retrieved components configurations : {}", components.size());

		// Custom fields
		final List<CustomFieldValue> customFieldValues = jiraDao.getCustomFieldValues(dataSource,
				slaComputations.getJira());
		log.info("Retrieved custom fields : {}", customFieldValues.size());
		final Map<Integer, CustomFieldEditor> customFields = getCustomFields(dataSource, customFieldValues,
				slaComputations.getJira());
		log.info("Retrieved custom field configurations : {}", customFields.size());

		// Parent relationships
		final Map<Integer, Integer> subTasks = jiraDao.getSubTasks(dataSource, slaComputations.getJira());
		log.info("Retrieved parent relashionships : {}", subTasks.size());

		log.info("End of full report data gathering, took {}",
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
		return AbstractToolPluginResource.download(new CsvWithCustomFieldsStreamingOutput(slaComputations,
				customFieldValues, customFields, componentAssociations, components, subTasks), file).build();
	}

	/**
	 * Return status history without SL computation.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param file
	 *            The user file name to use in download response.
	 * @return the stream ready to be read during the serialization.
	 */
	@GET
	@Path("{file:.*-status.csv}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getStatusHistory(@PathParam("subscription") final int subscription,
			@PathParam("file") final String file) {
		log.info("Status history report requested by '{}' for subscription '{}'",
				SecurityContextHolder.getContext().getAuthentication().getName(), subscription);
		final long start = System.currentTimeMillis();
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);
		final int jira = Integer.parseInt(parameters.get(JiraBaseResource.PARAMETER_PROJECT));
		final String pkey = parameters.get(JiraBaseResource.PARAMETER_PKEY);
		final DataSource dataSource = getDataSource(parameters);

		// Get changes, relevant holidays and project configuration
		log.info("Get changes of {}({})", pkey, jira);
		final List<JiraChangeItem> changes = jiraDao.getChanges(dataSource, jira, pkey, true, false);
		log.info("Retrieved changes : " + changes.size());

		// Compute the identifiers from the texts
		log.info("Get relevant text of project's statuses");
		final Map<Integer, String> statusText = jiraDao.getStatuses(dataSource, getInvolvedStatuses(changes),
				new ArrayList<>());
		log.info("End of status report data gathering, took {}",
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
		return AbstractToolPluginResource.download(new CsvStatusStreamingOutput(changes, statusText), file).build();
	}

	/**
	 * Return ordered custom field identifiers.
	 */
	private Map<Integer, CustomFieldEditor> getCustomFields(final DataSource dataSource,
			final List<CustomFieldValue> customFieldValues, final int project) {
		final java.util.Set<Integer> ids = new LinkedHashSet<>();
		for (final CustomFieldValue cv : customFieldValues) {
			ids.add(cv.getCustomField());
		}
		return jiraDao.getCustomFieldsById(dataSource, ids, project);
	}

	/**
	 * Return SLA computations as XLS input stream.
	 *
	 * @param subscription
	 *            The subscription identifier.
	 * @param file
	 *            The user file name to use in download response.
	 * @return the stream ready to be read during the serialization.
	 */
	@GET
	@Path("{file:.*.xml}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getSlaComputationsXls(@PathParam("subscription") final int subscription,
			@PathParam("file") final String file) {
		final JiraSlaComputations slaComputations = getSlaComputations(subscription, false);
		final Map<String, Processor<?>> tags = mapTags(slaComputations);

		// Get the template data
		return AbstractToolPluginResource.download(output -> {
			try (InputStream template = new ClassPathResource("csv/template/template-sla.xml").getInputStream();
					PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
				new Template<JiraSlaComputations>(IOUtils.toString(template, StandardCharsets.UTF_8)).write(writer,
						tags, slaComputations);
				writer.flush();
			}
		}, file).build();

	}

	/**
	 * Moment of day processor.
	 */
	private static class MomentProcessor extends Processor<Long> {

		protected MomentProcessor(final Object data) {
			super(data);
		}

		@Override
		public Object getValue(final Long context) {
			return DurationFormatUtils.formatDuration(context, "HH:mm:ss");
		}

	}

	/**
	 * Map tags to {@link Processor}
	 */
	private Map<String, Processor<?>> mapTags(final JiraSlaComputations slaComputations) {
		final Map<String, Processor<?>> tags = new LinkedHashMap<>();
		final Map<Integer, String> statusText = slaComputations.getStatusText();
		final FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.000");
		final Format idf = new NormalizeFormat();
		final Processor<Object> simpleProcessor = new Processor<>();
		final FormatProcessor<Object> formatProcessor = new FormatProcessor<>(df, simpleProcessor);

		// Issues ...
		tags.put("nbIssues", new Processor<>(slaComputations.getIssues().size() + 2));
		tags.put("nbIssuesColumns", new Processor<>(13 + slaComputations.getSlaConfigurations().size() * 10));
		tags.put("slaheaders", new Processor<>(slaComputations.getSlaConfigurations()));
		tags.put("slaheader", new BeanProcessor<>(SlaConfiguration.class, "name"));
		tags.put("issues", new Processor<>(slaComputations.getIssues()));
		tags.put("id", new BeanProcessor<>(IssueSla.class, "id"));
		tags.put("pkey", new BeanProcessor<>(IssueSla.class, "pkey"));
		tags.put("status", new MapProcessor<>(statusText, IssueSla.class, "status"));
		tags.put("type", new MapProcessor<>(slaComputations.getTypeText(), IssueSla.class, "type"));
		tags.put("priority", new MapProcessor<>(slaComputations.getPriorityText(), IssueSla.class, "priority"));
		tags.put("resolution", new MapProcessor<>(slaComputations.getResolutionText(), IssueSla.class, "resolution"));
		tags.put("createdDate", new FormatProcessor<>(df, new BeanProcessor<>(IssueSla.class, "created")));
		tags.put("createdTimestamp", new Processor<IssueSla>() {
			@Override
			public Object getValue(final IssueSla context) {
				return context.getCreated().getTime();
			}
		});
		tags.put("dueDate", new BeanProcessor<>(IssueSla.class, "dueDate"));
		tags.put("dueDateV", formatProcessor);
		tags.put("dueDateTimestampV", TIME_PROCESSOR);
		tags.put("reporter", new BeanProcessor<>(IssueSla.class, "reporter"));
		tags.put("assignee", new BeanProcessor<>(IssueSla.class, "assignee"));

		// ... followed by SLA durations
		tags.put("slaDurationStyleMS", toStyleProcessor(slaComputations, "s67", "s69"));
		tags.put("slaDurationStyleDate", toStyleProcessor(slaComputations, "s66", "s71"));
		tags.put("slaDurations", new BeanProcessor<>(IssueSla.class, "data"));
		tags.put("slaData", simpleProcessor);
		tags.put("slaDuration", new BeanProcessor<>(SlaData.class, "duration"));
		tags.put("slaDurationV", simpleProcessor);
		tags.put("slaRevisedDueDate", new BeanProcessor<>(SlaData.class, "revisedDueDate"));
		tags.put("slaRevisedDueDateV", formatProcessor);
		tags.put("slaRevisedDueDateTimestampV", TIME_PROCESSOR);
		tags.put("slaStart", new BeanProcessor<>(SlaData.class, START_WORKFLOW));
		tags.put("slaStartV", formatProcessor);
		tags.put("slaStartTimestampV", TIME_PROCESSOR);
		tags.put("slaStop", new BeanProcessor<>(SlaData.class, "stop"));
		tags.put("slaStopV", formatProcessor);
		tags.put("slaStopTimestampV", TIME_PROCESSOR);
		tags.put("slaDistanceStyleMS", toStyleProcessorDistance("s67", "s69"));
		tags.put("slaDistanceStyleDate", toStyleProcessorDistance("s66", "s71"));
		tags.put("slaDistance", new BeanProcessor<>(SlaData.class, "revisedDueDateDistance"));
		tags.put("slaDistanceV", simpleProcessor);

		// Business days definitions
		tags.put("nbNonBusinessDays", new Processor<>(slaComputations.getHolidays().size() + 2));
		tags.put("nonBusinessDays", new Processor<>(slaComputations.getHolidays()));
		tags.put("nonBusinessDate", formatProcessor);
		tags.put("nonBusinessTimestamp", TIME_PROCESSOR);

		// Business hours definitions
		final List<BusinessHours> businessHours = slaComputations.getBtConfiguration().getBusinessHours();
		tags.put("nbBusinessHours", new Processor<>(businessHours.size() + 2));
		tags.put("businessHours", new Processor<>(businessHours));
		tags.put(START_WORKFLOW, new MomentProcessor(new BeanProcessor<>(BusinessHours.class, START_WORKFLOW)));
		tags.put("end", new MomentProcessor(new BeanProcessor<>(BusinessHours.class, "end")));

		// Statuses definitions
		tags.put("nbStatus", new Processor<>(2 + statusText.size()));
		tags.put("allStatus", new Processor<>(statusText.entrySet()));
		tags.put("statusId", new Processor<Entry<Integer, String>>() {
			@Override
			public Object getValue(final Entry<Integer, String> context) {
				return context.getKey();
			}
		});
		tags.put("statusText", new Processor<Entry<Integer, String>>() {
			@Override
			public Object getValue(final Entry<Integer, String> context) {
				return context.getValue();
			}
		});
		tags.put("statusNormalizedText", new Processor<Entry<Integer, String>>() {
			@Override
			public Object getValue(final Entry<Integer, String> context) {
				return idf.format(context.getValue());
			}
		});

		// SLA tab contains SLA definition, average and rates

		// SLA Definitions
		tags.put("slas", new Processor<>(slaComputations.getSlas()));
		tags.put("nbSlas", new Processor<>(slaComputations.getSlas().size() + 2));
		tags.put("name", new BeanProcessor<>(Sla.class, "name"));
		tags.put("startStatuses", new NormalizedSortedProcessor<>(Sla.class, START_WORKFLOW));
		tags.put("stopStatuses", new NormalizedSortedProcessor<>(Sla.class, "stop"));
		tags.put("pauseStatuses", new NormalizedSortedProcessor<>(Sla.class, "pause"));
		tags.put("types", new NormalizedSortedProcessor<>(Sla.class, "types"));
		tags.put("priorities", new NormalizedSortedProcessor<>(Sla.class, "priorities"));
		tags.put("resolutions", new NormalizedSortedProcessor<>(Sla.class, "resolutions"));
		tags.put("threshold", new BeanProcessor<>(Sla.class, "threshold"));

		// Computation average and rate formula of SLA
		tags.put("nbIssuesMinus1", new Processor<>(slaComputations.getIssues().size() - 1));
		tags.put("slaIndex", new Processor<Sla>() {
			@Override
			public Object getValue(final Sla context) {
				return 13 + slaComputations.getSlas().indexOf(context) * 10;
			}
		});
		return tags;
	}

	/**
	 * Return the style corresponding to the threshold value.
	 *
	 * @param slaComputations
	 *            The SLA computations.
	 * @param styleNormal
	 *            The normal style.
	 * @param styleNormal
	 *            The style when the SLA is over.
	 * @return {@link SlaData} {@link Processor}.
	 */
	protected Processor<SlaData> toStyleProcessor(final JiraSlaComputations slaComputations, final String styleNormal,
			final String styleOver) {
		return new Processor<>() {
			@Override
			public Object getValue(final Deque<Object> contextData) {
				// Get the context
				final Object[] context = contextData.toArray();
				final SlaData data = (SlaData) contextData.getLast();
				final int index = (Integer) context[context.length - 2];
				final long threshold = slaComputations.getSlaConfigurations().get(index).getThreshold();

				// Check the threshold
				if (data != null && threshold != 0 && data.getDuration() > threshold) {
					// Out of time
					return styleOver;
				}
				return styleNormal;
			}
		};
	}

	/**
	 * Return the style corresponding to the distance sign.
	 *
	 * @param styleNormal
	 *            The normal style.
	 * @param styleNormal
	 *            The style when the SLA is over.
	 * @return {@link SlaData} {@link Processor}.
	 */
	protected Processor<SlaData> toStyleProcessorDistance(final String styleNormal, final String styleOver) {
		return new Processor<>() {
			@Override
			public Object getValue(final Deque<Object> contextData) {
				// Get the context
				final SlaData data = (SlaData) contextData.getLast();
				// Check the threshold
				if (data != null && data.getRevisedDueDateDistance() != null && data.getRevisedDueDateDistance() < 0) {
					// Out of time
					return styleOver;
				}
				return styleNormal;
			}
		};
	}

	private class NormalizedSortedProcessor<T> extends BeanProcessor<T> {

		/**
		 * Property bean access constructor.
		 *
		 * @param beanType
		 *            the source bean type.
		 * @param property
		 *            the property name.
		 */
		NormalizedSortedProcessor(final Class<T> beanType, final String property) {
			super(beanType, property, null);
		}

		@Override
		public Object getValue(final T context) {
			return StringUtils
					.join(identifierHelper.normalize(identifierHelper.asList((String) super.getValue(context))), ',');
		}
	}
}
