package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.security.JarVerificationService;

public class MultiInstallWizard extends Wizard {
	private static final String KEY_UNABLE = "MultiInstallWizard.error.unable";
	private static final String KEY_OLD = "MultiInstallWizard.error.old";
	private static final String KEY_SAVED_CONFIG =
		"MultiInstallWizard.savedConfig";
	private static final String KEY_INSTALLING =
		"MultiInstallWizard.installing";
	private MultiReviewPage reviewPage;
	private LicensePage licensePage;
	private MultiOptionalFeaturesPage optionalFeaturesPage;
	private MultiTargetPage targetPage;
	private PendingChange[] jobs;
	private IInstallConfiguration config;
	private boolean needLicensePage;
	private int installCount = 0;

	public MultiInstallWizard(PendingChange[] jobs) {
		this(jobs, true);
	}

	public MultiInstallWizard(PendingChange[] jobs, boolean needLicensePage) {
		setDialogSettings(UpdateUI.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(UpdateUIImages.DESC_INSTALL_WIZ);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		setWindowTitle(
			UpdateUI.getResourceString("MultiInstallWizard.wtitle"));
		this.jobs = jobs;
		this.needLicensePage = needLicensePage;
	}

	public boolean isSuccessfulInstall() {
		return installCount > 0;
	}

	/**
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		final PendingChange[] selectedJobs = reviewPage.getSelectedJobs();
		installCount = 0;

		if (targetPage != null) {
			// Check for duplication conflicts
			ArrayList conflicts =
				DuplicateConflictsDialog.computeDuplicateConflicts(
					targetPage.getTargetSites(),
					config);
			if (conflicts != null) {
				DuplicateConflictsDialog dialog =
					new DuplicateConflictsDialog(getShell(), conflicts);
				if (dialog.open() != 0)
					return false;
			}
		}

		// ok to continue		
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				try {
					MultiInstallWizard.makeConfigurationCurrent(config, null);
					execute(selectedJobs, monitor);
				} catch (InstallAbortedException e) {
					throw new InvocationTargetException(e);
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
			Throwable targetException = e.getTargetException();
			if (targetException instanceof InstallAbortedException) {
				return true;
			} else {
				UpdateUI.logException(e);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/*
	 * When we are uninstalling, there is not targetSite
	 */
	private void execute(
		PendingChange[] selectedJobs,
		IProgressMonitor monitor)
		throws InstallAbortedException, CoreException {
		monitor.beginTask(
			UpdateUI.getResourceString(KEY_INSTALLING),
			jobs.length);
		for (int i = 0; i < selectedJobs.length; i++) {
			PendingChange job = selectedJobs[i];
			SubProgressMonitor subMonitor =
				new SubProgressMonitor(
					monitor,
					1,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			executeOneJob(job, subMonitor);
			//monitor.worked(1);
			InstallWizard.saveLocalSite();
			installCount++;
		}
	}

	public void addPages() {
		reviewPage = new MultiReviewPage(jobs);
		addPage(reviewPage);

		config = createInstallConfiguration();
		boolean addLicensePage = false;
		boolean addOptionalFeaturesPage = false;
		boolean addTargetPage = false;

		for (int i = 0; i < jobs.length; i++) {
			PendingChange job = jobs[i];

			if (job.getJobType() == PendingChange.INSTALL) {
				if (needLicensePage && UpdateModel.hasLicense(job)) {
					addLicensePage = true;
				}
				if (UpdateModel.hasOptionalFeatures(job.getFeature())) {
					addOptionalFeaturesPage = true;
				}
				addTargetPage = true;
			}
		}
		if (addLicensePage) {
			licensePage = new LicensePage(true);
			addPage(licensePage);
		}
		if (addOptionalFeaturesPage) {
			optionalFeaturesPage = new MultiOptionalFeaturesPage(config);
			addPage(optionalFeaturesPage);
		}
		if (addTargetPage) {
			targetPage = new MultiTargetPage(config);
			addPage(targetPage);
		}
	}

	private boolean isPageRequired(IWizardPage page) {
		if (page.equals(licensePage)) {
			return reviewPage.hasSelectedJobsWithLicenses();
		}
		if (page.equals(optionalFeaturesPage)) {
			return reviewPage.hasSelectedJobsWithOptionalFeatures();
		}
		if (page.equals(targetPage)) {
			return reviewPage.hasSelectedInstallJobs();
		}
		return true;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage[] pages = getPages();
		boolean start = false;
		IWizardPage nextPage = null;

		if (page.equals(reviewPage)) {
			updateDynamicPages();
		}

		for (int i = 0; i < pages.length; i++) {
			if (pages[i].equals(page)) {
				start = true;
			} else if (start) {
				if (isPageRequired(pages[i])) {
					nextPage = pages[i];
					break;
				}
			}
		}
		return nextPage;
	}

	private void updateDynamicPages() {
		if (licensePage != null) {
			PendingChange[] licenseJobs =
				reviewPage.getSelectedJobsWithLicenses();
			licensePage.setJobs(licenseJobs);
		}
		if (optionalFeaturesPage != null) {
			PendingChange[] optionalJobs =
				reviewPage.getSelectedJobsWithOptionalFeatures();
			optionalFeaturesPage.setJobs(optionalJobs);
		}
		if (targetPage != null) {
			PendingChange[] installJobs = reviewPage.getSelectedInstallJobs();
			targetPage.setJobs(installJobs);
		}
	}

	public static IInstallConfiguration createInstallConfiguration() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config =
				localSite.cloneCurrentConfiguration();
			config.setLabel(Utilities.format(config.getCreationDate()));
			return config;
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return null;
		}
	}

