package org.eclipse.core.tests.resources.saveparticipant;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
/**
 * @see SaveManager1Test
 * @see SaveManager2Test
 */
public class SaveManager3Test extends SaveManagerTest {
/**
 * Need a zero argument constructor to satisfy the test harness.
 * This constructor should not do any real work nor should it be
 * called by user code.
 */
public SaveManager3Test() {
}
public SaveManager3Test(String name) {
	super(name);
}
public static Test suite() {
	// we do not add the whole class because the order is important
	TestSuite suite = new TestSuite();
	suite.addTest(new SaveManager3Test("testSaveParticipant"));
	suite.addTest(new SaveManager3Test("testBuilder"));
	suite.addTest(new SaveManager3Test("cleanUp"));
	return suite;
}
public void testBuilder() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	assertTrue("0.0", project.isAccessible());

	try {
		setAutoBuilding(false);
		touch(project);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		setAutoBuilding(true);
	} catch (CoreException e) {
		fail("1.0", e);
	}
	DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
	assertTrue("1.1", verifier.wasIncrementalBuild());

	IFile added = project.getFile("added file");
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
public void testSaveParticipant() {
	// SaveParticipant1Plugin
	IPluginDescriptor descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_1);
	SaveParticipant1Plugin plugin1 = null;
	try {
		plugin1 = (SaveParticipant1Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", plugin1 != null);

	// check saved state and delta
	plugin1.resetDeltaVerifier();
	IStatus status;
	try {
		status = plugin1.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			assertTrue("1.0", false);
		}
	} catch (CoreException e) {
		fail("1.1", e);
	}

	// SaveParticipant2Plugin
	descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_2);
	SaveParticipant2Plugin plugin2 = null;
	try {
		plugin2 = (SaveParticipant2Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("5.0", e);
	}
	assertTrue("5.1", plugin2 != null);

	// check saved state and delta
	plugin2.resetDeltaVerifier();
	// MyProject changes
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	IResource file = project.getFile("addedFile");
	plugin2.addExpectedChange(file, IResourceDelta.ADDED, 0);
	// Project2 changes
	project = getWorkspace().getRoot().getProject(PROJECT_2);
	plugin2.addExpectedChange(project, IResourceDelta.CHANGED, IResourceDelta.OPEN);
	file = project.getFile("addedFile");
	plugin2.addExpectedChange(file, project, IResourceDelta.ADDED, 0);
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
	plugin2.addExpectedChange(resources, IResourceDelta.ADDED, 0);
	//
	try {
		status = plugin2.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			fail("6.0");
		}
	} catch (CoreException e) {
		fail("6.1", e);
	}

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
public void cleanUp() throws CoreException {
	IProject[] projects = getWorkspace().getRoot().getProjects();
	getWorkspace().delete(projects, true, null);
}
}
