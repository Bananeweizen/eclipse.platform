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
package org.eclipse.debug.internal.ui.actions;

 
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Terminates all launches.
 */
public class TerminateAllAction extends AbstractListenerActionDelegate {
	
	protected void doAction(Object element) {
		//not used
	}
	
	protected void update() {
		ILaunchManager lManager= DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches= lManager.getLaunches();
		for (int i= 0; i< launches.length; i++) {
			ILaunch launch= launches[i];
			if (!launch.isTerminated()) {
				getAction().setEnabled(true);
				return;
			}
		}
		getAction().setEnabled(false);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		setAction(action);
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		ILaunchManager lManager= DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches= lManager.getLaunches();
		MultiStatus ms = new MultiStatus(DebugPlugin.getUniqueIdentifier(), 
			DebugException.REQUEST_FAILED, ActionMessages.getString("TerminateAllAction.Terminate_all_failed_3"), null); //$NON-NLS-1$
		for (int i= 0; i < launches.length; i++) {
			ILaunch launch= launches[i];
			if (!launch.isTerminated()) {
				try {
					launch.terminate();
				} catch (DebugException de) {
					ms.merge(de.getStatus());
				}
			}
		}
		if (!ms.isOK()) {
			IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				DebugUIPlugin.errorDialog(window.getShell(), ActionMessages.getString("TerminateAllAction.Terminate_All_2"),ActionMessages.getString("TerminateAllAction.Exceptions_occurred_attempting_to_terminate_all._5") , ms); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				DebugUIPlugin.log(ms);
			}
		}
	}

	protected void doHandleDebugEvent(DebugEvent event) {
		switch (event.getKind()) {
			case DebugEvent.TERMINATE :
				update();
				break;
			case DebugEvent.CREATE :
				update();
				break;
		}
	}		
}
