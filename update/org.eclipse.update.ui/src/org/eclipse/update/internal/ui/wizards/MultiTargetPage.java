package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.Hashtable;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.PendingChange;
import org.eclipse.update.internal.ui.parts.*;

public class MultiTargetPage extends BannerPage implements IDynamicPage {
	// NL keys
	private static final String KEY_TITLE =
		"MultiInstallWizard.TargetPage.title";
	private static final String KEY_DESC = "MultiInstallWizard.TargetPage.desc";
	private static final String KEY_JOBS_LABEL =
		"MultiInstallWizard.TargetPage.jobsLabel";
	private static final String KEY_SITE_LABEL =
		"MultiInstallWizard.TargetPage.siteLabel";
	private static final String KEY_NEW = "MultiInstallWizard.TargetPage.new";
	private static final String KEY_REQUIRED_FREE_SPACE =
		"MultiInstallWizard.TargetPage.requiredSpace";
	private static final String KEY_AVAILABLE_FREE_SPACE =
		"MultiInstallWizard.TargetPage.availableSpace";
	private static final String KEY_LOCATION =
		"MultiInstallWizard.TargetPage.location";
	private static final String KEY_LOCATION_MESSAGE =
		"MultiInstallWizard.TargetPage.location.message";
	private static final String KEY_LOCATION_EMPTY =
		"MultiInstallWizard.TargetPage.location.empty";
	private static final String KEY_LOCATION_ERROR_TITLE =
		"MultiInstallWizard.TargetPage.location.error.title";
	private static final String KEY_LOCATION_ERROR_MESSAGE =
		"MultiInstallWizard.TargetPage.location.error.message";
	private static final String KEY_ERROR_REASON =
		"MultiInstallWizard.TargetPage.location.error.reason";
	private static final String KEY_SIZE = "MultiInstallWizard.TargetPage.size";
	private static final String KEY_SIZE_UNKNOWN =
		"MultiInstallWizard.TargetPage.unknownSize";
	private TableViewer jobViewer;
	private TableViewer siteViewer;
	private IInstallConfiguration config;
	private ConfigListener configListener;
	private Label requiredSpaceLabel;
	private Label availableSpaceLabel;
	private PendingChange[] jobs;
	private Hashtable targetSites;
	private Button addButton;

