/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.standalone;

import org.eclipse.core.runtime.*;
import org.eclipse.update.internal.core.*;

/**
 * The application class used to launch standalone update commands.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 */
public class StandaloneUpdateApplication implements IPlatformRunnable {

	public final static Integer EXIT_ERROR = new Integer(1);
	private static boolean loggedException = false;

	/* (non-Javadoc)
	 * @see org.eclipse.core.boot.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		if (args == null)
			return EXIT_ERROR;
		if (args instanceof String[]) {
			String[] params = (String[]) args;
			CmdLineArgs cmdLineArgs = new CmdLineArgs(params);
			ScriptedCommand cmd = cmdLineArgs.getCommand();
			if (cmd == null) {
				System.out.println(Policy.bind("Standalone.cmdFailed", Platform.getLogFileLocation().toOSString())); //$NON-NLS-1$
				return EXIT_ERROR;
			}
			loggedException = false;
			boolean result = cmd.run();
			if (result) {
				if (loggedException) {
					System.out.println(Policy.bind("Standalone.cmdCompleteWithErrors", Platform.getLogFileLocation().toOSString()));//$NON-NLS-1$
				} else {
					System.out.println(Policy.bind("Standalone.cmdOK")); //$NON-NLS-1$
				}
				return IPlatformRunnable.EXIT_OK;
			} else {
				if (loggedException) {
					System.out.println(Policy.bind("Standalone.cmdFailed", Platform.getLogFileLocation().toOSString())); //$NON-NLS-1$
				} else {
					System.out.println(Policy.bind("Standalone.cmdFailedNoLog"));//$NON-NLS-1$
				}
				return EXIT_ERROR;
			}
		}
		return EXIT_ERROR;
	}
	public static void exceptionLogged() {
		loggedException = true;
	}

}
