package org.eclipse.debug.internal.ui.actions;

/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStep;

public class StepReturnActionDelegate extends StepActionDelegate {

	/**
	 * @see StepActionDelegate#checkCapability(IStep)
	 */
	protected boolean checkCapability(IStep element) {
		return element.canStepReturn();
	}

	/**
	 * @see StepActionDelegate#stepAction(IStep)
	 */
	protected void stepAction(IStep element) throws DebugException {
		element.stepReturn();
	}

	/**
	 * @see AbstractDebugActionDelegate#getStatusMessage()
	 */
	protected String getStatusMessage() {
		return ActionMessages.getString("StepReturnActionDelegate.Exceptions_occurred_attempting_to_run_to_return_of_the_frame._2"); //$NON-NLS-1$
	}

	/**
	 * @see AbstractDebugActionDelegate#getErrorDialogMessage()
	 */
	protected String getErrorDialogMessage() {
		return ActionMessages.getString("StepReturnActionDelegate.Run_to_return_failed._1"); //$NON-NLS-1$
	}

	/**
	 * @see AbstractDebugActionDelegate#getErrorDialogTitle()
	 */
	protected String getErrorDialogTitle() {
		return ActionMessages.getString("StepReturnActionDelegate.Run_to_Return_3"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.debug.internal.ui.actions.
	 * StepActionDelegate#getActionDefinitionId()
	 */
	protected String getActionDefinitionId() {
		return "org.eclipse.debug.internal.ui.actions.StepReturnActionDelegate";
	}
}