package org.eclipse.team.ui.sync;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.InputStream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ui.Policy;

/**
 * Encapsulates information about a resource that requires
 * contact with the Team API.
 */
public class MergeResource {
	private IRemoteSyncElement syncTree;
	
	/**
	 * Creates a new merge resource based on the given sync information.
	 */
	MergeResource(IRemoteSyncElement syncTree) {
		this.syncTree = syncTree;
	}
	
	/**
	 * The user has manually merged contents from the server into
	 * the local resource.  Clear the incoming changes from the server.
	 */
	public void confirmMerge() {
	//	manager.confirmMerge(syncTree.getResource());
	}
	
	/**
	 * Returns an InputStream for the base revision of this incoming resource.
	 */
	public InputStream getBaseRevision() throws CoreException {
		IRemoteResource remote = syncTree.getBase();
		if (remote != null && !remote.isContainer()) {
			try {
				return remote.getContents(null);
			} catch (TeamException exception) {
				// The remote resource has gone.
				return null;
			}
		}
		return null;
	}
	
	public String getExtension() {
		if (syncTree.isContainer()) {
			return ITypedElement.FOLDER_TYPE;
		}
		String name = getName();
		if (name != null) {
			int index = name.lastIndexOf('.');
			if (index == -1)
				return "";
			if (index == (name.length() - 1))
				return "";
			return name.substring(index + 1);
		}
		return ITypedElement.FOLDER_TYPE;
	}
	
	/**
	 * Returns an InputStream for the latest repository version of this incoming resource.
	 */
	public InputStream getLatestRevision() throws CoreException {
		IRemoteResource remote = syncTree.getRemote();
		try {
			return remote.getContents(null);
		} catch (TeamException e) {
			throw new CoreException(e.getStatus());
		}
	}
	
	/**
	 * Returns an InputStream for the local resource.
	 */
	public InputStream getLocalStream() throws CoreException {
		IResource left = syncTree.getLocal();
		if (left == null) return null;
		if (left.exists() && left.getType() == IResource.FILE) {
			return ((IFile)left).getContents(true);
		}
		return null;
	}

	public String getName() {
		return syncTree.getName();
	}
	
	/*
	 * @see IMergeResource#getResource.
	 */
	public IResource getResource() {
		return syncTree.getLocal();
	}

	public IRemoteSyncElement getSyncElement() {
		return syncTree;
	}
	
	/**
	 * Returns true if this merge resource has a base resource,
	 * and false otherwise.
	 */
	public boolean hasBaseRevision() {
		return syncTree.getBase() != null;
	}
	
	/**
	 * Returns true if this merge resource has a latest revision,
	 * and false otherwise.
	 */
	public boolean hasLatestRevision() {
		return syncTree.getRemote() != null;
	}
	
	/**
	 * Is this a leaf node, i.e. a file?
	 */
	public boolean isLeaf() {
		return !syncTree.isContainer();
	}
	
	/**
	 * Updates the given compare configuration with appropriate left, right
	 * and ancestor labels for this resource.
	 */
	public void setLabels(CompareConfiguration config) {
		String name = getName();
		config.setLeftLabel(Policy.bind("MergeResource.workspaceFile", name));
	
	
		IRemoteResource remote = syncTree.getRemote();
		if (remote != null) {
			config.setRightLabel(Policy.bind("MergeResource.repositoryFile", name));
	//		config.setRightLabel(TeamUIPlugin.getResourceString("MergeResource.repositoryFile", new Object[] {name, remote.getVersionName()} ));
		} else {
			config.setRightLabel(Policy.bind("MergeResource.noRepositoryFile"));
		}
	
		IRemoteResource base = syncTree.getBase();
		if (base != null) {
			config.setAncestorLabel(Policy.bind("MergeResource.commonFile", name));
	//		config.setAncestorLabel(TeamUIPlugin.getResourceString("MergeResource.commonFile", new Object[] {name, common.getVersionName()} ));
		} else {
			config.setAncestorLabel(Policy.bind("MergeResource.noCommonFile"));
		}
	}
}
