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
package org.eclipse.debug.internal.ui.views.variables;

 
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

/**
 * Variables viewer. As the user steps through code, this
 * we ensure that newly added varibles are visible.
 */
public class VariablesViewer extends TreeViewer {

	private Item fNewItem;
	
	/**
	 * Constructor for VariablesViewer.
	 * @param parent
	 */
	public VariablesViewer(Composite parent) {
		super(parent);
	}

	/**
	 * Constructor for VariablesViewer.
	 * @param parent
	 * @param style
	 */
	public VariablesViewer(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Constructor for VariablesViewer.
	 * @param tree
	 */
	public VariablesViewer(Tree tree) {
		super(tree);
	}
	
	/**
	 * Refresh the view, and then do another pass to
	 * update the foreground color for values that have changed
	 * since the last refresh. Values that have not
	 * changed are drawn with the default system foreground color.
	 * If the viewer has no selection, ensure that new items
	 * are visible.
	 * 
	 * @see Viewer#refresh()
	 */
	public void refresh() {
		super.refresh();
		
		if (getSelection().isEmpty() && getNewItem() != null) {
			if (!getNewItem().isDisposed()) {
				//ensure that new items are visible
				showItem(getNewItem());
			}
			setNewItem(null);
		}
	}
	
	/**
	 * @see AbstractTreeViewer#newItem(Widget, int, int)
	 */
	protected Item newItem(Widget parent, int style, int index) {
		if (index != -1) {
			//ignore the dummy items
			setNewItem(super.newItem(parent, style, index));
			return getNewItem();
		} 
		return	super.newItem(parent, style, index);
	}
	
	protected Item getNewItem() {
		return fNewItem;
	}

	protected void setNewItem(Item newItem) {
		fNewItem = newItem;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#setExpandedElements(Object[])
	 */
	public void setExpandedElements(Object[] elements) {
		getControl().setRedraw(false);
		super.setExpandedElements(elements);
		getControl().setRedraw(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#collapseAll()
	 */
	public void collapseAll() {
		//see https://bugs.eclipse.org/bugs/show_bug.cgi?id=39449
		if (getRoot() != null) {
			super.collapseAll();
		}
	}

}
