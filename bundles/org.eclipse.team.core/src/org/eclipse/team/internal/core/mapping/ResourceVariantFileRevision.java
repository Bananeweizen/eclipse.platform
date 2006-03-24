/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.core.variants.IResourceVariant;

public class ResourceVariantFileRevision extends FileRevision implements IAdaptable {
	private final IResourceVariant variant;

	protected ResourceVariantFileRevision(IResourceVariant variant) {
		this.variant = variant;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return variant.getStorage(monitor);
	}

	public String getName() {
		return variant.getName();
	}

	public String getContentIdentifier() {
		return variant.getContentIdentifier();
	}

	public IResourceVariant getVariant() {
		return variant;
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return this;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IResourceVariant.class)
			return variant;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}