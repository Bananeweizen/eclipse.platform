package org.eclipse.ui.externaltools.internal.ant.logger;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.externaltools.internal.ant.launchConfigurations.*;

/**
 * Processes task hyperlinks as lines are appended to the console
 */
public class TaskLineTracker implements IConsoleLineTracker {
	
	private IConsole fConsole;

	/**
	 * Constructor for TaskLineTracker.
	 */
	public TaskLineTracker() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public void init(IConsole console) {
		fConsole = console;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		TaskLinkManager.processNewLine(fConsole, line);
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
		TaskLinkManager.dispose(fConsole.getProcess());
		fConsole = null;
	}

}
