/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.tests.resources.saveparticipant;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
/**
 * This class needs to be used with SaveManager2Test. Basically this
 * one builds up an environment in a platform session and the other,
 * running in another session, makes sure the environment is safelly
 * restored.
 *
 * @see SaveManager2Test
 * @see SaveManager3Test
 */
public class SaveManager1Test extends SaveManagerTest {
/**
 * Need a zero argument constructor to satisfy the test harness.
 * This constructor should not do any real work nor should it be
 * called by user code.
 */
public SaveManager1Test() {
}
public SaveManager1Test(String name) {
	super(name);
}
// copy and paste into the scrapbook
public static void doIt() throws Exception {
	String[] testIds = { 
	"saveparticipant.SaveManager1Test", 
	"saveparticipant.SaveManager2Test", 
	"saveparticipant.SaveManager3Test"};
	for (int i = 0; i < testIds.length; i++) {
		Process p = Runtime.getRuntime().exec(new String[] {
		"java", "org.eclipse.core.tests.harness.launcher.Main", 
		"-test", testIds[i],
		"-platform", "c:/temp/save_manager",
		(i < (testIds.length-1) ? "-nocleanup" : "") });
		p.waitFor();
		java.io.InputStream input = p.getInputStream();
		int c;
		while ((c = input.read()) != -1)
			System.out.print((char) c);
		input.close();
		input = p.getErrorStream();
		while ((c = input.read()) != -1)
			System.out.print((char) c);
		input.close();
	}
	System.exit(-1);
}
public static Test suite() {
	// we do not add the whole class because the order is important
	TestSuite suite = new TestSuite();
	suite.addTest(new SaveManager1Test("testSaveEmptyWorkspace"));
	suite.addTest(new SaveManager1Test("testCreateMyProject"));
	suite.addTest(new SaveManager1Test("testCreateProject2"));
	suite.addTest(new SaveManager1Test("testAddSaveParticipant"));
	suite.addTest(new SaveManager1Test("testBuilder"));
	suite.addTest(new SaveManager1Test("testSaveWorkspace"));
	suite.addTest(new SaveManager1Test("testPostSave"));
	return suite;
}
public void testAddSaveParticipant() {
	// get plugin
	IPluginDescriptor descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_1);
	SaveParticipant1Plugin plugin1 = null;
	try {
		plugin1 = (SaveParticipant1Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", plugin1 != null);

	//	prepare plugin to the save operation
	plugin1.resetDeltaVerifier();
	IStatus status;
	try {
		status = plugin1.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			fail("1.0");
		}
	} catch (CoreException e) {
		fail("1.1", e);
	}
	plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

	// SaveParticipant2Plugin
	descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_2);
	SaveParticipant2Plugin plugin2 = null;
	try {
		plugin2 = (SaveParticipant2Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("5.0", e);
	}
	assertTrue("5.1", plugin2 != null);

	//	prepare plugin to the save operation
	plugin2.getDeltaVerifier().reset();
	try {
		status = plugin2.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			fail("6.0");
		}
	} catch (CoreException e) {
		fail("6.1", e);
	}
	plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

	// SaveParticipant3Plugin
	descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_3);
	SaveParticipant3Plugin plugin3 = null;
	try {
		plugin3 = (SaveParticipant3Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("7.0", e);
	}
	assertTrue("7.1", plugin3 != null);
	try {
		status = plugin3.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			fail("7.2");
		}
	} catch (CoreException e) {
		fail("7.3", e);
	}
}
/**
 * Create another project and leave it closed for next session.
 */
