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
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;

public class SystemTestExpression extends Expression {

	private String fProperty;
	private String fExpectedValue;
	
	private static final String ATT_PROPERTY= "property"; //$NON-NLS-1$
	
	public SystemTestExpression(IConfigurationElement element) throws CoreException {
		fProperty= element.getAttribute(ATT_PROPERTY);
		Expressions.checkAttribute(ATT_PROPERTY, fProperty);
		fExpectedValue= element.getAttribute(ATT_VALUE);
		Expressions.checkAttribute(ATT_VALUE, fExpectedValue);
	}
	
	public SystemTestExpression(String property, String expectedValue) {
		fProperty= property;
		fExpectedValue= expectedValue;
	}
	
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		String str= System.getProperty(fProperty);
		if (str == null) 
			return EvaluationResult.FALSE;
		return EvaluationResult.valueOf(str.equals(fExpectedValue));
	}
	
	//---- Debugging ---------------------------------------------------
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "<systemTest property=\"" + fProperty +  //$NON-NLS-1$
		  "\" value=\"" + fExpectedValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
