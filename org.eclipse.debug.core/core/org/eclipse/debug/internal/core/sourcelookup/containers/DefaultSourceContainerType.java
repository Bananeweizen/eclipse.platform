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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A default source lookup path. The default path is computed by a
 * source path computer.
 * 
 * @since 3.0
 */
public class DefaultSourceContainerType extends AbstractSourceContainerTypeDelegate {
	
	/**
	 * Unique identifier for the directory source container type
	 * (value <code>org.eclipse.debug.core.containerType.default</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.default"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#getMemento(org.eclipse.debug.internal.core.sourcelookup.ISourceContainer)
	 */
	public String getMemento(ISourceContainer container) throws CoreException {
		Document document = SourceLookupUtils.newDocument();
		Element element = document.createElement("default"); //$NON-NLS-1$
		document.appendChild(element);
		return SourceLookupUtils.serializeDocument(document);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#createSourceContainer(java.lang.String)
	 */
	public ISourceContainer createSourceContainer(String memento)throws CoreException {
		Node node = SourceLookupUtils.parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if ("default".equals(element.getNodeName())) { //$NON-NLS-1$
				return new DefaultSourceContainer();
			} else {
				abort(SourceLookupMessages.getString("DefaultSourceContainerType.6"), null); //$NON-NLS-1$
			}
		}
		abort(SourceLookupMessages.getString("DefaultSourceContainerType.7"), null); //$NON-NLS-1$
		return null;		
	}
	
}
