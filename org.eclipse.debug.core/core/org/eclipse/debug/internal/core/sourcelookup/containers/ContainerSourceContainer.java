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
package org.eclipse.debug.internal.core.sourcelookup.containers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;

/**
 * A container in the workspace. Source elements are searched
 * for within this container and optionally nested containers.
 * <p>
 * Names specified in <code>findSourceElements</code> method can
 * be simple or qualified. When a name is qualified, a file will
 * be searched for relative to this container, and optionally
 * nested containers.
 * </p>
 * 
 * @since 3.0
 */
public abstract class ContainerSourceContainer extends CompositeSourceContainer {

	private IContainer fContainer = null;
	private boolean fSubfolders = false;

	/**
	 * Constructs a source container on the given workspace container. 
	 * 
	 * @param container the container to search for source in
	 * @param subfolders whether nested folders should be searched
	 *  for source elements
	 */
	public ContainerSourceContainer(IContainer container, boolean subfolders) {
		fContainer = container;
		fSubfolders = subfolders;
	}
	
	/**
	 * Returns the workspace container this source container is
	 * rooted at.
	 *  
	 * @return the workspace container this source container is
	 * rooted at
	 */
	public IContainer getContainer() {
		return fContainer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#findSourceElements(java.lang.String)
	 */
	public Object[] findSourceElements(String name) throws CoreException {
		ArrayList sources = new ArrayList();
		IContainer container = getContainer();
		IPath path = new Path(name);
		IFile file = container.getFile(path);
		if (file.exists()) {
			sources.add(file);
		}
		
		//check subfolders		
		if ((isFindDuplicates() && fSubfolders) || (sources.isEmpty() && fSubfolders)) {
			ISourceContainer[] containers = getSourceContainers();
			for (int i=0; i < containers.length; i++) {
				Object[] objects = containers[i].findSourceElements(name);
				if (objects == null || objects.length == 0) {
					continue;
				}
				if (isFindDuplicates()) {
					for(int j=0; j < objects.length; j++)
						sources.add(objects[j]);
				} else {
					sources.add(objects[0]);
					break;
				}
			}
		}			
		
		if(sources.isEmpty())
			return EMPTY;
		return sources.toArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getName()
	 */
	public String getName() {		
		return getContainer().getName(); 
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ContainerSourceContainer) {
			ContainerSourceContainer loc = (ContainerSourceContainer) obj;
			return loc.getContainer().equals(getContainer());
		}	
		return false;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getContainer().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#isComposite()
	 */
	public boolean isComposite() {	
		return fSubfolders;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.containers.CompositeSourceContainer#createSourceContainers()
	 */
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if(fSubfolders) {
			IResource[] resources = getContainer().members();
			List list = new ArrayList(resources.length);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (resource.getType() == IResource.FOLDER) {
					list.add(new FolderSourceContainer((IFolder)resource, fSubfolders));
				}
			}
			ISourceContainer[] containers = (ISourceContainer[]) list.toArray(new ISourceContainer[list.size()]);
			for (int i = 0; i < containers.length; i++) {
				ISourceContainer container = containers[i];
				container.init(getDirector());
			}			
			return containers;
		}
		return new ISourceContainer[0];
	}

}
