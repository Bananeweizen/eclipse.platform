/**********************************************************************
Copyright (c) 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.ui.externaltools.internal.ant.editor.outline;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * XMLCore.java
 */
public class XMLCore {
	
	private static XMLCore inst;
	
	public static XMLCore getDefault() {
		if (inst == null) {
			inst= new XMLCore();
		}
			
		return inst;
	}
	
	private ArrayList fModelChangeListeners= new ArrayList();
	
	private XMLCore() { }

	public void addDocumentModelListener(IDocumentModelListener listener) {
		synchronized (fModelChangeListeners) {
			fModelChangeListeners.add(listener);
		}
	}
	
	public void removeDocumentModelListener(IDocumentModelListener listener) {
		synchronized (fModelChangeListeners) {
			fModelChangeListeners.remove(listener);
		}
	}
	
	public void notifyDocumentModelListeners(DocumentModelChangeEvent event) {
		Iterator i;
		synchronized (fModelChangeListeners) {
			i= new ArrayList(fModelChangeListeners).iterator();
		}
		while (i.hasNext()) {
			((IDocumentModelListener)i.next()).documentModelChanged(event);
		}
	}
}