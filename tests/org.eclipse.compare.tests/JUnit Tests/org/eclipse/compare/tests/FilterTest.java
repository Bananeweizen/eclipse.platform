/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import org.eclipse.compare.internal.CompareFilter;

import junit.framework.*;
import junit.framework.TestCase;

public class FilterTest extends TestCase {
	
	CompareFilter fFilter;
	
	public FilterTest(String name) {
		super(name);
	}
		
	public void testFilterFile() {
		CompareFilter f= new CompareFilter();
		f.setFilters("*.class");
		Assert.assertTrue("file foo.class should be filtered", f.filter("foo.class", false, false));
		Assert.assertFalse("file foo.java shouldn't be filtered", f.filter("foo.java", false, false));
	}
	
	public void testFilterDotFile() {
		CompareFilter f= new CompareFilter();
		f.setFilters(".cvsignore");
		Assert.assertTrue("file .cvsignore should be filtered", f.filter(".cvsignore", false, false));
		Assert.assertFalse("file foo.cvsignore shouldn't be filtered", f.filter("foo.cvsignore", false, false));
	}
	
	public void testFilterFolder() {
		CompareFilter f= new CompareFilter();
		f.setFilters("bin/");
		Assert.assertTrue("folder bin should be filtered", f.filter("bin", true, false));
		Assert.assertFalse("file bin shouldn't be filtered", f.filter("bin", false, false));
	}
	
	public void testMultiFilter() {
		CompareFilter f= new CompareFilter();
		f.setFilters("*.class, .cvsignore, bin/");
		Assert.assertTrue("file foo.class should be filtered", f.filter("foo.class", false, false));
		Assert.assertFalse("file foo.java shouldn't be filtered", f.filter("foo.java", false, false));
		Assert.assertTrue("file .cvsignore should be filtered", f.filter(".cvsignore", false, false));
		Assert.assertFalse("file foo.cvsignore shouldn't be filtered", f.filter("foo.cvsignore", false, false));
		Assert.assertTrue("folder bin should be filtered", f.filter("bin", true, false));
		Assert.assertFalse("file bin shouldn't be filtered", f.filter("bin", false, false));
	}
	
	public void testVerify() {
		//Assert.assertNull("filters don't verify", Filter.validateResourceFilters("*.class, .cvsignore, bin/"));
		//Assert.assertNotNull("filters shouldn't verify", Filter.validateResourceFilters("bin//"));
	}
}
