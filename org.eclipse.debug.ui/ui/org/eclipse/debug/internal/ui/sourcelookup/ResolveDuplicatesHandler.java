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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Status handler to prompt for dupicate source element resolution.
 * 
 * @since 3.0
 */
public class ResolveDuplicatesHandler implements IStatusHandler {
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IStatusHandler#handleStatus(org.eclipse.core.runtime.IStatus, java.lang.Object)
	 */
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		Object[] args = (Object[])source;
		IStackFrame frame = (IStackFrame) args[0];
		List sources = (List) args[1];
		return resolveSourceElement(frame, sources);
	}
	
	public Object resolveSourceElement(IStackFrame frame, List sources) {
		Object file = null;
		sources = removeSourceNotFoundEditors(sources);
		if(sources.size() == 1) {
			return sources.get(0);
		} else if(sources.size() == 0) {
			return null;
		}
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(DebugUIPlugin.getShell(), new WorkbenchLabelProvider());
		dialog.setMultipleSelection(false);
		dialog.setTitle(SourceLookupUIMessages.getString("ResolveDuplicatesHandler.0")); //$NON-NLS-1$
		dialog.setMessage(SourceLookupUIMessages.getString("ResolveDuplicatesHandler.1")); //$NON-NLS-1$
		dialog.setElements(sources.toArray());
		dialog.open();											
		if(dialog.getReturnCode() == Window.OK) {
			file = dialog.getFirstResult();		
		}
		return file;
	}
	
	/**
	 * Remove extra source not found editors, if any.
	 * If multiple source not found editors and no "real" source inputs,
	 * return the first source not found editor.
	 * @param sources the list to be filtered
	 * @return the filtered list, may be empty
	 */
	private List removeSourceNotFoundEditors(List sources){
		Iterator iterator = sources.iterator();
		List filteredList = new ArrayList();
		Object next;
		while(iterator.hasNext()) {
			next = iterator.next();
			if (!(next instanceof CommonSourceNotFoundEditor)) {
				filteredList.add(next);
			}
		}
		if (filteredList.isEmpty() && sources.get(0) != null) {
			filteredList.add(sources.get(0));
		}
		return filteredList;
	}
}
