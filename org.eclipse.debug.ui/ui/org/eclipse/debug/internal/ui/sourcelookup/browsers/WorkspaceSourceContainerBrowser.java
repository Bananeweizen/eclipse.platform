/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.swt.widgets.Shell;

/**
 * The browser for creating workspace source containers.
 * 
 * @since 3.0
 */
public class WorkspaceSourceContainerBrowser implements ISourceContainerBrowser {
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser#createSourceContainers(org.eclipse.swt.widgets.Shell)
	 */
	public ISourceContainer[] createSourceContainers(Shell shell) {
		ISourceContainer[] containers = new ISourceContainer[1];
		
		containers[0] = new WorkspaceSourceContainer();
		
		return containers;		
	}
	
}
