/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.performance;

import java.io.File;

import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.PartInitException;

public class OpenAntEditorTest extends PerformanceTestCase {
	
	public static final String PATH= "buildfiles/performance";
	
	public static final String FILE_PREFIX= "build";
	
	public static final String FILE_SUFFIX= ".xml";
	
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	public void testOpenAntEditor1() throws PartInitException {
		// cold run
		IFile file= getIFile("build.xml");
		measureOpenInEditor(new IFile[]{file});
	}
	
	public void testOpenAntEditor2() throws PartInitException {
		// warm run
		IFile file= getIFile("build.xml");
		measureOpenInEditor(new IFile[]{file});
	}
	
	protected IFile getIFile(String buildFileName) {
		return getProject().getFolder("buildfiles").getFolder("performance").getFile(buildFileName);	
	}
	
	protected File getBuildFile(String buildFileName) {
		IFile file = getIFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists());
		return file.getLocation().toFile();
	}
	
	/**
	 * Returns the 'AntUITests' project.
	 * 
	 * @return the test project
	 */
	protected IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
	}
	
	protected void measureOpenInEditor(IFile[] files) throws PartInitException {
		try {
			for (int i= 0, n= files.length; i < n; i++) {
				startMeasuring();
				EditorTestHelper.openInEditor(files[i], true);
				stopMeasuring();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
			 commitMeasurements();
	 		 assertPerformance();
		} finally {
			EditorTestHelper.closeAllEditors();
		}
	}
	
	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}