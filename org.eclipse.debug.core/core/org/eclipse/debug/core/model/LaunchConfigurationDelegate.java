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
package org.eclipse.debug.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.core.DebugCoreMessages;

/**
 * Default implementation of a launch configuration delegate. Provides
 * convenience methods for computing the build order of projects,
 * building projects, and searching for errors in the workspace. The
 * default pre-launch check prompts the user to launch in debug mode
 * if breakpoints are present in the workspace. 
 * <p>
 * Clients implementing launch configration delegates should subclass
 * this class.
 * </p>
 * @since 3.0
 */
public abstract class LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {
	
	/**
	 * Status code for which a UI prompter is registered.
	 */
	protected static final IStatus promptStatus = new Status(IStatus.INFO, "org.eclipse.debug.ui", 200, "", null);  //$NON-NLS-1$//$NON-NLS-2$
	
	/**
	 * Status code for which a prompter is registered to ask the user if they
	 * want to launch in debug mode when breakpoints are present.
	 */
	protected static final IStatus switchToDebugPromptStatus = new Status(IStatus.INFO, "org.eclipse.debug.core", 201, "", null);  //$NON-NLS-1$//$NON-NLS-2$
	
	/**
	 * Status code for which a prompter is registered to ask the user if the
	 * want to continue launch despite existing compile errors
	 */
	protected static final IStatus complileErrorPromptStatus = new Status(IStatus.INFO, "org.eclipse.debug.core", 202, "", null); //$NON-NLS-1$ //$NON-NLS-2$
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#buildForLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		IProject[] projects = getBuildOrder(configuration, mode);
		if (projects == null) {
			return true;
		} else {
			buildProjects(projects, monitor);
			return false;
		}
	}
	
	/**
	 * Returns the projects to build before launching the given launch configuration
	 * or <code>null</code> if the entire workspace should be built incrementally.
	 * Subclasses should override as required.
	 * 
	 * @param configuration the configuration being launched
	 * @param mode launch mode
	 * @return projects to build, in build order, or <code>null</code>
	 * @throws CoreException if an exception occurrs
	 */
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}
	
	/**
	 * Returns the set of projects to use when searching for errors or <code>null</code> 
	 * if no search is to be done.  
	 * 
	 * @param projects the list of projects to sort into build order
	 * @return a list of projects.
	 */
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#finalLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		IProject[] projects = getProjectsForProblemSearch(configuration, mode);
		if (projects == null) {
			return true; //continue launch
		} else {
			boolean continueLaunch = true;
			
			monitor.subTask(DebugCoreMessages.getString("LaunchConfigurationDelegate.6")); //$NON-NLS-1$
			for (int i = 0; i < projects.length; i++) {
				monitor.subTask(DebugCoreMessages.getString("LaunchConfigurationDelegate.7") + projects[i].getName()); //$NON-NLS-1$
				if (existsProblems(projects[i], IMarker.SEVERITY_ERROR)) {
					IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);
					if (prompter != null) {
						continueLaunch = ((Boolean) prompter.handleStatus(complileErrorPromptStatus, configuration)).booleanValue();
						break;
					}
				}	
			}	
			
			return continueLaunch;
		}
	}
	
	/* (non-Javadoc)
	 * 
	 * If launching in run mode, and the configuration supports debug mode, check
	 * if there are any breakpoints in the workspace, and ask the user if they'd
	 * rather launch in debug mode.
	 * 
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (mode.equals(ILaunchManager.RUN_MODE)  && configuration.supportsMode(ILaunchManager.DEBUG_MODE)) {
			IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
			if (!breakpointManager.isEnabled()) {
				// no need to check breakpoints individually.
				return true; 
			}
			IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();
			for (int i = 0; i < breakpoints.length; i++) {
				if (breakpoints[i].isEnabled()) {
					IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);
					if (prompter != null) {
						boolean lauchInDebugModeInstead = ((Boolean)prompter.handleStatus(switchToDebugPromptStatus, configuration)).booleanValue();
						if (lauchInDebugModeInstead) { 
							return false; //kill this launch
						} 
					}
					// if no user prompt, or user says to continue (no need to check other breakpoints)
					return true;
				}
			}
		}	
		// no enabled breakpoints... continue launch
		return true;
	}
	
	/**
	 * Returns an array of projects in their suggested build order
	 * containing all of the projects specified by <code>baseProjects</code>
	 * and all of their referenced projects.
	 *  
	 * @param baseProjects a collection of projetcs
	 * @return an array of projects in their suggested build order
	 * containing all of the projects specified by <code>baseProjects</code>
	 * @throws CoreException if an error occurs while computing referenced
	 *  projects
	 */
	protected IProject[] computeReferencedBuildOrder(IProject[] baseProjects) throws CoreException {
		HashSet unorderedProjects = new HashSet();
		for(int i = 0; i< baseProjects.length; i++) {
			unorderedProjects.add(baseProjects[i]);
			addReferencedProjects(baseProjects[i], unorderedProjects);
		}
		IProject[] projectSet = (IProject[]) unorderedProjects.toArray(new IProject[unorderedProjects.size()]);
		return computeBuildOrder(projectSet);
	}
	
	
	/**
	 * Adds all projects referenced by <code>project</code> to the given
	 * set.
	 * 
	 * @param project project
	 * @param references set to which referenced projects are added
	 * @throws CoreException if an error occurs while computing referenced
	 *  projects
	 */
	protected void addReferencedProjects(IProject project, Set references) throws CoreException{
		if (project.isOpen()) {
			IProject[] projects = project.getReferencedProjects();
			for (int i = 0; i < projects.length; i++) {
				IProject refProject= projects[i];
				if (refProject.exists() && !references.contains(refProject)) {
					references.add(refProject);
					addReferencedProjects(refProject, references);
				}
			}		
		}
	}
	
	/**  
	 * Returns a list of projects in their suggested build order from the
	 * given unordered list of projects.
	 * 
	 * @param projects the list of projects to sort into build order
	 * @return a new array containing all projects from <code>projects</code> sorted
	 *   according to their build order.
	 */
	protected IProject[] computeBuildOrder(IProject[] projects) { 
		String[] orderedNames = ResourcesPlugin.getWorkspace().getDescription().getBuildOrder();
		if (orderedNames != null) {
			List orderedProjects = new ArrayList(projects.length);
			//Projects may not be in the build order but should be built if selected
			List unorderedProjects = Arrays.asList(projects);
			
			for (int i = 0; i < orderedNames.length; i++) {
				String projectName = orderedNames[i];
				for (Iterator iterator = unorderedProjects.iterator(); iterator.hasNext(); ) {
					IProject project = (IProject)iterator.next();
					if (project.getName().equals(projectName)) {
						orderedProjects.add(project);
						iterator.remove();
						break;
					}
				}
			}
			//Add anything not specified before we return
			orderedProjects.addAll(unorderedProjects);
			return (IProject[]) orderedProjects.toArray(new IProject[orderedProjects.size()]);
		}
		
		// Computing build order returned null, try the project prerequisite order
		IWorkspace.ProjectOrder po = ResourcesPlugin.getWorkspace().computeProjectOrder(projects);
		return po.projects;
	}	
	
	/**
	 * Returns whether the given project contains any problem markers of the
	 * specified severity.
	 * 
	 * @param proj the project to search
	 * @param severity the severity of error(s) to search for
	 * @return whether the given project contains any problem markers of the
	 * specified or greater severity
	 * @throws CoreException if an error occurs while searching for
	 *  problem markers
	 */
	protected boolean existsProblems(IProject proj, int severity) throws CoreException {
		IMarker[] markers = proj.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		if (markers.length > 0) {
			for (int i = 0; i < markers.length; i++) {
				if (((Integer)markers[i].getAttribute(IMarker.SEVERITY)).intValue() >= severity) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Performs an incremental build on each of the given projects.
	 * 
	 * @param projects projects to build
	 * @param monitor progress monitor
	 * @throws CoreException if an exception occurrs while building
	 */
	protected void buildProjects(IProject[] projects, IProgressMonitor monitor) throws CoreException {
		for (int i = 0; i < projects.length; i++ ) {
			projects[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}
	}
}
