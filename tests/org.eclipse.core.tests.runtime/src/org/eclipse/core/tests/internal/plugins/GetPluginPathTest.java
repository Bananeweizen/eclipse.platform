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
package org.eclipse.core.tests.internal.plugins;

import java.io.*;
import org.eclipse.core.internal.plugins.PluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.WorkspaceSessionTest;

public class GetPluginPathTest extends WorkspaceSessionTest {
	
public GetPluginPathTest() {
	super(null);
}
public GetPluginPathTest(String name) {
	super(name);
}
public void testGetRelevantStrings() {
	String basePath = null;
	String pluginPath = null;
	String pluginName = "org.eclipse.core.tests.runtime";
	
	pluginPath = ((PluginDescriptor)Platform.getPluginRegistry().getPluginDescriptor(pluginName)).getLocation().concat("Plugin_Testing/plugins.activation.1/");
	int i = pluginPath.indexOf(pluginName);
	if (i != -1)
		basePath = pluginPath.substring (0, i);
	if (basePath == null && pluginPath == null)
		return;
		
	String fileName = System.getProperty("user.dir").concat("/testingTemp.plugin-path");
	File pluginPathFile = new File(fileName);
	DataOutputStream output = null;
	try {
		output = new DataOutputStream(new FileOutputStream(pluginPathFile));
	} catch (IOException ioe) {
		System.out.print ("1.0 IOException encountered on creation of " + fileName);
	}
	PrintWriter printNow = new PrintWriter (output);
	try {
		if (basePath != null)
			printNow.println("base=" + basePath);
		if (pluginPath != null)
			printNow.println("test=" + pluginPath);
	} finally {
		try {
			printNow.flush();
			printNow.close();
			output.close();
		} catch (IOException ioe) {
		}
	}
	pluginPathFile.delete();
}
}

