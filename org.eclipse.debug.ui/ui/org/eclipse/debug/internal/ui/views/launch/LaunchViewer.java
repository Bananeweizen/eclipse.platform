/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;


import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.ILazyLabelListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * The launch viewer displays a tree of launches.
 */
public class LaunchViewer extends TreeViewer implements ILazyLabelListener {
	
	LaunchView fView;
		
	/**
	 * Overridden to fix bug 39709 - duplicate items in launch viewer. The
	 * workaround is required since debug creation events (which result in
	 * additions to the tree) are processed asynchrnously with the expanding
	 * of a launch/debug target in the tree. 
	 * 
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#add(java.lang.Object, java.lang.Object)
	 */
	public void add(Object parentElement, Object childElement) {
		if (doFindItem(childElement) == null) {
			super.add(parentElement, childElement);
		}
	}

	public LaunchViewer(Composite parent, LaunchView view) {
		super(new Tree(parent, SWT.MULTI));
		setUseHashlookup(true);
		fView= view;
	}
			
	/**
	 * Update the icons for all stack frame children of the given thread.
	 */	
	protected void updateStackFrameIcons(IThread parentThread) {
		Widget parentItem= findItem(parentThread);
		if (parentItem != null) {
			Item[] items= getItems((Item)parentItem);
			for (int i = 0; i < items.length; i++) {
				TreeItem treeItem = (TreeItem)items[i];
				updateOneStackFrameIcon(treeItem, (IStackFrame)treeItem.getData());
			}
		}
	}
	
	/**
	 * For the given stack frame and associated TreeItem, update the icon on the
	 * TreeItem.
	 */
	protected void updateOneStackFrameIcon(TreeItem treeItem, IStackFrame stackFrame) {
		ILabelProvider provider = (ILabelProvider) getLabelProvider();
		Image image = provider.getImage(stackFrame);
		if (image != null) {
			treeItem.setImage(image);
		}			
	}
	
	/**
	 * @see StructuredViewer#refresh(Object)
	 */
	public void refresh(Object element) {
		//@see bug 7965 - Debug view refresh flicker
		getControl().setRedraw(false);
		super.refresh(element);
		getControl().setRedraw(true);
	}

	/*
	 * @see org.eclipse.debug.ui.ILazyLabelListener#lazyTextComputed(java.lang.Object, java.lang.String)
	 */
	public void lazyTextsComputed(final Object[] elements,final String[] texts) {
		fView.asyncExec(new Runnable() {
			public void run() {
				for (int i= 0, length= elements.length; i < length; i++) {
					((Item)findItem(elements[i])).setText(texts[i]);
				}
			}
		});
	}

	/*
	 * @see org.eclipse.debug.ui.ILazyLabelListener#lazyImagesComputed(Object[], Image[])
	 */
	public void lazyImagesComputed(final Object[] elements, final Image[] images) {
		fView.asyncExec(new Runnable() {
			public void run() {
				for (int i= 0, length= elements.length; i < length; i++) {
					((Item)findItem(elements[i])).setImage(images[i]);
				}
			}
		});
	}
}

