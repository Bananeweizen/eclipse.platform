package org.eclipse.debug.internal.ui.actions;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewPart;
 
/**
 * Removes all terminated/detached launches from the
 * active debug view.
 */
public class RemoveAllTerminatedAction extends AbstractRemoveAllActionDelegate implements ILaunchesListener {

	/**
	 * @see ListenerActionDelegate#doHandleDebugEvent(DebugEvent)
	 */	
	protected void doHandleDebugEvent(DebugEvent event) {	
		if (event.getKind() == DebugEvent.TERMINATE) {
			Object source= event.getSource();
			if (source instanceof IDebugTarget) {
				ILaunch launch= ((IDebugTarget)source).getLaunch();
				if (launch.isTerminated() && launchIsRegistered(launch)) {
					getAction().setEnabled(true);
				}
			} else if (source instanceof IProcess) {
				ILaunch launch= ((IProcess)source).getLaunch();
				if (launch.isTerminated() && launchIsRegistered(launch)) {
					getAction().setEnabled(true);
				}
			}
		}
	}

	private boolean launchIsRegistered(ILaunch iLaunch) {
		ILaunch[] launches= DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (int i = 0; i < launches.length; i++) {
			ILaunch launch = launches[i];
			if (launch.equals(iLaunch)) {
				return true;
			}
		}
		return false;
	}

	/** 
	 * Updates the enabled state of this action to enabled if at
	 * least one launch is terminated and relative to the current perspective.
	 */
	protected void update() {
		Object[] elements = getElements();
		if (elements != null) {
			for (int i= 0; i < elements.length; i++) {
				if (elements[i] instanceof ILaunch) {
					ILaunch launch= (ILaunch)elements[i];
					if (launch.isTerminated()) {
						getAction().setEnabled(true);
						return;
					}
				}
			}
		}
		getAction().setEnabled(false);
	}

	/**
	 * Returns the top level elements in the active debug
	 * view, or <code>null</code> if none.
	 * 
	 * @return array of object
	 */
	protected Object[] getElements() {
		IDebugView view = (IDebugView)getView().getAdapter(IDebugView.class);
		if (view != null) {
			Viewer viewer = view.getViewer();
			if (viewer instanceof StructuredViewer) {
				IStructuredContentProvider cp = (IStructuredContentProvider)((StructuredViewer)viewer).getContentProvider();
				Object input = viewer.getInput();
				return cp.getElements(input);
			}
		}
		return null;
	}

	protected void doAction() {
		Object[] elements = getElements();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		List removed = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof ILaunch) {
				ILaunch launch = (ILaunch)elements[i];
				if (launch.isTerminated()) {
					removed.add(launch);
				}
			}
		}
		if (!removed.isEmpty()) {
			manager.removeLaunches((ILaunch[])removed.toArray(new ILaunch[removed.size()]));
		}		
	}

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
		super.init(view);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
		
	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		//removes as a debug event listener
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}
	
	/**
	 * @see ILaunchesListener#launchesAdded(ILaunch[])
	 */
	public void launchesAdded(ILaunch[] launches) {
	}

	/**
	 * @see ILaunchesListener#launchesChanged(ILaunch[])
	 */
	public void launchesChanged(ILaunch[] launches) {
	}

	/**
	 * @see ILaunchesListener#launchesRemoved(ILaunch[])
	 */
	public void launchesRemoved(ILaunch[] launches) {
		if (getAction().isEnabled()) {
			update();
		}
	}
}

