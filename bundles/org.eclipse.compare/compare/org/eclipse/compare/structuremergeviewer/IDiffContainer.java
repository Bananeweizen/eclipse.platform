/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.compare.structuremergeviewer;

/**
 * <code>IDiffContainer</code> is a <code>IDiffElement</code> with children.
 * <p>
 * <code>IDiffContainer</code> are the inner nodes displayed
 * by the <code>DiffTreeViewer</code>.
 * <code>IDiffContainer</code> are typically created as the result of performing
 * a compare with the <code>Differencer</code>.
 * <p>
 * Clients may implement this interface, or use one of the standard implementations,
 * <code>DiffContainer</code> or <code>DiffNode</code>.
 *
 * @see Differencer
 * @see DiffTreeViewer
 */
public interface IDiffContainer extends IDiffElement {

	/**
	 * Returns whether this container has at least one child.
	 * In some cases this methods avoids having to call the
	 * potential more costly <code>getChildren</code> method.
	 * 
	 * @return <code>true</code> if this container has at least one child 
	 */
	boolean hasChildren();

	/**
	 * Returns the children of this container.
	 * If this container has no children an empty array is returned (not <code>null</code>).
	 * 
	 * @return the children of this container as an array
	 */
	IDiffElement[] getChildren();

	/**
	 * Adds the given child to this container.
	 * If the child is already contained in this container, this method has no effect.
	 *
	 * @param child the child to be added to this container
	 */
	void add(IDiffElement child);
	
	/**
	 * Removes the given child from this container.
	 * If the container becomes empty it is removed from its container.
	 * If the child is not contained in this container, this method has no effect.
	 *
	 * @param child the child to be removed from this container
	 */
	void removeToRoot(IDiffElement child);
}
