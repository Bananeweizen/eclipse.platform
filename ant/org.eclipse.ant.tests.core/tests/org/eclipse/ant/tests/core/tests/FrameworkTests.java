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
import org.eclipse.ant.core.Property;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class FrameworkTests extends AbstractAntTest {
	
	public FrameworkTests(String name) {
		super(name);
	}
	
	public void testClasspathOrdering() throws MalformedURLException, CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getProject().getFolder("lib").getFile("classpathOrdering1.jar").getLocation().toFile().getAbsolutePath();
		URL url= new URL("file:" + path);
		
		path= getProject().getFolder("lib").getFile("classpathOrdering2.jar").getLocation().toFile().getAbsolutePath();
		URL url2= new URL("file:" + path);
		
		URL urls[] = prefs.getCustomURLs();
		URL newUrls[] = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url;
		newUrls[urls.length + 1] = url2;
		prefs.setCustomURLs(newUrls);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering1"));
		assertSuccessful();
		
		restorePreferenceDefaults();
		
		urls = prefs.getCustomURLs();
		newUrls = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url2;
		newUrls[urls.length + 1] = url;
		prefs.setCustomURLs(newUrls);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering2"));
		assertSuccessful();
	}
	
	public void testNoDefaultTarget() {
		try {
			run("NoDefault.xml", new String[]{"test"}, false);
		} catch (CoreException e) {
			String msg= e.getMessage();
			assertTrue("Message incorrect: " + msg, msg.equals("Default target 'build' does not exist in this project"));
			return;
		}
		assertTrue("Build files with no default targets should not be accepted", false);
	}
	
	/**
	 * Ensures that tasks like javac work when includeAntRuntime is specified
	 * bug 20857
	 */
	public void testIncludeAntRuntime() throws CoreException {
		run("javac.xml", new String[]{"build","refresh"}, false); //standard compiler
		assertSuccessful();
		IFile classFile= getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class");
		assertTrue("Class file was not generated", classFile.exists());
		run("javac.xml", new String[]{"-Duse.eclipse.compiler=true", "clean", "build", "refresh"}, false); //JDTCompiler
		assertSuccessful();
		classFile= getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class");
		assertTrue("Class file was not generated", classFile.exists());
	}
	
	/**
	 * Tests the properties added using a global property file
	 */
	public void testGlobalPropertyFile() throws CoreException {
		
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[]{path});
		
		run("TestForEcho.xml", new String[]{});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		
		restorePreferenceDefaults();
	}
	
	/**
	 * Tests the properties added using a global property
	 */
	public void testGlobalProperty() throws CoreException {
		
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomProperties(new Property[]{new Property("eclipse.is.cool", "Yep"), new Property("JUnitTest", "true")});
		
		run("TestForEcho.xml", new String[]{});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("JUnitTests should have a value of true", "true".equals(AntTestChecker.getDefault().getUserProperty("JUnitTest")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		
		restorePreferenceDefaults();
	}
	
	public void testGlobalPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[]{path});
		
		run("echoing.xml", new String[]{"-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		restorePreferenceDefaults();
	}
	
	/**
	 * Tests setting ANT_HOME
	 */
	public void testSettingAntHome() throws CoreException {
		try {
			AntCorePlugin.getPlugin().getPreferences().setAntHome(getAntHome());
			run("echoing.xml");
			assertTrue("ANT_HOME not set correctly", getAntHome().equals(System.getProperty("ant.home")));
			AntCorePlugin.getPlugin().getPreferences().setAntHome("");
			run("echoing.xml");
			assertTrue("ANT_HOME not set correctly", null == System.getProperty("ant.home"));
		} finally {
			restorePreferenceDefaults();
		}
		
		
	}
}
