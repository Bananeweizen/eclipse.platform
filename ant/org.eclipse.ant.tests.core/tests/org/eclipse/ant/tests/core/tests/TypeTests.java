package org.eclipse.ant.tests.core.tests;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.ant.core.*;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.CoreException;

public class TypeTests extends AbstractAntTest {

	public TypeTests(String name) {
		super(name);
	}

	public void testAddType() throws MalformedURLException, CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		Type newType= new Type();
		String path= getProject().getFolder("lib").getFile("antTestsSupport.jar").getLocation().toFile().getAbsolutePath();
		URL url= new URL("file:" + path);
		newType.setLibrary(url);
		newType.setTypeName("AntTestType");
		newType.setClassName("org.eclipse.ant.tests.core.types.AntTestType");
		prefs.setCustomTypes(new Type[]{newType});
		
		Task newTask= new Task();
		path= getProject().getFolder("lib").getFile("antTestsSupport.jar").getLocation().toFile().getAbsolutePath();
		url= new URL("file:" + path);
		newTask.setLibrary(url);
		newTask.setTaskName("AntTestTaskWithCustomType");
		newTask.setClassName("org.eclipse.ant.tests.core.tasks.AntTestTaskWithCustomType");
		prefs.setCustomTasks(new Task[]{newTask});
		
		run("CustomType.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("Test adding a custom type"));
		assertSuccessful();
	}
	
	/*public void testRemoveType() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTypes(new Type[]{});
		try {
			run("CustomType.xml");
		} catch (CoreException ce) {
			assertTrue("Exception from undefined type is incorrect", ce.getMessage().endsWith("as this is not an Ant bug."));
			return;
		}
		assertTrue("Build should have failed as type no longer defined", false);
		restorePreferenceDefaults();
	}*/
}
