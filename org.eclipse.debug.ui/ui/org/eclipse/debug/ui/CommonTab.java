package org.eclipse.debug.ui;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Common launch configuration tab to specify the location a launch configuration
 * is stored, whether it should appear in the favorites list, and perspective
 * switching for an associated launch.
 * <p>
 * Clients may instantiate this class. This class is not intended to be subclassed.
 * </p>
 * @since 2.0
 */
public class CommonTab extends AbstractLaunchConfigurationTab {
		
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
	 * Label for the run perspective combo box
	 */
	private Label fRunPerspectiveLabel;
	
	/**
	 * The combo box specifying the debug perspective
	 */
	private Combo fDebugPerspectiveCombo;
	
	/**
	 * Label for the debug perspective combo box
	 */
	private Label fDebugPerspectiveLabel;

	/**
	 * The label that acts as header for the 'switch to perspective' widgets
	 */
	private Label fSwitchToLabel;

	/**
	 * Check box list for specifying favorites
	 */
	private CheckboxTableViewer fFavoritesTable;
		
	/**
	 * Constant for the name of the drop-down choice 'None' for perspectives.
	 */
	private static final String PERSPECTIVE_NONE_NAME = LaunchConfigurationsMessages.getString("CommonTab.None"); //$NON-NLS-1$
	
	/**
	 * Constant for the name of the drop-down choice 'Default' for perspectives.
	 */
	private static final String PERSPECTIVE_DEFAULT_NAME = LaunchConfigurationsMessages.getString("CommonTab.Default"); //$NON-NLS-1$
	
