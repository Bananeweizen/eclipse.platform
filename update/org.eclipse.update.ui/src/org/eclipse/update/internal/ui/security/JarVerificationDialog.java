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
package org.eclipse.update.internal.ui.security;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;

/**
 * 
 */
public class JarVerificationDialog extends TitleAreaDialog {

	private IVerificationResult _VerificationResult = null;
	private IDialogPage _DialogPage;
	private Composite pageContainer;
	private Image defaultImage = null;
	private ImageDescriptor defaultImageDescriptor =
		UpdateUIImages.DESC_INSTALL_WIZ;
	
	/**
	 * Constructor for JarVerificationDialog.
	 * @param parentShell
	 * @param newWizard
	 */
	public JarVerificationDialog(Shell parentShell,IDialogPage dialogPage, IVerificationResult verificationResult) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);		
		_VerificationResult = verificationResult;
		_DialogPage = dialogPage;
		if (dialogPage instanceof JarVerificationPage){
			((JarVerificationPage)_DialogPage).setTitleAreaDialog(this);
		}
	}

	/**
	 * Add buttons to the dialog's button bar.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		if (_VerificationResult.getVerificationCode()
			!= IVerificationResult.TYPE_ENTRY_CORRUPTED) {

			if (_VerificationResult.isFeatureVerification()) {
				createButton(
					parent,
					IDialogConstants.OK_ID,
					UpdateUI.getString("JarVerificationDialog.Install"), //$NON-NLS-1$
					false);
				//$NON-NLS-1$
			} else {
				createButton(
					parent,
					IDialogConstants.OK_ID,
					UpdateUI.getString("JarVerificationDialog.Continue"), //$NON-NLS-1$
					false);
				//$NON-NLS-1$				
			}

			// Radio button: Cancel installation
			//----------------------------------
			createButton(
				parent,
				IDialogConstants.CANCEL_ID,
				UpdateUI.getString("JarVerificationDialog.Cancel"), //$NON-NLS-1$
				true);
			//$NON-NLS-1$							
		} else {
			createButton(
				parent,
				IDialogConstants.CANCEL_ID,
				UpdateUI.getString("JarVerificationDialog.Cancel"), //$NON-NLS-1$
				true);
		}
		getButton(IDialogConstants.CANCEL_ID).setFocus();
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		Composite compositeParent = (Composite)super.createDialogArea(parent);
		setTitleImage(this.getImage());
		setTitle(UpdateUI.getString("JarVerificationDialog.Title")); //$NON-NLS-1$
		
		_DialogPage.createControl(compositeParent);
		pageContainer=(Composite)_DialogPage.getControl();
		GridData gd = new GridData(GridData.FILL_BOTH);
		pageContainer.setLayoutData(gd);
		pageContainer.setFont(parent.getFont());		
		
		// Build the separator line
		Label separator= new Label(compositeParent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		return compositeParent;
	}
	
		/**
	 * @see IDialogPage#getImage()
	 */
	public Image getImage() {
		if (defaultImage == null)
			defaultImage = defaultImageDescriptor.createImage();

		return defaultImage;
	}

	public boolean close() {
		// dispose of image
		if (defaultImage != null) {
			defaultImage.dispose();
			defaultImage = null;
		}
		return super.close();		
	}
}
