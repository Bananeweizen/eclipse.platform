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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * ConsoleTerminateAction
 */
public class ConsoleTerminateAction extends Action implements IUpdate {

	private ProcessConsole fConsole;

	/**
	 * Creates a terminate action for the console 
	 */
	public ConsoleTerminateAction(ProcessConsole console) {
		super(ConsoleMessages.getString("ConsoleTerminateAction.0")); //$NON-NLS-1$
		fConsole = console;
		setToolTipText(ConsoleMessages.getString("ConsoleTerminateAction.1")); //$NON-NLS-1$
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));
		update();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		IProcess process = fConsole.getProcess(); 
		setEnabled(process.canTerminate());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		try {
			fConsole.getProcess().terminate();
		} catch (DebugException e) {
			// TODO: report exception
		}
	}

}
