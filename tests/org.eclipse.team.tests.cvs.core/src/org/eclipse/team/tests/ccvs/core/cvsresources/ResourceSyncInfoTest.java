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
package org.eclipse.team.tests.ccvs.core.cvsresources;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ResourceSyncInfoTest extends EclipseTest {

	public ResourceSyncInfoTest() {
		super();
	}
	
	public ResourceSyncInfoTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ResourceSyncInfoTest.class);
		return new CVSTestSetup(suite);
	}
		
	public void testEntryLineParsing() {
		
		// testing malformed entry lines first
		try {
			new ResourceSyncInfo("//////", null, null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("//1.1///", null, null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("/file.txt////", null, null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
		try {
			new ResourceSyncInfo("/file.txt//////////", null, null);			
			fail();
		} catch(CVSException e) {
			// Error expected
		}
	}
	
	public void testEntryLineConstructor() throws CVSException {		
		ResourceSyncInfo info;
		info = new ResourceSyncInfo("/file.java/-1.1/Mon Feb 25 21:44:02 2002/-k/", null, null);
		assertTrue(info.isDeleted());
		
		info = new ResourceSyncInfo("/file.java/0/something/-k/", null, null);
		assertTrue(info.isAdded());
		
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, null);
		assertTrue(info.getTag() != null);
		
		Date timestamp = new Date(123000);
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/", null, timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		String permissions = "u=rwx,g=rwx,o=rwx";
		info = new ResourceSyncInfo("/file.java/2.0/Mon Feb 25 21:44:02 2002/-k/Tv1", permissions, null);
		assertTrue(info.getPermissions().equals(permissions));
		
		info = new ResourceSyncInfo("D/file.java////", null, null);
		assertTrue(info.isDirectory());
	}
	
	public void testConstructor() throws CVSException {
		ResourceSyncInfo info;
		
		info = new ResourceSyncInfo("folder");
		assertTrue(info.isDirectory());
		
		info = new ResourceSyncInfo("/file.java/-2.34/Mon Feb 25 21:44:02 2002/-k/Tv1", null, null);
		assertTrue(info.isDeleted());
		assertTrue(info.getRevision().equals("2.34"));
		
		info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, null);
		assertTrue(info.isAdded());
	}
	
	public void testMergeTimestamps() throws CVSException {
		ResourceSyncInfo info, info2;
		Date timestamp = new Date(123000);
		Date timestamp2 = new Date(654000);
				
		info = new ResourceSyncInfo("/file.java/1.1//-kb/", null, timestamp);
		assertTrue(!info.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp));		
		
		// test merged entry lines the server and ensure that their entry line format is compatible
		info = new ResourceSyncInfo("/file.java/1.1/+=/-kb/", null, timestamp);
		String entryLine = info.getEntryLine();
		info2 = new ResourceSyncInfo(entryLine, null, null);
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(info.isNeedsMerge(timestamp) && info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp().equals(timestamp) && info2.getTimeStamp().equals(timestamp));		

		info = new ResourceSyncInfo("/file.java/1.1/+modified/-kb/", null, null);
		entryLine = info.getEntryLine();
		info2 = new ResourceSyncInfo(entryLine, null, null);	
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp) && !info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp()==null && info2.getTimeStamp()==null);
	}
	
	public void testTimestampCompatibility() throws CVSException, CoreException {
		String entryLine1 = "/a.bin/1.1/Mon Feb  9 21:44:02 2002/-kb/";
		String entryLine2 = "/a.bin/1.1/Mon Feb 9 21:44:02 2002/-kb/";
		String entryLine3 = "/a.bin/1.1/Mon Feb 09 21:44:02 2002/-kb/";		
		ResourceSyncInfo info1 = new ResourceSyncInfo(entryLine1, null, null);
		ResourceSyncInfo info2 = new ResourceSyncInfo(entryLine2, null, null);
		ResourceSyncInfo info3 = new ResourceSyncInfo(entryLine3, null, null);
		Date date1 = info1.getTimeStamp();
		Date date2 = info2.getTimeStamp();
		Date date3 = info3.getTimeStamp();
		assertTrue(date1.equals(date2));
		assertTrue(date1.equals(date3));
		assertTrue(date2.equals(date3));
	}
	
	public void testRevisionComparison() {
		assertTrue(ResourceSyncInfo.isLaterRevision("1.9", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.9"));
		
		assertTrue(ResourceSyncInfo.isLaterRevision("1.8.1.2", "1.8"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8", "1.8.1.2"));
		assertTrue( ! ResourceSyncInfo.isLaterRevision("1.8.1.2", "1.7"));
		
		assertTrue( ! ResourceSyncInfo.isLaterRevision("0", "1.1"));
		assertTrue(ResourceSyncInfo.isLaterRevision("1.1", "0"));
	}
	
	public void testRevisionOnBranchComparison() throws CVSException {
		ResourceSyncInfo syncInfo1 = new ResourceSyncInfo("/name/1.5/dummy timestamp//", null, null);
		ResourceSyncInfo syncInfo2 = new ResourceSyncInfo("/name/1.4/dummy timestamp//", null, null);
		
		ResourceSyncInfo syncInfo3 = new ResourceSyncInfo("/name/1.4.1.2/dummy timestamp//Nb1", null, null);
		ResourceSyncInfo syncInfo4 = new ResourceSyncInfo("/name/1.4/dummy timestamp//Nb1", null, null);
		
		ResourceSyncInfo syncInfo5 = new ResourceSyncInfo("/name/1.4.1.2/dummy timestamp//Tv1", null, null);
		
		assertTrue(ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo1.getBytes(), syncInfo2.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo2.getBytes(), syncInfo1.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo1.getBytes(), syncInfo1.getBytes()));
		
		assertTrue(ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo3.getBytes(), syncInfo4.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo3.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo4.getBytes()));
		
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo5.getBytes(), syncInfo4.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo4.getBytes(), syncInfo5.getBytes()));
		assertTrue( ! ResourceSyncInfo.isLaterRevisionOnSameBranch(syncInfo5.getBytes(), syncInfo5.getBytes()));
	}
}
