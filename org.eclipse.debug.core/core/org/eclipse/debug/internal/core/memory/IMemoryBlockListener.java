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
 * Listener for add memory / remove memory events.
 * Implementer should add itself to IMemoryBlockManager to
 * be notified of these events.
 * @since 3.0
 */
public interface IMemoryBlockListener {
	
	/**
	 * Called when a memory block is added.
	 * @param memory
	 */
	void MemoryBlockAdded(IMemoryBlock memory);

	/**
	 * Called when a memory block is removed.
	 * @param memory
	 */
	void MemoryBlockRemoved(IMemoryBlock memory);
	
}
