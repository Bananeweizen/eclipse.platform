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
 * Represent a memory rendering in Memory Rendering View 
 * @since 3.0
 */
public interface IMemoryRendering
{
	/**
	 * @return the memory block of this rendering.
	 */
	public IMemoryBlock getBlock();

	/**
	 * @return the rendering id of this rendering.
	 */
	public String getRenderingId();
	

}