	public static void makeConfigurationCurrent(
		IInstallConfiguration config,
		PendingChange job)
		throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		if (job != null && job.getJobType() == PendingChange.INSTALL) {
			if (job.getFeature().isPatch()) {
				// Installing a patch - preserve the current configuration
				IInstallConfiguration cconfig =
					localSite.getCurrentConfiguration();
				IInstallConfiguration savedConfig =
					localSite.addToPreservedConfigurations(cconfig);
				VersionedIdentifier vid =
					job.getFeature().getVersionedIdentifier();
				String key = "@" + vid.getIdentifier() + "_" + vid.getVersion();
				String newLabel =
					UpdateUI.getFormattedMessage(KEY_SAVED_CONFIG, key);
				savedConfig.setLabel(newLabel);
				UpdateModel model =
					UpdateUI.getDefault().getUpdateModel();
				model.fireObjectChanged(savedConfig, null);
			}
		}
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

	private void executeOneJob(PendingChange job, IProgressMonitor monitor)
		throws CoreException {
		IConfiguredSite targetSite = null;
		Object[] optionalElements = null;
		IFeatureReference[] optionalFeatures = null;
		if (job.getJobType() == PendingChange.INSTALL) {
			if (optionalFeaturesPage != null) {
				optionalElements =
					optionalFeaturesPage.getOptionalElements(job);
				optionalFeatures =
					optionalFeaturesPage.getCheckedOptionalFeatures(job);
			}
			if (targetPage != null) {
				targetSite = targetPage.getTargetSite(job);
			}
		}
		executeOneJob(
			job,
			targetSite,
			optionalElements,
			optionalFeatures,
			monitor);
	}

	private void executeOneJob(
		PendingChange job,
		IConfiguredSite targetSite,
		Object[] optionalElements,
		IFeatureReference[] optionalFeatures,
		IProgressMonitor monitor)
		throws CoreException {

		IFeature feature = job.getFeature();
		if (job.getJobType() == PendingChange.UNINSTALL) {
			//find the  config site of this feature
			IConfiguredSite site = findConfigSite(feature, config);
			if (site != null) {
				site.remove(feature, monitor);
			} else {
				// we should do something here
				throwError(
					UpdateUI.getFormattedMessage(
						KEY_UNABLE,
						feature.getLabel()));
			}
		} else if (job.getJobType() == PendingChange.INSTALL) {
			if (optionalFeatures == null)
				targetSite.install(feature, getVerificationListener(), monitor);
			else
				targetSite.install(
					feature,
					optionalFeatures,
					getVerificationListener(),
					monitor);
			IFeature oldFeature = job.getOldFeature();
			if (oldFeature != null && !job.isOptionalDelta()) {
				if (optionalElements != null)
					preserveOptionalState(config, targetSite, optionalElements);
				boolean oldSuccess = unconfigure(config, oldFeature);
				if (!oldSuccess) {
					if (!isNestedChild(oldFeature))
						// "eat" the error if nested child
						throwError(
							UpdateUI.getFormattedMessage(
								KEY_OLD,
								oldFeature.getLabel()));
				}
			}
			if (oldFeature == null) {
				ensureUnique(config, feature, targetSite);
				if (optionalFeatures != null) {
					preserveOriginatingURLs(feature, optionalFeatures);
				}
			}
		} else if (job.getJobType() == PendingChange.CONFIGURE) {
			configure(feature);
			ensureUnique(config, feature, targetSite);
		} else if (job.getJobType() == PendingChange.UNCONFIGURE) {
			unconfigure(config, job.getFeature());
		} else {
			// should not be here
			return;
		}
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		job.markProcessed();
		model.fireObjectChanged(job, null);
	}

