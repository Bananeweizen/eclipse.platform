package org.eclipse.debug.internal.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;

public class CopyToClipboardActionDelegate extends ControlActionDelegate {
	
	private ContentViewer fViewer;
	
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
		super.init(view);
		setViewer((ContentViewer)view.getViewSite().getSelectionProvider());

	}
	
	/**
	 * @see ControlActionDelegate#isEnabledFor(Object)
	 */
	public boolean isEnabledFor(Object element) {
		return element instanceof IDebugElement;
	}

	/**
	 * @see ControlActionDelegate#doAction(Object)
	 */
	protected void doAction(Object element, StringBuffer buffer) {
		append(element, buffer, (ILabelProvider)getViewer().getLabelProvider(), 0);
	}
	
	/**
	 * @see ControlActionDelegate#doAction(Object)
	 */
	protected void doAction(Object element) {
		StringBuffer buffer= new StringBuffer();
		doAction(element, buffer);
		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		Clipboard clipboard= new Clipboard(getViewer().getControl().getDisplay());
		clipboard.setContents(
			new String[]{buffer.toString()}, 
			new Transfer[]{plainTextTransfer});
	}


	/** 
	 * Appends the representation of the specified element (using the label provider and indent)
	 * to the buffer.  For elements down to stack frames, children representations
	 * are append to the buffer as well.
	 */
	protected void append(Object e, StringBuffer buffer, ILabelProvider lp, int indent) {
		for (int i= 0; i < indent; i++) {
			buffer.append('\t');
		}
		buffer.append(lp.getText(e));
		buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
		if (shouldAppendChildren(e)) {
			Object[] children= new Object[0];
			children= getChildren(e);
			for (int i = 0;i < children.length; i++) {
				Object de= children[i];
				append(de, buffer, lp, indent + 1);
			}
		}
	}

	/**
	 * @see ControlActionDelegate#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IDebugHelpContextIds.COPY_TO_CLIPBOARD_ACTION;
	}
	
	protected Object getParent(Object e) {
		return ((ITreeContentProvider) getViewer().getContentProvider()).getParent(e);
	}
	
	/**
	 * Returns the children of the parent after applying the filters
	 * that are present in the viewer.
	 */
	protected Object[] getChildren(Object parent) {
		Object[] children= ((ITreeContentProvider)getViewer().getContentProvider()).getChildren(parent);
		ViewerFilter[] filters= ((StructuredViewer)getViewer()).getFilters();
		if (filters != null) {
			for (int i= 0; i < filters.length; i++) {
				ViewerFilter f = filters[i];
				children = f.filter(getViewer(), parent, children);
			}
		}
		return children;
	}
	
	/**
	 * Do the specific action using the current selection.
	 */
	public void run() {
		final Iterator iter= pruneSelection();
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				StringBuffer buffer= new StringBuffer();
				while (iter.hasNext()) {
					doAction(iter.next(), buffer);
				}
				TextTransfer plainTextTransfer = TextTransfer.getInstance();
				Clipboard clipboard= new Clipboard(getViewer().getControl().getDisplay());		
				clipboard.setContents(
					new String[]{buffer.toString()}, 
					new Transfer[]{plainTextTransfer});
			}
		});
	}
	
	/**
	 * Removes the duplicate items from the selection.
	 * That is, if both a parent and a child are in a selection
	 * remove the child.
	 */
	protected Iterator pruneSelection() {
		IStructuredSelection selection= (IStructuredSelection)getViewer().getSelection();
		List elements= new ArrayList(selection.size());
		Iterator iter= selection.iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (isEnabledFor(element)) {
				if(walkHierarchy(element, elements)) {
					elements.add(element);
				}
			}
		}
		return elements.iterator();
	}
	
	/**
	 * Returns whether the parent of the specified
	 * element is already contained in the collection.
	 */
	protected boolean walkHierarchy(Object element, List elements) {
		Object parent= getParent(element);
		if (parent == null) {
			return true;
		}
		if (elements.contains(parent)) {
			return false;
		}
		return walkHierarchy(parent, elements);		
	}

	/**
	 * @see ControlActionDelegate#setActionImages(IAction)
	 */
	protected void setActionImages(IAction action) { 	
		action.setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_COPY));
		action.setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_COPY));
		action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_COPY));
	}
	
	protected boolean shouldAppendChildren(Object e) {
		return e instanceof IDebugTarget || e instanceof IThread;
	}
	
	/**
	 * @see ControlActionDelegate#getStatusMessage()
	 */
	protected String getStatusMessage() {
		return ActionMessages.getString("CopyToClipboardActionDelegate.Copy_failed"); //$NON-NLS-1$
	}

	/**
	 * @see ControlActionDelegate#getErrorDialogMessage()
	 */
	protected String getErrorDialogMessage() {
		return null;
	}

	/**
	 * @see ControlActionDelegate#getErrorDialogTitle()
	 */
	protected String getErrorDialogTitle() {
		return null;
	}
	/**
	 * @see ControlActionDelegate#getToolTipText()
	 */
	protected String getToolTipText() {
		return ActionMessages.getString("CopyToClipboardActionDelegate.Copy_to_Clipboard_2"); //$NON-NLS-1$
	}
	/**
	 * @see ControlActionDelegate#getText()
	 */
	protected String getText() {
		return ActionMessages.getString("CopyToClipboardActionDelegate.&Copy_Stack_3"); //$NON-NLS-1$
	}
	
	protected ContentViewer getViewer() {
		return fViewer;
	}

	protected void setViewer(ContentViewer viewer) {
		fViewer = viewer;
	}
}