public void testAnotherProject() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	try {
		project.create(null);
		project.open(null);
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", project.exists());
	assertTrue("0.2", project.isOpen());

	try {
		project.close(null);
	} catch (CoreException e) {
		fail("1.0", e);
	}
	assertTrue("1.1", project.exists());
	assertTrue("1.2", !project.isOpen());

	// when closing and opening the project again, it should still exist
	project = getWorkspace().getRoot().getProject(PROJECT_1);
	try {
		project.open(null);
	} catch (CoreException e) {
		fail("2.0", e);
	}
	assertTrue("2.1", project.exists());
	assertTrue("2.2", project.isOpen());

	// create some children
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
	ensureExistsInWorkspace(resources, true);
	assertExistsInFileSystem("3.1", resources);
	assertExistsInWorkspace("3.2", resources);

	try {
		project.close(null);
		project.open(null);
	} catch (CoreException e) {
		fail("4.0", e);
	}
	assertExistsInFileSystem("4.1", resources);
	assertExistsInWorkspace("4.2", resources);

	try {
		getWorkspace().save(true, null);
	} catch (CoreException e) {
		fail("5.0", e);
	}
}
public void testBuilder() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	assertTrue("0.0", project.isAccessible());

	// Create and set a build spec for the project
	try {
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		description.setBuildSpec(new ICommand[] { command });
		project.setDescription(description, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
	} catch (CoreException e) {
		fail("2.0", e);
	}

	// close and open the project and see if the builder gets a good delta
	try {
		project.close(null);
		project.open(null);
	} catch (CoreException e) {
		fail("3.0", e);
	}
	IFile added = project.getFile("added file");
	DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
	verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
	try {
		added.create(getRandomContents(), true, null);
	} catch (CoreException e) {
		fail("3.1", e);
	}
	assertTrue("3.2", verifier.wasAutoBuild());
	assertTrue("3.3", verifier.isDeltaValid());
	// remove the file because we don't want it to affect any other delta in the test
	try {
		added.delete(true, false, null);
	} catch (CoreException e) {
		fail("3.4", e);
	}
}
/**
 * Create some resources and save the workspace.
 */
public void testCreateMyProject() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	try {
		project.create(null);
		project.open(null);
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", project.exists());
	assertTrue("0.2", project.isOpen());

	try {
		project.close(null);
	} catch (CoreException e) {
		fail("1.0", e);
	}
	assertTrue("1.1", project.exists());
	assertTrue("1.2", !project.isOpen());

	// when closing and opening the project again, it should still exist
	project = getWorkspace().getRoot().getProject(PROJECT_1);
	try {
		project.open(null);
	} catch (CoreException e) {
		fail("2.0", e);
	}
	assertTrue("2.1", project.exists());
	assertTrue("2.2", project.isOpen());

	// create some children
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
	ensureExistsInWorkspace(resources, true);
	assertExistsInFileSystem("3.1", resources);
	assertExistsInWorkspace("3.2", resources);

	try {
		project.close(null);
		project.open(null);
	} catch (CoreException e) {
		fail("4.0", e);
	}
	assertExistsInFileSystem("4.1", resources);
	assertExistsInWorkspace("4.2", resources);
}
/**
 * Create another project and leave it closed for next session.
 */
public void testCreateProject2() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
	try {
		project.create(null);
		project.open(null);
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", project.exists());
	assertTrue("0.2", project.isOpen());

	// create some children
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
	ensureExistsInWorkspace(resources, true);
	assertExistsInFileSystem("3.1", resources);
	assertExistsInWorkspace("3.2", resources);

	// add a builder to this project
	try {
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(SimpleBuilder.BUILDER_ID);
		description.setBuildSpec(new ICommand[] { command });
		project.setDescription(description, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
	} catch (CoreException e) {
		fail("4.0", e);
	}

	try {
		project.close(null);
	} catch (CoreException e) {
		fail("5.0", e);
	}
	assertTrue("5.1", project.exists());
	assertTrue("5.2", !project.isOpen());
}
public void testPostSave() {
	// get plugin
	IPluginDescriptor descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_1);
	SaveParticipant1Plugin plugin = null;
	try {
		plugin = (SaveParticipant1Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", plugin != null);

	// look at the plugin save lifecycle
	IStatus status = plugin.getSaveLifecycleLog();
	if (!status.isOK()) {
		System.out.println(status.getMessage());
		assertTrue("1.0", false);
	}
}
}
