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
package org.eclipse.core.tests.plugintests;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.WorkspaceSessionTest;

public class CmdLinePreferencesTest extends WorkspaceSessionTest {
public CmdLinePreferencesTest() {
	super(null);
}
public CmdLinePreferencesTest(String name) {
	super(name);
}

public void testCmdLinePrefsOnly() {
	IPluginRegistry registry = InternalPlatform.getPluginRegistry();
	
	// Check the case where the -pluginCustomization command line
	// parameter has been used.
	IPluginDescriptor resPlugin = registry.getPluginDescriptor("commandLinePluginPreferences");
	Preferences prefs = null;
	try {
		prefs = resPlugin.getPlugin().getPluginPreferences();
	} catch (CoreException ce) {
		fail("0.1 core exception from getPlugin");
	}
	String[] defaultNames = prefs.defaultPropertyNames();
	String[] prefNames = prefs.propertyNames();
	
	assertTrue("1.1 One default preference", defaultNames.length == 1);
	assertTrue("1.2 No explicit preferences", prefNames.length == 0);
	assertTrue("1.3 Preference name", defaultNames[0].equals("CommandLinePreference"));
	assertTrue("1.4 Preference value",
		prefs.getString("CommandLinePreference").equals("From the command line specified file"));
}
}
