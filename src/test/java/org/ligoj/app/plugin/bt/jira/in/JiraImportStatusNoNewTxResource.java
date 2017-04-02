package org.ligoj.app.plugin.bt.jira.in;

import javax.transaction.Transactional;

import org.ligoj.app.plugin.bt.jira.model.ImportStatus;
import org.ligoj.app.plugin.bt.jira.model.UploadMode;
import org.springframework.stereotype.Component;

/**
 * No TxType.REQUIRES_NEW transaction to avoid table locking with HSQL during JUnit execution.
 */
@Component
@Transactional
public class JiraImportStatusNoNewTxResource extends JiraImportStatusResource {

	@Override
	public void moveForwardImportStep(final ImportStatus importStatus) {
		super.moveForwardImportStep(importStatus);
	}

	@Override
	public void releaseImport(final int subscription, final boolean failed) {
		super.releaseImport(subscription, failed);
	}

	@Override
	public ImportStatus checkAndLockImport(final int subscription, final UploadMode mode) {
		return super.checkAndLockImport(subscription, mode);
	}

}
