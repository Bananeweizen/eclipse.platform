package org.eclipse.debug.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.actions.ExecutionAction;

/**
 * Action to launch the last launch configuration that was succuessfully
 * launched, in run mode. If no configurations have been launched,
 * the launch configuration dialog is opened.
 * </p>
 * <p>
 * This class is not intended to be subclassed. This class may
 * be instantiated.
 * </p>
 * @since 2.0
 */
public final class RunAction extends ExecutionAction /*implements IViewActionDelegate*/ {
	
	public RunAction() {
		//only used as a delegate
	}

	/**
	 * @see ExecutionAction#getMode()
	 */
	protected String getMode() {
		return ILaunchManager.RUN_MODE;
	}

}