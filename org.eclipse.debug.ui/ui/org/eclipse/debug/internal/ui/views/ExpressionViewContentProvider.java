package org.eclipse.debug.internal.ui.views;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
 
/**
 * Provides contents for the expression view
 */
public class ExpressionViewContentProvider extends VariablesViewContentProvider {

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		Object[] children= null;
		try {
			if (parent instanceof IExpressionManager) {
				// do not cache parents
				return ((IExpressionManager)parent).getExpressions();
			} else if (parent instanceof IExpression) {
				children = ((IExpression)parent).getValue().getVariables();
			} else if (parent instanceof IVariable) {
				children = ((IVariable)parent).getValue().getVariables();
			}
			if (children != null) {
				cache(parent, children);
				return children;
			}
		} catch (DebugException e) {
			DebugUIPlugin.logError(e);
		}
		return new Object[0];
	}
	
	/**
	 * @see ITreeContentProvider
	 */
	public Object getParent(Object item) {
		if (item instanceof IExpression) {
			return DebugPlugin.getDefault().getExpressionManager();
		}
		return super.getParent(item);
	}	
}
