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
package org.eclipse.core.tests.runtime;

import junit.framework.*;
import org.eclipse.core.tests.internal.osgi.PlatformAdminTest;
import org.eclipse.core.tests.runtime.model.ConfigurationElementModelTest;


public class AllTests extends TestCase {
/**
 * AllTests constructor comment.
 * @param name java.lang.String
 */
public AllTests() {
	super(null);
}
/**
 * AllTests constructor comment.
 * @param name java.lang.String
 */
public AllTests(String name) {
	super(name);
}
public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(PathTest.suite());
	suite.addTest(PlatformTest.suite());
	suite.addTest(PreferencesTest.suite());
	suite.addTest(PreferenceExportTest.suite());
	suite.addTest(org.eclipse.core.tests.internal.runtime.AllTests.suite());
	suite.addTest(ConfigurationElementModelTest.suite());
	suite.addTest(IRegistryChangeEventTest.suite());
	suite.addTest(PlatformAdminTest.suite());
//	suite.addTest(org.eclipse.core.tests.internal.plugins.AllTests.suite());
	suite.addTest(org.eclipse.core.tests.internal.registrycache.AllTests.suite());
	suite.addTest(org.eclipse.core.tests.runtime.jobs.AllTests.suite());
	return suite;
}
}
