/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.core.filehistory;

import java.net.URI;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;

public class CVSFileRevision extends FileRevision {

	protected ILogEntry entry;

	public CVSFileRevision(ILogEntry entry) {
		this.entry = entry;
	}

	public long getTimestamp() {
		return entry.getDate().getTime();
	}

	public String getAuthor() {
		return entry.getAuthor();
	}

	public String getComment() {
		return entry.getComment();
	}

	public boolean isPredecessorOf(IFileRevision revision) {
		long compareRevisionTime = revision.getTimestamp();
		return (this.getTimestamp() < compareRevisionTime);
	}

	public boolean isDescendentOf(IFileRevision revision) {
		long compareRevisionTime = revision.getTimestamp();
		return (this.getTimestamp() > compareRevisionTime);
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		RemoteFile remoteFile = (RemoteFile) entry.getRemoteFile();
		return remoteFile.getStorage(monitor);
	}

	public String getName(){
		return entry.getRemoteFile().getName();
	}
	
	public String getContentIdentifier() {
		return entry.getRevision();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof CVSFileRevision){
			CVSFileRevision objRevision = (CVSFileRevision) obj;
			ICVSRemoteFile remFile = objRevision.getCVSRemoteFile();
			if (remFile.equals(this.getCVSRemoteFile()) &&
				objRevision.getContentIdentifier().equals(this.getContentIdentifier()))
				return true;
		}
		return false;
	}

	public URI getURI() {
		return null;//return entry.getRemoteFile().
	}

	public ITag[] getTags() {
		return entry.getTags();
	}

	public boolean exists() {
		return !entry.isDeletion();
	}
	
	public ICVSRemoteFile getCVSRemoteFile(){
		return entry.getRemoteFile();
	}
	
	public boolean isPropertyMissing() {
		//If we have an author and a comment then we consider this revision complete
		if (entry.getAuthor() == null)
			return true;
		
		return false;
	}
	
	
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return new CVSFileRevision(getCVSRemoteFile().getLogEntry(monitor));
	}
}
