package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.UpdateUIPluginImages;
import org.eclipse.update.internal.ui.model.PendingChange;
import org.eclipse.update.internal.ui.model.UpdateModel;
import org.eclipse.update.internal.ui.security.JarVerificationService;

public class InstallWizard extends Wizard {
	private static final String KEY_UNABLE = "InstallWizard.error.unable";
	private static final String KEY_OLD = "InstallWizard.error.old";
	private ReviewPage reviewPage;
	private TargetPage targetPage;
	private PendingChange job;
	private boolean successfulInstall = false;
	private IInstallConfiguration config;

	public InstallWizard(PendingChange job) {
		setDialogSettings(UpdateUIPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(UpdateUIPluginImages.DESC_INSTALL_WIZ);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		this.job = job;
	}

	public boolean isSuccessfulInstall() {
		return successfulInstall;
	}

	private boolean hasLicense() {
		IFeature feature = job.getFeature();
		IURLEntry info = feature.getLicense();
		if (info == null)
			return false;
		return info.getAnnotation() != null && info.getAnnotation().length() > 0;
	}

	/**
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		final IConfiguredSite targetSite =
			(targetPage == null) ? null : targetPage.getTargetSite();
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					successfulInstall = false;
					makeConfigurationCurrent(config);
					execute(targetSite, monitor);
					saveLocalSite();
					successfulInstall = true;
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException e) {
			UpdateUIPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public void addPages() {
		reviewPage = new ReviewPage(job);
		addPage(reviewPage);

		config = createInstallConfiguration();

		if (job.getJobType() == PendingChange.INSTALL) {
			if (hasLicense()) {
				addPage(new LicensePage(job));
			}
			targetPage = new TargetPage(job, config);
			addPage(targetPage);
		}
	}

	public static IInstallConfiguration createInstallConfiguration() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.cloneCurrentConfiguration();
			config.setLabel(Utilities.format(config.getCreationDate()));
			return config;
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
			return null;
		}
	}

	public static void makeConfigurationCurrent(IInstallConfiguration config) throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.addConfiguration(config);
	}

	public static void saveLocalSite() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.save();
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return page.getNextPage() == null && super.canFinish();
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return super.getPreviousPage(page);
	}
	public IWizardPage getNextPage(IWizardPage page) {
		return super.getNextPage(page);
	}
	/*
	 * When we are uninstalling, there is not targetSite
	 */
	private void execute(IConfiguredSite targetSite, IProgressMonitor monitor)
		throws CoreException {
		IFeature feature = job.getFeature();
		if (job.getJobType() == PendingChange.UNINSTALL) {
			//find the  config site of this feature
			IConfiguredSite site = findConfigSite(feature, config);
			if (site != null) {
				site.remove(feature, monitor);
			} else {
				// we should do something here
				throwError(UpdateUIPlugin.getResourceString(KEY_UNABLE));
			}
		} else if (job.getJobType() == PendingChange.INSTALL) {
			targetSite.install(feature, getVerificationListener(), monitor);
			IFeature oldFeature = job.getOldFeature();
			if (oldFeature!=null) {
				boolean oldSuccess = unconfigure(oldFeature);
				if (!oldSuccess)
					throwError(UpdateUIPlugin.getResourceString(KEY_OLD));
			}
		} else if (job.getJobType() == PendingChange.CONFIGURE) {
			configure(job.getFeature());
		} else if (job.getJobType() == PendingChange.UNCONFIGURE) {
			unconfigure(job.getFeature());
		} else {
			// should not be here
			return;
		}
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addPendingChange(job);
	}

	private void throwError(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				UpdateUIPlugin.getPluginId(),
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}

	static IConfiguredSite findConfigSite(IFeature feature, IInstallConfiguration config) throws CoreException {
		IConfiguredSite[] configSites = config.getConfiguredSites();
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite site = configSites[i];
			if (site.getSite().equals(feature.getSite())) {
				return site;
			}
		}
		return null;
	}

	private boolean unconfigure(IFeature feature) throws CoreException {
		IConfiguredSite site = findConfigSite(feature, config);
		if (site != null) {
			return site.unconfigure(feature);
		}
		return false;
	}
	private void configure(IFeature feature) throws CoreException {
		IConfiguredSite site = findConfigSite(feature, config);
		if (site != null) {
			site.configure(feature);
		}
	}

	private IVerificationListener getVerificationListener() {
		return new JarVerificationService(this.getShell());
	}
}