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
package org.eclipse.core.expressions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.internal.expressions.CompositeExpression;
import org.eclipse.core.internal.expressions.StandardElementHandler;


/**
 * An element handler converts a {@link IConfigurationElement} into a 
 * corresponding expression object. If the configuration element represents
 * a composite expression (like and, or, adapt, ...) then the method <code>
 * processChildren</code> should be used to convert the children as well.
 * <p>
 * The class should be subclassed by clients wishing to provide an element
 * handler for special expressions.
 * </p>
 * @since 3.0 
 */
public abstract class ElementHandler {
	
	private static final ElementHandler INSTANCE= new StandardElementHandler();
	
	/**
	 * The default element handler which can cope with all XML expression elements
	 * defined by the common expression language.
	 * 
	 * @return the default element handler
	 */
	public static ElementHandler getDefault() {
		return INSTANCE;
	}
	
	/**
	 * Creates the corresponding expression for the given configuration element.
	 * 
	 * @param converter the expression converter used to initiate the
	 *  conversion process
	 * 
	 * @param config the configuration element to convert
	 * 
	 * @return the corresponding expression
	 * 
	 * @throws CoreException if the conversion failed
	 */
	public abstract Expression create(ExpressionConverter converter, IConfigurationElement config) throws CoreException;
	
	/**
	 * Converts the children of the given configuration element and adds them 
	 * to the given composite expression.
	 * 
	 * @param converter the converter used to do the actual conversion
	 * @param element the configuration element for which the children 
	 *  are to be processed
	 * @param expression the composite expression representing the result
	 *  of the conversion
	 * 
	 * @throws CoreException if the conversion failed
	 */
	protected void processChildren(ExpressionConverter converter, IConfigurationElement element, CompositeExpression expression) throws CoreException {
		converter.processChildren(element, expression);
	}
}
