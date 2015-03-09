/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.swt.widgets.Tree;

public class TreeTextOperationTarget implements ITextOperationTarget {

	private Tree tree;

	public TreeTextOperationTarget(Tree tree) {
		this.tree = tree;
	}

	/**
	 * @see org.eclipse.jface.text.ITextOperationTarget#canDoOperation(int)
	 */
	@Override
	public boolean canDoOperation(int operation) {
		return true;
	}

	/**
	 * @see org.eclipse.jface.text.ITextOperationTarget#doOperation(int)
	 */
	@Override
	public void doOperation(int operation) {
		//TODO: add support to other operations
		switch (operation) {
			case ITextOperationTarget.SELECT_ALL :
				tree.selectAll();
		}
	}
}
