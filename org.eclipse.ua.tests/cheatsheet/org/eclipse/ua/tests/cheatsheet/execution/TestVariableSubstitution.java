/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.cheatsheet.execution;

/**
 * Test variable substitution in cheatsheets. This functionality is used by 
 * command execution
 */

import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

import junit.framework.TestCase;

public class TestVariableSubstitution extends TestCase {
	private CheatSheetManager manager;
	
	@Override
	protected void setUp() throws Exception {
		manager = new CheatSheetManager(new CheatSheetElement("name"));
		manager.setData("p1", "one");
		manager.setData("p2", "two");
	}
	
	private String substitute(String input) {
		return manager.performVariableSubstitution(input);
	}

	public void testNoSubstitution() {
		assertEquals("abcdefg", substitute("abcdefg"));
	}

	public void testFullString() {
		assertEquals("one", substitute("${p1}"));
	}

	public void testEmbeddedString() {
		assertEquals("AoneB", substitute("A${p1}B"));
	}

	public void testRepeatedSubstitution() {
		assertEquals("oneXone", substitute("${p1}X${p1}"));
	}

	public void testMultipleSubstitution() {
		assertEquals("onetwo", substitute("${p1}${p2}"));
	}
	
	public void testNonexistentParameter() {
		assertEquals("one", substitute("${p1}${p3}"));
	}
	
	public void testUnterminatedParameter() {
		assertEquals("${p1", "${p1");
	}
	

}