	/**
	 * Modify listener that simply updates the owning launch configuration dialog.
	 */
	private ModifyListener fBasicModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
	};
	
	/**
	 * Selection listener that simply updates the owning launch configuration dialog.
	 */
	private SelectionAdapter fBasicSelectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				updateLaunchConfigurationDialog();
			}
	};
	
	/**
	 * @see ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		WorkbenchHelp.setHelp(getControl(), IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		

		createVerticalSpacer(comp, 1);
		
		Composite radioComp = new Composite(comp, SWT.NONE);
		GridLayout radioLayout = new GridLayout();
		radioLayout.marginHeight = 0;
		radioLayout.marginWidth = 0;
		radioComp.setLayout(radioLayout);
		
		setLocalSharedLabel(new Label(radioComp, SWT.NONE));
		getLocalSharedLabel().setText(LaunchConfigurationsMessages.getString("CommonTab.Type_of_launch_configuration__2")); //$NON-NLS-1$
		
		setLocalRadioButton(new Button(radioComp, SWT.RADIO));
		getLocalRadioButton().setText(LaunchConfigurationsMessages.getString("CommonTab.L&ocal_3")); //$NON-NLS-1$
		setSharedRadioButton(new Button(radioComp, SWT.RADIO));
		getSharedRadioButton().setText(LaunchConfigurationsMessages.getString("CommonTab.S&hared_4")); //$NON-NLS-1$
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
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		locationComp.setLayoutData(gd);
		
		setSharedLocationLabel(new Label(locationComp, SWT.NONE));
		getSharedLocationLabel().setText(LaunchConfigurationsMessages.getString("CommonTab.Location_of_shared_confi&guration__5")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		getSharedLocationLabel().setLayoutData(gd);
		
		setSharedLocationText(new Text(locationComp, SWT.SINGLE | SWT.BORDER));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		getSharedLocationText().setLayoutData(gd);
		getSharedLocationText().addModifyListener(fBasicModifyListener);
		
		setSharedLocationButton(createPushButton(locationComp, LaunchConfigurationsMessages.getString("CommonTab.&Browse_6"), null));	 //$NON-NLS-1$
		getSharedLocationButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSharedLocationButtonSelected();
			}
		});	

		getLocalRadioButton().setSelection(true);
		setSharedEnabled(false);

		createVerticalSpacer(comp, 1);
		
		setSwitchToLabel(new Label(comp, SWT.HORIZONTAL | SWT.LEFT));
		getSwitchToLabel().setText(LaunchConfigurationsMessages.getString("CommonTab.Switch_to/Open_perspective_when_launched_in__7")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 3;
		getSwitchToLabel().setLayoutData(gd);
		
		Composite perspComp = new Composite(comp, SWT.NONE);
		GridLayout perspLayout = new GridLayout();
		perspLayout.marginHeight = 0;
		perspLayout.marginWidth = 0;
		perspLayout.numColumns = 2;
		perspComp.setLayout(perspLayout);
		
		setRunPerspectiveLabel(new Label(perspComp, SWT.NONE));
		getRunPerspectiveLabel().setText(LaunchConfigurationsMessages.getString("CommonTab.Run_mode_8")); //$NON-NLS-1$
		
		setRunPerspectiveCombo(new Combo(perspComp, SWT.DROP_DOWN | SWT.READ_ONLY));
		gd = new GridData(GridData.GRAB_HORIZONTAL);
		getRunPerspectiveCombo().setLayoutData(gd);
		fillWithPerspectives(getRunPerspectiveCombo());
		getRunPerspectiveCombo().addModifyListener(fBasicModifyListener);
		
		setDebugPerspectiveLabel(new Label(perspComp, SWT.NONE));
		getDebugPerspectiveLabel().setText(LaunchConfigurationsMessages.getString("CommonTab.Debug_mode_9")); //$NON-NLS-1$
		
		setDebugPerspectiveCombo(new Combo(perspComp, SWT.DROP_DOWN |SWT.READ_ONLY));
		gd = new GridData(GridData.GRAB_HORIZONTAL);
		getDebugPerspectiveCombo().setLayoutData(gd);		
		fillWithPerspectives(getDebugPerspectiveCombo());				
		getDebugPerspectiveCombo().addModifyListener(fBasicModifyListener);
		
		createVerticalSpacer(comp, 1);
		
		Composite favComp = new Composite(comp, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		favComp.setLayoutData(gd);
		GridLayout favLayout = new GridLayout();
		favLayout.marginHeight = 0;
		favLayout.marginWidth = 0;
		favLayout.numColumns = 2;
		favLayout.makeColumnsEqualWidth = true;
		favComp.setLayout(favLayout);
		
		Label favLabel = new Label(favComp, SWT.HORIZONTAL | SWT.LEFT);
		favLabel.setText(LaunchConfigurationsMessages.getString("CommonTab.Display_in_favorites_menu__10")); //$NON-NLS-1$
		gd = new GridData(GridData.BEGINNING);
		gd.horizontalSpan = 2;
		favLabel.setLayoutData(gd);
		
		fFavoritesTable = CheckboxTableViewer.newCheckList(favComp, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Control table = fFavoritesTable.getControl();
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 1;
		table.setLayoutData(gd);
		fFavoritesTable.setContentProvider(new FavoritesContentProvider());
		fFavoritesTable.setLabelProvider(new FavoritesLabelProvider());
		fFavoritesTable.addCheckStateListener(
			new ICheckStateListener() {
				/**
				 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
				 */
				public void checkStateChanged(CheckStateChangedEvent event) {
					updateLaunchConfigurationDialog();
				}
			});
	}

	
	private void setSharedLocationButton(Button sharedLocationButton) {
		this.fSharedLocationButton = sharedLocationButton;
	}

	private Button getSharedLocationButton() {
		return fSharedLocationButton;
	}

	private void setSharedLocationText(Text sharedLocationText) {
		this.fSharedLocationText = sharedLocationText;
	}

	private Text getSharedLocationText() {
		return fSharedLocationText;
	}

	private void setSharedLocationLabel(Label sharedLocationLabel) {
		this.fSharedLocationLabel = sharedLocationLabel;
	}

	private Label getSharedLocationLabel() {
		return fSharedLocationLabel;
	}

	private void setLocalSharedLabel(Label localSharedLabel) {
		fLocalSharedLabel = localSharedLabel;
	}

	private Label getLocalSharedLabel() {
		return fLocalSharedLabel;
	}

 	private void setLocalRadioButton(Button button) {
 		fLocalRadioButton = button;
 	}
 	
 	private Button getLocalRadioButton() {
 		return fLocalRadioButton;
 	} 	
 	
 	private void setSharedRadioButton(Button button) {
 		fSharedRadioButton = button;
 	}
 	
 	private Button getSharedRadioButton() {
 		return fSharedRadioButton;
 	} 	
 	
	/**
	 * Returns the perspective combo assoicated with the
	 * debug perspective button.
	 * 
	 * @return a combo box
	 */
	private Combo getDebugPerspectiveCombo() {
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
	 * Returns the perspective combo assoicated with the
	 * run perspective button.
	 * 
	 * @return a combo box
	 */
	private Combo getRunPerspectiveCombo() {
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

	private void setRunPerspectiveLabel(Label fRunPerspectiveLabel) {
		this.fRunPerspectiveLabel = fRunPerspectiveLabel;
	}

	private Label getRunPerspectiveLabel() {
		return fRunPerspectiveLabel;
	}

	private void setDebugPerspectiveLabel(Label fDebugPerspectiveLabel) {
		this.fDebugPerspectiveLabel = fDebugPerspectiveLabel;
	}

	private Label getDebugPerspectiveLabel() {
		return fDebugPerspectiveLabel;
	}

	private void setSwitchToLabel(Label switchToLabel) {
		fSwitchToLabel = switchToLabel;
	}

	private Label getSwitchToLabel() {
		return fSwitchToLabel;
	}

	private void handleSharedRadioButtonSelected() {
		setSharedEnabled(isShared());
		updateLaunchConfigurationDialog();
	}
	
	private void setSharedEnabled(boolean enable) {
		getSharedLocationLabel().setEnabled(enable);
		getSharedLocationText().setEnabled(enable);
		getSharedLocationButton().setEnabled(enable);
	}
	
	private boolean isShared() {
		return getSharedRadioButton().getSelection();
	}
	
	private void handleSharedLocationButtonSelected() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
																	   getWorkspaceRoot(),
																	   false,
																	   LaunchConfigurationsMessages.getString("CommonTab.Select_a_location_for_the_launch_configuration_13")); //$NON-NLS-1$
		
		String currentContainerString = getSharedLocationText().getText();
		IContainer currentContainer = getContainer(currentContainerString);
		if (currentContainer != null) {
			IPath path = currentContainer.getFullPath();
			dialog.setInitialSelections(new Object[] {path});
		}
		
		dialog.showClosedProjects(false);
		dialog.open();
		Object[] results = dialog.getResult();		
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			String containerName = path.toOSString();
			getSharedLocationText().setText(containerName);
		}		
	}
	
	private IContainer getContainer(String path) {
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
	private IPerspectiveDescriptor getPerspectiveWithLabel(String label) {		
		return PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithLabel(label);
	}
	
	/**
	 * Returns the perspective with the given id, or
	 * <code>null</code> if none is found.
	 * 
	 * @param id perspective identifier
	 * @return perspective descriptor
	 */
	private IPerspectiveDescriptor getPerspectiveWithId(String id) {		
		return PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(id);
	}	

	/**
	 * Fills the given combo box with the labels of all existing
	 * perspectives and one to indicate 'none'.
	 * 
	 * @param combo combo box
	 */
	private void fillWithPerspectives(Combo combo) {
		combo.add(PERSPECTIVE_NONE_NAME);
		combo.add(PERSPECTIVE_DEFAULT_NAME);
		IPerspectiveRegistry reg = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor[] persps = reg.getPerspectives();
		for (int i = 0; i < persps.length; i++) {
			combo.add(persps[i].getLabel());
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {	
		updateLocalSharedFromConfig(configuration);
		updateSharedLocationFromConfig(configuration);
		updateRunPerspectiveFromConfig(configuration);
		updateDebugPerspectiveFromConfig(configuration);
		updateFavoritesFromConfig(configuration);
	}
	
	private void updateLocalSharedFromConfig(ILaunchConfiguration config) {
		boolean isShared = !config.isLocal();
		getSharedRadioButton().setSelection(isShared);
		getLocalRadioButton().setSelection(!isShared);
		setSharedEnabled(isShared);
	}
	
	private void updateSharedLocationFromConfig(ILaunchConfiguration config) {
		IFile file = config.getFile();
		if (file != null) {
			IContainer parent = file.getParent();
			if (parent != null) {
				String containerName = parent.getFullPath().toOSString();
				getSharedLocationText().setText(containerName);
			}
		}
	}
	
	private void updateRunPerspectiveFromConfig(ILaunchConfiguration config) {
		ILaunchConfigurationType type = null;
		String runPerspID = null;
		try {
			runPerspID = config.getAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
			type = config.getType();
		} catch (CoreException ce) {
			updatePerspectiveCombo(getRunPerspectiveCombo(), null);
			getRunPerspectiveCombo().setEnabled(false);
			getRunPerspectiveLabel().setEnabled(false);
			return;
		}
		updatePerspectiveCombo(getRunPerspectiveCombo(), runPerspID);
		boolean enable = type.supportsMode(ILaunchManager.RUN_MODE);
		getRunPerspectiveCombo().setEnabled(enable);
		getRunPerspectiveLabel().setEnabled(enable);
	}
	
	private void updateDebugPerspectiveFromConfig(ILaunchConfiguration config) {
		ILaunchConfigurationType type = null;
		String debugPerspID = null;
		try {			
			debugPerspID = config.getAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);
			type = config.getType();
		} catch (CoreException ce) {
			updatePerspectiveCombo(getDebugPerspectiveCombo(), null);
			getDebugPerspectiveCombo().setEnabled(false);
			getDebugPerspectiveLabel().setEnabled(false);
			return;
		}
		updatePerspectiveCombo(getDebugPerspectiveCombo(), debugPerspID);
		boolean enable = type.supportsMode(ILaunchManager.DEBUG_MODE);
		getDebugPerspectiveCombo().setEnabled(enable);
		getDebugPerspectiveLabel().setEnabled(enable);
	}
	
	private void updateFavoritesFromConfig(ILaunchConfiguration config) {
		fFavoritesTable.setInput(config);
		fFavoritesTable.setCheckedElements(new Object[]{});
		try {
			List groups = config.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, new ArrayList());
			if (groups.isEmpty()) {
				// check old attributes for backwards compatible
				if (config.getAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, false)) {
					groups.add(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
				}
				if (config.getAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, false)) {
					groups.add(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
				}
			}
			if (!groups.isEmpty()) {
				List list = new ArrayList();
				Iterator iterator = groups.iterator();
				while (iterator.hasNext()) {
					String id = (String)iterator.next();
					LaunchGroupExtension extension = getLaunchConfigurationManager().getLaunchGroup(id);
					list.add(extension);
				}
				fFavoritesTable.setCheckedElements(list.toArray());
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
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
	private void updatePerspectiveCombo(Combo combo, String id) {
		if ((id == null) || (id.equals(IDebugUIConstants.PERSPECTIVE_NONE))) {
			combo.setText(PERSPECTIVE_NONE_NAME);
		} else if (id.equals(IDebugUIConstants.PERSPECTIVE_DEFAULT)) {
			combo.setText(PERSPECTIVE_DEFAULT_NAME);
		} else {
			IPerspectiveDescriptor pd = getPerspectiveWithId(id);
			if (pd == null) {
				// perpective does not exist - reset
				updatePerspectiveCombo(combo, null);
			} else {
				combo.setText(pd.getLabel());
			}
		}
	}

	private void updateConfigFromLocalShared(ILaunchConfigurationWorkingCopy config) {
		if (isShared()) {
			String containerPathString = getSharedLocationText().getText();
			IContainer container = (IContainer) getContainer(containerPathString);
			config.setContainer(container);
		} else {
			config.setContainer(null);
		}
	}
	
	/**
	 * Update the run perspective attribute based on current
	 * UI settings.
	 */
	private void updateConfigFromRunPerspective(ILaunchConfigurationWorkingCopy config) {
		String selectedText = getRunPerspectiveCombo().getText();
		String perspID = null;
		if (selectedText.equals(PERSPECTIVE_NONE_NAME)) {
			perspID = IDebugUIConstants.PERSPECTIVE_NONE;
		}
		else if (selectedText.equals(PERSPECTIVE_DEFAULT_NAME)) {
			perspID = IDebugUIConstants.PERSPECTIVE_DEFAULT;
		} else {
			IPerspectiveDescriptor descriptor = getPerspectiveWithLabel(selectedText);
			if (descriptor != null) {
				perspID = descriptor.getId();
			}
		}
		config.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, perspID);
	}
	
	/**
	 * Update the debug perspective attribute based on current
	 * UI settings.
	 */
	private void updateConfigFromDebugPerspective(ILaunchConfigurationWorkingCopy config) {
		String selectedText = getDebugPerspectiveCombo().getText();
		String perspID = null;
		if (selectedText.equals(PERSPECTIVE_NONE_NAME)) {
			perspID = IDebugUIConstants.PERSPECTIVE_NONE;
		}
		else if (selectedText.equals(PERSPECTIVE_DEFAULT_NAME)) {
			perspID = IDebugUIConstants.PERSPECTIVE_DEFAULT;
		} else {
			IPerspectiveDescriptor descriptor = getPerspectiveWithLabel(selectedText);
			if (descriptor != null) {
				perspID = descriptor.getId();
			}
		}
		config.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, perspID);
	}
	
	/**
	 * Update the favorite settings.
	 * 
	 * NOTE: set to NULL instead of false for backwards compatibility
	 *  when comparing if content is equal, since 'false' is default
	 * 	and will be missing for older configs.
	 */
	private void updateConfigFromFavorites(ILaunchConfigurationWorkingCopy config) {
		try {
			Object[] checked = fFavoritesTable.getCheckedElements();
			boolean debug = config.getAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, false);
			boolean run = config.getAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, false);
			if (debug || run) {
				// old attributes
				List groups = new ArrayList();
				int num = 0;
				if (debug) {
					groups.add(getLaunchConfigurationManager().getLaunchGroup(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP));
					num++;
				}
				if (run) {
					num++;
					groups.add(getLaunchConfigurationManager().getLaunchGroup(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP));
				}
				// see if there are any changes
				if (num == checked.length) {
					boolean different = false;
					for (int i = 0; i < checked.length; i++) {
						if (!groups.contains(checked[i])) {
							different = true;
							break;
						}
					}
					if (!different) {
						return;
					}
				}
			} 
			// erase old attributes (if any)
			config.setAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, (String)null);
			config.setAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, (String)null);
			// new attribute
			List groups = null;
			for (int i = 0; i < checked.length; i++) {
				LaunchGroupExtension group = (LaunchGroupExtension)checked[i];
				if (groups == null) {
					groups = new ArrayList();
				}
				groups.add(group.getIdentifier());
			}
			config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, groups);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}		
	}	
	
	/**
	 * Convenience method for getting the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setMessage(null);
		setErrorMessage(null);
		
		return validateLocalShared();		
	}
	
	private boolean validateLocalShared() {
		if (isShared()) {
			String path = fSharedLocationText.getText().trim();
			IContainer container = getContainer(path);
			if (container == null || container.equals(ResourcesPlugin.getWorkspace().getRoot())) {
				setErrorMessage(LaunchConfigurationsMessages.getString("CommonTab.Invalid_shared_configuration_location_14")); //$NON-NLS-1$
				return false;
			} else if (!container.getProject().isOpen()) {
				setErrorMessage(LaunchConfigurationsMessages.getString("CommonTab.Cannot_save_launch_configuration_in_a_closed_project._1")); //$NON-NLS-1$
				return false;				
			}
		}
		
		return true;		
	}

	/**
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setContainer(null);
		
		config.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);

		config.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
	}

	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		updateConfigFromDebugPerspective(configuration);
		updateConfigFromRunPerspective(configuration);
		updateConfigFromLocalShared(configuration);
		updateConfigFromFavorites(configuration);
	}

	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LaunchConfigurationsMessages.getString("CommonTab.&Common_15"); //$NON-NLS-1$
	}
	
	/**
	 * @see ILaunchConfigurationTab#canSave()
	 */
	public boolean canSave() {
		return validateLocalShared();
	}

	/**
	 * @see ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return DebugUITools.getImage(IDebugUIConstants.IMG_PERSPECTIVE_DEBUG);
	}

	class FavoritesContentProvider implements IStructuredContentProvider {
		/**
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			LaunchGroupExtension[] groups = getLaunchConfigurationManager().getLaunchGroups();
			List possibleGroups = new ArrayList();
			ILaunchConfiguration configuration = (ILaunchConfiguration)inputElement;
			for (int i = 0; i < groups.length; i++) {
				LaunchGroupExtension extension = groups[i];
				LaunchHistory history = getLaunchConfigurationManager().getLaunchHistory(extension.getIdentifier());
				if (history != null && history.accepts(configuration)) {
					possibleGroups.add(extension);
				} 
			}
			return possibleGroups.toArray();
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {
		}

	}
	
	class FavoritesLabelProvider implements ITableLabelProvider {
		
		private Map fImages = new HashMap();
		
		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			Image image = (Image)fImages.get(element);
			if (image == null) {
				ImageDescriptor descriptor = ((LaunchGroupExtension)element).getImageDescriptor();
				if (descriptor != null) {
					image = descriptor.createImage();
					fImages.put(element, image);
				}
			}
			return image;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			return ((LaunchGroupExtension)element).getLabel();
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
			Iterator images = fImages.values().iterator();
			while (images.hasNext()) {
				Image image = (Image)images.next();
				image.dispose();
			}
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
		}
		
	}
	
	/**
	 * Convenience accessor
	 */
	protected LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}
}

