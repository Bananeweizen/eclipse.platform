package org.eclipse.debug.internal.ui.views.variables;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

/**
 * Variables viewer. As the user steps through code, this
 * viewer renders variables that have changed with a 
 * different foreground color thereby drawing attention
 * to the values that have changed.
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

}