package org.eclipse.debug.ui;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others.
All rights reserved. � This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
�
Contributors:
**********************************************************************/
 
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Common function for launch configuration tabs.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see ILaunchConfigurationTab
 * @since 2.0
 */
public abstract class AbstractLaunchConfigurationTab implements ILaunchConfigurationTab {
	
	/**
	 * The control for this page, or <code>null</code>
	 */
	private Control fControl;

	/**
	 * The launch configuration dialog this tab is
	 * contained in.
	 */
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;
	
	/**
	 * Current error message, or <code>null</code>
	 */
	private String fErrorMessage;
	
	/**
	 * Current message, or <code>null</code>
	 */
	private String fMessage;
		
	/**
	 * Returns the dialog this tab is contained in, or
	 * <code>null</code> if not yet set.
	 * 
	 * @return launch configuration dialog, or <code>null</code>
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fLaunchConfigurationDialog;
	}	
		
	/**
	 * Updates the buttons and message in this page's launch
	 * configuration dialog.
	 */
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			getLaunchConfigurationDialog().updateMessage();
			getLaunchConfigurationDialog().updateButtons();
		}
	}
				
	/**
	 * @see ILaunchConfigurationTab#getControl()
	 */
	public Control getControl() {
		return fControl;
	}

	/**
	 * Sets the control to be displayed in this tab.
	 * 
	 * @param control the control for this tab
	 */
	protected void setControl(Control control) {
		fControl = control;
	}

	/**
	 * @see ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/**
	 * @see ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		return fMessage;
	}

	/**
	 * By default, do nothing.
	 * 
	 * @see ILaunchConfigurationTab#launched(ILaunch)
	 */
	public void launched(ILaunch launch) {
	}

	/**
	 * @see ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
	 */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		fLaunchConfigurationDialog = dialog;
	}
	
	/**
	 * Sets this page's error message, possibly <code>null</code>.
	 * 
	 * @param errorMessage the error message or <code>null</code>
	 */
	protected void setErrorMessage(String errorMessage) {
		fErrorMessage = errorMessage;
	}

	/**
	 * Sets this page's message, possibly <code>null</code>.
	 * 
	 * @param message the message or <code>null</code>
	 */
	protected void setMessage(String message) {
		fMessage = message;
	}
	
	/**
	 * Convenience method to return the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}	
	
	/**
	 * By default, do nothing.
	 * 
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}
	
	/**
	 * Returns the shell this tab is contained in, or <code>null</code>.
	 * 
	 * @return the shell this tab is contained in, or <code>null</code>
	 */
	protected Shell getShell() {
		Control control = getControl();
		if (control != null) {
			return control.getShell();
		}
		return null;
	}
	
	/**
	 * Creates and returns a new push button with the given
	 * label and/or image.
	 * 
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * @param image image of <code>null</code>
	 * 
	 * @return a new push button
	 */
	protected Button createPushButton(Composite parent, String label, Image image) {
		return SWTUtil.createPushButton(parent, label, image);	
	}
	
	/**
	 * Creates and returns a new radio button with the given
	 * label and/or image.
	 * 
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * 
	 * @return a new radio button
	 */
	protected Button createRadioButton(Composite parent, String label) {
		return SWTUtil.createRadioButton(parent, label);	
	}	
	
	/**
	 * @see ILaunchConfigurationTab#canSave()
	 */
	public boolean canSave() {
		return true;
	}
	
	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return true;
	}

	/**
	 * Create some empty space.
	 */
	protected void createVerticalSpacer(Composite comp, int colSpan) {
		Label label = new Label(comp, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = colSpan;
		label.setLayoutData(gd);
	}	
	/**
	 * @see ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/**
	 * Convenience method to set a boolean attribute of on a launch
	 * configuration. If the value being set is the default, the attribute's
	 * value is set to <code>null</code>.
	 * 
	 * @param attribute
	 * @param configuration
	 * @param value
	 * @param defaultValue
	 * @since 2.1
	 */
	protected void setAttribute(String attribute, ILaunchConfigurationWorkingCopy configuration, boolean value, boolean defaultValue) {
		if (value == defaultValue) {
			configuration.setAttribute(attribute, (String)null);
		} else {
			configuration.setAttribute(attribute, value);
		}
	}
}

