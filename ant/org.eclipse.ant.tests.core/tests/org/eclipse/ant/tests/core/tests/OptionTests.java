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
package org.eclipse.ant.tests.core.tests;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class OptionTests extends AbstractAntTest {
	
	protected static final String UNKNOWN_ARG= "Unknown argument: ";
	protected static final String START_OF_HELP= "ant [options] [target [target2 [target3] ...]]";
	protected static final String VERSION= "Apache Ant version 1.5.3 compiled on April 9 2003";
	 
	public OptionTests(String name) {
		super(name);
	}
	
	/**
	 * Tests the "-help" option
	 */
	public void testHelp() throws CoreException {
		run("TestForEcho.xml", new String[]{"-help"});
		assertTrue("One message should have been logged", AntTestChecker.getDefault().getMessagesLoggedCount() == 1);
		assertTrue("Help is incorrect", getLastMessageLogged() != null && getLastMessageLogged().startsWith(START_OF_HELP));
	}
	
	/**
	 * Tests the "-version" option
	 */
	public void testVersion() throws CoreException {
		run("TestForEcho.xml", new String[]{"-version"});
		assertTrue("One message should have been logged", AntTestChecker.getDefault().getMessagesLoggedCount() == 1);
		assertTrue("Version is incorrect", VERSION.equals(getLastMessageLogged()));
	}
	
	/**
	 * Tests the "-projecthelp" option when it will not show (quite mode)
	 */
	public void testProjecthelp() throws CoreException {
		run("TestForEcho.xml", new String[]{"-projecthelp"});
		assertTrue("4 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Subtargets:"));
	}
	
	/**
	 * Tests the "-projecthelp" option when it will not show (quite mode)
	 */
	public void testProjecthelpQuiet() throws CoreException {
		run("TestForEcho.xml", new String[]{"-projecthelp", "-q"});
		assertTrue("no messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 0);
	}
	
	/**
	 * Tests the "-listener" option with a listener that is not an instance of BuildListener
	 */
	public void testListenerBad() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-listener", "java.lang.String"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("java.lang.String which was specified to be a build listener is not an instance of org.apache.tools.ant.BuildListener."));
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false);
	}
	
	/**
	 * Tests passing an unrecognized argument
	 */
	public void testUnknownArg() throws CoreException {	
		run("TestForEcho.xml", new String[]{"-listenr"});
        assertTrue("Unrecognized option message should have been logged before successful build",
					(AntTestChecker.getDefault().getMessagesLoggedCount() == 6)
					 && (getLoggedMessage(5).startsWith(UNKNOWN_ARG))
                     && (getLastMessageLogged().startsWith(BUILD_SUCCESSFUL)));
	}
	
	/**
	 * Tests specifying the -logfile with no arg
	 */
	public void testLogFileWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-logfile"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a log file when using the -log argument", false);
	}
	
	/**
	 * Tests specifying the -logfile
	 */
	public void testLogFile() throws CoreException, IOException {
		run("TestForEcho.xml", new String[]{"-logfile", "TestLogFile.txt"});
		IFile file= checkFileExists("TestLogFile.txt");
		InputStream stream =file.getContents();
		
		InputStreamReader in= null;
		try {		
			in= new InputStreamReader(new BufferedInputStream(stream));
			StringBuffer buffer= new StringBuffer();
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n= in.read(readBuffer);
			}
			assertTrue("File should have started with Buildfile", buffer.toString().startsWith("Buildfile"));
		} finally {
			in.close();
			stream.close();
		}
		
	}
	
	/**
	 * Tests specifying the -logger with no arg
	 */
	public void testLoggerWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-logger"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a classname when using the -logger argument", false);
	}
	
	/**
	 * Tests the "-logger" option with a logger that is not an instance of BuildLogger
	 */
	public void testLoggerBad() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-logger", "java.lang.String"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false);
	}
	
	/**
	 * Tests the "-logger" option with two loggers specified...only one is allowed
	 */
	public void testTwoLoggers() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-logger", "java.lang.String", "-q", "-logger", "java.lang.String"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("As only one logger can be specified", false);
	}
	
	/**
	 * Tests specifying the -listener with no arg
	 */
	public void testListenerWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-listener"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a listeners when using the -listener argument ", false);
	}
	
	/**
	 * Tests specifying the -listener with a class that will not be found
	 */
	public void testListenerClassNotFound() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-listener", "TestBuildListener"});
		} catch (CoreException e) {
			String message= e.getStatus().getException().getMessage();
			assertTrue("Should be ClassNotFoundException", "java.lang.ClassNotFoundException: TestBuildListener".equals(message));
			return;
		}
		assertTrue("A CoreException should have occurred as the listener class will not be found", false);
		
	}
	/**
	 * Tests specifying the -listener option
	 */
	public void testListener() throws CoreException {
		run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER});
		assertSuccessful();
		assertTrue("A listener should have been added named: " + ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
	}
	
	/**
	 * Tests specifying the -listener option multiple times...which is allowed
	 */
	public void testListenerMultiple() throws CoreException {
		run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER, "-listener", ANT_TEST_BUILD_LISTENER});
		assertSuccessful();
		assertTrue("A listener should have been added named: " + ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
		assertTrue("Two listeners should have been added", AntTestChecker.getDefault().getListeners().size() == 2);
	}
	
	/**
	 * Tests specifying the -listener option multiple times, with one missing the arg
	 */
	public void testListenerMultipleWithBad() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER, "-q", "-listener", "-verbose"});
		} catch(CoreException e) {
			//You must specify a listener for all -listener arguments
			return;
		}
		assertTrue("You must specify a listener for all -listener arguments ", false);
	}
	
	/**
	 * Tests specifying the -buildfile with no arg
	 */
	public void testBuildFileWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-buildfile"});
		} catch (CoreException ce) {
			//You must specify a buildfile when using the -buildfile argument
			return;
		}
		assertTrue("You must specify a buildfile when using the -buildfile argument", false);
	}
	
	/**
	 * Tests specifying the -buildfile
	 */
	public void testBuildFile() throws CoreException {
		String buildFileName= getProject().getFolder("buildfiles").getFile("echoing.xml").getLocation().toFile().getAbsolutePath();
		run("TestForEcho.xml", new String[]{"-buildfile", buildFileName}, false, "buildfiles");
		
		assertTrue("Should have been 1 tasks, was: " + AntTestChecker.getDefault().getTaskStartedCount(), AntTestChecker.getDefault().getTaskStartedCount() == 1);
	}
	
	/**
	 * Tests specifying a target at the command line that does not exist.
	 */
	public void testSpecifyBadTargetAsArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"echo2"}, false);
		} catch (CoreException ce) {
			return;
		}
		assertTrue("A core exception should have occurred as the target does not exist", false);
	}
	
	/**
	 * Tests specifying a target at the command line
	 */
	public void testSpecifyTargetAsArg() throws CoreException {
		run("echoing.xml", new String[]{"echo3"}, false);
		assertTrue("3 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 3);
		assertSuccessful();
	}
	
	/**
	 * Tests specifying a target at the command line with other options
	 */
	public void testSpecifyTargetAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo3"}, false);
		assertTrue("4 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
		List messages= AntTestChecker.getDefault().getMessages();
		//ensure that echo3 target executed and only that target
		assertTrue("echo3 target not executed", messages.get(2).equals("echo3"));
		assertSuccessful();
	}
	
	/**
	 * Tests specifying targets at the command line with other options
	 */
	public void testSpecifyTargetsAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo2", "echo3"}, false);
		assertTrue("5 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 5);
		List messages= AntTestChecker.getDefault().getMessages();
		//ensure that echo2 target executed
		assertTrue("echo2 target not executed", messages.get(2).equals("echo2"));
		assertSuccessful();
	}
	
	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	public void testSpecifyTargetAsArgAndQuiet() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo3", "-quiet"}, false);
		assertTrue("2 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 2);
	}
	
	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	public void testMinusD() throws CoreException {
		run("echoing.xml", new String[]{"-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	public void testPropertyFileWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-propertyfile"});
		} catch (CoreException ce) {
			String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a property filename when using the -propertyfile argument"));
			return;
		}
		assertTrue("You must specify a property filename when using the -propertyfile argument", false);
	}
	
	/**
	 * A build should succeed when a property file is not found.
	 * The error is reported and the build continues.
	 */
	public void testPropertyFileFileNotFound() throws CoreException {
		
		run("TestForEcho.xml", new String[]{"-propertyfile", "qq.txt"});
		assertSuccessful();
		String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Message incorrect!: " + msg, msg.startsWith("Could not load property file qq.txt:"));
	}
	
	public void testPropertyFile() throws CoreException {
		run("TestForEcho.xml", new String[]{"-propertyfile", getPropertyFileName()});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	public void testPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		run("echoing.xml", new String[]{"-propertyfile", getPropertyFileName(), "-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	public void testInputHandlerWithNoArg() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a classname when using the -inputhandler argument"));
			return;
		}
		assertTrue("You must specify a classname when using the -inputhandler argument", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with two handlers specified...only one is allowed
	 */
	public void testInputHandlerMultiple() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler", "-q", "-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("Only one input handler class may be specified."));
			return;
		}
		assertTrue("As only one input handler can be specified", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a input handler that is not an instance of InputHandler
	 */
	public void testInputHandlerBad() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "java.lang.StringBuffer"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("The specified input handler class java.lang.StringBuffer does not implement the org.apache.tools.ant.input.InputHandler interface"));
			return;
		}
		assertTrue("Incorrect inputhandler", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a input handler that is not a defined class
	 */
	public void testInputHandlerBad2() throws CoreException {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "ja.lang.StringBuffer"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.startsWith("Unable to instantiate specified input handler class ja.lang.StringBuffer"));
			return;
		}
		assertTrue("Incorrect inputhandler", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a test input handler
	 */
	public void testInputHandler() throws CoreException {
		
		run("input.xml", new String[]{"-inputhandler", "org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler"});
		assertSuccessful();
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("testing handling input requests"));
		
	}
	
	/**
	 * Tests the "-diagnostics" option with no ANT_HOME set
	 * bug 25693
	 */
	public void testDiagnosticsWithNoAntHome() throws CoreException {
		
		run("input.xml", new String[]{"-diagnostics"});
		String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Message incorrect: " + msg, msg.equals("------- Ant diagnostics report -------"));
	}
		
	/**
	 * Tests the "-diagnostics" option with ANT_HOME set
	 * bug 25693
	 */
	public void testDiagnostics() throws CoreException {
		AntCorePlugin.getPlugin().getPreferences().setAntHome(getAntHome());
		try {
			run("input.xml", new String[]{"-diagnostics"});
		} finally {
			restorePreferenceDefaults();
		}
		
		String msg= (String)AntTestChecker.getDefault().getMessages().get(12);
		//msg depends on whether self hosting testing or build testing
		assertTrue("Message incorrect: " + msg, msg.endsWith("org.apache.ant") || msg.endsWith("org.apache.ant_1.5.3"));
	}
	
	/**
	 * Tests the "-quiet" still reports build successful
	 * bug 34488
	 */
	public void testMinusQuiet() throws CoreException {
		run("TestForEcho.xml", new String[]{"-quiet"});
		assertSuccessful();	
	}
}
