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
package org.eclipse.update.internal.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.operations.*;

public class ToggleSiteOperation
	extends Operation
	implements IToggleSiteOperation {

	private IConfiguredSite site;

	public ToggleSiteOperation(IConfiguredSite site) {
		super();
		this.site = site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean execute(
		IProgressMonitor monitor,
		IOperationListener listener)
		throws CoreException {
		if (site == null)
			return false;
		boolean oldValue = site.isEnabled();
		site.setEnabled(!oldValue);
		IStatus status = UpdateManager.getValidator().validateCurrentState();
		if (status != null) {
			// revert
			site.setEnabled(oldValue);
			throw new CoreException(status);
		} else {
			try {
				SiteManager.getLocalSite().save();
				OperationsManager.fireObjectChanged(site, "");
				return true; // will restart
			} catch (CoreException e) {
				//revert
				site.setEnabled(oldValue);
				UpdateManager.logException(e);
				throw e;
			}
		}
	}
}
