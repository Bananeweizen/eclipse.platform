package org.eclipse.debug.internal.ui.launchConfigurations;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * This tab appears in the LaunchConfigurationDialog for all launch configuration
 * types.  It collects information that governs where the configuration is stored,
 * whether or not it is shared via standard VCM mechanisms, and which perspectives to
 * open/switch to on a run or debug launch.
 */
public class CommonTab implements ILaunchConfigurationTab {

	// Flag that when true, prevents the owning dialog's status area from getting updated.
	// Used when multiple config attributes are getting updated at once.
	private boolean fBatchUpdate = false;
	
	// Local/shared UI widgets
	private Label fLocalSharedLabel;
	private Button fLocalRadioButton;
	private Button fSharedRadioButton;
	
	// Shared location UI widgets
	private Label fSharedLocationLabel;
	private Text fSharedLocationText;
	private Button fSharedLocationButton;
	
	/**
	 * The combo box specifying the run perspective
	 */
	private Combo fRunPerspectiveCombo;
	
	/**
	 * The combo box specifying the debug perspective
	 */
	private Combo fDebugPerspectiveCombo;
	
	/**
	 * The check box specifying whether to use the run perspective
	 */
	private Button fRunPerspectiveButton;
	
	/**
	 * The check box specifying whether to use the debug perspective
	 */
	private Button fDebugPerspectiveButton;	
	
	/**
	 * The label that acts as header for the 'switch to perspective' widgets
	 */
	private Label fSwitchToLabel;
	
	// The launch configuration dialog that owns this tab
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;
	
	// The launch config working copy providing the values shown on this tab
	private ILaunchConfigurationWorkingCopy fWorkingCopy;
	
	//private static final String SHARED_LOCATION_CONTAINER_KEY = "shared_location_container_key";

	protected void setLaunchDialog(ILaunchConfigurationDialog dialog) {
		fLaunchConfigurationDialog = dialog;
	}
	
	protected ILaunchConfigurationDialog getLaunchDialog() {
		return fLaunchConfigurationDialog;
	}
	
	protected void setWorkingCopy(ILaunchConfigurationWorkingCopy workingCopy) {
		fWorkingCopy = workingCopy;
	}
	
	protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}
	
	/**
	 * @see ILaunchConfigurationTab#createTabControl(TabItem)
	 */
	public Control createTabControl(ILaunchConfigurationDialog dialog, TabItem tabItem) {
		setLaunchDialog(dialog);
		
		Composite comp = new Composite(tabItem.getParent(), SWT.NONE);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd;

		createVerticalSpacer(comp);
		
		Composite radioComp = new Composite(comp, SWT.NONE);
		GridLayout radioLayout = new GridLayout();
		radioLayout.marginHeight = 0;
		radioLayout.marginWidth = 0;
		radioComp.setLayout(radioLayout);
		
		setLocalSharedLabel(new Label(radioComp, SWT.NONE));
		getLocalSharedLabel().setText("Type of launch configuration:");
		
		setLocalRadioButton(new Button(radioComp, SWT.RADIO));
		getLocalRadioButton().setText("L&ocal");
		setSharedRadioButton(new Button(radioComp, SWT.RADIO));
		getSharedRadioButton().setText("S&hared");
		getSharedRadioButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSharedRadioButtonSelected();
			}
		});
		
		Composite locationComp = new Composite(comp, SWT.NONE);
		GridLayout locationLayout = new GridLayout();
		locationLayout.numColumns = 2;
		locationLayout.marginHeight = 0;
		locationLayout.marginWidth = 0;
		locationComp.setLayout(locationLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		locationComp.setLayoutData(gd);
		
		setSharedLocationLabel(new Label(locationComp, SWT.NONE));
		getSharedLocationLabel().setText("Location of shared confi&guration:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		getSharedLocationLabel().setLayoutData(gd);
		
		setSharedLocationText(new Text(locationComp, SWT.SINGLE | SWT.BORDER));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		getSharedLocationText().setLayoutData(gd);
		getSharedLocationText().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateConfigFromLocalShared();
			}
		});
		
		setSharedLocationButton(new Button(locationComp, SWT.PUSH));
		getSharedLocationButton().setText("&Browse...");	
		getSharedLocationButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSharedLocationButtonSelected();
			}
		});	

		getLocalRadioButton().setSelection(true);
		setSharedEnabled(false);

		createVerticalSpacer(comp);
		createVerticalSpacer(comp);
		
		setSwitchToLabel(new Label(comp, SWT.HORIZONTAL | SWT.LEFT));
		getSwitchToLabel().setText("Switch to/Open perspective when launched:");
		gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 3;
		getSwitchToLabel().setLayoutData(gd);
		
		Composite perspComp = new Composite(comp, SWT.NONE);
		GridLayout perspLayout = new GridLayout();
		perspLayout.numColumns = 2;
		perspComp.setLayout(perspLayout);
		
		setRunPerspectiveButton(new Button(perspComp, SWT.CHECK));
		getRunPerspectiveButton().setText("R&un Mode:");
		getRunPerspectiveButton().addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateConfigFromRunPerspective();
				}
			}
		);
				
		setRunPerspectiveCombo(new Combo(perspComp, SWT.DROP_DOWN | SWT.READ_ONLY));
		gd = new GridData(GridData.GRAB_HORIZONTAL);
		getRunPerspectiveCombo().setLayoutData(gd);
		getRunPerspectiveCombo().addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateConfigFromRunPerspective();
				}
			}
		);
		fillWithPerspectives(getRunPerspectiveCombo());
		
		setDebugPerspectiveButton(new Button(perspComp, SWT.CHECK));
		getDebugPerspectiveButton().setText("&Debug Mode:");
		getDebugPerspectiveButton().addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateConfigFromDebugPerspective();
				}
			}
		);
		
		setDebugPerspectiveCombo(new Combo(perspComp, SWT.DROP_DOWN |SWT.READ_ONLY));
		gd = new GridData(GridData.GRAB_HORIZONTAL);
		getDebugPerspectiveCombo().setLayoutData(gd);
		getDebugPerspectiveCombo().addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateConfigFromDebugPerspective();
				}
			}
		);		
		fillWithPerspectives(getDebugPerspectiveCombo());				
				
		return comp;
	}

	/**
	 * Create some empty space 
	 */
	protected void createVerticalSpacer(Composite comp) {
		new Label(comp, SWT.NONE);
	}

	protected void setBatchUpdate(boolean update) {
		fBatchUpdate = update;
	}
	
	protected boolean isBatchUpdate() {
		return fBatchUpdate;
	}

	protected void setSharedLocationButton(Button sharedLocationButton) {
		this.fSharedLocationButton = sharedLocationButton;
	}

	protected Button getSharedLocationButton() {
		return fSharedLocationButton;
	}

	protected void setSharedLocationText(Text sharedLocationText) {
		this.fSharedLocationText = sharedLocationText;
	}

	protected Text getSharedLocationText() {
		return fSharedLocationText;
	}

	protected void setSharedLocationLabel(Label sharedLocationLabel) {
		this.fSharedLocationLabel = sharedLocationLabel;
	}

	protected Label getSharedLocationLabel() {
		return fSharedLocationLabel;
	}

	protected void setLocalSharedLabel(Label localSharedLabel) {
		fLocalSharedLabel = localSharedLabel;
	}

	protected Label getLocalSharedLabel() {
		return fLocalSharedLabel;
	}

 	protected void setLocalRadioButton(Button button) {
 		fLocalRadioButton = button;
 	}
 	
 	protected Button getLocalRadioButton() {
 		return fLocalRadioButton;
 	} 	
 	
 	protected void setSharedRadioButton(Button button) {
 		fSharedRadioButton = button;
 	}
 	
 	protected Button getSharedRadioButton() {
 		return fSharedRadioButton;
 	} 	
 	
	/**
	 * Returns the perspective combo assoicated with the
	 * debug perspective button.
	 * 
	 * @return a combo box
	 */
	protected Combo getDebugPerspectiveCombo() {
		return fDebugPerspectiveCombo;
	}

	/**
	 * Sets the perspective combo assoicated with the
	 * debug perspective button.
	 * 
	 * @param combo a combo box
	 */
	private void setDebugPerspectiveCombo(Combo combo) {
		fDebugPerspectiveCombo = combo;
	}

	/**
	 * Returns the check box indicating whether the perspective should
	 * be changed when the configuration is launched in debug mode.
	 * 
	 * @return a check box button
	 */
	protected Button getDebugPerspectiveButton() {
		return fDebugPerspectiveButton;
	}

	/**
	 * Sets the check box indicating whether the perspective should
	 * be changed when the configuration is launched in debug mode.
	 * 
	 * @param button a check box button
	 */
	private void setDebugPerspectiveButton(Button button) {
		fDebugPerspectiveButton = button;
	}

	/**
	 * Returns the perspective combo assoicated with the
	 * run perspective button.
	 * 
	 * @return a combo box
	 */
	protected Combo getRunPerspectiveCombo() {
		return fRunPerspectiveCombo;
	}

	/**
	 * Sets the perspective combo assoicated with the
	 * run perspective button.
	 * 
	 * @param combo a combo box
	 */
	private void setRunPerspectiveCombo(Combo combo) {
		fRunPerspectiveCombo = combo;
	}

	/**
	 * Returns the check box indicating whether the perspective should
	 * be changed when the configuration is launched in run mode.
	 * 
	 * @return a check box button
	 */
	protected Button getRunPerspectiveButton() {
		return fRunPerspectiveButton;
	}

	/**
	 * Sets the check box indicating whether the perspective should
	 * be changed when the configuration is launched in run mode.
	 * 
	 * @param button a check box button
	 */
	private void setRunPerspectiveButton(Button button) {
		fRunPerspectiveButton = button;
	}

	private void setSwitchToLabel(Label switchToLabel) {
		fSwitchToLabel = switchToLabel;
	}

	private Label getSwitchToLabel() {
		return fSwitchToLabel;
	}

	protected void handleSharedRadioButtonSelected() {
		setSharedEnabled(isShared());
		updateConfigFromLocalShared();
	}
	
	protected void setSharedEnabled(boolean enable) {
		getSharedLocationLabel().setEnabled(enable);
		getSharedLocationText().setEnabled(enable);
		getSharedLocationButton().setEnabled(enable);
	}
	
	protected boolean isShared() {
		return getSharedRadioButton().getSelection();
	}
	
	protected void handleSharedLocationButtonSelected() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
																	   getWorkspaceRoot(),
																	   false,
																	   "Select a location for the launch configuration");
		
		String currentContainerString = getSharedLocationText().getText();
		IContainer currentContainer = getContainer(currentContainerString);
		if (currentContainer != null) {
			IPath path = currentContainer.getFullPath();
			dialog.setInitialSelections(new Object[] {path});
		}
		
		
		dialog.open();
		Object[] results = dialog.getResult();		
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			IContainer container = (IContainer) getWorkspaceRoot().findMember(path);
			String containerName = path.toOSString();
			getSharedLocationText().setText(containerName);
		}		
	}
	
	protected IContainer getContainer(String path) {
		Path containerPath = new Path(path);
		return (IContainer) getWorkspaceRoot().findMember(containerPath);
	}
	
	/**
	 * Returns the perspective with the given label, or
	 * <code>null</code> if none is found.
	 * 
	 * @param label perspective label
	 * @return perspective descriptor
	 */
	protected IPerspectiveDescriptor getPerspectiveWithLabel(String label) {		
		return PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithLabel(label);
	}
	
	/**
	 * Returns the perspective with the given id, or
	 * <code>null</code> if none is found.
	 * 
	 * @param id perspective identifier
	 * @return perspective descriptor
	 */
	protected IPerspectiveDescriptor getPerspectiveWithId(String id) {		
		return PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(id);
	}	

	/**
	 * Fills the given combo box with the labels of all existing
	 * perspectives.
	 * 
	 * @param combo combo box
	 */
	protected void fillWithPerspectives(Combo combo) {
		IPerspectiveRegistry reg = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor[] persps = reg.getPerspectives();
		for (int i = 0; i < persps.length; i++) {
			combo.add(persps[i].getLabel());
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#setLaunchConfiguration(ILaunchConfigurationWorkingCopy)
	 */
	public void setLaunchConfiguration(ILaunchConfigurationWorkingCopy launchConfiguration) {
		if (launchConfiguration.equals(getWorkingCopy())) {
			return;
		}
		
		setBatchUpdate(true);
		updateWidgetsFromConfig(launchConfiguration);
		setBatchUpdate(false);

		setWorkingCopy(launchConfiguration);
	}

	/**
	 * Set values for all UI widgets in this tab using values kept in the specified
	 * launch configuration.
	 */
	protected void updateWidgetsFromConfig(ILaunchConfiguration config) {
		updateLocalSharedFromConfig(config);
		updateSharedLocationFromConfig(config);
		updateRunPerspectiveFromConfig(config);
		updateDebugPerspectiveFromConfig(config);
	}
	
	protected void updateLocalSharedFromConfig(ILaunchConfiguration config) {
		boolean isShared = !config.isLocal();
		getSharedRadioButton().setSelection(isShared);
		getLocalRadioButton().setSelection(!isShared);
		setSharedEnabled(isShared);
	}
	
	protected void updateSharedLocationFromConfig(ILaunchConfiguration config) {
		IFile file = config.getFile();
		if (file != null) {
			IContainer parent = file.getParent();
			if (parent != null) {
				String containerName = parent.getFullPath().toOSString();
				getSharedLocationText().setText(containerName);
			}
		}
	}
	
	protected void updateRunPerspectiveFromConfig(ILaunchConfiguration config) {
		String runPerspID = null;
		try {
			runPerspID = config.getAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
		} catch (CoreException ce) {
		}
		updateButtonAndCombo(getRunPerspectiveButton(), getRunPerspectiveCombo(), runPerspID);
	}
	
	protected void updateDebugPerspectiveFromConfig(ILaunchConfiguration config) {
		String debugPerspID = null;
		try {
			debugPerspID = config.getAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);
		} catch (CoreException ce) {
		}
		updateButtonAndCombo(getDebugPerspectiveButton(), getDebugPerspectiveCombo(), debugPerspID);
	}
	
	/**
	 * Based on the given perspective identifier, update the settings
	 * of the button and associated combo box. The check box is selected
	 * when there is a valid perspective, and the combo box is set to
	 * display the label of the associated perspective. The check box is
	 * deselected, and the combo box is set to the default value (debug
	 * perspective) when the identfier is <code>null</code>.
	 * 
	 * @param button check box button
	 * @param combo combo box with perspective labels
	 * @param id perspective identifier or <code>null</code>
	 */
	protected void updateButtonAndCombo(Button button, Combo combo, String id) {
		if (id == null) {
			button.setSelection(false);
			combo.setText(getPerspectiveWithId(IDebugUIConstants.ID_DEBUG_PERSPECTIVE).getLabel());
		} else {
			button.setSelection(true);
			IPerspectiveDescriptor pd = getPerspectiveWithId(id);
			if (pd == null) {
				// perpective does not exist - reset
				updateButtonAndCombo(button, combo, null);
			} else {
				combo.setText(pd.getLabel());
			}
		}
	}

	protected void updateConfigFromLocalShared() {
		if (getWorkingCopy() != null) {
			if (isShared()) {
				String containerPathString = getSharedLocationText().getText();
				IContainer container = (IContainer) getContainer(containerPathString);
				getWorkingCopy().setContainer(container);
			} else {
				getWorkingCopy().setContainer(null);
			}
			refreshStatus();
		}
	}
	
	/**
	 * Update the run perspective attribute based on current
	 * UI settings.
	 */
	protected void updateConfigFromRunPerspective() {
		if (getRunPerspectiveButton().getSelection()) {
			getWorkingCopy().setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE,
				getPerspectiveWithLabel(getRunPerspectiveCombo().getText()).getId());
		} else {
			getWorkingCopy().setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
		}	
		refreshStatus();	
	}
	
	/**
	 * Update the debug perspective attribute based on current
	 * UI settings.
	 */
	protected void updateConfigFromDebugPerspective() {
		if (getDebugPerspectiveButton().getSelection()) {
			getWorkingCopy().setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE,
				getPerspectiveWithLabel(getDebugPerspectiveCombo().getText()).getId());
		} else {
			getWorkingCopy().setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);
		}		
		refreshStatus();	
	}	

	protected void refreshStatus() {
		if (!isBatchUpdate()) {
			getLaunchDialog().refreshStatus();
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}

	/**
	 * Convenience method to get the shell.  It is important that the shell be the one 
	 * associated with the launch configuration dialog, and not the active workbench
	 * window.
	 */
	private Shell getShell() {
		return getLocalSharedLabel().getShell();
	}
	
	/**
	 * Convenience method for getting the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}

