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
package org.eclipse.debug.internal.core.memory;

import org.eclipse.debug.core.model.IMemoryBlock;


/**
 * Implementation of IMemoryRendering
 * @since 3.0
 */
public class MemoryRendering implements IMemoryRendering
{
	private IMemoryBlock fBlock;
	private String fRenderingId;
	
	
	public MemoryRendering(IMemoryBlock block,  String renderingId)
	{
		fBlock = block;
		fRenderingId = renderingId;
	}
	
	/**
	 * @return
	 */
	public IMemoryBlock getBlock()
	{
		return fBlock;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryRendering#getRenderingId()
	 */
	public String getRenderingId()
	{
		return fRenderingId;
	}


}
