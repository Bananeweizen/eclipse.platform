package org.eclipse.debug.internal.ui.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Common function/behavior for launch configuration view actions
 */
public abstract class AbstractLaunchConfigurationAction extends SelectionListenerAction {
	
	/**
	 * Allows a requestor to abort this action.
	 */
	public interface IConfirmationRequestor {
		/**
		 * Returns whether this action should proceed. Confirmation is requested
		 * when an action is run.
		 * 
		 * @return whether this action should proceed
		 */
		public boolean getConfirmation();
	}
	
	/**
	 * This action's confirmation requestor or <code>null</code> if none
	 */
	private IConfirmationRequestor fConfirmationRequestor;
	
	/**
	 * The viewer this action is working on
	 */
	private Viewer fViewer;

	/**
	 * Constructor for AbstractLaunchConfigurationAction.
	 * @param text
	 */
	public AbstractLaunchConfigurationAction(String text, Viewer viewer) {
		super(text);
		fViewer = viewer;
		fViewer.addSelectionChangedListener(this);
	}

	/**
	 * Returns the shell this action is contained in.
	 * 
	 * @return the shell this action is contained in
	 */
	protected Shell getShell() {
		return getViewer().getControl().getShell();
	}
	
	/**
	 * Returns the viewer this action is working on
	 * 
	 * @return the viewer this action is working on
	 */
	protected Viewer getViewer() {
		return fViewer;
	}
	
	/**
	 * Performs this action once confirmation has been aquired. Subclasses
	 * should override this method.
	 */
	protected abstract void performAction();
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public final void run() {
		if (fConfirmationRequestor != null) {
			if (!fConfirmationRequestor.getConfirmation()) {
				return;
			}
		}
		performAction();
	}
	
	/**
	 * Sets this action's confirmation requestor.
	 * 
	 * @param confirmationRequestor
	 */
	public void setConfirmationRequestor(IConfirmationRequestor confirmationRequestor) {
		fConfirmationRequestor = confirmationRequestor;
	}
	
	/**
	 * Disposes this action
	 */
	public void dispose() {
		fViewer.removeSelectionChangedListener(this);
	}
	
	/**
	 * Show an error dialog on the given exception.
	 * 
	 * @param exception
	 */
	protected void errorDialog(CoreException exception) {
		ErrorDialog.openError(getShell(), null, null, exception.getStatus());
	}

}
