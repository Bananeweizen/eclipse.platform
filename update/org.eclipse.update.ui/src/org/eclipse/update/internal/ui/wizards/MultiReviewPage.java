package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.forms.ActivityConstraints;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;

public class MultiReviewPage extends BannerPage {
	// NL keys
	private static final String KEY_TITLE =
		"MultiInstallWizard.MultiReviewPage.title";
	private static final String KEY_DESC =
		"MultiInstallWizard.MultiReviewPage.desc";
	private static final String KEY_C_TASK =
		"MultiInstallWizard.MultiReviewPage.c.task";
	private static final String KEY_C_FEATURE =
		"MultiInstallWizard.MultiReviewPage.c.feature";
	private static final String KEY_C_VERSION =
		"MultiInstallWizard.MultiReviewPage.c.version";
	private static final String KEY_C_PROVIDER =
		"MultiInstallWizard.MultiReviewPage.c.provider";
	private static final String KEY_COUNTER =
		"MultiInstallWizard.MultiReviewPage.counter";
	private static final String KEY_FILTER_CHECK =
		"MultiInstallWizard.MultiReviewPage.filterCheck";

	private PendingChange[] jobs;
	private Label counterLabel;
	private CheckboxTableViewer tableViewer;
	private IStatus validationStatus;
	private Button statusButton;
	private Button filterCheck;
	private ContainmentFilter filter = new ContainmentFilter();

	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return jobs;
		}
	}

	class JobsLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int column) {
			PendingChange job = (PendingChange) obj;
			IFeature feature = job.getFeature();

			switch (column) {
				case 1 :
					return feature.getLabel();
				case 2 :
					return feature
						.getVersionedIdentifier()
						.getVersion()
						.toString();
				case 3 :
					return feature.getProvider();
				case 0 :
					return getJobName(job);
			}
			return "";
		}
		public Image getColumnImage(Object obj, int column) {
			if (column == 1) {
				PendingChange job = (PendingChange) obj;
				IFeature feature = job.getFeature();
				boolean patch = feature.isPatch();
				UpdateLabelProvider provider =
					UpdateUIPlugin.getDefault().getLabelProvider();
				if (patch)
					return provider.get(UpdateUIPluginImages.DESC_EFIX_OBJ);
				else
					return provider.get(UpdateUIPluginImages.DESC_FEATURE_OBJ);
			}
			return null;
		}
	}

	class ContainmentFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object child) {
			return !isContained((PendingChange) child);
		}
		private boolean isContained(PendingChange job) {
			if (job.getJobType() != PendingChange.INSTALL)
				return false;
			VersionedIdentifier vid = job.getFeature().getVersionedIdentifier();
			Object[] selected = tableViewer.getCheckedElements();
			for (int i = 0; i < selected.length; i++) {
				PendingChange candidate = (PendingChange) selected[i];
				if (candidate.equals(job))
					continue;
				IFeature feature = candidate.getFeature();
				if (includes(feature, vid))
					return true;
			}
			return false;
		}
		private boolean includes(IFeature feature, VersionedIdentifier vid) {
			try {
				IFeatureReference[] irefs =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < irefs.length; i++) {
					IFeatureReference iref = irefs[i];
					IFeature ifeature = iref.getFeature();
					VersionedIdentifier ivid =
						ifeature.getVersionedIdentifier();
					if (ivid.equals(vid))
						return true;
					if (includes(ifeature, vid))
						return true;
				}
			} catch (CoreException e) {
			}
			return false;
		}
	}

	/**
	 * Constructor for ReviewPage
	 */
	public MultiReviewPage(PendingChange[] jobs) {
		super("MultiReview");
		setTitle(UpdateUIPlugin.getResourceString(KEY_TITLE));
		setDescription(UpdateUIPlugin.getResourceString(KEY_DESC));
		this.jobs = orderJobs(jobs);
		UpdateUIPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void dispose() {
		UpdateUIPlugin.getDefault().getLabelProvider().disconnect(this);
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
		label.setText("&You are about to execute the following tasks:");
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		Control table = createTable(client);
		gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);

		Composite buttonContainer = new Composite(client, SWT.NULL);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText("&Select All");
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText("&Deselect All");
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});

		statusButton = new Button(buttonContainer, SWT.PUSH);
		statusButton.setText("&Show Status...");
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		statusButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(statusButton);
		statusButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showStatus();
			}
		});

		counterLabel = new Label(client, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);

		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUIPlugin.getResourceString(KEY_FILTER_CHECK));
		filterCheck.setSelection(true);
		tableViewer.addFilter(filter);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (filterCheck.getSelection())
					tableViewer.addFilter(filter);
				else
					tableViewer.removeFilter(filter);
				pageChanged();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterCheck.setLayoutData(gd);
		pageChanged();

		WorkbenchHelp.setHelp(client, "org.eclipse.update.ui.ReviewPage");
		return client;
	}

	private Control createTable(Composite parent) {
		tableViewer =
			CheckboxTableViewer.newCheckList(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_TASK));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_FEATURE));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_VERSION));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_PROVIDER));

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(30));
		layout.addColumnData(new ColumnWeightData(80, true));
		layout.addColumnData(new ColumnWeightData(30));
		layout.addColumnData(new ColumnWeightData(100, true));

		table.setLayout(layout);

		tableViewer.setContentProvider(new JobsContentProvider());
		tableViewer.setLabelProvider(new JobsLabelProvider());
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				pageChanged();
			}
		});
		tableViewer.setInput(UpdateUIPlugin.getDefault().getUpdateModel());
		tableViewer.setAllChecked(true);
		return table;
	}

	private String getJobName(PendingChange job) {
		switch (job.getJobType()) {
			case PendingChange.INSTALL :
				return "Install";
			case PendingChange.CONFIGURE :
				return "Enable";
			case PendingChange.UNCONFIGURE :
				return "Disable";
			case PendingChange.UNINSTALL :
				return "Uninstall";
		}
		return "?";
	}

	private void pageChanged() {
		Object[] checked = tableViewer.getCheckedElements();
		int totalCount = tableViewer.getTable().getItemCount();
		String total = "" + totalCount;
		String selected = "" + checked.length;
		counterLabel.setText(
			UpdateUIPlugin.getFormattedMessage(
				KEY_COUNTER,
				new String[] { selected, total }));
		if (checked.length > 0) {
			validateSelection();
		} else {
			setErrorMessage(null);
			setPageComplete(false);
			validationStatus = null;
		}
		statusButton.setEnabled(validationStatus != null);
	}

	private void handleSelectAll(boolean select) {
		tableViewer.setAllChecked(select);
		pageChanged();
	}
	
	PendingChange [] orderJobs(PendingChange [] jobs) {
		ArrayList result = new ArrayList();
		PendingChange [] input = new PendingChange [jobs.length];
		System.arraycopy(jobs, 0, input, 0, jobs.length);
		//Add jobs to unconfigure.
		addJobs(input, result, PendingChange.UNCONFIGURE, false);
		//Add jobs to configure.
		addJobs(input, result, PendingChange.CONFIGURE, false);
		//Add regular feature installs
		addJobs(input, result, PendingChange.INSTALL, false);
		//Add patch installs
		addJobs(input, result, PendingChange.INSTALL, true);
		//Add the remainder (only uninstalls)
		addJobs(input, result, -1, false);
		return (PendingChange[])result.toArray(new PendingChange[result.size()]);
	}
	
	private void addJobs(PendingChange [] input, ArrayList result, int type, boolean patch) {
		for (int i=0; i<input.length; i++) {
			PendingChange job = input[i];
			if (job==null) continue;
			boolean match=false;
			if (type == -1) match = true;
			else {
				if (type==job.getJobType()) {
					if (type==PendingChange.INSTALL) {
						if (job.getFeature().isPatch()==patch)
							match=true;
					}
					else match=true;
				}
			}
			if (match) {
				result.add(job);
				input[i] = null;
			}
		}
	}

	public PendingChange[] getSelectedJobs() {
		Object[] selected = tableViewer.getCheckedElements();
		PendingChange[] jobs = new PendingChange[selected.length];
		System.arraycopy(selected, 0, jobs, 0, selected.length);
		return jobs;
	}

	public void validateSelection() {
		PendingChange[] jobs = getSelectedJobs();
		validationStatus = ActivityConstraints.validatePendingChanges(jobs);
		setPageComplete(validationStatus == null);
		String errorMessage = null;

		if (validationStatus != null) {
			errorMessage =
				"Invalid combination - select \"Show Status...\" for details.";
		}
		setErrorMessage(errorMessage);
	}

	private void showStatus() {
		if (validationStatus != null) {
			ErrorDialog.openError(
				UpdateUIPlugin.getActiveWorkbenchShell(),
				null,
				null,
				validationStatus);
		}
	}

	public boolean hasSelectedJobsWithLicenses() {
		Object[] selected = tableViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() != PendingChange.INSTALL)
				continue;
			if (UpdateModel.hasLicense(job))
				return true;
		}
		return false;
	}

	public boolean hasSelectedJobsWithOptionalFeatures() {
		Object[] selected = tableViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() != PendingChange.INSTALL)
				continue;
			if (UpdateModel.hasOptionalFeatures(job.getFeature()))
				return true;
		}
		return false;
	}

	public boolean hasSelectedInstallJobs() {
		Object[] selected = tableViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() == PendingChange.INSTALL)
				return true;
		}
		return false;
	}

	public PendingChange[] getSelectedJobsWithLicenses() {
		Object[] selected = tableViewer.getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() != PendingChange.INSTALL)
				continue;
			if (UpdateModel.hasLicense(job))
				list.add(job);
		}
		return (PendingChange[]) list.toArray(new PendingChange[list.size()]);
	}

	public PendingChange[] getSelectedJobsWithOptionalFeatures() {
		Object[] selected = tableViewer.getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() != PendingChange.INSTALL)
				continue;
			if (UpdateModel.hasOptionalFeatures(job.getFeature()))
				list.add(job);
		}
		return (PendingChange[]) list.toArray(new PendingChange[list.size()]);
	}

	public PendingChange[] getSelectedInstallJobs() {
		Object[] selected = tableViewer.getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (job.getJobType() == PendingChange.INSTALL)
				list.add(job);
		}
		return (PendingChange[]) list.toArray(new PendingChange[list.size()]);
	}
}