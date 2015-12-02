/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.toc;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpPlugin;

public class TocProviderTest extends TestCase {
	
	/*
	 * Returns an instance of this Test.
	 */
	public static Test suite() {
		return new TestSuite(TocProviderTest.class);
	}

	/**
	 * Verify that the tocProvider extension in this plug-in contributes a TOC which can be linked to an anchor
	 */
	public void testTocProvider() throws Exception {
		IToc[] tocs = HelpPlugin.getTocManager().getTocs("en");
		IToc uaToc = null;
		for (IToc toc : tocs) {
			if ("User Assistance Tests".equals(toc.getLabel())) {
				uaToc = toc;
			}
		}
		assertNotNull("User Assistance Tests not found", uaToc);
		ITopic[] children = uaToc.getTopics();
		int generatedParentTopics = 0;
		for (ITopic child : children) {
			if ("Generated Parent".equals(child.getLabel())) {
				generatedParentTopics++;
				assertEquals(4, child.getSubtopics().length);
			}
		}
		assertEquals(1, generatedParentTopics);
		
	}

}
