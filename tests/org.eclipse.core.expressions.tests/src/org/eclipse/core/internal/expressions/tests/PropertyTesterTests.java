/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;

import org.eclipse.core.internal.expressions.Property;
import org.eclipse.core.internal.expressions.TestExpression;
import org.eclipse.core.internal.expressions.TypeExtensionManager;

import org.osgi.framework.Bundle;

public class PropertyTesterTests extends TestCase {
	
	private A a;
	private B b;
	private I i;

	private static final TypeExtensionManager fgManager= new TypeExtensionManager("propertyTesters"); //$NON-NLS-1$
	
	// Needs additional local test plug-ins
	private static final boolean TEST_DYNAMIC_AND_ACTIVATION= false;
	
	public static Test suite() {
		return new TestSuite(PropertyTesterTests.class);
	}
	
	protected void setUp() throws Exception {
		a= new A();
		b= new B();
		i= b;
	}
	
	public void testSimple() throws Exception {
		assertTrue(test(a, "simple", null,"simple")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "simple", null,"simple")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testInherited() throws Exception {
		assertTrue(test(b, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(b, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testUnknown() throws Exception {
		try {
			test(a, "unknown", null, null); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);
	}
	
	public void testOverridden() throws Exception {
		assertTrue(test(a, "overridden", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		A b_as_a= b;
		assertTrue(test(b_as_a, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "overridden", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b_as_a, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testOdering() throws Exception {
		assertTrue(test(b, "ordering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		I other= new I() {};
		assertTrue(test(other, "ordering", null, "I")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(b, "ordering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(other, "ordering", null, "I")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testChaining() throws Exception {
		assertTrue(test(a, "chaining", null, "A2")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "chaining", null, "A2")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	// This test is questionable. It depends on if core runtime can
	// guaratee any ordering in the plug-in registry.
	public void testChainOrdering() throws Exception {
		assertTrue(test(a, "chainOrdering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "chainOrdering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testWrongNameSpace() throws Exception {
		try {
			test(a, "differentNamespace", null, null); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);		
	}
	
	public void testDynamicPlugin() throws Exception {
		if (!TEST_DYNAMIC_AND_ACTIVATION)
			return;
		
		A receiver= new A();
		Property p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(!p.isInstantiated());
		Bundle bundle= Platform.getBundle("org.eclipse.core.expressions.tests.dynamic"); //$NON-NLS-1$
		bundle.start();
		p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(p.isInstantiated());
		bundle.stop();
		p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(!p.isInstantiated());
	}
	
	public void testPluginActivation() throws Exception {
		if (!TEST_DYNAMIC_AND_ACTIVATION)
			return;
		
		Bundle bundle= Platform.getBundle("org.eclipse.core.expressions.tests.forceActivation"); //$NON-NLS-1$
		assertTrue(bundle.getState() == Bundle.RESOLVED);
		
		A receiver= new A();
		TestExpression exp= new TestExpression("org.eclipse.core.expressions.tests.forceActivation", "testing", null, null, true);
		EvaluationContext context= new EvaluationContext(null, receiver);
		EvaluationResult result= exp.evaluate(context);
		assertTrue(result == EvaluationResult.NOT_LOADED);
		assertTrue(bundle.getState() == Bundle.RESOLVED);
		Property p= TestExpression.testGetTypeExtensionManager().getProperty(receiver, "org.eclipse.core.expressions.tests.forceActivation", "testing", false); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(!p.isInstantiated());
		
		context.setAllowPluginActivation(true);
		exp.evaluate(context);
		assertTrue(bundle.getState() == Bundle.ACTIVE);
		p= TestExpression.testGetTypeExtensionManager().getProperty(receiver, "org.eclipse.core.expressions.tests.forceActivation", "testing", false); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(p.isInstantiated());
	}
	
	public void testDifferentNameSpace() throws Exception {
		assertTrue(test("org.eclipse.core.internal.expressions.tests2", a, "differentNamespace", null, "A3"));		 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	private boolean test(Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		Property p= fgManager.getProperty(receiver, "org.eclipse.core.internal.expressions.tests", property); //$NON-NLS-1$
		assertTrue(p.isInstantiated());
		return p.test(receiver, args, expectedValue);
	}
	
	private boolean test(String namespace, Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		Property p= fgManager.getProperty(receiver, namespace, property);
		assertTrue(p.isInstantiated());
		return p.test(receiver, args, expectedValue);
	}	
}
