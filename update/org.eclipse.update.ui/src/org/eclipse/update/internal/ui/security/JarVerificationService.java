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
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.ui.*;
/**
 *
 */
public class JarVerificationService implements IVerificationListener {

	/*
	 * The JarVerifier is a instance variable
	 * bacause we want to reuse it upon multiple calls
	 */

	/*
	 * the Shell
	 */
	private Shell shell;

	/*
	 * If no shell, create a new shell 
	 */
	public JarVerificationService() {
		this(null);
	}
	
	/*
	 * 
	 */
	public JarVerificationService(Shell aShell) {
		shell = aShell;

		// find the default display and get the active shell
		if (shell == null) {
			final Display disp = Display.getDefault();
			if (disp == null) {
				shell = new Shell(new Display());
			} else {
				disp.syncExec(new Runnable() {
					public void run() {
						shell = disp.getActiveShell();
					}
				});
			}
		}
	}

	/*
	 * 
	 */
	private int openWizard(IVerificationResult result) {
		int code;
		IDialogPage page = new JarVerificationPage(result);
		JarVerificationDialog dialog =
				new JarVerificationDialog(shell,page,result);
		dialog.create();
		dialog.getShell().setSize(600, 500);
		dialog.getShell().setText(UpdateUI.getString("JarVerificationDialog.wtitle"));
		dialog.open();
		if (dialog.getReturnCode() == JarVerificationDialog.OK)
			code = CHOICE_INSTALL_TRUST_ALWAYS;
		else
			code = CHOICE_ABORT;

		return code;

	}

	/*
	 * 
	 */
	public int prompt(final IVerificationResult verificationResult){
		if (!UpdateCore.getPlugin().getPluginPreferences().getBoolean(UpdateCore.P_CHECK_SIGNATURE)) 
			return CHOICE_INSTALL_TRUST_ALWAYS;

		if (verificationResult.alreadySeen()) return CHOICE_INSTALL_TRUST_ALWAYS;

		switch (verificationResult.getVerificationCode()) {
			case IVerificationResult.UNKNOWN_ERROR :
					return CHOICE_ERROR;

			case IVerificationResult.VERIFICATION_CANCELLED:
					return CHOICE_ABORT;

			// cannot verify it: do not prompt user.
			case IVerificationResult.TYPE_ENTRY_UNRECOGNIZED: 
				return CHOICE_INSTALL_TRUST_ALWAYS;				
			
			default :
				{
					final int[] wizardResult = new int[1];					
					shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							wizardResult[0] = openWizard(verificationResult);
						}
					});
					return wizardResult[0];
				}
		}
	}
}
