package org.ligoj.app.plugin.bt.jira;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.bt.BtConfigurationVo;
import org.ligoj.app.plugin.bt.BugTrackerResource;
import org.ligoj.app.plugin.bt.BusinessHoursEditionVo;
import org.ligoj.app.plugin.bt.IdentifierHelper;
import org.ligoj.app.plugin.bt.SlaConfiguration;
import org.ligoj.app.plugin.bt.SlaEditionVo;
import org.ligoj.app.plugin.bt.dao.BugTrackerConfigurationRepository;
import org.ligoj.app.plugin.bt.dao.BusinessHoursRepository;
import org.ligoj.app.plugin.bt.dao.CalendarRepository;
import org.ligoj.app.plugin.bt.dao.HolidayRepository;
import org.ligoj.app.plugin.bt.dao.SlaRepository;
import org.ligoj.app.plugin.bt.model.BugTrackerConfiguration;
import org.ligoj.app.plugin.bt.model.BusinessHours;
import org.ligoj.app.plugin.bt.model.Calendar;
import org.ligoj.app.plugin.bt.model.Sla;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link BugTrackerResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class BugTrackerResourceTest extends AbstractJiraUploadTest {

	@Autowired
	private BugTrackerResource resource;

	@Autowired
	private BugTrackerConfigurationRepository repository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private HolidayRepository holidayRepository;

	@Autowired
	private SlaRepository slaRepository;

	@Autowired
	private CalendarRepository calendarRepository;

	@Autowired
	private BusinessHoursRepository businessHoursRepository;

	@Autowired
	private IdentifierHelper identifierHelper;

	@Before
	public void prepareSubscription() throws IOException {
		persistSystemEntities();
		persistEntities("csv", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
		this.subscription = getSubscription("MDA");
	}

	@Test
	public void getConfiguration() throws Exception {

		slaRepository.findBySubscription(subscription).get(0).setTypes("Bug,New Feature");
		slaRepository.findBySubscription(subscription).get(0).setPriorities("Blocker,Critical");
		slaRepository.findBySubscription(subscription).get(0).setResolutions("Fixed,Won't Fix");

		final BtConfigurationVo configurationVo = resource.getConfiguration(subscription);

		// Check SLAs
		final List<SlaConfiguration> slas = configurationVo.getSlas();
		Assert.assertEquals(1, slas.size());
		final SlaConfiguration sla = slas.get(0);
		Assert.assertEquals("Délais de fermeture", sla.getDescription());
		Assert.assertEquals("Livraison", sla.getName());
		Assert.assertTrue(sla.getId() > 0);
		Assert.assertEquals(1, sla.getPause().size());
		Assert.assertEquals("RESOLVED", sla.getPause().get(0));
		Assert.assertEquals("OPEN", sla.getStart().get(0));
		Assert.assertEquals("CLOSED", sla.getStop().get(0));
		Assert.assertEquals(36000000L, sla.getThreshold());

		// Check SLA types
		Assert.assertEquals(2, sla.getTypes().size());
		Assert.assertEquals("Bug", sla.getTypes().get(0));
		Assert.assertEquals("New Feature", sla.getTypes().get(1));

		// Check SLA priorities
		Assert.assertEquals(2, sla.getPriorities().size());
		Assert.assertEquals("Blocker", sla.getPriorities().get(0));
		Assert.assertEquals("Critical", sla.getPriorities().get(1));

		// Check SLA resolutions
		Assert.assertEquals(2, sla.getResolutions().size());
		Assert.assertEquals("Fixed", sla.getResolutions().get(0));
		Assert.assertEquals("Won't Fix", sla.getResolutions().get(1));

		// Check business ranges
		final INamableBean<Integer> calendar = configurationVo.getCalendar();
		Assert.assertEquals("France", calendar.getName());
		Assert.assertTrue(calendar.getId() > 0);
		final List<BusinessHours> businessHours = configurationVo.getBusinessHours();
		Assert.assertEquals(2, businessHours.size());
		Assert.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, businessHours.get(0).getStart());
		Assert.assertEquals(12 * DateUtils.MILLIS_PER_HOUR, businessHours.get(0).getEnd());
		Assert.assertEquals(13 * DateUtils.MILLIS_PER_HOUR, businessHours.get(1).getStart());
		Assert.assertEquals(18 * DateUtils.MILLIS_PER_HOUR, businessHours.get(1).getEnd());
		Assert.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, businessHours.get(0).getStart());

		// Check available statuses
		final List<String> statuses = configurationVo.getStatuses();
		Assert.assertEquals(5, statuses.size());
		Assert.assertEquals("CLOSED", statuses.get(0));
		Assert.assertEquals("IN PROGRESS", statuses.get(1));
		Assert.assertEquals("OPEN", statuses.get(2));
		Assert.assertEquals("REOPENED", statuses.get(3));
		Assert.assertEquals("RESOLVED", statuses.get(4));
		Assert.assertEquals(9 * DateUtils.MILLIS_PER_HOUR, businessHours.get(0).getStart());

		// Check available types
		final List<String> types = configurationVo.getTypes();
		Assert.assertEquals(5, types.size());
		Assert.assertEquals("Bug", types.get(0));
		Assert.assertEquals("New Feature", types.get(1));
		Assert.assertEquals("Question", types.get(2));
		Assert.assertEquals("Sub-task", types.get(3));
		Assert.assertEquals("Task", types.get(4));

		// Check available priorities
		final List<String> priorities = configurationVo.getPriorities();
		Assert.assertEquals(5, priorities.size());
		Assert.assertEquals("Blocker", priorities.get(0));
		Assert.assertEquals("Critical", priorities.get(1));
		Assert.assertEquals("Major", priorities.get(2));
		Assert.assertEquals("Minor", priorities.get(3));
		Assert.assertEquals("Trivial", priorities.get(4));

		// Check available resolutions
		final List<String> resolutions = configurationVo.getResolutions();
		Assert.assertEquals(6, resolutions.size());
		Assert.assertEquals("Cannot Reproduce", resolutions.get(0));
		Assert.assertEquals("Done", resolutions.get(1));
		Assert.assertEquals("Duplicate", resolutions.get(2));
		Assert.assertEquals("Fixed", resolutions.get(3));
		Assert.assertEquals("Incomplete", resolutions.get(4));
		Assert.assertEquals("Won't Fix", resolutions.get(5));
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteUnknown() {
		resource.delete(-1, false);
	}

	@Test
	public void delete() {
		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		em.persist(project);

		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt"));
		em.persist(subscription);

		final Calendar calendar = new Calendar();
		calendar.setName("Any");
		em.persist(calendar);

		final BugTrackerConfiguration configuration = new BugTrackerConfiguration();
		configuration.setSubscription(subscription);
		configuration.setCalendar(calendar);
		em.persist(configuration);

		final Sla sla = new Sla();
		sla.setName("Any");
		sla.setConfiguration(configuration);
		sla.setStart("Open");
		sla.setStop("Resolved");
		em.persist(sla);

		final BusinessHours businessHours = new BusinessHours();
		businessHours.setConfiguration(configuration);
		em.persist(businessHours);

		Assert.assertEquals(1, subscriptionRepository.findAllByProject(project.getId()).size());
		em.flush();
		em.clear();

		resource.delete(subscription.getId(), false);
		subscriptionRepository.delete(subscription);
		em.flush();
		em.clear();
		Assert.assertEquals(0, subscriptionRepository.findAllByProject(project.getId()).size());
	}

	@Test
	public void createCreateDefault() {
		slaRepository.deleteAll();
		repository.deleteAll();
		businessHoursRepository.deleteAll();
		holidayRepository.deleteAll();
		calendarRepository.deleteAll();
		em.flush();
		em.clear();

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		em.persist(project);
		em.flush();

		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt"));
		em.persist(subscription);
		em.flush();
		em.clear();

		resource.create(subscription.getId());
		em.flush();
		em.clear();

		// Check default SLA
		final BugTrackerConfiguration configuration = repository.findBySubscriptionFetch(subscription.getId());
		checkSla(configuration);

		// Check default calendar
		final Calendar calendar = calendarRepository.getDefault();
		Assert.assertEquals(configuration.getCalendar(), calendar);
		Assert.assertNotNull(calendar);
		Assert.assertEquals(4, calendar.getHolidays().size());
		Assert.assertEquals("Default", calendar.getName());
		Assert.assertNotNull(calendar.getHolidays().get(0).getName());
		Assert.assertNotNull(calendar.getHolidays().get(0).getDate());

		// Check default business hours
		Assert.assertEquals(1, configuration.getBusinessHours().size());
		Assert.assertEquals(8 * DateUtils.MILLIS_PER_HOUR, configuration.getBusinessHours().get(0).getStart());
		Assert.assertEquals(18 * DateUtils.MILLIS_PER_HOUR, configuration.getBusinessHours().get(0).getEnd());

		em.flush();
		em.clear();
		Assert.assertEquals(1, subscriptionRepository.findAllByProject(project.getId()).size());
	}

	private void checkSla(final BugTrackerConfiguration configuration) {
		Assert.assertNotNull(configuration);
		final List<Sla> slas = slaRepository.findBySubscription(configuration.getSubscription().getId());
		Assert.assertEquals(1, slas.size());
		final Sla sla = slas.get(0);
		Assert.assertEquals("Closing", sla.getName());
		Assert.assertEquals("Closing : Open->Closed", sla.getDescription());
		Assert.assertEquals(0, sla.getThreshold());
		Assert.assertEquals("OPEN", sla.getStart());
		Assert.assertEquals("CLOSED", sla.getStop());
		Assert.assertNull(sla.getPause());
	}

	@Test
	public void create() {
		createOrLink(subscription -> resource.create(subscription.getId()));
	}

	@Test
	public void link() {
		createOrLink(subscription -> resource.link(subscription.getId()));
	}

	public void createOrLink(final Consumer<Subscription> function) {

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		em.persist(project);
		em.flush();

		final Subscription subscription = new Subscription();
		subscription.setProject(project);
		subscription.setNode(nodeRepository.findOneExpected("service:bt"));
		em.persist(subscription);
		em.flush();
		em.clear();

		function.accept(subscription);
		em.flush();
		em.clear();

		// Check SLA
		final BugTrackerConfiguration configuration = repository.findBySubscriptionFetch(subscription.getId());
		checkSla(configuration);

		// Check calendar
		final Calendar calendar = configuration.getCalendar();
		Assert.assertNotNull(calendar);
		Assert.assertEquals(44, calendar.getHolidays().size());
		Assert.assertEquals("France", calendar.getName());
		Assert.assertEquals("Jour de l'an", calendar.getHolidays().get(0).getName());
		Assert.assertNotNull(calendar.getHolidays().get(0).getDate());

		// Check default business hours
		Assert.assertEquals(1, configuration.getBusinessHours().size());
		Assert.assertEquals(8 * DateUtils.MILLIS_PER_HOUR, configuration.getBusinessHours().get(0).getStart());
		Assert.assertEquals(18 * DateUtils.MILLIS_PER_HOUR, configuration.getBusinessHours().get(0).getEnd());

		em.flush();
		em.clear();
		Assert.assertEquals(1, subscriptionRepository.findAllByProject(project.getId()).size());
	}

	@Test
	public void addSla() {
		em.flush();
		em.clear();
		final SlaEditionVo vo = new SlaEditionVo();
		vo.setName("AA");
		vo.setDescription("ADescription");
		vo.setStart(identifierHelper.asList("Open"));
		vo.setStop(identifierHelper.asList("Closed"));
		vo.setPause(new ArrayList<>());
		vo.getPause().add("Wait");
		vo.getPause().add("Expect");
		vo.setThreshold(5);
		vo.setSubscription(subscription);
		final int id = resource.addSla(vo);
		Assert.assertTrue(id > 0);
		em.flush();
		em.clear();
		final Sla sla = slaRepository.findBySubscription(subscription).iterator().next();
		Assert.assertEquals(sla.getId().intValue(), id);
		Assert.assertEquals("AA", sla.getName());
		Assert.assertEquals("ADescription", sla.getDescription());
		Assert.assertEquals("OPEN", sla.getStart());
		Assert.assertEquals("CLOSED", sla.getStop());
		Assert.assertEquals("EXPECT,WAIT", sla.getPause());
		Assert.assertEquals(5, sla.getThreshold());
	}

	@Test
	public void deleteSla() {
		final int id = slaRepository.findBySubscription(subscription).iterator().next().getId();
		em.flush();
		em.clear();
		resource.deleteSla(id);
		em.flush();
		em.clear();
		Assert.assertEquals(0, slaRepository.findBySubscription(subscription).size());
	}

	@Test
	public void addSlaBoundStart() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("start", "SlaBound"));

		final SlaEditionVo vo = new SlaEditionVo();
		vo.setName("AA");
		vo.setStart(identifierHelper.asList("Open"));
		vo.setStop(identifierHelper.asList("Resolved"));
		vo.setPause(new ArrayList<>());
		vo.getPause().add("Open");
		vo.setSubscription(subscription);
		em.flush();
		em.clear();
		resource.addSla(vo);
	}

	@Test
	public void addSlaBoundEnd() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("stop", "SlaBound"));

		final SlaEditionVo vo = new SlaEditionVo();
		vo.setName("AA");
		vo.setStart(identifierHelper.asList("Open"));
		vo.setStop(identifierHelper.asList("Resolved"));
		vo.setPause(new ArrayList<>());
		vo.getPause().add("Resolved");
		vo.setSubscription(subscription);
		em.flush();
		em.clear();
		resource.addSla(vo);
	}

	@Test
	public void updateSla() {
		final Sla oldEntity = slaRepository.findBySubscription(subscription).get(0);
		final SlaEditionVo vo = new SlaEditionVo();
		vo.setName("AA");
		vo.setDescription("ADescription");
		vo.setStart(identifierHelper.asList("Open"));
		vo.setStop(identifierHelper.asList("Resolved"));
		vo.setPause(new ArrayList<>());
		vo.getPause().add("One");
		vo.getPause().add("Any");
		vo.setThreshold(5);
		vo.setSubscription(subscription);
		vo.setId(oldEntity.getId());
		em.flush();
		em.clear();
		resource.updateSla(vo);
		em.flush();
		em.clear();
		final Sla sla = slaRepository.findBySubscription(subscription).iterator().next();
		Assert.assertEquals(sla.getId(), vo.getId());
		Assert.assertEquals("AA", sla.getName());
		Assert.assertEquals("ADescription", sla.getDescription());
		Assert.assertEquals("OPEN", sla.getStart());
		Assert.assertEquals("RESOLVED", sla.getStop());
		Assert.assertEquals("ANY,ONE", sla.getPause());
		Assert.assertEquals(5, sla.getThreshold());
	}

	@Test
	public void addBusinessHours() {
		final BusinessHoursEditionVo vo = new BusinessHoursEditionVo();
		vo.setStart(1);
		vo.setEnd(2);
		vo.setSubscription(subscription);
		em.flush();
		em.clear();
		final int id = resource.addBusinessHours(vo);
		Assert.assertTrue(id > 0);
		em.flush();
		em.clear();
		final BusinessHours entity = repository.findBySubscription(subscription).getBusinessHours().iterator().next();
		Assert.assertEquals(entity.getId().intValue(), id);
		Assert.assertEquals(1, entity.getStart());
		Assert.assertEquals(2, entity.getEnd());
	}

	@Test
	public void updateBusinessHours() {
		final BusinessHours oldEntity = repository.findBySubscription(subscription).getBusinessHours().iterator()
				.next();
		final BusinessHoursEditionVo vo = new BusinessHoursEditionVo();
		vo.setStart(1);
		vo.setEnd(2);
		vo.setSubscription(subscription);
		vo.setId(oldEntity.getId());
		em.flush();
		em.clear();
		resource.updateBusinessHours(vo);
		em.flush();
		em.clear();
		final BusinessHours entity = repository.findBySubscription(subscription).getBusinessHours().iterator().next();
		Assert.assertEquals(entity.getId(), vo.getId());
		Assert.assertEquals(1, entity.getStart());
		Assert.assertEquals(2, entity.getEnd());
	}

	@Test
	public void addBusinessHoursOverlapsStart() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("start", "Overlap"));

		final BusinessHoursEditionVo vo = new BusinessHoursEditionVo();
		vo.setStart(10 * DateUtils.MILLIS_PER_HOUR);
		vo.setEnd(23 * DateUtils.MILLIS_PER_HOUR);
		vo.setSubscription(subscription);
		em.flush();
		em.clear();
		resource.addBusinessHours(vo);
	}

	@Test
	public void addBusinessHoursOverlapsEnd() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("stop", "Overlap"));

		final BusinessHoursEditionVo vo = new BusinessHoursEditionVo();
		vo.setStart(2 * DateUtils.MILLIS_PER_HOUR);
		vo.setEnd(1 * DateUtils.MILLIS_PER_HOUR);
		vo.setSubscription(subscription);
		em.flush();
		em.clear();
		resource.addBusinessHours(vo);
	}

	@Test
	public void deleteBusinessHours() {
		Assert.assertEquals(2, repository.findBySubscription(subscription).getBusinessHours().size());
		final int id = repository.findBySubscription(subscription).getBusinessHours().iterator().next().getId();
		em.flush();
		em.clear();
		resource.deleteBusinessHours(id);
		em.flush();
		em.clear();
		Assert.assertEquals(1, repository.findBySubscription(subscription).getBusinessHours().size());
	}

	@Test(expected = BusinessException.class)
	public void deleteLastBusinessHours() {
		Assert.assertEquals(2, repository.findBySubscription(subscription).getBusinessHours().size());
		int id = repository.findBySubscription(subscription).getBusinessHours().iterator().next().getId();
		em.flush();
		em.clear();
		resource.deleteBusinessHours(id);
		em.flush();
		em.clear();
		Assert.assertEquals(1, repository.findBySubscription(subscription).getBusinessHours().size());
		em.flush();
		em.clear();

		// Try to delete the last one
		id = repository.findBySubscription(subscription).getBusinessHours().iterator().next().getId();
		resource.deleteBusinessHours(id);
		Assert.assertEquals(1, repository.findBySubscription(subscription).getBusinessHours().size());
	}

	@Test
	public void setCalendar() {
		final Calendar calendar = new Calendar();
		calendar.setName("Any");
		calendarRepository.saveAndFlush(calendar);
		final int id = calendar.getId();
		em.clear();

		resource.setCalendar(subscription, id);
		em.flush();
		em.clear();
		Assert.assertEquals(id, repository.findBySubscription(subscription).getCalendar().getId().intValue());

	}

	@Test
	public void getCalendars() {
		final Calendar calendar2 = new Calendar();
		calendar2.setName("Any2");
		calendarRepository.saveAndFlush(calendar2);
		final Calendar calendar1 = new Calendar();
		calendar1.setName("Any1");
		calendarRepository.saveAndFlush(calendar1);
		em.flush();
		em.clear();

		Assert.assertEquals(3, resource.getAllCalendars().size());
		Assert.assertEquals("Any1", resource.getAllCalendars().iterator().next().getName());

	}
}
