/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.expressions.ExpressionInfo;

public class ResolveExpression extends CompositeExpression {

	private String fVariable;
	private Object[] fArgs;
	
	private static final String ATT_VARIABLE= "variable";  //$NON-NLS-1$
	private static final String ATT_ARGS= "args";  //$NON-NLS-1$

	/**
	 * The seed for the hash code for all resolve expressions.
	 */
	private static final int HASH_INITIAL= ResolveExpression.class.getName().hashCode();
	
	public ResolveExpression(IConfigurationElement configElement) throws CoreException {
		fVariable= configElement.getAttribute(ATT_VARIABLE);
		Expressions.checkAttribute(ATT_VARIABLE, fVariable);
		fArgs= Expressions.getArguments(configElement, ATT_ARGS);
	}
	
	public ResolveExpression(String variable, Object[] args) {
		Assert.isNotNull(variable);
		fVariable= variable;
		fArgs= args;
	}

	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Object variable= context.resolveVariable(fVariable, fArgs);
		if (variable == null) {
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.VARIABLE_NOT_DEFINED,
				Messages.format(ExpressionMessages.ResolveExpression_variable_not_defined, fVariable))); 
		}
		return evaluateAnd(new EvaluationContext(context, variable));
	}
	
	public void collectExpressionInfo(ExpressionInfo info) {
		ExpressionInfo other= new ExpressionInfo();
		super.collectExpressionInfo(other);
		if (other.hasDefaultVariableAccess()) {
			info.addVariableNameAccess(fVariable);
		}
		info.mergeExceptDefaultVariable(other);
	}

	public boolean equals(final Object object) {
		if (!(object instanceof ResolveExpression))
			return false;
		
		final ResolveExpression that= (ResolveExpression)object;
		return this.fVariable.equals(that.fVariable) 
				&& equals(this.fArgs, that.fArgs) 
				&& equals(this.fExpressions, that.fExpressions);
	}

	public int hashCode() {
		if (fHashCode == HASH_CODE_NOT_COMPUTED) {
			fHashCode= HASH_INITIAL * HASH_FACTOR + hashCode(fExpressions);
			fHashCode= fHashCode * HASH_FACTOR + hashCode(fArgs);
			fHashCode= fHashCode * HASH_FACTOR + fVariable.hashCode();
			if (fHashCode == HASH_CODE_NOT_COMPUTED) {
				fHashCode++;
			}
		}
		return fHashCode;
	} 
}