/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.update.internal.ui.model;

import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @version 	1.0
 * @author
 */
public abstract class FeatureAdapter extends UIModelObject implements IFeatureAdapter {
	private boolean included=false;

	/*
	 * @see IFeatureAdapter#getInstallConfiguration()
	 */
	public IInstallConfiguration getInstallConfiguration() {
		return null;
	}
	
	public boolean isIncluded() {
		return included;
	}
	
	protected void setIncluded(boolean included) {
		this.included = included;
	}
	
	public String toString() {
		try {
			IFeature feature = getFeature(null);
			return feature.getLabel();
		}
		catch (CoreException e) {
			return "<failure>";
		}
	}
	public boolean hasIncludedFeatures(IProgressMonitor monitor) {
		try {
			IFeatureReference [] included = getFeature(monitor).getIncludedFeatureReferences();
			return included.length>0;
		}
		catch (CoreException e) {
			return false;
		}
	}
}
