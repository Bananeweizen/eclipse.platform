package org.eclipse.update.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.update.internal.core.*;

public class JarVerificationDialog extends Dialog {

	protected int _iVerificationResult = 0;
	protected File _file = null;
	protected String _strComponentName = null;
	protected String _strId = null;
	protected String _strProviderName = null;

	protected Button _buttonInstall;
	protected Button _buttonCancel;

	public static boolean COMPONENT_TO_INSTALL = false;
/**
 *
 */
public JarVerificationDialog(Shell shell, String strId, String strComponentName, String strProviderName, File file, int iVerificationResult) {
	super(shell);

	_iVerificationResult = iVerificationResult;
	_file = file;
	_strId = strId;
	_strComponentName = strComponentName;
	_strProviderName = strProviderName;
}
public boolean close() {
	COMPONENT_TO_INSTALL = okToInstall();
	return super.close();
}
/**
 * Add buttons to the dialog's button bar.
 *
 * Subclasses should override.
 *
 * @param parent the button bar composite
 */
protected void createButtonsForButtonBar(Composite parent) {
	createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
}
/**
 * Creates and returns the contents of the upper part 
 * of the dialog (above the button bar).
 *
 * Subclasses should overide.
 *
 * @param the parent composite to contain the dialog area
 * @return the dialog area control
 */
protected Control createDialogArea(Composite compositeParent) {

	getShell().setText(UpdateManagerStrings.getString("S_Verification"));

	// Composite: Client
	//------------------
	Composite compositeClient = new Composite(compositeParent, SWT.NULL);
	GridLayout grid = new GridLayout();
	compositeClient.setLayout(grid);
	compositeClient.setLayoutData(new GridData(GridData.FILL_BOTH));

	// Text: Information
	//------------------
	Text textInformation = new Text(compositeClient, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
	textInformation.setLayoutData(new GridData(GridData.GRAB_VERTICAL | GridData.FILL_HORIZONTAL));

	StringBuffer strb = new StringBuffer();

	switch (_iVerificationResult) {

		case JarVerifier.NOT_SIGNED :
			strb.append(UpdateManagerStrings.getString("S_You_are_about_to_install_an_unsigned_component") + ".");
			strb.append("\n\n");
			strb.append(UpdateManagerStrings.getString("S_This_component_has_not_been_digitally_signed") + ".");
			strb.append("\n");
			strb.append(UpdateManagerStrings.getString("S_The_provider_of_this_component_cannot_be_verified") + ".");
			strb.append("\n");
			strb.append(UpdateManagerStrings.getString("S_Installing_this_component_may_corrupt_your_installation") + ".");
			textInformation.setText(strb.toString());
			break;
			
		case JarVerifier.CORRUPTED :
			strb.append(UpdateManagerStrings.getString("S_The_content_of_this_component_has_been_corrupted") + ".");
			strb.append("\n\n");
			strb.append(UpdateManagerStrings.getString("S_This_component_will_not_be_installed") + ".");
			textInformation.setText(strb.toString());
			break;

		case JarVerifier.INTEGRITY_VERIFIED :
			strb.append(UpdateManagerStrings.getString("S_You_are_about_to_install_a_signed_component") + ".");
			strb.append("\n\n");
			strb.append(UpdateManagerStrings.getString("S_The_certificates_used_to_authenticate_this_component_are_not_recognized") + ".");
			strb.append("\n");
			strb.append(UpdateManagerStrings.getString("S_The_provider_of_this_component_cannot_be_verified") + ".");
			strb.append("\n");
			strb.append(UpdateManagerStrings.getString("S_Installing_this_component_may_corrupt_your_installation") + ".");
			textInformation.setText(strb.toString());
			break;
	}

	// Composite: Information labels
	//------------------------------
	Composite compositeInformation = new Composite(compositeClient, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 0;
	compositeInformation.setLayout(layout);
	compositeInformation.setLayoutData(new GridData(GridData.FILL_BOTH));

	// Label: File name
	//-----------------
	Label label = new Label(compositeInformation, SWT.NULL);
	label.setText(UpdateManagerStrings.getString("S_File_name") + ": ");

	label = new Label(compositeInformation, SWT.NULL);
	label.setText(_file.getName());

	// Component name
	//---------------
	if (_strComponentName != null && _strComponentName.length() > 0) {
		label = new Label(compositeInformation, SWT.NULL);
		label.setText(UpdateManagerStrings.getString("S_Component_name") + ": ");

		label = new Label(compositeInformation, SWT.NULL);
		label.setText(_strComponentName);
	}

	// Component identifier
	//---------------------
	if (_strId != null && _strId.length() > 0) {
		label = new Label(compositeInformation, SWT.NULL);
		label.setText(UpdateManagerStrings.getString("S_Component_identifier") + ": ");

		label = new Label(compositeInformation, SWT.NULL);
		label.setText(_strId);
	}

	// Provider name
	//--------------
	if (_strProviderName != null && _strProviderName.length() > 0) {
		label = new Label(compositeInformation, SWT.NULL);
		label.setText(UpdateManagerStrings.getString("S_Provider") + ": ");

		label = new Label(compositeInformation, SWT.NULL);
		label.setText(_strProviderName);
	}

	if (_iVerificationResult != JarVerifier.CORRUPTED) {

		// Group box
		//----------
		Group group = new Group(compositeClient, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL));

		// Text: Instruction
		//------------------
		Text textInstruction = new Text(group, SWT.MULTI | SWT.READ_ONLY);
		textInstruction.setText(UpdateManagerStrings.getString("S_You_may_choose_to_install_the_component_or_cancel_its_installation"));

		// Radio button: Install
		//----------------------
		_buttonInstall = new Button(group, SWT.RADIO);
		_buttonInstall.setText(UpdateManagerStrings.getString("S_Install_component"));
		_buttonInstall.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Radio button: Cancel installation
		//----------------------------------
		_buttonCancel = new Button(group, SWT.RADIO);
		_buttonCancel.setText(UpdateManagerStrings.getString("S_Cancel_component_installation"));
		_buttonCancel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_buttonCancel.setSelection(true);
	}

	return compositeClient;
}
/**
 * Returns true if the component is to be installed
 * called by the Wizard when Finish is executed.
 */
public boolean okToInstall() {
	return _buttonInstall != null ? _buttonInstall.getSelection() : false;
}
}
