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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;

/**
 * A folder in the local file system. Source elements returned
 * from <code>findSourceElements(...)</code> are instances
 * of <code>LocalFileStorage</code>.
 * 
 * @since 3.0
 */

public class DirectorySourceContainer extends CompositeSourceContainer {
	
	// root directory
	private File fDirectory;
	// whether to search subfolders
	private boolean fSubfolders = false;
	
	/**
	 * Consutructs an external folder container for the
	 * directory identified by the given path.
	 * 
	 * @param dirPath path to a directory in the local file system
	 * @param subfolders whether folders within the root directory
	 *  should be searched for source elements
	 */
	public DirectorySourceContainer(IPath dirPath, boolean subfolders) {
		this(dirPath.toFile(), subfolders);
	}
	
	/**
	 * Consutructs an external folder container for the
	 * directory identified by the given file.
	 * 
	 * @param dir a directory in the local file system
	 * @param subfolders whether folders within the root directory
	 *  should be searched for source elements
	 */
	public DirectorySourceContainer(File dir, boolean subfolders) {
		fDirectory = dir;
		fSubfolders = subfolders;
	}	
		
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getName()
	 */
	public String getName() {
		return fDirectory.getName();
	}	
	
	/**
	 * Returns the root directory in the local file system associated
	 * with this source container.
	 * 
	 * @return the root directory in the local file system associated
	 * with this source container
	 */
	public File getDirectory() {
		return fDirectory;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getType()
	 */
	public ISourceContainerType getType() {
		return SourceLookupUtils.getSourceContainerType(DirectorySourceContainerType.TYPE_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#findSourceElements(java.lang.String)
	 */
	public Object[] findSourceElements(String name) throws CoreException {
		ArrayList sources = new ArrayList();
		File directory = getDirectory();
		File file = new File(directory, name);
		if (file.exists() && file.isFile()) {
			sources.add(new LocalFileStorage(file));
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
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#isComposite()
	 */
	public boolean isComposite() {
		return fSubfolders;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof DirectorySourceContainer) {
			DirectorySourceContainer container = (DirectorySourceContainer) obj;
			return container.getDirectory().equals(getDirectory());
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getDirectory().hashCode();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.containers.CompositeSourceContainer#createSourceContainers()
	 */
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if (isComposite()) {
			String[] files = fDirectory.list();
			if (files != null) {
				List dirs = new ArrayList();
				for (int i = 0; i < files.length; i++) {
					String name = files[i];
					File file = new File(getDirectory(), name);
					if (file.exists() && file.isDirectory()) {
						dirs.add(new DirectorySourceContainer(file, true));
					}
				}
				ISourceContainer[] containers = (ISourceContainer[]) dirs.toArray(new ISourceContainer[dirs.size()]);
				for (int i = 0; i < containers.length; i++) {
					ISourceContainer container = containers[i];
					container.init(getDirector());
				}				
				return containers;
			}
		}
		return new ISourceContainer[0];
	}

}
