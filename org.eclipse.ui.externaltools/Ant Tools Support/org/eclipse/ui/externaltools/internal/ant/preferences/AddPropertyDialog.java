package org.eclipse.ui.externaltools.internal.ant.preferences;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;
import org.eclipse.ui.help.WorkbenchHelp;

public class AddPropertyDialog extends Dialog {

	private String fName;
	private String fValue;

	private String fTitle;
	
	private Label fNameLabel;
	private Text fNameText;
	private Label fValueLabel;
	private Text fValueText;
	
	private String[] fInitialValues;

	public AddPropertyDialog(Shell shell, String title, String[] initialValues) {
		super(shell);
		fTitle = title;
		fInitialValues= initialValues;
	}

	/**
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NULL);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 2;
		comp.setLayout(topLayout);
		comp.setFont(parent.getFont());
		GridData gd;
		
		fNameLabel = new Label(comp, SWT.NONE);
		fNameLabel.setText(AntPreferencesMessages.getString("AddPropertyDialog.&Name__1")); //$NON-NLS-1$
		fNameLabel.setFont(comp.getFont());
		
		fNameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fNameText.setText(fInitialValues[0]);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fNameText.setLayoutData(gd);
		fNameText.setFont(comp.getFont());
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateButtons();
			}
		});
		
		fValueLabel = new Label(comp, SWT.NONE);
		fValueLabel.setText(AntPreferencesMessages.getString("AddPropertyDialog.&Value__2")); //$NON-NLS-1$
		fValueLabel.setFont(comp.getFont());
		
		fValueText = new Text(comp, SWT.BORDER | SWT.SINGLE);
		fValueText.setText(fInitialValues[1]);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fValueText.setLayoutData(gd);
		fValueText.setFont(comp.getFont());
		fValueText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateButtons();
			}
		});		
		
		return comp;
	}
	
	/**
	 * Return the name/value pair entered in this dialog.  If the cancel button was hit,
	 * both will be <code>null</code>.
	 */
	public String[] getNameValuePair() {
		return new String[] {fName, fValue};
	}
	
	/**
	 * @see Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			fName= fNameText.getText();
			fValue = fValueText.getText();
		} else {
			fName = null;
			fValue = null;
		}
		super.buttonPressed(buttonId);
	}
	
	/**
	 * @see Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (fTitle != null) {
			shell.setText(fTitle);
		}
		WorkbenchHelp.setHelp(shell, IExternalToolsHelpContextIds.ADD_PROPERTY_DIALOG);
	}
	
	/**
	 * Enable the OK button if valid input
	 */
	protected void updateButtons() {
		String name = fNameText.getText().trim();
		String value = fValueText.getText().trim();
		getButton(IDialogConstants.OK_ID).setEnabled((name.length() > 0) &&(value.length() > 0));
	}
	
	/**
	 * Enable the buttons on creation.
	 */
	public void create() {
		super.create();
		updateButtons();
	}
}
