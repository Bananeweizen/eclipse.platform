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
package org.eclipse.debug.internal.ui.console;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.RemoveAllTerminatedAction;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * ConsoleRemoveAllTerminatedAction
 */
public class ConsoleRemoveAllTerminatedAction extends Action implements IUpdate, IDebugEventSetListener {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
	 */
	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			DebugEvent event = events[i];
			Object source = event.getSource();
			if (event.getKind() == DebugEvent.TERMINATE && (source instanceof IDebugTarget || source instanceof IProcess)) {
				DebugUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						update();
					}
				});
			}
		}
		
	}

	public void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (int i = 0; i < launches.length; i++) {
			ILaunch launch = launches[i];
			if (launch.isTerminated()) {
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		RemoveAllTerminatedAction.removeTerminatedLaunches(launches);
	}

	/**
	 * 
	 */
	public ConsoleRemoveAllTerminatedAction() {
		super(ConsoleMessages.getString("ConsoleRemoveAllTerminatedAction.0")); //$NON-NLS-1$
		setToolTipText(ConsoleMessages.getString("ConsoleRemoveAllTerminatedAction.1")); //$NON-NLS-1$
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		DebugPlugin.getDefault().addDebugEventListener(this);
		update();
	}

	
}
