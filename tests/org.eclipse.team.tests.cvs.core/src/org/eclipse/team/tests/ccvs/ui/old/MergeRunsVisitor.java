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


public class MergeRunsVisitor implements ILogEntryVisitor {
	private RootEntry defaultRoot;
	private RootEntry root;
	private LogEntryContainer parent;
	
	/**
	 * Creates a new visitor to merge series of log entries.
	 * @param root the root of an existing log to merge into, or null
	 */
	public MergeRunsVisitor(RootEntry root) {
		this.defaultRoot = root;
		this.parent = null;
	}
	
	/**
	 * Returns the root of the newly merged log, or null if none.
	 */
	public RootEntry getMergedRoot() {
		return root;
	}
	
	public void visitRootEntry(RootEntry entry) {
		root = defaultRoot;
		if (root == null) {
			root = new RootEntry(null, entry.getName(), entry.getSDKBuildId(), entry.getTimestamp());
		} 
		parent = root;
		entry.acceptChildren(this);
	}

	public void visitCaseEntry(CaseEntry entry) {
		LogEntryContainer oldParent = parent; 
		CaseEntry newEntry = (CaseEntry) parent.findMember(entry.getName(), CaseEntry.class);
		if (newEntry == null) {
			newEntry = new CaseEntry(parent, entry.getName(), entry.getClassName());
		}
		parent = newEntry;
		entry.acceptChildren(this);
		parent = oldParent;
	}

	public void visitGroupEntry(GroupEntry entry) {
		LogEntryContainer oldParent = parent;
		GroupEntry newEntry = (GroupEntry) parent.findMember(entry.getName(), GroupEntry.class);
		if (newEntry == null) {
			newEntry = new GroupEntry(parent, entry.getName());
		}
		parent = newEntry;
		entry.acceptChildren(this);
		parent = oldParent;
	}

	public void visitTaskEntry(TaskEntry entry) {
		TaskEntry newEntry = (TaskEntry) parent.findMember(entry.getName(), TaskEntry.class);
		if (newEntry == null) {
			newEntry = new TaskEntry(parent, entry.getName());
		}
		Result[] results = entry.getResults();
		for (int i = 0; i < results.length; i++) {
			newEntry.addResult(results[i]);
		}
	}
}
