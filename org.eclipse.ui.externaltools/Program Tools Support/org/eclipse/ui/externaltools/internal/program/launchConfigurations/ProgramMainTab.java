package org.eclipse.ui.externaltools.internal.program.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;
import org.eclipse.ui.externaltools.internal.model.ToolUtil;
import org.eclipse.ui.externaltools.internal.ui.FileSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

public class ProgramMainTab extends ExternalToolsMainTab {

	/**
	 * Prompts the user for a program location within the workspace and sets the
	 * location as a String containing the workspace_loc variable or
	 * <code>null</code> if no location was obtained from the user.
	 */
	protected void handleWorkspaceLocationButtonSelected() {
		FileSelectionDialog dialog;
		dialog = new FileSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), ExternalToolsProgramMessages.getString("ProgramMainTab.Select")); //$NON-NLS-1$
		dialog.open();
		IFile file = dialog.getResult();
		if (file == null) {
			return;
		}
		StringBuffer buf = new StringBuffer();
		ToolUtil.buildVariableTag(IExternalToolConstants.VAR_WORKSPACE_LOC, file.getFullPath().toString(), buf);
		String text= buf.toString();
		if (text != null) {
			locationField.setText(text);
		}
	}
	
	
   	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_PROGRAM_MAIN_TAB);
	}
	
}