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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsImages;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

/**
 * The external tools main tab allows the user to configure primary attributes
 * of external tool launch configurations such as the location, working directory,
 * and arguments.
 */
public abstract class ExternalToolsMainTab extends AbstractLaunchConfigurationTab {
	public final static String FIRST_EDIT = "editedByExternalToolsMainTab"; //$NON-NLS-1$

	protected Text locationField;
	protected Text workDirectoryField;
	protected Button fileLocationButton;
	protected Button workspaceLocationButton;
	protected Button variablesLocationButton;
	protected Button fileWorkingDirectoryButton;
	protected Button workspaceWorkingDirectoryButton;
	protected Button variablesWorkingDirectoryButton;

	protected Text argumentField;
	protected Button argumentVariablesButton;

	protected SelectionAdapter selectionAdapter;
	
	protected boolean fInitializing= false;
	private boolean userEdited= false;

	protected WidgetListener fListener= new WidgetListener();
	
	/**
	 * A listener to update for text modification and widget selection.
	 */
	protected class WidgetListener extends SelectionAdapter implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			if (!fInitializing) {
				setDirty(true);
				userEdited= true;
				updateLaunchConfigurationDialog();
			}
		}
		public void widgetSelected(SelectionEvent e) {
			setDirty(true);
			Object source= e.getSource();
			if (source == workspaceLocationButton) {
				handleWorkspaceLocationButtonSelected();
			} else if (source == fileLocationButton) {
				handleFileLocationButtonSelected();
			} else if (source == workspaceWorkingDirectoryButton) {
				handleWorkspaceWorkingDirectoryButtonSelected();
			} else if (source == fileWorkingDirectoryButton) {
				handleFileWorkingDirectoryButtonSelected();
			} else if (source == argumentVariablesButton) {
				handleVariablesButtonSelected(argumentField);
			} else if (source == variablesLocationButton) {
				handleVariablesButtonSelected(locationField);
			} else if (source == variablesWorkingDirectoryButton) {
				handleVariablesButtonSelected(workDirectoryField);
			}
		}

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		mainComposite.setFont(parent.getFont());
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);

		createLocationComponent(mainComposite);
		createWorkDirectoryComponent(mainComposite);
		createArgumentComponent(mainComposite);
		createVerticalSpacer(mainComposite, 1);
		
		Dialog.applyDialogFont(parent);
	}
	
	/**
	 * Creates the controls needed to edit the location
	 * attribute of an external tool
	 * 
	 * @param group the composite to create the controls in
	 */
	protected void createLocationComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(getLocationLabel());
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayout(layout);
		group.setLayoutData(gridData);
		
		Composite composite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns=1;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		locationField = new Text(composite, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		locationField.setLayoutData(gridData);
		locationField.addModifyListener(fListener);
		
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(parent.getFont());
		
		workspaceLocationButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Browse_Workspace..._3"), null); //$NON-NLS-1$
		workspaceLocationButton.addSelectionListener(fListener);
		fileLocationButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Brows&e_File_System..._4"), null); //$NON-NLS-1$
		fileLocationButton.addSelectionListener(fListener);
		variablesLocationButton = createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.31"), null); //$NON-NLS-1$
		variablesLocationButton.addSelectionListener(fListener);	
	}
	
	/**
	 * Returns the label used for the location widgets. Subclasses may wish to override.
	 */
	protected String getLocationLabel() {
		return ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Location___2"); //$NON-NLS-1$
	}

	/**
	 * Creates the controls needed to edit the working directory
	 * attribute of an external tool
	 * 
	 * @param parent the composite to create the controls in
	 */
	protected void createWorkDirectoryComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(getWorkingDirectoryLabel());
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayout(layout);
		group.setLayoutData(gridData);
		
		Composite composite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		workDirectoryField = new Text(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		workDirectoryField.setLayoutData(data);
		workDirectoryField.addModifyListener(fListener);
		
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(gridData);
		buttonComposite.setFont(parent.getFont());
		
		workspaceWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Browse_Wor&kspace..._6"), null); //$NON-NLS-1$
		workspaceWorkingDirectoryButton.addSelectionListener(fListener);
		fileWorkingDirectoryButton= createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Browse_F&ile_System..._7"), null); //$NON-NLS-1$
		fileWorkingDirectoryButton.addSelectionListener(fListener);
		variablesWorkingDirectoryButton = createPushButton(buttonComposite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.32"), null); //$NON-NLS-1$
		variablesWorkingDirectoryButton.addSelectionListener(fListener);		
	}
	
	/**
	 * Return the String to use as the label for the working directory field.
	 * Subclasses may wish to override.
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
		Group group = new Group(parent, SWT.NONE);
		group.setText(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.&Arguments___1")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		GridData gridData = new GridData(GridData.FILL_BOTH);
		group.setLayout(layout);
		group.setLayoutData(gridData);
		
		Composite composite = new Composite(group, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		composite.setLayout(layout);
		composite.setLayoutData(gridData);
		
		argumentField = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		gridData.heightHint = 30;
		argumentField.setLayoutData(gridData);
		argumentField.addModifyListener(fListener);
		
		argumentVariablesButton= createPushButton(composite, ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Varia&bles..._2"), null); //$NON-NLS-1$
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		argumentVariablesButton.setLayoutData(gridData);
		argumentVariablesButton.addSelectionListener(fListener);

		Label instruction = new Label(composite, SWT.NONE);
		instruction.setText(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.3")); //$NON-NLS-1$
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		instruction.setLayoutData(gridData);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(FIRST_EDIT, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		fInitializing= true;
		updateLocation(configuration);
		updateWorkingDirectory(configuration);
		updateArgument(configuration);
		fInitializing= false;
		setDirty(false);
	}
	
	/**
	 * Updates the working directory widgets to match the state of the given launch
	 * configuration.
	 */
	protected void updateWorkingDirectory(ILaunchConfiguration configuration) {
		String workingDir= ""; //$NON-NLS-1$
		try {
			workingDir= configuration.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_10"), ce); //$NON-NLS-1$
		}
		workDirectoryField.setText(workingDir);
	}
	
	/**
	 * Updates the location widgets to match the state of the given launch
	 * configuration.
	 */
	protected void updateLocation(ILaunchConfiguration configuration) {
		String location= ""; //$NON-NLS-1$
		try {
			location= configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_10"), ce); //$NON-NLS-1$
		}
		locationField.setText(location);
	}

	/**
	 * Updates the argument widgets to match the state of the given launch
	 * configuration.
	 */
	protected void updateArgument(ILaunchConfiguration configuration) {
		String arguments= ""; //$NON-NLS-1$
		try {
			arguments= configuration.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
			ExternalToolsPlugin.getDefault().log(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.Error_reading_configuration_7"), ce); //$NON-NLS-1$
		}
		argumentField.setText(arguments);
	}

	/* (non-Javadoc)
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
		
		if(userEdited) {
			configuration.setAttribute(FIRST_EDIT, (String)null);
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
		boolean newConfig = false;
		try {
			newConfig = launchConfig.getAttribute(FIRST_EDIT, false);
		} catch (CoreException e) {
			//assume false is correct
		}
		return validateLocation(newConfig) && validateWorkDirectory();
	}
	
	/**
	 * Validates the content of the location field.
	 */
	protected boolean validateLocation(boolean newConfig) {
		String value = locationField.getText().trim();
		if (value.length() < 1) {
			if (newConfig) {
				setErrorMessage(null);
				setMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.30"));  //$NON-NLS-1$
			} else {
				setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_cannot_be_empty_18")); //$NON-NLS-1$
				setMessage(null);
			}
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
			if (!newConfig) {
				setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_does_not_exist_19")); //$NON-NLS-1$
			}
			return false;
		}
		if (!file.isFile()) {
			if (!newConfig) {
				setErrorMessage(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsMainTab.External_tool_location_specified_is_not_a_file_20")); //$NON-NLS-1$
			}
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
	
	/**
	 * Prompts the user to choose a location from the filesystem and
	 * sets the location as the full path of the selected file.
	 */
	protected void handleFileLocationButtonSelected() {
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
	
	/**
	 * Returns a new variable expression with the given variable and the given argument.
	 * @see IStringVariableManager#generateVariableExpression(String, String)
	 */
	protected String newVariableExpression(String varName, String arg) {
		return VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(varName, arg);
	}
	
	/**
	 * Prompts the user to choose a working directory from the filesystem.
	 */
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
	 * A variable entry button has been pressed for the given text
	 * field. Prompt the user for a variable and enter the result
	 * in the given field.
	 */
	private void handleVariablesButtonSelected(Text textField) {
		String variable = getVariable();
		if (variable != null) {
			textField.append(variable);
		}
	}

	/**
	 * Prompts the user to choose and configure a variable and returns
	 * the resulting string, suitable to be used as an attribute.
	 */
	private String getVariable() {
		StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
		dialog.open();
		return dialog.getVariableExpression();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return ExternalToolsImages.getImage(IExternalToolConstants.IMG_TAB_MAIN);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
}