package org.eclipse.debug.internal.ui.actions;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Opens the launch configuration dialog on a single launch configuration, based
 * on the the launch associated with the selected element.
 */
public class EditLaunchConfigurationAction extends SelectionListenerAction {
	
	private ILaunchConfiguration fConfiguration = null;
	private String fMode =null;

	/**
	 * Constructs a new action.
	 */
	public EditLaunchConfigurationAction() {
		super(""); //$NON-NLS-1$
		setEnabled(false);
	}

	/**
	 * @see org.eclipse.ui.actions.SelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		setLaunchConfiguration(null);
		setMode(null);
		if (selection.size() == 1) {
			Object object = selection.getFirstElement();
			ILaunch launch = null;
			if (object instanceof IAdaptable) {
				launch = (ILaunch)((IAdaptable)object).getAdapter(ILaunch.class); 
			}
			if (launch == null) {
				if (object instanceof ILaunch) {
					launch = (ILaunch)object;
				} else if (object instanceof IDebugElement) {
					launch = ((IDebugElement)object).getLaunch();
				} else if (object instanceof IProcess) {
					launch = ((IProcess)object).getLaunch();
				}
			}
			if (launch != null) {
				ILaunchConfiguration configuration = launch.getLaunchConfiguration();
				if (configuration != null) {
					setLaunchConfiguration(configuration);
					setMode(launch.getLaunchMode());
					setText(configuration.getName() + "..."); //$NON-NLS-1$
					ImageDescriptor descriptor = null;
					try {
						descriptor = DebugPluginImages.getImageDescriptor(configuration.getType().getIdentifier());
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
					setImageDescriptor(descriptor);
				}
			}
		}
		return getLaunchConfiguration() != null;
	}

	protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfiguration = configuration;
	}
	
	protected ILaunchConfiguration getLaunchConfiguration() {
		return fConfiguration;
	}
	
	protected void setMode(String mode) {
		fMode = mode;
	}
	
	protected String getMode() {
		return fMode;
	}
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		LaunchGroupExtension group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(getLaunchConfiguration(), getMode());
		if (group != null) {
			DebugUITools.openLaunchConfigurationDialog(
				DebugUIPlugin.getShell(), getLaunchConfiguration(),
				group.getIdentifier(), null);
		}
	}

}
