package org.ligoj.app.plugin.bt.jira.in;

import java.util.Date;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.plugin.bt.jira.dao.ImportStatusRepository;
import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JIRA import issues resource.
 */
@Path(JiraImportPluginResource.IMPORT_URL + "/status")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class JiraImportStatusResource {

	@Autowired
	protected SubscriptionRepository subscriptionRepository;

	@Autowired
	private ImportStatusRepository repository;

	/**
	 * Return status of import.
	 * 
	 * @param subscription
	 *            the subscription identifier
	 * @return status of import.
	 */
	@GET
	@Path("{subscription:\\d+}")
	public ImportStatus getImportStatus(@PathParam("subscription") final int subscription) {
		return repository.findBySubscription(subscription);
	}

	/**
	 * Retrieve an existing or new {@link ImportStatus}
	 */
	private ImportStatus createImportStatusAsNeeded(final int subscription) {
		ImportStatus importStatus = repository.findBySubscription(subscription);
		if (importStatus == null) {
			final Subscription subscriptionEntity = subscriptionRepository.findOneExpected(subscription);
			importStatus = new ImportStatus();
			importStatus.setSubscription(subscriptionEntity);
		}

		return importStatus;
	}

	/**
	 * Move forward the the step of given import status.
	 * 
	 * @param importStatus
	 *            the status to update.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	public void moveForwardImportStep(final ImportStatus importStatus) {
		importStatus.setStep(importStatus.getStep() + 1);
		repository.saveAndFlush(importStatus);
	}

	/**
	 * Mark the given subscription as finished import.
	 * 
	 * @param subscription
	 *            The subscription to reset.
	 * @param failed
	 *            The import failed
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	public void releaseImport(final int subscription, final boolean failed) {
		repository.finishBySubscription(subscription, new Date(), failed);
	}

	/**
	 * Check there no running import.
	 * 
	 * @param subscription
	 *            the subscription to lock.
	 * @param mode
	 *            the upload mode.
	 * @return the locked import status.
	 */
	@Transactional(value = TxType.REQUIRES_NEW)
	public ImportStatus checkAndLockImport(final int subscription, final UploadMode mode) {
		// Check there is no running import
		synchronized (this) {
			final ImportStatus importStatusService = repository.findBySameServiceNotFinished(subscription);

			if (importStatusService != null) {
				// On this service, there is already a running import
				throw new BusinessException("A running import is not yet finished on this instance", importStatusService.getAuthor(),
						importStatusService.getStart(), importStatusService.getJira());
			}

			// Initialize starting information
			final ImportStatus importStatus = createImportStatusAsNeeded(subscription);
			importStatus.setAuthor(SecurityContextHolder.getContext().getAuthentication().getName());
			importStatus.setStep(1);
			importStatus.setStart(new Date());

			// Rest old values
			importStatus.setChanges(null);
			importStatus.setFailed(false);
			importStatus.setComponents(null);
			importStatus.setCustomFields(null);
			importStatus.setEnd(null);
			importStatus.setIssueFrom(null);
			importStatus.setIssues(null);
			importStatus.setJira(null);
			importStatus.setJiraVersion(null);
			importStatus.setLabels(null);
			importStatus.setMaxIssue(null);
			importStatus.setMinIssue(null);
			importStatus.setMode(mode);
			importStatus.setNewIssues(null);
			importStatus.setNewVersions(null);
			importStatus.setNewComponents(null);
			importStatus.setPkey(null);
			importStatus.setPriorities(null);
			importStatus.setResolutions(null);
			importStatus.setStatuses(null);
			importStatus.setIssueTo(null);
			importStatus.setTypes(null);
			importStatus.setUsers(null);
			importStatus.setVersions(null);
			importStatus.setScriptRunner(null);
			importStatus.setStatusChanges(null);
			importStatus.setSynchronizedJira(null);
			importStatus.setCanSynchronizeJira(null);
			repository.save(importStatus);
			return importStatus;
		}
	}

}
