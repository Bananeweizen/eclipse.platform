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

import java.math.BigInteger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;

/**
 * Represents a memory block.
 * 
 * When the memory block is changed, fire a CHANGE Debug Event.
 * 
 *  When firing change event, be aware of the following:
 *  - whenever a change event is fired, the content provider for Memory View / Memory Rendering
 *    view checks to see if memory has actually changed.  
 *  - If memory has actually changed, a refresh will commence.  Changes to the memory block
 *    will be computed and will be shown with the delta icons.
 *  - If memory has not changed, content will not be refreshed.  However, previous delta information 
 * 	  will be erased.  The screen will be refreshed to show that no memory has been changed.  (All
 *    delta icons will be removed.)
 * 
 * Please note that these APIs will be called multiple times by the Memory View.
 * To improve performance, debug adapters need to cache the content of its memory block and only
 * retrieve updated data when necessary.
 * @since 3.0
 */
public interface IExtendedMemoryBlock extends IMemoryBlock {
	
	/**
	 * Returns the expression of this memory block.
	 * The expression will be used to construct the tab label of the memory view.
	 *
	 * @return expression of the memory block.
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the engine.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public String getExpression() throws DebugException;	
	
	/**
	 * Get the base address of this memory block in BigInteger
	 * @return 
	 */
	public BigInteger getBigBaseAddress();
	
	/**
	 * @return address size in number of bytes
	 */
	public int getAddressSize();
	
	
	/**
	 * Indicate if the base address of this memory block block could be modified
	 * If return true, setBaseAddress will be used to change the base address
	 * of this memory block.
	 * * @return
	 */
	public boolean supportBaseAddressModification();
	
	/**
	 * @return true to indicate that the memory block manages the changes in the
	 * memory block.  If the memory block manages changes, the memory block is
	 * expected to cache the MemoryByte array returned by getBytesFromOffset and
	 * getBytesFromAddress.  The change information will not be calculated by
	 * the memory view.  Intead, the memory block keeps track of the bytes
	 * and marks the bytes as changed/unchanged.  Turn off both the CHANGE and UNCHANGED
	 * bits if the memory block does not contain history for the address.
	 * 
	 * If this function returns false, the Memory View will calculate
	 * delta information for each byte based on its history.
	 */
	public boolean isMemoryChangesManaged();
	
	/**
	 * Set the base address of this memory block
	 * @param address
	 * @throws DebugException if the method fails.  Reasons inlucde:
	 * <ul><li>Failure communicating with the engine.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public void setBaseAddress(BigInteger address) throws DebugException;

	
	/**
	 * Get bytes based on offset and length.  Memory at base address + offset 
	 * should be returned.
	 * Return an array of IMemory.  Each IMemory object represents a section of the
	 * memory block.  The IMemory object allows debug adapters to provide more information
	 * about a section of memory.  Refer to IMemory for details. 
	 * 
	 * @param offset
	 * @param length
	 * @return
	 * @throws DebugException if the method fails.
	 */
	public MemoryByte[] getBytesFromOffset(long offset, long length) throws DebugException;
	
	
	/**
	 * Get bytes based on a given address.  
	 * 
	 * Return an array of IMemory.  Each IMemory object represents a section of the
	 * memory block.  The IMemory object allows debug adapters to provide more information
	 * about a section of memory.  Refer to IMemory for details. 
	 * 
	 * @param address
	 * @param length
	 * @return
	 * @throws DebugException if method fails
	 */
	public MemoryByte[] getBytesFromAddress(BigInteger address, long length) throws DebugException;	

	/**
	 * @return true if the platform is big endian, false otherwise
	 */
	public boolean isBigEndian();

	
	/**
	 * Enable this memory block.  Block is enabled when its view tab is in focus.
	 */
	public void enable();
	
	
	/**
	 * Disable this memory block.  Block is disabled when its view tab loses focus.
	 */
	public void disable();
	
	/**
	 * Indicate if this memory block is enabled/disabled.
	 * @return
	 */
	public boolean isEnabled();
	
	
	/**
	 * Delete this memory block.
	 */
	public void delete();
	
	/**
	 * Return the IMemoryBlockRetrieval responsible for getting
	 * this memory block.
	 * @return
	 */
	public IMemoryBlockRetrieval getMemoryBlockRetrieval();
}
