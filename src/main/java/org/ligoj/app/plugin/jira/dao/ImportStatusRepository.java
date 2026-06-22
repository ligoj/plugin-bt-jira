/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.jira.dao;

import org.ligoj.app.dao.task.LongTaskSubscriptionRepository;
import org.ligoj.app.plugin.jira.model.ImportStatus;

/**
 * {@link ImportStatus} repository.
 */
public interface ImportStatusRepository extends LongTaskSubscriptionRepository<ImportStatus> {

	// All is delegated
}
