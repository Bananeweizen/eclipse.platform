/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.demo;

import java.io.File;

import org.apache.tools.ant.taskdefs.Javac;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

public class ProjectCreator {
		
	public IJavaProject createJavaProjectFromJavacNode(String projectName, Javac javacTask) throws CoreException {
		
		IJavaProject javaProject = createJavaProject(projectName);
		
		File destDir= javacTask.getDestdir();
		String destDirName= destDir.getName();
		org.apache.tools.ant.types.Path sourceDirs= javacTask.getSrcdir();
		createSourceDirectories(destDir, destDirName, sourceDirs, javaProject);
		
		// add rt.jar
		addVariableEntry(javaProject, new Path(JavaRuntime.JRELIB_VARIABLE), new Path(JavaRuntime.JRESRC_VARIABLE), new Path(JavaRuntime.JRESRCROOT_VARIABLE));
		
		setClasspath(javacTask, javaProject);
		
		javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		return javaProject;
	}
	
	private void setClasspath(Javac javacTask, IJavaProject javaProject) throws JavaModelException {
		org.apache.tools.ant.types.Path classpath= javacTask.getClasspath();
		if (classpath == null) {
			return;
		}
		String[] classpaths= classpath.list();
		for (int i = 0; i < classpaths.length; i++) {
			String cp = classpaths[i];
			File classpathEntry= new File(cp);
			addLibrary(javaProject, new Path(classpathEntry.getAbsolutePath()));
		}
	}

	private void createSourceDirectories(File destDir, String destDirName, org.apache.tools.ant.types.Path sourceDirs, IJavaProject javaProject) throws CoreException {
		//create the source directories
		String[] sourceDirectories= sourceDirs.list();
		for (int i = 0; i < sourceDirectories.length; i++) {
			String srcDir = sourceDirectories[i];
			File srcDirectory= new File(srcDir);
			addSourceContainer(javaProject, srcDirectory.getName(), srcDir, destDirName, destDir.getAbsolutePath());
		}
	}

	private IJavaProject createJavaProject(String projectName) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		
		if (!project.isOpen()) {
			project.open(null);
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID);
		}
		
		IJavaProject jproject= JavaCore.create(project);
		
		jproject.setRawClasspath(new IClasspathEntry[0], null);
		
		return jproject;	
	}
	
	private void addNatureToProject(IProject proj, String natureId) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, null);
	}
	
	/**
	 * Adds a source container to a IJavaProject.
	 */		
	private IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String srcName, String srcPath, String outputName, String outputPath) throws CoreException {
		IProject project= jproject.getProject();
		IContainer container= null;
		if (srcName == null || srcName.length() == 0) {
			container= project;
		} else {
			IFolder folder= project.getFolder(srcName);
			if (!folder.exists()) {
				folder.createLink(new Path(srcPath), IResource.ALLOW_MISSING_LOCAL, null);
			}
			container= folder;
		}
		IPackageFragmentRoot root= jproject.getPackageFragmentRoot(container);

		IFolder output = null;
		if (outputName!= null) {
			output = project.getFolder(outputName);
			if (!output.exists()) {
				output.createLink(new Path(outputPath), IResource.ALLOW_MISSING_LOCAL, null);
			}
		}
				
		IClasspathEntry cpe= JavaCore.newSourceEntry(root.getPath(), new IPath[0], output.getFullPath());
		
		addToClasspath(jproject, cpe);		
		return root;
	}	
	
	private void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
		IClasspathEntry[] oldEntries= jproject.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(cpe)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= cpe;
		jproject.setRawClasspath(newEntries, null);
	}
	
	/**
	 * Adds a variable entry with source attachment to a IJavaProject if the path can be resolved.
	 */			
	private void addVariableEntry(IJavaProject jproject, IPath path, IPath sourceAttachPath, IPath sourceAttachRoot) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newVariableEntry(path, sourceAttachPath, sourceAttachRoot);
		addToClasspath(jproject, cpe);
		IPath resolvedPath= JavaCore.getResolvedVariablePath(path);
		if (resolvedPath != null) {
			jproject.getPackageFragmentRoot(resolvedPath.toString());
		}
	}
	
	/**
	 * Adds a library entry with source attchment to a IJavaProject.
	 */			
	private void addLibrary(IJavaProject jproject, IPath path) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newLibraryEntry(path, null, null);
		addToClasspath(jproject, cpe);
		jproject.getPackageFragmentRoot(path.toString());
	}
}