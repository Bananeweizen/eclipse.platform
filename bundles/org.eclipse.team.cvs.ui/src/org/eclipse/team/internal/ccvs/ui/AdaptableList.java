package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.internal.model.WorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A list of adaptable objects.  This is a generic list that can
 * be used to display an arbitrary set of adaptable objects in the workbench.
 * Also implements the IWorkbenchAdapter interface for simple display
 * and navigation.
 */
public class AdaptableList extends WorkbenchAdapter implements IAdaptable {
	protected List children = new ArrayList();
	/**
	 * Creates a new adaptable list with the given children.
	 */
	public AdaptableList() {
	}
	/**
	 * Creates a new adaptable list with the given children.
	 */
	public AdaptableList(IAdaptable[] newChildren) {
		for (int i = 0; i < newChildren.length; i++) {
			children.add(newChildren[i]);
		}
	}
	/**
	 * Adds all the adaptable objects in the given enumeration to this list.
	 * Returns this list.
	 */
	public AdaptableList add(Iterator e) {
		while (e.hasNext()) {
			add((IAdaptable)e.next());
		}
		return this;
	}
	/**
	 * Adds the given adaptable object to this list.  Returns this list.
	 */
	public AdaptableList add(IAdaptable a) {
		children.add(a);
		return this;
	}
	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	/**
	 * Returns the elements in this list.
	 */
	public Object[] getChildren() {
		return children.toArray();
	}
	/**
	 * Returns the elements in this list.
	 * @see IWorkbenchAdapter#getChildren
	 */
	public Object[] getChildren(Object o) {
		return children.toArray();
	}
	/**
	 * Adds the given adaptable object to this list.
	 */
	public void remove(IAdaptable a) {
		children.remove(a);
	}
	/**
	 * Returns the number of items in the list
	 */
	public int size() {
		return children.size();
	}
}