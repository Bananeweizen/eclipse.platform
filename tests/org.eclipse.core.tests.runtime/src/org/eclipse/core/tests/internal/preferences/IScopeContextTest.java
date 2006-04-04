/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class IScopeContextTest extends RuntimeTest {

	public IScopeContextTest() {
		super("");
	}

	public IScopeContextTest(String name) {
		super(name);
	}

	public static Test suite() {
		// all test methods are named "test..."
		return new TestSuite(IScopeContextTest.class);
		//		TestSuite suite = new TestSuite();
		//		suite.addTest(new IScopeContextTest("test"));
		//		return suite;
	}

	public void testGetNode() {
		IScopeContext context = new InstanceScope();

		// null
		try {
			context.getNode(null);
			fail("1.0");
		} catch (IllegalArgumentException e) {
			// expected
		}

		// valid single segment
		String qualifier = Long.toString(System.currentTimeMillis());
		Preferences node = context.getNode(qualifier);
		assertNotNull("2.0", node);
		String expected = "/instance/" + qualifier;
		String actual = node.absolutePath();
		assertEquals("2.1", expected, actual);

		// path
		qualifier = new Path(Long.toString(System.currentTimeMillis())).append("a").toString();
		node = context.getNode(qualifier);
		assertNotNull("3.0", node);
		expected = "/instance/" + qualifier;
		actual = node.absolutePath();
		assertEquals("3.1", expected, actual);
	}
}
