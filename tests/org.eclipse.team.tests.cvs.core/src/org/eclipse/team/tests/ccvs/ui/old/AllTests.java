/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui.old;


import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(SyncTests.class);
		suite.addTestSuite(WorkflowTests.class);
		//suite.addTestSuite(CommandTests.class);
    	return new BenchmarkTestSetup(suite);
	}	
	
	public AllTests(String name) {
		super(name);
	}
	public AllTests() {
		super();
	}
}
