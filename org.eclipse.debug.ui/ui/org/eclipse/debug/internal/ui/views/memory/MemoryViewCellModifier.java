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

package org.eclipse.debug.internal.ui.views.memory;

import java.math.BigInteger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.core.memory.IExtendedMemoryBlock;
import org.eclipse.debug.internal.core.memory.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

/**
 * @since 3.0
 */
public class MemoryViewCellModifier implements ICellModifier
{
	private static final String PREFIX = "MemoryViewCellModifier."; //$NON-NLS-1$
	public static final String TITLE = PREFIX + "failure_title"; //$NON-NLS-1$
	public static final String FAILED = PREFIX + "failed";  //$NON-NLS-1$
	public static final String DATA_IS_INVALID = PREFIX + "data_is_invalid";  //$NON-NLS-1$
	public static final String DATA_IS_TOO_LONG = PREFIX + "data_is_too_long";  //$NON-NLS-1$

	private boolean editActionInvoked = false;
	private ITableMemoryViewTab fViewTab;
	
	public MemoryViewCellModifier(ITableMemoryViewTab viewTab)
	{
		fViewTab = viewTab;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property)
	{	
		boolean canModify = true;	
		try
		{	
			if (!(element instanceof MemoryViewLine))
				return false;
				
			if (!editActionInvoked)
				return false;	
				
			if (fViewTab == null)
				return false;	
				
			if (fViewTab.getMemoryBlock().supportsValueModification() == false)
			{
				return false;	
			}
			
			MemoryViewLine line = (MemoryViewLine)element;
			if (MemoryViewLine.P_ADDRESS.equals(property))
			   return false;
			else
			{
				int offset = Integer.valueOf(property, 16).intValue();
				int end = offset + fViewTab.getColumnSize();
				
			
				for (int i=offset; i<end; i++)
				{
					MemoryByte oneByte = line.getByte(i);
//					if (((oneByte.flags & MemoryByte.VALID) != MemoryByte.VALID) || 
//						((oneByte.flags & MemoryByte.READONLY) == MemoryByte.READONLY))
					if ((oneByte.flags & MemoryByte.READONLY) == MemoryByte.READONLY)
					{
						canModify = false;
					}
				}
				return canModify;
			}	
		}
		catch (NumberFormatException e)
		{
			canModify = false;
			return canModify;
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(Object element, String property)
	{
		// give back the value of the column
		
		if (!(element instanceof MemoryViewLine))
			return null;
			
		MemoryViewLine line = (MemoryViewLine)element;
		try
		{
			if (MemoryViewLine.P_ADDRESS.equals(property))
			   return line.getAddress();
			else
			{	
				int offset = Integer.valueOf(property, 16).intValue();
				int end = offset + fViewTab.getColumnSize();
				

				//Ask for label provider
				byte[] memory = line.getByteArray(offset, end);

				IBaseLabelProvider labelProvider = ((MemoryViewTab)fViewTab).getTableViewer().getLabelProvider();
				if(labelProvider instanceof AbstractTableViewTabLabelProvider)
				{	
					
					if (line.isAvailable(offset, end))
					{
						// ask the renderer for a string representation of the bytes
						offset  = Integer.valueOf(property, 16).intValue();
						AbstractMemoryRenderer renderer = ((AbstractTableViewTabLabelProvider)labelProvider).getRenderer();
						
						BigInteger address = new BigInteger(((MemoryViewLine)element).getAddress(), 16);
						address = address.add(BigInteger.valueOf(offset)); 
						
						return renderer.getString(fViewTab.getRenderingId(), address, memory);
					}
					else
					{
						// if the range is not available, just return padded string
						return line.getPaddedString(offset, end);
					}
				}
				
				return ""; //$NON-NLS-1$
			}
		}
		catch (NumberFormatException e)
		{
			return "00"; //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(Object element, String property, Object value)
	{
		MemoryViewLine line = null;
		if (!(element instanceof MemoryViewLine))
		{
			line = (MemoryViewLine)((TableItem)element).getData();
		}
		else
		{
			line = (MemoryViewLine)element;
		}
			
		// calculate offset to update	
		IMemoryBlock memory = fViewTab.getMemoryBlock();

		int lineOffset = Integer.valueOf(property, 16).intValue();
		
		long offset = getOffset(memory, line.getAddress(), lineOffset);
		
		// validate data
		if (!(value instanceof String))
			return;
					
		if (!(fViewTab instanceof MemoryViewTab))
			return;	
		
		try
		{
			byte[] bytes = null;
			
			String oldValue = (String)getValue(line, property);		
			
			if (!oldValue.equals(value))
			{	
				// try label provider
				IBaseLabelProvider labelProvider = ((MemoryViewTab)fViewTab).getTableViewer().getLabelProvider();
				if(labelProvider instanceof AbstractTableViewTabLabelProvider)
				{	
					int offsetToLine = Integer.valueOf(property, 16).intValue();
					int end = offsetToLine + fViewTab.getColumnSize();
					
					byte[] oldArray= line.getByteArray(offsetToLine, end);
					
					BigInteger address = new BigInteger(line.getAddress(), 16);
					address = address.add(BigInteger.valueOf(offsetToLine)); 
					
					bytes = ((AbstractTableViewTabLabelProvider)labelProvider).getRenderer().getBytes(fViewTab.getRenderingId(), address, oldArray, (String)value);
					
					if (bytes == null)
						return;
					
					if (bytes.length == 0)
						return;
				}
				else
					return;
			}
			else
			{	
				// return if value has not changed
				return;
			}
		
		
			memory.setValue(offset, bytes);
		}
		catch (DebugException e)
		{
			MemoryViewUtil.openError(DebugUIMessages.getString(TITLE), DebugUIMessages.getString(FAILED), e);
		}
		catch(NumberFormatException e)
		{
			MemoryViewUtil.openError(DebugUIMessages.getString(TITLE), 
					DebugUIMessages.getString(FAILED) + "\n" + DebugUIMessages.getString(DATA_IS_INVALID), null); //$NON-NLS-1$
		}
		
	}

	private long getOffset(IMemoryBlock memory, String lineAddress, int lineOffset) {
		
		BigInteger lineAddr = new BigInteger(lineAddress, 16);
		BigInteger memoryAddr;
		
		if (memory instanceof IExtendedMemoryBlock)
		{
			memoryAddr = ((IExtendedMemoryBlock)memory).getBigBaseAddress();
		}
		else
		{
			memoryAddr = BigInteger.valueOf(memory.getStartAddress());
		}
		
		if (memoryAddr == null)
			memoryAddr = new BigInteger("0"); //$NON-NLS-1$
		
		long offset = lineAddr.subtract(memoryAddr).longValue();
		
		return offset + lineOffset;
	}
	


	/**
	 * @param editActionInvoked The editActionInvoked to set.
	 */
	public void setEditActionInvoked(boolean editActionInvoked) {
		this.editActionInvoked = editActionInvoked;
	}

}
