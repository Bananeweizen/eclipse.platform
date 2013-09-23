/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.testplugin;

import org.eclipse.ant.core.IAntPropertyValueProvider;

public class AntTestPropertyValueProvider implements IAntPropertyValueProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.ant.core.IAntPropertyValueProvider#getAntPropertyValue(java.lang.String)
	 */
	@Override
	public String getAntPropertyValue(String antPropertyName) {
		return "AntTestPropertyValueProvider"; //$NON-NLS-1$
	}

}
