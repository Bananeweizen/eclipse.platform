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
package org.eclipse.update.internal.api.search;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;

/**
 * This class can be added to the update search request
 * to filter out features that do not match the current
 * environment settings.
 * 
 * @see UpdateSearchRequest
 * @see IUpdateSearchFilter
 */
public class EnvironmentFilter extends BaseFilter {
	public boolean accept(IFeature match) {
		return UpdateManagerUtils.isValidEnvironment(match);
	}
}
