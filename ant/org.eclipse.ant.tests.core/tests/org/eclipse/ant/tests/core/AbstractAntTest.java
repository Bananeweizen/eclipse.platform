package org.eclipse.ant.tests.core;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import junit.framework.TestCase;

import org.eclipse.ant.core.*;
import org.eclipse.ant.tests.core.testplugin.AntFileRunner;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;


 
/**
 * Tests for Ant core
 */
public abstract class AbstractAntTest extends TestCase {
	
	protected static final String BUILD_SUCCEESSFUL= "BUILD SUCCESSFUL";
	public static final String ANT_TEST_BUILD_LOGGER = "org.eclipse.ant.tests.core.testloggers.TestBuildLogger"; //$NON-NLS-1$
	public static final String ANT_TEST_BUILD_LISTENER= "org.eclipse.ant.tests.core.testloggers.TestBuildListener";
	
	/**
	 * Returns the 'AntTests' project.
	 * 
	 * @return the test project
	 */
	protected IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject("AntTests");
	}

	public static final int DEFAULT_TIMEOUT = 30000;
	
	
	public static IProject project;
	
	
	public AbstractAntTest(String name) {
		super(name);
	}
	
	protected IFile getBuildFile(String buildFileName) {
		IFile file = getProject().getFolder("scripts").getFile(buildFileName);
		assertTrue("Could not find script file named: " + buildFileName, file.exists());
		return file;
	}
	
	protected IFolder getWorkingDirectory(String workingDirectoryName) {
		IFolder folder = getProject().getFolder(workingDirectoryName);
		assertTrue("Could not find the working directory named: " + workingDirectoryName, folder.exists());
		return folder;
	}
	
	protected IFile checkFileExists(String fileName) throws CoreException {
		getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
		IFile file = getProject().getFolder("scripts").getFile(fileName);
		assertTrue("Could not find file named: " + fileName, file.exists());
		return file;
	}
	
	public void run(String buildFileName) throws CoreException {
		run(buildFileName, null, true);
	}
	
	public void run(String buildFileName, String[] args) throws CoreException {
		run(buildFileName, args, true);
	}
	
	public void run(String buildFileName, String[] args, boolean retrieveTargets) throws CoreException {
		run(buildFileName, args, retrieveTargets, "");
	}
	
	public void run(String buildFileName, String[] args, boolean retrieveTargets, String workingDir) throws CoreException {
		AntTestChecker.reset();
		IFile buildFile= null;
		if (buildFileName != null) {
			buildFile= getBuildFile(buildFileName);
		}
		AntFileRunner runner= new AntFileRunner();
		String[] targets= null;
		if (retrieveTargets) {
			targets= getTargetNames(buildFileName);
		}
		if (workingDir.length() > 0) {
			workingDir= getWorkingDirectory(workingDir).getLocation().toFile().getAbsolutePath();
		} 
		runner.run(buildFile, targets, args, workingDir, true);
		assertTrue("Build starts did not equal build finishes", AntTestChecker.getDefault().getBuildsStartedCount() == AntTestChecker.getDefault().getBuildsFinishedCount());
	}
	
	protected TargetInfo[] getTargets(String buildFileName) throws CoreException {
		IFile buildFile= getBuildFile(buildFileName);
		
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(buildFile.getLocation().toFile().getAbsolutePath());
	 	return runner.getAvailableTargets();
	}
	
	protected String[] getTargetNames(String buildFileName) throws CoreException {
		TargetInfo[] infos= getTargets(buildFileName);
		String[] names= new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			TargetInfo info = infos[i];
			names[i]= info.getName();
		}
		return names;
	}
	
	protected String[] getTargetDescriptions(String buildFileName) throws CoreException {
		TargetInfo[] infos= getTargets(buildFileName);
		String[] descriptions= new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			TargetInfo info = infos[i];
			descriptions[i]= info.getDescription();
		}
		return descriptions;
	}
	
	/**
	 * Returns the name of the project containing the given target in the given build file or
	 * <code>null</code> if no project name can be found.	 */
	protected String getProjectName(String buildFileName, String targetName) throws CoreException {
		TargetInfo info= getTarget(buildFileName, targetName);
		if (info != null) {
			return info.getProject();
		}
		return null;
	}
	
	/**
	 * Returns the dependencies of the target with the given name in the given build file or <code>null</code>
	 * if no such target can be found.
	 */
	protected String[] getDependencies(String buildFileName, String targetName) throws CoreException {
		TargetInfo info= getTarget(buildFileName, targetName);
		if (info != null) {
			return info.getDependencies();
		}
		return null;
	}
	
	/**
	 * Returns the target with the given name in the given build file or <code>null</code>
	 * if no such target can be found.	 */
	protected TargetInfo getTarget(String buildFileName, String targetName) throws CoreException {
		TargetInfo[] infos= getTargets(buildFileName);
		for (int i= 0, numTargets= infos.length; i < numTargets; i++) {
			if (infos[i].getName().equals(targetName)) {
				return infos[i];
			}
		}
		return null;
	}
	
	protected String getLastMessageLogged() {
		return AntTestChecker.getDefault().getLastMessageLogged();
	}
	
	protected void assertSuccessful() {
		assertTrue("Build was not flagged as successful: " + getLastMessageLogged(), BUILD_SUCCEESSFUL.equals(getLastMessageLogged()));
	}
	
	protected String getPropertyFileName() {
		return getProject().getFolder("resources").getFile("test.properties").getLocation().toFile().getAbsolutePath();
	}
	
	protected void restorePreferenceDefaults() {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomURLs(prefs.getDefaultCustomURLs());
		prefs.setCustomTasks(new Task[]{});
		prefs.setCustomTypes(new Type[]{});
	}
}

