/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.launchConfigurations;


import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.stringsubstitution.StringVariableSelectionDialog;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsImages;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

public abstract class ExternalToolsMainTab extends AbstractLaunchConfigurationTab {

	protected Text locationField;
	protected Text workDirectoryField;
	protected Button fileLocationButton;
	protected Button workspaceLocationButton;
	protected Button fileWorkingDirectoryButton;
	protected Button workspaceWorkingDirectoryButton;

	protected Text argumentField;
	protected Button variableButton;

	protected SelectionAdapter selectionAdapter;
	
	/**
	 * A listener to update for text modification and widget selection.
	 */
	protected class WidgetListener extends SelectionAdapter implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
		public void widgetSelected(SelectionEvent e) {
			Object source= e.getSource();
			if (source == variableButton) {
				StringVariableSelectionDialog dialog= new StringVariableSelectionDialog(getShell());
				if (dialog.open() == Window.OK) {
					argumentField.insert(dialog.getVariableExpression());
				}
			} else if (source == workspaceLocationButton) {
				handleWorkspaceLocationButtonSelected();
			} else if (source == fileLocationButton) {
				handleLocationButtonSelected();
			} else if (source == workspaceWorkingDirectoryButton) {
				handleWorkspaceWorkingDirectoryButtonSelected();
			} else if (source == fileWorkingDirectoryButton) {
				handleFileWorkingDirectoryButtonSelected();
			}
		}
	}
	
	protected WidgetListener fListener= new WidgetListener();
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);
		mainComposite.setFont(parent.getFont());
		createLocationComponent(mainComposite);
		createWorkDirectoryComponent(mainComposite);
		createArgumentComponent(mainComposite);
		createVerticalSpacer(mainComposite, 2);
	}
	
	/**
	 * Creates the controls needed to edit the location
	 * attribute of an external tool
	 * 
	 * @param parent the composite to create the controls in
	 */
	protected void createLocationComponent(Composite parent) {
		Font font = parent.getFont();
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayout(layout);
		composite.setLayoutData(gridData);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Location___2")); //$NON-NLS-1$
		label.setFont(font);
		
		locationField = new Text(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		locationField.setLayoutData(data);
		locationField.setFont(font);
		
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(font);
		
		createVerticalSpacer(buttonComposite, 1);
		
		workspaceLocationButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Browse_Workspace..._3"), null); //$NON-NLS-1$
		workspaceLocationButton.addSelectionListener(fListener);
		fileLocationButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Brows&e_File_System..._4"), null); //$NON-NLS-1$
		fileLocationButton.addSelectionListener(fListener);
	}
	
	/**
	 * Creates the controls needed to edit the working directory
	 * attribute of an external tool
	 * 
	 * @param parent the composite to create the controls in
	 */
	protected void createWorkDirectoryComponent(Composite parent) {
		Font font = parent.getFont();
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayout(layout);
		composite.setLayoutData(gridData);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(getWorkingDirectoryLabel());
		label.setFont(font);
		
		workDirectoryField = new Text(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		workDirectoryField.setLayoutData(data);
		workDirectoryField.setFont(font);
		
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(font);
		
		createVerticalSpacer(buttonComposite, 1);
		workspaceWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Browse_Wor&kspace..._6"), null); //$NON-NLS-1$
		workspaceWorkingDirectoryButton.addSelectionListener(fListener);
		fileWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Browse_F&ile_System..._7"), null); //$NON-NLS-1$
		fileWorkingDirectoryButton.addSelectionListener(fListener);
	}
	/**
	 * Return the String to use as the label for the working directory field.
	 * @return String
	 */
	protected String getWorkingDirectoryLabel() {
		return ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Working_&Directory__5"); //$NON-NLS-1$
	}
	
	/**
	 * Creates the controls needed to edit the argument and
	 * prompt for argument attributes of an external tool
	 *
	 * @param parent the composite to create the controls in
	 */
	protected void createArgumentComponent(Composite parent) {
		Font font = parent.getFont();

		Label label = new Label(parent, SWT.NONE);
		label.setText(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Arguments___1")); //$NON-NLS-1$
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setFont(font);

		argumentField = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint= 40;
		argumentField.setLayoutData(data);
		argumentField.setFont(font);
		argumentField.addModifyListener(fListener);

		variableButton= createPushButton(parent, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Varia&bles..._2"), null); //$NON-NLS-1$
		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		variableButton.setLayoutData(gridData);
		variableButton.addSelectionListener(fListener);

		Label instruction = new Label(parent, SWT.NONE);
		instruction.setText(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Note__Enclose_an_argument_containing_spaces_using_double-quotes_(__)._Not_applicable_for_variables._3")); //$NON-NLS-1$
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		instruction.setLayoutData(data);
		instruction.setFont(font);
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateLocation(configuration);
		updateWorkingDirectory(configuration);
		updateArgument(configuration);
	}
	
	protected void updateWorkingDirectory(ILaunchConfiguration configuration) {
		String workingDir= ""; //$NON-NLS-1$
		try {
			workingDir= configuration.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_10"), ce); //$NON-NLS-1$
		}
		workDirectoryField.setText(workingDir);
		workDirectoryField.addModifyListener(fListener);
		
	}
	
	protected void updateLocation(ILaunchConfiguration configuration) {
		String location= ""; //$NON-NLS-1$
		try {
			location= configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_10"), ce); //$NON-NLS-1$
		}
		locationField.setText(location);
		locationField.addModifyListener(fListener);
	}

	protected void updateArgument(ILaunchConfiguration configuration) {
		String arguments= ""; //$NON-NLS-1$
		try {
			arguments= configuration.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_7"), ce); //$NON-NLS-1$
		}
		argumentField.setText(arguments);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String location= locationField.getText().trim();
		if (location.length() == 0) {
			configuration.setAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
		} else {
			configuration.setAttribute(IExternalToolConstants.ATTR_LOCATION, location);
		}
		
		String workingDirectory= workDirectoryField.getText().trim();
		if (workingDirectory.length() == 0) {
			configuration.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, (String)null);
		} else {
			configuration.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, workingDirectory);
		}

		String arguments= argumentField.getText().trim();
		if (arguments.length() == 0) {
			configuration.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, (String)null);
		} else {
			configuration.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Main_17"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		return validateLocation() && validateWorkDirectory();
	}
	
	/**
	 * Validates the content of the location field.
	 */
	protected boolean validateLocation() {
		String value = locationField.getText().trim();
		if (value.length() < 1) {
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_cannot_be_empty_18")); //$NON-NLS-1$
			setMessage(null);
			return false;
		}
		
		String location = null;
		try {
			location= getValue(value);
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return false;
		}
		
		
		File file = new File(location);
		if (!file.exists()) { // The file does not exist.
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_does_not_exist_19")); //$NON-NLS-1$
			return false;
		}
		if (!file.isFile()) {
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_specified_is_not_a_file_20")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the value of the given string with all variables substituted (if any).
	 * 
	 * @param expression expression with variables
	 * @return resolved value of expression
	 * @exception CoreException if variable substitution fails
	 */
	private String getValue(String expression) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(expression);
	}

	/**
	 * Validates the content of the working directory field.
	 */
	protected boolean validateWorkDirectory() {
		
		String value = workDirectoryField.getText().trim();
		if (value.length() <= 0) {
			return true;
		}

		String dir = null;
		try {
			dir= getValue(value);
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			return false;
		}
			
		File file = new File(dir);
		if (!file.exists()) { // The directory does not exist.
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_working_directory_does_not_exist_or_is_invalid_21")); //$NON-NLS-1$
			return false;
		}
		if (!file.isDirectory()) {
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Not_a_directory")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	protected void handleLocationButtonSelected() {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
		fileDialog.setFileName(locationField.getText());
		String text= fileDialog.open();
		if (text != null) {
			locationField.setText(text);
		}
	}
	
	/**
	 * Prompts the user for a workspace location within the workspace and sets
	 * the location as a String containing the workspace_loc variable or
	 * <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceLocationButtonSelected() {
		ResourceSelectionDialog dialog;
		dialog = new ResourceSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Select_a_resource_22")); //$NON-NLS-1$
		dialog.open();
		Object[] results = dialog.getResult();
		if (results == null || results.length < 1) {
			return;
		}
		IResource resource = (IResource)results[0];
		locationField.setText(newVariableExpression("workspace_loc", resource.getFullPath().toString())); //$NON-NLS-1$
	}
	
	protected String newVariableExpression(String varName, String arg) {
		return VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(varName, arg);
	}
	
	/**
	 * Prompts the user for a working directory location within the workspace
	 * and sets the working directory as a String containing the workspace_loc
	 * variable or <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceWorkingDirectoryButtonSelected() {
		ContainerSelectionDialog containerDialog;
		containerDialog = new ContainerSelectionDialog(
			getShell(), 
			ResourcesPlugin.getWorkspace().getRoot(),
			false,
			ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.23")); //$NON-NLS-1$
		containerDialog.open();
		Object[] resource = containerDialog.getResult();
		String text= null;
		if (resource != null && resource.length > 0) {
			text= newVariableExpression("workspace_loc", ((IPath)resource[0]).toString()); //$NON-NLS-1$
		}
		if (text != null) {
			workDirectoryField.setText(text);
		}
	}
	
	protected void handleFileWorkingDirectoryButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.23")); //$NON-NLS-1$
		dialog.setFilterPath(workDirectoryField.getText());
		String text= dialog.open();
		if (text != null) {
			workDirectoryField.setText(text);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return ExternalToolsImages.getImage(IExternalToolConstants.IMG_TAB_MAIN);
	}
	
}
