/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tests.runtime.jobs;

/**
 * 
 */
class CustomTestJob extends TestJob {
	private int familyType = TestJobFamily.TYPE_NONE;
	public CustomTestJob(String name, int type) {
		super(name);
		familyType = type;
	}
	
	public CustomTestJob(String name, int ticks, int tickLength, int type) {
		super(name, ticks, tickLength);
		familyType = type;
	}
	
	public boolean belongsTo(Object family) {
		return ((family instanceof TestJobFamily) && (((TestJobFamily)family).getType() == familyType));
	}

}