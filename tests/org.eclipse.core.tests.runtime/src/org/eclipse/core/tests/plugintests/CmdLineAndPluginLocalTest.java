package org.eclipse.core.tests.plugintests;

/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial test suite
 ******************************************************************************/
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.WorkspaceSessionTest;

public class CmdLineAndPluginLocalTest extends WorkspaceSessionTest {

public CmdLineAndPluginLocalTest() {
	super(null);
}
public CmdLineAndPluginLocalTest(String name) {
	super(name);
}

public void testCmdLineAndPluginLocal() {
	IPluginRegistry registry = InternalPlatform.getPluginRegistry();
	
	// Preferences in the file specified by -pluginCustomization
	// command line parameter and in the plugin state area
	IPluginDescriptor resPlugin = registry.getPluginDescriptor("cmdLineAndPluginLocal");
	Preferences prefs = null;
	try {
		prefs = resPlugin.getPlugin().getPluginPreferences();
	} catch (CoreException ce) {
		fail("0.1 core exception from getPlugin");
	}
	String[] defaultNames = prefs.defaultPropertyNames();
	String[] prefNames = prefs.propertyNames();
	assertTrue("1.0 Three default preferences", defaultNames.length == 3);
	assertTrue("1.1 No explicit preferences", prefNames.length == 0);
	// Do we have the right names for the default preferences?
	boolean foundPluginLocalPreference = false;
	boolean foundCommonPreference = false;
	boolean foundCommandLinePreference = false;
	foundCommonPreference = false;
	for (int i = 0; i < defaultNames.length; i++) {
		if (defaultNames[i].equals("CommandLinePreference"))
			foundCommandLinePreference = true;
		else if (defaultNames[i].equals("commonCmdLineAndPluginLocalPreference"))
			foundCommonPreference = true;
		else if (defaultNames[i].equals("PluginLocalPreference"))
			foundPluginLocalPreference = true;
	}
	assertTrue("1.2 Got right default preference names",
		foundCommandLinePreference && foundCommonPreference && foundPluginLocalPreference);
	// Check preference values
	assertTrue("1.3 PluginLocalPreference value",
		prefs.getString("PluginLocalPreference").equals("From the local plugin directory of the plugin cmdLineAndPluginLocal"));
	assertTrue("1.4 CommandLinePreference value",
		prefs.getString("CommandLinePreference").equals("From the command line specified file via the plugin cmdLineAndPluginLocal"));
	assertTrue("1.5 commonCmdLineAndPluginLocalPreference value",
		prefs.getString("commonCmdLineAndPluginLocalPreference").equals("Common preference from the command line via the plugin cmdLineAndPluginLocal"));
}
}
