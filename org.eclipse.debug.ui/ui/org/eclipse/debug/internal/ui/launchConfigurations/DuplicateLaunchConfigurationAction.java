package org.eclipse.debug.internal.ui.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Duplicates the selected launch configuration.
 */
public class DuplicateLaunchConfigurationAction extends AbstractLaunchConfigurationAction {
	
	/**
	 * Action identifier for IDebugView#getAction(String)
	 */
	public static final String ID_DUPLICATE_ACTION = DebugUIPlugin.getUniqueIdentifier() + ".ID_DUPLICATE_ACTION"; //$NON-NLS-1$

	/**
	 * Constructs an action to duplicate a launch configuration 
	 */
	public DuplicateLaunchConfigurationAction(Viewer viewer) {
		super("&Duplicate", viewer);
	}

	/**
	 * @see AbstractLaunchConfigurationAction#performAction()
	 */
	protected void performAction() {
		ILaunchConfiguration original = (ILaunchConfiguration)getStructuredSelection().getFirstElement();
		String newName = DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(original.getName());
		try {
			ILaunchConfigurationWorkingCopy newWorkingCopy = original.copy(newName);
			newWorkingCopy.doSave();
		} catch (CoreException e) {
			errorDialog(e);
		}
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		return selection.size() == 1 && selection.getFirstElement() instanceof ILaunchConfiguration;
	}

}
