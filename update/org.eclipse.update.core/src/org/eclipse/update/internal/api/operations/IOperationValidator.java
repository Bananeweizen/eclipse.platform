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
package org.eclipse.update.internal.api.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;

/**
 * Operation validator
 * @since 3.0
 */
public interface IOperationValidator {

	/**
	 * Called before performing install.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingInstall(IFeature oldFeature, IFeature newFeature);

	/**
	 * Called before performing operation.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingConfig(IFeature feature);
	
	/**
	 * Called before performing operation.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingUnconfig(IFeature feature);
	
	/**
	 * Called before performing operation.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingReplaceVersion(IFeature feature, IFeature anotherFeature);
	
	/**
	 * Called before processing a delta.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validateSessionDelta(
		ISessionDelta delta,
		IFeatureReference[] deltaRefs);

	/**
	 * Called before doing a revert/ restore operation
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingRevert(IInstallConfiguration config);

	/**
	 * Called by the UI before doing a batched processing of
	 * several pending changes.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validatePendingChanges(IInstallFeatureOperation[] jobs);

	/**
	 * Check the current state.
	 * Returns null when status cannot be reported.
	 */
	public IStatus validateCurrentState();
}