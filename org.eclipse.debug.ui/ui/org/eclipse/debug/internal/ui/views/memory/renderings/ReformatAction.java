/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Resize all columns
 * 
 * @since 3.0
 */
public class ReformatAction extends Action {
	
	AbstractTableRendering fRendering;
	
	private static final String PREFIX = "ReformatAction."; //$NON-NLS-1$
	private static final String TITLE = PREFIX + "title"; //$NON-NLS-1$
	
	public ReformatAction(AbstractTableRendering rendering)
	{
		super(DebugUIMessages.getString(TITLE));
		WorkbenchHelp.setHelp(this, IDebugUIConstants.PLUGIN_ID + ".ReformatAction_context"); //$NON-NLS-1$
		fRendering = rendering;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		fRendering.resizeColumnsToPreferredSize();
	}

}
