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
package org.eclipse.debug.internal.core.sourcelookup.containers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;

/**
 * Common function for source containers.
 * <p>
 * Clients implementing source containers should subclass this class.
 * </p>
 * @sicne 3.0
 */
public abstract class AbstractSourceContainer extends PlatformObject implements ISourceContainer {
	
	public static final Object[] EMPTY = new Object[0];
	
	private ISourceLookupDirector fDirector;
	
	/**
	 * Throws an exception with the given message and underlying exception.
	 * 
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException
	 */
	protected void abort(String message, Throwable exception) throws CoreException {
		SourceLookupUtils.abort(message, exception);
	}
	
	/* (non-Javadoc)
	 * 
	 * By default, do nothing. Subclasses should override as requried.
	 * 
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#dispose()
	 */
	public void dispose() {
		fDirector = null;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#getSourceContainers()
	 */
	public ISourceContainer[] getSourceContainers() throws CoreException {
		return new ISourceContainer[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#isComposite()
	 */
	public boolean isComposite() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer#init(org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector)
	 */
	public void init(ISourceLookupDirector director) {
		fDirector = director;
	}
	
	/**
	 * Returns the source lookup director this source container registered
	 * in, or <code>null</code> if none.
	 * 
	 * @return the source lookup director this source container registered
	 * in, or <code>null</code> if none
	 */
	protected ISourceLookupDirector getDirector() {
		return fDirector;
	}
	
	/**
	 * Returns whether this container's source lookup director is configured
	 * to search for duplicate source elements.
	 * 
	 * @return whether this container's source lookup director is configured
	 * to search for duplicate source elements
	 */
	protected boolean isFindDuplicates() {
		return getDirector().isFindDuplicates();
	}
}