	static void ensureUnique(
		IInstallConfiguration config,
		IFeature feature,
		IConfiguredSite targetSite)
		throws CoreException {
		boolean patch = false;
		if (targetSite == null)
			targetSite = feature.getSite().getCurrentConfiguredSite();
		IImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				patch = true;
				break;
			}
		}
		// Only need to check features that patch other features.
		if (!patch)
			return;
		IFeature localFeature = findLocalFeature(targetSite, feature);
		ArrayList oldFeatures = new ArrayList();
		// First collect all older active features that
		// have the same ID as new features marked as 'unique'.
		collectOldFeatures(localFeature, targetSite, oldFeatures);
		// Now unconfigure old features to enforce uniqueness
		for (int i = 0; i < oldFeatures.size(); i++) {
			IFeature oldFeature = (IFeature) oldFeatures.get(i);
			unconfigure(config, oldFeature);
		}
	}

	private void throwError(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				UpdateUI.getPluginId(),
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}

	static IConfiguredSite findConfigSite(
		IFeature feature,
		IInstallConfiguration config)
		throws CoreException {
		IConfiguredSite[] configSites = config.getConfiguredSites();
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite site = configSites[i];
			if (site.getSite().equals(feature.getSite())) {
				return site;
			}
		}
		return null;
	}

	private static boolean unconfigure(
		IInstallConfiguration config,
		IFeature feature)
		throws CoreException {
		IConfiguredSite site = findConfigSite(feature, config);
		if (site != null) {
			PatchCleaner cleaner = new PatchCleaner(site, feature);
			boolean result = site.unconfigure(feature);
			cleaner.dispose();
			return result;
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

	private boolean isNestedChild(IFeature feature) {
		IConfiguredSite[] csites = config.getConfiguredSites();
		try {
			for (int i = 0; csites != null && i < csites.length; i++) {
				IFeatureReference[] refs = csites[i].getConfiguredFeatures();
				for (int j = 0; refs != null && j < refs.length; j++) {
					IFeature parent = refs[j].getFeature();
					IFeatureReference[] children =
						parent.getIncludedFeatureReferences();
					for (int k = 0;
						children != null && k < children.length;
						k++) {
						IFeature child = children[k].getFeature();
						if (feature.equals(child))
							return true;
					}
				}
			}
		} catch (CoreException e) {
			// will return false
		}
		return false;
	}

	static void preserveOptionalState(
		IInstallConfiguration config,
		IConfiguredSite targetSite,
		Object[] optionalElements) {
		for (int i = 0; i < optionalElements.length; i++) {
			FeatureHierarchyElement fe =
				(FeatureHierarchyElement) optionalElements[i];
			Object[] children = fe.getChildren(true);
			preserveOptionalState(config, targetSite, children);
			if (!fe.isEnabled(config)) {
				IFeature newFeature = fe.getFeature();
				try {
					IFeature localFeature =
						findLocalFeature(targetSite, newFeature);
					if (localFeature != null)
						targetSite.unconfigure(localFeature);
				} catch (CoreException e) {
					// Eat this - we will leave with it
				}
			}
		}
	}

	static void collectOldFeatures(
		IFeature feature,
		IConfiguredSite targetSite,
		ArrayList result)
		throws CoreException {
		IFeatureReference[] included = feature.getIncludedFeatureReferences();
		for (int i = 0; i < included.length; i++) {
			IFeatureReference iref = included[i];
			IFeature ifeature = iref.getFeature();
			// find other features and unconfigure
			String id = iref.getVersionedIdentifier().getIdentifier();
			IFeature[] sameIds =
				UpdateUI.searchSite(id, targetSite, true);
			for (int j = 0; j < sameIds.length; j++) {
				IFeature sameId = sameIds[j];
				// Ignore self.
				if (sameId.equals(ifeature))
					continue;
				result.add(sameId);
			}
			collectOldFeatures(ifeature, targetSite, result);
		}
	}

	private static IFeature findLocalFeature(
		IConfiguredSite csite,
		IFeature feature)
		throws CoreException {
		IFeatureReference[] refs = csite.getConfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			VersionedIdentifier refVid = ref.getVersionedIdentifier();
			if (feature.getVersionedIdentifier().equals(refVid))
				return ref.getFeature();
		}
		return null;
	}

	private void preserveOriginatingURLs(
		IFeature feature,
		IFeatureReference[] optionalFeatures) {
		// walk the hieararchy and preserve the originating URL
		// for all the optional features that are not chosen to
		// be installed.
		URL url = feature.getSite().getURL();
		try {
			IIncludedFeatureReference[] irefs =
				feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IIncludedFeatureReference iref = irefs[i];
				boolean preserve = false;
				if (iref.isOptional()) {
					boolean onTheList = false;
					for (int j = 0; j < optionalFeatures.length; j++) {
						if (optionalFeatures[j].equals(iref)) {
							//was on the list
							onTheList = true;
							break;
						}
					}
					if (!onTheList)
						preserve = true;
				}
				if (preserve) {
					try {
						String id =
							iref.getVersionedIdentifier().getIdentifier();
						UpdateUI.setOriginatingURL(id, url);
					} catch (CoreException e) {
						// Silently ignore
					}
				} else {
					try {
						IFeature ifeature = iref.getFeature();
						preserveOriginatingURLs(ifeature, optionalFeatures);
					} catch (CoreException e) {
						// Silently ignore
					}
				}
			}
		} catch (CoreException e) {
			// Silently ignore
		}
	}
}
