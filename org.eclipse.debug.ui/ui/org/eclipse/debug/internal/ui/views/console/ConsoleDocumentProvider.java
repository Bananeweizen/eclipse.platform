package org.eclipse.debug.internal.ui.views.console;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

/**
 * Default document provider for the processes. By default a document is created
 * which is contected to the streams proxy of the associated process.
 */
public class ConsoleDocumentProvider extends AbstractDocumentProvider {

	/**
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createDocument(java.lang.Object)
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		if (element instanceof IProcess) {
			IProcess process = (IProcess)element;
			IConsoleColorProvider contentProvider = getContentProvider(process);
			ConsoleDocument doc= new ConsoleDocument(contentProvider);
			ConsoleDocumentPartitioner partitioner = new ConsoleDocumentPartitioner(process, contentProvider);
			ConsoleLineNotifier lineNotifier = getLineNotifier(process);
			partitioner.connect(doc);
			if (lineNotifier != null) {
				partitioner.connectLineNotifier(lineNotifier);
			}
			return doc;
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createAnnotationModel(java.lang.Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element)
		throws CoreException {
		return null;
	}

	/**
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doSaveDocument(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
	 */
	protected void doSaveDocument(
		IProgressMonitor monitor,
		Object element,
		IDocument document,
		boolean overwrite)
		throws CoreException {
	}

	/**
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#disposeElementInfo(java.lang.Object, org.eclipse.ui.texteditor.AbstractDocumentProvider.ElementInfo)
	 */
	protected void disposeElementInfo(Object element, ElementInfo info) {
		ConsoleDocument document = (ConsoleDocument)info.fDocument; 
		document.getDocumentPartitioner().disconnect();
		super.disposeElementInfo(element, info);
	}

	/**
	 * Returns a content provider for the given process.
	 *  	 * @param process	 * @return IConsoleColorProvider	 */
	protected IConsoleColorProvider getContentProvider(IProcess process) {
		String type = process.getAttribute(IProcess.ATTR_PROCESS_TYPE);
		IConsoleColorProvider contentProvider = null;
		if (type != null) {
			contentProvider = ConsoleDocumentManager.getDefault().getColorProvider(type);
		}
		if (contentProvider == null) {
			contentProvider = new ConsoleColorProvider();
		}
		return contentProvider;
	}
	
	/**
	 * Returns the line notifier for this console, or <code>null</code> if none.
	 * 
	 * @param process
	 * @return line notifier, or <code>null</code>
	 */
	protected ConsoleLineNotifier getLineNotifier(IProcess process) {
		String type = process.getAttribute(IProcess.ATTR_PROCESS_TYPE);
		if (type != null) {
			return ConsoleDocumentManager.getDefault().newLineNotifier(type);
		}
		return null;
	}
}
