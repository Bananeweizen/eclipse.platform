/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import junit.framework.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class FileLocatorTest extends TestCase {

	private final static String searchLocation = "$nl$/intro/messages.properties";

	private final static String nl = "aa_BB"; // make sure we have a stable NL value

	private final static String mostSpecificPath = "/nl/aa/BB/intro/messages.properties";
	private final static String lessSpecificPath = "/nl/aa/intro/messages.properties";
	private final static String nonSpecificPath = "/intro/messages.properties";

	public FileLocatorTest(String name) {
		super(name);
	}

	public void testFileLocatorFind() throws IOException, BundleException {
		Bundle bundle = BundleTestingHelper.installBundle("Plugin", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "fileLocator/testFileLocator");
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
		Bundle fragment = BundleTestingHelper.installBundle("Fragment", RuntimeTestsPlugin.getContext(), RuntimeTestsPlugin.TEST_FILES_ROOT + "fileLocator/testFileLocator.nl");
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {fragment});

		IPath path = new Path(searchLocation);
		Map map = new HashMap(1);
		map.put("$nl$", nl);

		URL oneSolution = FileLocator.find(bundle, path, map);
		assertNotNull(oneSolution);
		assertTrue(oneSolution.getPath().equals(mostSpecificPath));
		assertBundleURL(oneSolution);

		URL[] solutions = FileLocator.findEntries(bundle, path, map);

		// expected:
		// Bundle/nl/aa/BB/intro/messages.properties, 
		// Fragment/nl/aa/BB/intro/messages.properties, 
		// Bundle/nl/aa/intro/messages.properties, 
		// Fragment/nl/aa/intro/messages.properties, 
		// Bundle/121/intro/messages.properties

		assertTrue(solutions.length == 5);

		assertTrue(solutions[0].getPath().equals(mostSpecificPath));
		assertBundleURL(solutions[0]);
		assertTrue(solutions[1].getPath().equals(mostSpecificPath));
		assertFragmentURL(solutions[1]);

		assertTrue(solutions[2].getPath().equals(lessSpecificPath));
		assertBundleURL(solutions[2]);
		assertTrue(solutions[3].getPath().equals(lessSpecificPath));
		assertFragmentURL(solutions[3]);

		assertTrue(solutions[4].getPath().equals(nonSpecificPath));
		assertBundleURL(solutions[4]);

		// remove the first bundle
		fragment.uninstall();
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {fragment});
		bundle.uninstall();
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {bundle});
	}

	private Bundle getHostBundle(URL url) {
		String host = url.getHost();
		Long hostId = Long.decode(host);
		assertNotNull(hostId);
		return RuntimeTestsPlugin.getContext().getBundle(hostId.longValue());
	}

	private void assertBundleURL(URL url) {
		Bundle hostBundle = getHostBundle(url);
		assertNotNull(hostBundle);
		assertTrue(hostBundle.getSymbolicName().equals("fileLocatorTest"));
	}

	private void assertFragmentURL(URL url) {
		Bundle hostBundle = getHostBundle(url);
		assertNotNull(hostBundle);
		assertTrue(hostBundle.getSymbolicName().equals("fileLocatorTest.nl"));
	}

	public static Test suite() {
		TestSuite sameSession = new TestSuite(FileLocatorTest.class.getName());
		sameSession.addTest(new FileLocatorTest("testFileLocatorFind"));
		return sameSession;
	}
}
