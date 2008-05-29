/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * An action which toggles the breakpoint manager's enablement.
 * This causes debug targets which honor the manager's enablement
 * to skip (not suspend for) all breakpoints. 
 * 
 * This class also implements the window action delegate for the action presented as
 * part of the "Breakpoints" group for the "Run" menu.
 */
public class SkipAllBreakpointsAction extends Action implements IWorkbenchWindowActionDelegate, IBreakpointManagerListener {
	
	//The real action if this is an action delegate
	private IAction fAction;
	
	/**
	 * Workbench part or <code>null</code> if not installed in a part
	 */
	private IWorkbenchPart fPart = null;
	
	public SkipAllBreakpointsAction() {
		super(ActionMessages.SkipAllBreakpointsAction_0); 
		setToolTipText(ActionMessages.SkipAllBreakpointsAction_0); 
		setDescription(ActionMessages.SkipAllBreakpointsAction_2); 
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_SKIP_BREAKPOINTS));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SKIP_ALL_BREAKPOINT_ACTION);
		updateActionCheckedState();
	}
	
	/**
	 * Constructs an action in the given part.
	 * 
	 * @param part 
	 */
	public SkipAllBreakpointsAction(IWorkbenchPart part) {
		this();
		fPart = part;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run(){
		IWorkbenchSiteProgressService progressService = null;
		if (fPart != null) {
			 progressService =  (IWorkbenchSiteProgressService)fPart.getSite().
			 	getAdapter(IWorkbenchSiteProgressService.class);
		}
		Job job = new Job(getText()) {
			protected IStatus run(IProgressMonitor monitor) {
				if (!monitor.isCanceled()) {
					IBreakpointManager bm = getBreakpointManager();
					bm.setEnabled(!bm.isEnabled());
				}
				return Status.OK_STATUS;
			}
		};
		if (progressService != null) {
			progressService.schedule(job);
		} else {
			job.schedule();
		}
	}
	
	/**
	 * Updates the action's checked state to be opposite the enabled
	 * state of the breakpoint manager.
	 */
	public void updateActionCheckedState() {
		if (fAction != null) {
			fAction.setChecked(!getBreakpointManager().isEnabled());
		} else {
			setChecked(!getBreakpointManager().isEnabled());
		}
	}
	
	/**
	 * Returns the global breakpoint manager.
	 * 
	 * @return the global breakpoint manager
	 */
	public static IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		getBreakpointManager().removeBreakpointManagerListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		updateActionCheckedState();
		getBreakpointManager().addBreakpointManagerListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fAction = action;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointManagerListener#breakpointManagerEnablementChanged(boolean)
	 */
	public void breakpointManagerEnablementChanged(boolean enabled) {
		if (fAction != null) {
			fAction.setChecked(!enabled);
		}
	}
}
