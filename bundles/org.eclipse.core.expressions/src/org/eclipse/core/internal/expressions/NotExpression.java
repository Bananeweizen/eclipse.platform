/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.core.internal.expressions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;

public class NotExpression extends Expression {

	private Expression fExpression;

	public NotExpression(Expression expression) {
		Assert.isNotNull(expression);
		fExpression= expression;
	}
	
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		return fExpression.evaluate(context).not();
	}
}
