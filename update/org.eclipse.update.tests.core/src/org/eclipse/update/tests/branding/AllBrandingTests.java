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
package org.eclipse.update.tests.branding;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.update.tests.UpdateManagerTestCase;

/**
 * Manages the API tests
 */
public class AllBrandingTests extends UpdateManagerTestCase {
	/**
	 * Constructor
	 */
	public AllBrandingTests(String name) {
		super(name);
	}
	
	/**
	 * List of API tests
	 */
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.setName("Branding Tests");

		suite.addTest(new TestSuite(BundleProviderTest.class));	
		suite.addTest(new TestSuite(ProductTest.class));	

		return suite;
	}
}
