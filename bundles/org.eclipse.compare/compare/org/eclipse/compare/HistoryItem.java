/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.compare;

import java.io.InputStream;
import java.io.BufferedInputStream;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.*;

/**
 * A combination <code>IFileState</code> and <code>ITypedElement</code> that can be used as
 * an input to a compare viewer or other places where an <code>IStreamContentAccessor</code>
 * is needed.
 * <p>
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 */
public class HistoryItem implements IStreamContentAccessor, ITypedElement, IModificationDate {
	
	private ITypedElement fBase;
	private IFileState fFileState; 

	/**
	 * Creates a <code>HistoryItem</code> object which combines the given <code>IFileState</code>
	 * and <code>ITypedElement</code> into an object
	 * which is suitable as input for a compare viewer or <code>ReplaceWithEditionDialog</code>.
	 *
	 * @param base the implementation of the <code>ITypedElement</code> interface delegates to this base <code>ITypedElement</code>
	 * @param fileState the <code>IFileState</code> from which the streamable contents and the modification time is derived from
	 */
	public HistoryItem(ITypedElement base, IFileState fileState) {
		fBase= base;
		fFileState= fileState;
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getName
	 */
	public String getName() {
		return fBase.getName();
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getImage
	 */
	public Image getImage() {
		return fBase.getImage();
	}
	
	/* (non-Javadoc)
	 * see ITypedElement.getType
	 */
	public String getType() {
		return fBase.getType();
	}

	/* (non-Javadoc)
	 * see IModificationDate.getModificationDate
	 */
	public long getModificationDate() {
		return fFileState.getModificationTime();
	}
	
	/* (non-Javadoc)
	 * see IStreamContentAccessor.getContents
	 */
	public InputStream getContents() throws CoreException {
		return new BufferedInputStream(fFileState.getContents());
	}
}

