package org.eclipse.ant.tests.core.tests;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.tests.core.AbstractAntTest;

public class TypeTests extends AbstractAntTest {

	public TypeTests(String name) {
		super(name);
	}

	public void testAddType() throws MalformedURLException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		Type newType= new Type();
		String path= getProject().getFolder("lib").getFile("antTestsSupport.jar").getLocation().toFile().getAbsolutePath();
		URL url= new URL("file:" + path);
		newType.setLibrary(url);
		newType.setTypeName("AntTestType");
		newType.setClassName("org.eclipse.ant.tests.core.types.AntTestType");
		prefs.setCustomTypes(new Type[]{newType});
	}

	public void testRemoveType() {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTypes(new Type[0]);
	}
}
