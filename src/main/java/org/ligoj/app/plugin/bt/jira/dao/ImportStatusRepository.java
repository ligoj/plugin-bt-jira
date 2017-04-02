package org.ligoj.app.plugin.bt.jira.dao;

import java.util.Date;

import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link ImportStatus} repository.
 */
public interface ImportStatusRepository extends RestRepository<ImportStatus, Integer> {

	/**
	 * Delete the import status associated to the given subscription.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 */
	@Modifying
	@Query("DELETE ImportStatus i WHERE i.subscription.id = ?1")
	void deleteBySubscription(int subscription);

	/**
	 * Return not finished import status of a given project.
	 * 
	 * @param project
	 *            the project identifier.
	 * @return <code>null</code> or a not finished import status of a given subscription.
	 */
	@Query("FROM ImportStatus i WHERE i.subscription.project.id = ?1 AND i.end IS NULL")
	ImportStatus findByProjectNotFinished(int project);

	/**
	 * Return an active import status for the same service than the given one. .
	 * 
	 * @param subscription
	 *            The subscription to delete.
	 * @return the import status of a given subscription.
	 */
	@Query("SELECT i FROM ImportStatus i, Subscription s WHERE s.id = ?1 AND i.subscription.node = s.node AND i.end IS NULL")
	ImportStatus findBySameServiceNotFinished(int subscription);

	/**
	 * Return import status of a given subscription.
	 * 
	 * @param subscription
	 *            The subscription to delete.
	 * @return the import status of a given subscription.
	 */
	@Query("FROM ImportStatus i WHERE i.subscription.id = ?1")
	ImportStatus findBySubscription(int subscription);

	/**
	 * mark the given subscription as finished import.
	 * 
	 * @param subscription
	 *            The subscription to reset.
	 * @param date
	 *            the end date.
	 * @param failed
	 *            The import failed
	 */
	@Modifying
	@Query("UPDATE ImportStatus i SET i.end = ?2, i.failed = ?3 WHERE i.subscription.id  = ?1")
	void finishBySubscription(int subscription, Date date, boolean failed);
}
