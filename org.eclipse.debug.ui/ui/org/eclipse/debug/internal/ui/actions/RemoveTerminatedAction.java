package org.eclipse.debug.internal.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugViewAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewPart;
 
/**
 * Removes all terminated/detached launches from the
 * active debug view.
 */
public class RemoveTerminatedAction extends ListenerActionDelegate implements ILaunchListener {

	/**
	 * @see ListenerActionDelegate#doHandleDebugEvent(DebugEvent)
	 */	
	protected void doHandleDebugEvent(DebugEvent event) {	
		if (event.getKind() == DebugEvent.TERMINATE) {
			Object source= event.getSource();
			if (source instanceof IDebugTarget) {
				if (((IDebugTarget)source).getLaunch().isTerminated()) {
					getAction().setEnabled(true);
				}
			} else if (source instanceof IProcess) {
				if (((IProcess)source).getLaunch().isTerminated()) {
					getAction().setEnabled(true);
				}
			}
		}
	}

	/**
	 * Removes all of the terminated launches relevant to the
	 * active launch view.
	 */
	public void run() {
		doAction(null);
	}

	/** 
	 * Updates the enabled state of this action to enabled if at
	 * least one launch is terminated and relative to the current perspective.
	 */
	public void update() {
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
		IDebugViewAdapter view = (IDebugViewAdapter)getView().getAdapter(IDebugViewAdapter.class);
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

	/**
	 * @see ControlActionDelegate#doAction(Object)
	 */
	protected void doAction(Object notUsed) {
		Object[] elements = getElements();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof ILaunch) {
				ILaunch launch = (ILaunch)elements[i];
				if (launch.isTerminated()) {
					manager.removeLaunch(launch);
				}
			}
		}
		getAction().setEnabled(false);
	}

	/**
	 * @see ControlActionDelegate#getErrorDialogMessage()
	 */
	protected String getErrorDialogMessage() {
		return null;
	}

	/**
	 * @see ControlActionDelegate#getErrorDialogTitle()
	 */
	protected String getErrorDialogTitle() {
		return null;
	}

	/**
	 * @see ControlActionDelegate#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IDebugHelpContextIds.REMOVE_ACTION;
	}

	/**
	 * @see ControlActionDelegate#getStatusMessage()
	 */
	protected String getStatusMessage() {
		return null;
	}

	/**
	 * @see ControlActionDelegate#getText()
	 */
	protected String getText() {
		return null;
	}

	/**
	 * @see ControlActionDelegate#getToolTipText()
	 */
	protected String getToolTipText() {
		return ActionMessages.getString("RemoveTerminatedAction.Remove_All_Terminated_Launches_2");
	}

	/**
	 * @see ControlActionDelegate#isEnabledFor(Object)
	 */
	public boolean isEnabledFor(Object element) {
		return false;
	}

	/**
	 * @see ControlActionDelegate#setActionImages(IAction)
	 */
	protected void setActionImages(IAction action) {
		action.setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_TERMINATED));
		action.setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_TERMINATED));
		action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_REMOVE_TERMINATED));
	}
	
	/**
	 * RemoveTerminatedAction cares nothing about the current selection
	 */
	public void selectionChanged(IAction action, ISelection s) {
		if (!fInitialized) {
			action.setEnabled(false);
			setAction(action);
			setActionImages(action);
			fInitialized = true;
		}
	}

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
		super.init(view);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}
		
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}
	
	/**
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
	}

	/**
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}

	/**
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		if (getAction().isEnabled()) {
			update();
		}
	}

}

