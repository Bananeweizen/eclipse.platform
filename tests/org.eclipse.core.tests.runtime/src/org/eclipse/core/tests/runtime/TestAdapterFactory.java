/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime;

import junit.framework.Assert;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 */
public class TestAdapterFactory extends Assert implements IAdapterFactory {
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		assertTrue("Request for wrong adapter", adaptableObject instanceof TestAdaptable);
		return new TestAdapter();
	}

	public Class[] getAdapterList() {
		return new Class[] {TestAdapter.class};
	}
}
