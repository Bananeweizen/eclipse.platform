/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup.containers;

import java.util.ArrayList;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;
import org.eclipse.debug.internal.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.ui.IWorkingSet;

/**
 * A working set in the workspace.  Source is searched for in the projects (referenced
 * projects) and folders (sub-folders) that are part of the working set.  Files in the set
 * are currently ignored since we don't support file containers in general.
 * 
 * @since 3.0
 */
public class WorkingSetSourceContainer extends CompositeSourceContainer{
	
	private IWorkingSet fWorkingSet;
		
	/**
	 * Creates a source container for the working set.
	 * @param workingSet the working set represented by this container
	 */
	public WorkingSetSourceContainer(IWorkingSet workingSet) {
		fWorkingSet = workingSet;		
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getName()
	 */
	public String getName() {
		return fWorkingSet.getName();
	}
		
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof WorkingSetSourceContainer) {			
			return obj.equals(fWorkingSet);			
		}				
		return false;
	}		
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getType()
	 */
	public ISourceContainerType getType() {
		return SourceLookupUtils.getSourceContainerType(WorkingSetSourceContainerType.TYPE_ID);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getSourceContainers()
	 */
	public ISourceContainer[] getSourceContainers() {
		IAdaptable[] elements = fWorkingSet.getElements();
		
		if(elements == null)
			return new ISourceContainer[0];
		
		ArrayList locationList = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IResource resource = (IResource) elements[i].getAdapter(IResource.class);
			
			if (resource != null) {
				switch (resource.getType()) {
				case IResource.FOLDER:							
					locationList.add(new FolderSourceContainer((IFolder)resource, true));											
					break;
				case IResource.PROJECT:
					locationList.add(new ProjectSourceContainer((IProject)resource, true));			
					break;
					//if the element corresponds to an IFile, do nothing
					//TODO make file source location??
				}
			}
		}
		
		return (ISourceContainer[])locationList.toArray(new ISourceContainer[locationList.size()]);
	}
	
		
}