	static class JobTargetSite {
		PendingChange job;
		IConfiguredSite affinitySite;
		IConfiguredSite defaultSite;
		IConfiguredSite targetSite;
	}

	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return jobs;
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object parent) {
			return config.getConfiguredSites();
		}
	}

	class TableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		/**
		* @see ITableLabelProvider#getColumnImage(Object, int)
		*/
		public Image getColumnImage(Object obj, int col) {
			if (obj instanceof IConfiguredSite)
				return UpdateUI.getDefault().getLabelProvider().get(
					UpdateUIImages.DESC_LSITE_OBJ);
			if (obj instanceof PendingChange) {
				PendingChange job = (PendingChange) obj;
				boolean patch = job.getFeature().isPatch();
				ImageDescriptor base =
					patch
						? UpdateUIImages.DESC_EFIX_OBJ
						: UpdateUIImages.DESC_FEATURE_OBJ;
				int flags = 0;
				JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
				if (jobSite == null || jobSite.targetSite == null)
					flags = UpdateLabelProvider.F_ERROR;
				return UpdateUI.getDefault().getLabelProvider().get(
					base,
					flags);
			}
			return null;
		}

		/**
		 * @see ITableLabelProvider#getColumnText(Object, int)
		 */
		public String getColumnText(Object obj, int col) {
			if (obj instanceof PendingChange && col == 0) {
				PendingChange job = (PendingChange) obj;
				IFeature feature = job.getFeature();
				return feature.getLabel()
					+ " "
					+ feature.getVersionedIdentifier().getVersion().toString();
			}
			if (obj instanceof IConfiguredSite && col == 0) {
				IConfiguredSite csite = (IConfiguredSite) obj;
				ISite site = csite.getSite();
				URL url = site.getURL();
				return url.getFile();

			}
			return null;
		}
	}

	class ConfigListener implements IInstallConfigurationChangedListener {
		public void installSiteAdded(IConfiguredSite csite) {
			siteViewer.add(csite);
			siteViewer.setSelection(new StructuredSelection(csite));
		}

		public void installSiteRemoved(IConfiguredSite csite) {
			siteViewer.remove(csite);
		}
	}

	/**
	 * Constructor for ReviewPage
	 */
	public MultiTargetPage(IInstallConfiguration config) {
		super("MultiTarget");
		setTitle(UpdateUI.getResourceString(KEY_TITLE));
		setDescription(UpdateUI.getResourceString(KEY_DESC));
		this.config = config;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		configListener = new ConfigListener();
		targetSites = new Hashtable();
	}

	public void setJobs(PendingChange[] jobs) {
		this.jobs = jobs;
	}

	public static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		PendingChange pendingChange) {
		return getDefaultTargetSite(config, pendingChange, true);
	}

	private static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		PendingChange pendingChange,
		boolean checkAffinityFeature) {
		IFeature oldFeature = pendingChange.getOldFeature();
		IFeature newFeature = pendingChange.getFeature();
		if (oldFeature != null) {
			// We should install into the same site as
			// the old feature
			try {
				return InstallWizard.findConfigSite(oldFeature, config);
			} catch (CoreException e) {
				UpdateUI.logException(e, false);
				return null;
			}
		}

		// This is a new install. Check if there is 
		// a disabled feature with the same ID
		String newFeatureID =
			newFeature.getVersionedIdentifier().getIdentifier();
		IConfiguredSite sameSite = findSameIdFeatureSite(config, newFeatureID);
		if (sameSite != null) {
			return sameSite;
		}

		if (checkAffinityFeature) {
			return getAffinitySite(config, newFeature);
		}
		return null;
	}

	private static IConfiguredSite getAffinitySite(
		IInstallConfiguration config,
		IFeature newFeature) {
		// check if the affinity feature is installed
		String affinityID = newFeature.getAffinityFeature();
		if (affinityID != null) {
			IConfiguredSite affinitySite =
				findSameIdFeatureSite(config, affinityID);
			if (affinitySite != null)
				return affinitySite;
		}
		return null;
	}

	private static IConfiguredSite findSameIdFeatureSite(
		IInstallConfiguration config,
		String featureID) {
		if (featureID == null)
			return null;
		IConfiguredSite[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			IConfiguredSite site = sites[i];
			IFeatureReference[] refs = site.getFeatureReferences();
			for (int j = 0; j < refs.length; j++) {
				IFeatureReference ref = refs[j];
				try {
					IFeature feature = ref.getFeature();
					if (featureID
						.equals(
							feature
								.getVersionedIdentifier()
								.getIdentifier())) {
						// found it
						return site;
					}
				} catch (CoreException e) {
					UpdateUI.logException(e, false);
				}
			}
		}
		return null;
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		config.removeInstallConfigurationChangedListener(configListener);
		super.dispose();
	}

	/**
	 * @see DialogPage#createControl(Composite)
	 */
	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getResourceString(KEY_JOBS_LABEL));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createJobViewer(client);

		new Label(client, SWT.NULL);

		label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getResourceString(KEY_SITE_LABEL));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createSiteViewer(client);

		Composite buttonContainer = new Composite(client, SWT.NULL);
		GridLayout blayout = new GridLayout();
		blayout.marginWidth = blayout.marginHeight = 0;
		buttonContainer.setLayout(blayout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		addButton = new Button(buttonContainer, SWT.PUSH);
		addButton.setText(UpdateUI.getResourceString(KEY_NEW));
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addTargetLocation();
			}
		});
		addButton.setEnabled(false);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		addButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(addButton);
		Composite status = new Composite(client, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		status.setLayoutData(gd);
		layout = new GridLayout();
		layout.numColumns = 2;
		status.setLayout(layout);
		label = new Label(status, SWT.NULL);
		label.setText(
			UpdateUI.getResourceString(KEY_REQUIRED_FREE_SPACE));
		requiredSpaceLabel = new Label(status, SWT.NULL);
		requiredSpaceLabel.setLayoutData(
			new GridData(GridData.FILL_HORIZONTAL));
		label = new Label(status, SWT.NULL);
		label.setText(
			UpdateUI.getResourceString(KEY_AVAILABLE_FREE_SPACE));
		availableSpaceLabel = new Label(status, SWT.NULL);
		availableSpaceLabel.setLayoutData(
			new GridData(GridData.FILL_HORIZONTAL));

		WorkbenchHelp.setHelp(client, "org.eclipse.update.ui.ReviewPage");
		return client;
	}

	private void createJobViewer(Composite parent) {
		jobViewer =
			new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		Table table = jobViewer.getTable();
		table.setLayoutData(gd);
		jobViewer.setContentProvider(new JobsContentProvider());
		jobViewer.setLabelProvider(new TableLabelProvider());

		jobViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleJobsSelected((IStructuredSelection) event.getSelection());
			}
		});
	}

	private void handleJobsSelected(IStructuredSelection selection) {
		PendingChange job = (PendingChange) selection.getFirstElement();
		siteViewer.setInput(job);
		JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
		addButton.setEnabled(jobSite.affinitySite == null);
		if (jobSite.targetSite != null) {
			siteViewer.setSelection(
				new StructuredSelection(jobSite.targetSite));
		}
	}

	private void computeDefaultTargetSites() {
		targetSites.clear();
		for (int i = 0; i < jobs.length; i++) {
			PendingChange job = jobs[i];
			JobTargetSite jobSite = new JobTargetSite();
			jobSite.job = job;
			jobSite.defaultSite = getDefaultTargetSite(config, job, false);
			jobSite.affinitySite = getAffinitySite(config, job.getFeature());
			if (jobSite.affinitySite == null)
				jobSite.affinitySite = job.getDefaultTargetSite();
			jobSite.targetSite =
				jobSite.affinitySite != null
					? jobSite.affinitySite
					: jobSite.defaultSite;
			if (jobSite.targetSite == null)
				jobSite.targetSite = getFirstTarget(jobSite);
			targetSites.put(job, jobSite);
		}
	}

	private void createSiteViewer(Composite parent) {
		siteViewer =
			new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		Table table = siteViewer.getTable();
		table.setLayoutData(gd);
		siteViewer.setContentProvider(new TableContentProvider());
		siteViewer.setLabelProvider(new TableLabelProvider());
		siteViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object obj) {
				IConfiguredSite site = (IConfiguredSite) obj;
				PendingChange job = (PendingChange) siteViewer.getInput();
				JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
				return getSiteVisibility(site, jobSite);
			}
		});
		siteViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				selectTargetSite((IStructuredSelection) selection);
			}
		});

		if (config != null)
			config.addInstallConfigurationChangedListener(configListener);
	}

	public void setVisible(boolean visible) {
		if (visible) {
			computeDefaultTargetSites();
			jobViewer.setInput(jobs);
		}
		super.setVisible(visible);
		if (visible) {
			jobViewer.getTable().setFocus();
		}
	}

	private boolean getSiteVisibility(
		IConfiguredSite site,
		JobTargetSite jobSite) {
		// If affinity site is known, only it should be shown
		if (jobSite.affinitySite != null) {
			// Must compare referenced sites because
			// configured sites themselves may come from 
			// different configurations
			return site.getSite().equals(jobSite.affinitySite.getSite());
		}

		// If this is the default target site, let it show
		if (site.equals(jobSite.defaultSite))
			return true;
		// Not the default. If update, show only private sites.
		// If install, allow product site + private sites.
		if (site.isPrivateSite() && site.isUpdatable())
			return true;
		if (jobSite.job.getOldFeature() == null && site.isProductSite())
			return true;
		return false;
	}

	private void verifyNotEmpty(boolean empty) {
		String errorMessage = null;
		if (empty)
			errorMessage = UpdateUI.getResourceString(KEY_LOCATION_EMPTY);
		setErrorMessage(errorMessage);
		setPageComplete(!empty);
	}

	private IConfiguredSite getFirstTarget(JobTargetSite jobSite) {
		IConfiguredSite firstSite = jobSite.targetSite;
		if (firstSite == null) {
			IConfiguredSite[] sites = config.getConfiguredSites();
			for (int i = 0; i < sites.length; i++) {
				IConfiguredSite csite = sites[i];
				if (getSiteVisibility(csite, jobSite)) {
					firstSite = csite;
					break;
				}
			}
		}
		return firstSite;
	}

	private void selectTargetSite(IStructuredSelection selection) {
		IConfiguredSite site = (IConfiguredSite) selection.getFirstElement();
		PendingChange job = (PendingChange) siteViewer.getInput();
		if (job != null) {
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
			jobSite.targetSite = site;
			pageChanged();
		}
		updateStatus(site);
	}

	private void addTargetLocation() {
		DirectoryDialog dd = new DirectoryDialog(getContainer().getShell());
		dd.setMessage(UpdateUI.getResourceString(KEY_LOCATION_MESSAGE));
		String path = dd.open();
		if (path != null) {
			File file = new File(path);
			addConfiguredSite(getContainer().getShell(), config, file, false);
		}
	}

	public static boolean addConfiguredSite(
		Shell shell,
		IInstallConfiguration config,
		File file,
		boolean linked) {
		try {
			IConfiguredSite csite = null;
			if (linked) {
				csite = config.createLinkedConfiguredSite(file);
				config.addConfiguredSite(csite);
			} else {
				csite = config.createConfiguredSite(file);
				IStatus status = csite.verifyUpdatableStatus();
				if (status.isOK())
					config.addConfiguredSite(csite);
				else {
					String title =
						UpdateUI.getResourceString(
							KEY_LOCATION_ERROR_TITLE);
					String message =
						UpdateUI.getFormattedMessage(
							KEY_LOCATION_ERROR_MESSAGE,
							file.getPath());
					String message2 =
						UpdateUI.getFormattedMessage(
							KEY_ERROR_REASON,
							status.getMessage());
					message = message + "\r\n" + message2;
					ErrorDialog.openError(shell, title, message, status);
					return false;
				}
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return false;
		}
		return true;
	}

	private void updateStatus(Object element) {
		if (element == null) {
			requiredSpaceLabel.setText("");
			availableSpaceLabel.setText("");
			return;
		}
		IConfiguredSite site = (IConfiguredSite) element;
		URL url = site.getSite().getURL();
		String fileName = url.getFile();
		File file = new File(fileName);
		long available = LocalSystemInfo.getFreeSpace(file);
		long required = computeRequiredSizeFor(site);
		if (required == -1)
			requiredSpaceLabel.setText(
				UpdateUI.getResourceString(KEY_SIZE_UNKNOWN));
		else
			requiredSpaceLabel.setText(
				UpdateUI.getFormattedMessage(KEY_SIZE, "" + required));

		if (available == LocalSystemInfo.SIZE_UNKNOWN)
			availableSpaceLabel.setText(
				UpdateUI.getResourceString(KEY_SIZE_UNKNOWN));
		else
			availableSpaceLabel.setText(
				UpdateUI.getFormattedMessage(KEY_SIZE, "" + available));
	}

	private long computeRequiredSizeFor(IConfiguredSite site) {
		long totalSize = 0;
		for (int i = 0; i < jobs.length; i++) {
			PendingChange job = jobs[i];
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
			if (site.equals(jobSite.targetSite)) {
				long jobSize =
					site.getSite().getInstallSizeFor(job.getFeature());
				if (jobSize == -1)
					return -1;
				totalSize += jobSize;
			}
		}
		return totalSize;
	}

	private void pageChanged() {
		boolean empty = false;
		for (Enumeration enum = targetSites.elements();
			enum.hasMoreElements();
			) {
			JobTargetSite jobSite = (JobTargetSite) enum.nextElement();
			if (jobSite.targetSite == null) {
				empty = true;
				break;
			}
			IFeature feature = jobSite.job.getFeature();
			if (feature.isPatch()) {
				// Patches must go together with the features
				// they are patching.
				JobTargetSite patchedSite = findPatchedFeature(feature);
				if (patchedSite != null
					&& jobSite.targetSite != null
					&& patchedSite.targetSite != null
					&& jobSite.targetSite.equals(patchedSite.targetSite)
						== false) {
					setErrorMessage(
						"Patch '"
							+ feature.getLabel()
							+ "' must be installed in the same site as '"
							+ patchedSite.job.getFeature().getLabel()
							+ "'");
					setPageComplete(false);
					return;
				}
			}
		}
		verifyNotEmpty(empty);
	}

	private JobTargetSite findPatchedFeature(IFeature patch) {

		for (Enumeration enum = targetSites.elements();
			enum.hasMoreElements();
			) {
			JobTargetSite jobSite = (JobTargetSite) enum.nextElement();
			IFeature target = jobSite.job.getFeature();
			if (target.equals(patch))
				continue;
			if (UpdateUI.isPatch(target, patch))
				return jobSite;

		}
		return null;
	}
	
	public JobTargetSite [] getTargetSites() {
		JobTargetSite [] sites = new JobTargetSite[jobs.length];
		for (int i=0; i<jobs.length; i++) {
			PendingChange job = jobs[i];
			JobTargetSite jobSite = (JobTargetSite)targetSites.get(job);
			sites[i] = jobSite;
		}
		return sites;
	}

	public IConfiguredSite getTargetSite(PendingChange job) {
		JobTargetSite jobSite = (JobTargetSite)targetSites.get(job);
		if (jobSite!=null) return jobSite.targetSite;
		return null;
	}
}