/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsLaunchConfigurationMessages;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.ui.FileSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

public class AntMainTab extends ExternalToolsMainTab {

	protected Button captureOutputButton;
	private String currentLocation= null;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		try {
			currentLocation= configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
		} catch (CoreException e) {
		}
		updateCaptureOutput(configuration);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		try {
			//has the location changed
			String newLocation= configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
			if (newLocation != null) {
				if (!newLocation.equals(currentLocation)) {
					updateTargetsTab();
					currentLocation= newLocation;
				}
			} else if (currentLocation != null){
				updateTargetsTab();
				currentLocation= newLocation;
			}
		} catch (CoreException e) {
		}
		setAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, configuration, captureOutputButton.getSelection(), true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		WorkbenchHelp.setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);
		mainComposite.setFont(parent.getFont());
		createLocationComponent(mainComposite);
		createWorkDirectoryComponent(mainComposite);
		createArgumentComponent(mainComposite);
		createVerticalSpacer(mainComposite, 2);
		createCaptureOutputComponent(mainComposite);
		Dialog.applyDialogFont(parent);
	}
	
	/**
	 * Creates the controls needed to edit the capture output attribute of an
	 * external tool
	 *
	 * @param parent the composite to create the controls in
	 */
	protected void createCaptureOutputComponent(Composite parent) {
		captureOutputButton = createCheckButton(parent, AntLaunchConfigurationMessages.getString("AntMainTab.Capture_&output_1")); //$NON-NLS-1$
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		captureOutputButton.setLayoutData(data);
		captureOutputButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}
	
	protected void updateCaptureOutput(ILaunchConfiguration configuration) {
		boolean captureOutput= true;
		try {
			captureOutput= configuration.getAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, true);
		} catch (CoreException ce) {
			AntUIPlugin.log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_7"), ce); //$NON-NLS-1$
		}
		captureOutputButton.setSelection(captureOutput);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab#handleWorkspaceLocationButtonSelected()
	 */
	protected void handleWorkspaceLocationButtonSelected() {
		FileSelectionDialog dialog;
		dialog = new FileSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), AntLaunchConfigurationMessages.getString("AntMainTab.&Select_a_build_file__1")); //$NON-NLS-1$
		dialog.setFileFilter("*.xml", true); //$NON-NLS-1$
		dialog.open();
		IStructuredSelection result = dialog.getResult();
		if (result == null) {
			return;
		}
		Object file= result.getFirstElement();
		if (file instanceof IFile) {
			locationField.setText(VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", ((IFile)file).getFullPath().toString())); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab#getWorkingDirectoryLabel()
	 */
	protected String getWorkingDirectoryLabel() {
		return AntLaunchConfigurationMessages.getString("AntMainTab.Base_&Directory__3"); //$NON-NLS-1$
	}
	
	private void updateTargetsTab() {
		//the location has changed...set the targets tab to 
		//need to be recomputed
		ILaunchConfigurationTab[] tabs=  getLaunchConfigurationDialog().getTabs();
		for (int i = 0; i < tabs.length; i++) {
			ILaunchConfigurationTab tab = tabs[i];
			if (tab instanceof AntTargetsTab) {
				((AntTargetsTab)tab).setDirty(true);
				break;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab#getLocationLabel()
	 */
	protected String getLocationLabel() {
		return AntLaunchConfigurationMessages.getString("AntMainTab.6"); //$NON-NLS-1$
	}
}