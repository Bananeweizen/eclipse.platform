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
package org.eclipse.update.internal.ui.preferences;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.UpdateUI;

/**
 * Insert the type's description here.
 * @see PreferencePage
 */
public class UpdateSettingsPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {
	private Text mappingsFile;

	/**
	 * The constructor.
	 */
	public UpdateSettingsPreferencePage() {
		setDescription(UpdateUI.getString("UpdateSettingsPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * Insert the method's description here.
	 * @see PreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		Label label = new Label(container, SWT.NULL);
		label.setText(UpdateUI.getString("UpdateSettingsPreferencePage.label")); //$NON-NLS-1$
		mappingsFile = new Text(container, SWT.SINGLE | SWT.BORDER);

		initialize();
		mappingsFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textChanged();
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mappingsFile.setLayoutData(gd);
		return container;
	}

	private void initialize() {
		Preferences pref = UpdateCore.getPlugin().getPluginPreferences();
		String text = pref.getString(UpdateUtils.P_MAPPINGS_FILE);
		mappingsFile.setText(text);
		textChanged();
	}

	private void textChanged() {
		String text = mappingsFile.getText();
		if (text.length() > 0) {
			try {
				new URL(text);
			} catch (MalformedURLException e) {
				setValid(false);
				setErrorMessage(UpdateUI.getString("UpdateSettingsPreferencePage.invalid")); //$NON-NLS-1$
				return;
			}
		}
		setValid(true);
		setErrorMessage(null);
	}

	public boolean performOk() {
		Preferences pref = UpdateCore.getPlugin().getPluginPreferences();
		String text = mappingsFile.getText();
		if (text.length() > 0)
			pref.setValue(UpdateUtils.P_MAPPINGS_FILE, text);
		else
			pref.setToDefault(UpdateUtils.P_MAPPINGS_FILE);
		UpdateCore.getPlugin().savePluginPreferences();
		return true;
	}

	protected void performDefaults() {
		mappingsFile.setText(""); //$NON-NLS-1$
		super.performDefaults();
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		/*
		WorkbenchHelp.setHelp(
			parent,"org.eclipse.update.ui.AppServerPreferencePage");
		*/
	}
}