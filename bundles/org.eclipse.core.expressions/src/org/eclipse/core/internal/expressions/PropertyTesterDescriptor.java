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
import org.eclipse.core.runtime.IPluginDescriptor;

import org.eclipse.core.expressions.IPropertyTester;

/* package */ class PropertyTesterDescriptor implements IPropertyTester {
	
	private String fProperties;
	private String fNamespace;
	private IConfigurationElement fConfigElement;
	
	private static final String PROPERTIES= "properties"; //$NON-NLS-1$
	private static final String NAMESPACE= "namespace"; //$NON-NLS-1$
	private static final String CLASS= "class";  //$NON-NLS-1$
	
	public PropertyTesterDescriptor(IConfigurationElement element) {
		fConfigElement= element;
		StringBuffer buffer= new StringBuffer(","); //$NON-NLS-1$
		String properties= element.getAttribute(PROPERTIES);
		for (int i= 0; i < properties.length(); i++) {
			char ch= properties.charAt(i);
			if (!Character.isWhitespace(ch))
				buffer.append(ch);
		}
		buffer.append(',');
		fProperties= buffer.toString();
		fNamespace= fConfigElement.getAttribute(NAMESPACE);
	}
	
	public String getProperties() {
		return fProperties;
	}
	
	public String getNamespace() {
		return fNamespace;
	}
	
	public boolean handles(String namespace, String property) {
		return fNamespace.equals(namespace) && fProperties.indexOf("," + property + ",") != -1;  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public boolean isLoaded() {
		return false;
	}
	
	public boolean canLoad() {
		IPluginDescriptor plugin= fConfigElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		return plugin.isPluginActivated();
	}
	
	public IPropertyTester load() throws CoreException {
		return (IPropertyTester)fConfigElement.createExecutableExtension(CLASS);
	}
	
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		Assert.isTrue(false, "Method should never be called"); //$NON-NLS-1$
		return false;
	}
}
