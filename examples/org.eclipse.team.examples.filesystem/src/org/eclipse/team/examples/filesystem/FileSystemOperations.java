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
package org.eclipse.team.examples.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemResourceVariant;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;

/**
 * The get and put operations for the file system provider.
 */
public class FileSystemOperations {

	// A reference to the provider
	private FileSystemProvider provider;

	FileSystemOperations(FileSystemProvider provider) {
		this.provider = provider;
	}

	/**
	 * Make the local state of the project match the remote state by getting any out-of-sync 
	 * resources. The overrideOutgoing flag is used to indicate whether locally modified
	 * files should also be replaced or left alone.
	 * @param resources the resources to get
	 * @param depth the depth of the operation
	 * @param overrideOutgoing whether locally modified resources should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void get(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		try {
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("GetAction.working"), 100); //$NON-NLS-1$
			// Refresh the subscriber so we have the latest remote state
			FileSystemSubscriber.getInstance().refresh(resources, depth, new SubProgressMonitor(progress, 30));
			internalGet(resources, depth, overrideOutgoing, new SubProgressMonitor(progress, 70));
		} finally {
			progress.done();
		}
	}

	/**
	 * Checkout the given resources to the given depth by setting any files
	 * to writtable (i.e set read-only to <coce>false</code>.
	 * @param resources the resources to be checked out
	 * @param depth the depth of the checkout
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void checkout(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException {
		try {
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("FileSystemSimpleAccessOperations.1"), resources.length); //$NON-NLS-1$
			for (int i = 0; i < resources.length; i++) {
				Policy.checkCanceled(progress);
				resources[i].accept(new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						if (resource.getType() == IResource.FILE) {
							//TODO: lock the file on the 'server'.
							resource.setReadOnly(false);
						}
						return true;
					}
				}, depth, false /* include phantoms */);
				progress.worked(1);
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		} finally {
			progress.done();
		}
	}

	/**
	 * Checkin the given resources to the given depth by replacing the remote (i.e. file system)
	 * contents with the local workspace contents. 
	 * @param resources the resources
	 * @param depth the depth of the operation
	 * @param overrideIncoming indicate whether incoming remote changes should be replaced
	 * @param progress a progress monitor
	 * @throws TeamException
	 */
	public void checkin(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		try {
			// ensure the progress monitor is not null
			progress = Policy.monitorFor(progress);
			progress.beginTask(Policy.bind("PutAction.working"), 100); //$NON-NLS-1$
			// Refresh the subscriber so we have the latest remote state
			FileSystemSubscriber.getInstance().refresh(resources, depth, new SubProgressMonitor(progress, 30));
			internalPut(resources, depth, overrideIncoming, new SubProgressMonitor(progress, 70));
		} finally {
			progress.done();
		}
	}

	/**
	 * Return whether the local resource is checked out. A resource
	 * is checked out if it is a file that is not read-only. Folders
	 * are always checked out.
	 * @param resource the resource
	 * @return whether the resource is checked out and can be modified
	 */
	public boolean isCheckedOut(IResource resource) {
		if (resource.getType() != IResource.FILE) return true;
		return !resource.isReadOnly();
	}

	/*
	 * Get the resource variant for the given resource.
	 */
	private FileSystemResourceVariant getResourceVariant(IResource resource) {
		return (FileSystemResourceVariant)provider.getResourceVariant(resource);
	}
	
	private void internalGet(IResource[] resources, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		// Traverse the resources and get any that are out-of-sync
		progress.beginTask(Policy.bind("GetAction.working"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		for (int i = 0; i < resources.length; i++) {
			Policy.checkCanceled(progress);
			if (resources[i].getType() == IResource.FILE) {
				internalGet((IFile) resources[i], overrideOutgoing, progress);
			} else if (depth != IResource.DEPTH_ZERO) {
				internalGet((IContainer)resources[i], depth, overrideOutgoing, progress);
			}
			progress.worked(1);
		}
	}

	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalGet(IContainer container, int depth, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		try {
			ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List toDelete = new ArrayList();
			if (container.getType() == IResource.FOLDER) {
				IFolder folder = (IFolder)container;
				FileSystemResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null) {
					// Create the local folder
					folder.create(false, true, progress);
					synchronizer.setBaseBytes(folder, remote.asBytes());
				} else if (folder.exists() && remote == null) {
					// Schedule the folder for removal but delay in
					// case the folder contains outgoing changes
					toDelete.add(folder);
				}
			}
			
			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0) {
				internalGet(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideOutgoing, progress);
			}
		
			// Remove any empty folders
			for (Iterator iter = toDelete.iterator(); iter.hasNext(); ) {
				IFolder folder = (IFolder) iter.next();
				if (folder.members().length == 0) {
					folder.delete(false, true, progress);
					synchronizer.flush(folder, IResource.DEPTH_INFINITE);
				}
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	/*
	 * Get the file if it is out-of-sync.
	 */
	private void internalGet(IFile localFile, boolean overrideOutgoing, IProgressMonitor progress) throws TeamException {
		ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = FileSystemSubscriber.getInstance().getResourceComparator();
		FileSystemResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = provider.getResourceVariant(localFile, baseBytes);
		if (!synchronizer.hasSyncBytes(localFile) || (
				synchronizer.isLocallyModified(localFile) && !overrideOutgoing)) {
			// Do not overwrite the local modification
			return;
		}
		if (base != null && remote == null) {
			// The remote no longer exists so remove the local
			try {
				localFile.delete(false, true, progress);
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
		if (!synchronizer.isLocallyModified(localFile) && comparator.compare(base, remote)) {
			// The base and remote are the same and there's no local changes
			// so nothing needs to be done
		}
		try {
			//Copy from the local file to the remote file:
			InputStream source = null;
			try {
				// Get the remote file content.
				source = remote.getContents();
				// Set the local file content to be the same as the remote file.
				if (localFile.exists())
					localFile.setContents(source, false, false, progress);
				else
					localFile.create(source, false, progress);
			} finally {
				if (source != null)
					source.close();
			}
			// Mark as read-only to force a checkout before editing
			localFile.setReadOnly(true);
		} catch (IOException e) {
			throw FileSystemPlugin.wrapException(e);
		} catch (CoreException e) {
			throw FileSystemPlugin.wrapException(e);
		}
	}
	
	private void internalPut(IResource[] resources, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		// ensure the progress monitor is not null
		progress = Policy.monitorFor(progress);
		progress.beginTask(Policy.bind("PutAction.working"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		for (int i = 0; i < resources.length; i++) {
			Policy.checkCanceled(progress);
			if (resources[i].getType() == IResource.FILE) {
				internalPut((IFile)resources[i], overrideIncoming, progress);
			} else if (depth > 0) { //Assume that resources are either files or containers.
				internalPut((IContainer)resources[i], depth, overrideIncoming, progress);
			}
			progress.worked(1);
		}
		progress.done();
	}
	
	/*
	 * Get the file if it is out-of-sync.
	 */
	private void internalPut(IFile localFile, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
		IResourceVariantComparator comparator = FileSystemSubscriber.getInstance().getResourceComparator();
		FileSystemResourceVariant remote = getResourceVariant(localFile);
		byte[] baseBytes = synchronizer.getBaseBytes(localFile);
		IResourceVariant base = provider.getResourceVariant(localFile, baseBytes);
		
		// Check whether we are overriding a remote change
		if (base == null && remote != null && !overrideIncoming) {
			// The remote is an incoming (or conflicting) addition.
			// Do not replace unless we are overriding
			return;
		} else  if (base != null && remote == null) {
			// The remote is an incoming deletion
			if (!localFile.exists()) {
				// Conflicting deletion. Clear the synchronizer.
				synchronizer.flush(localFile, IResource.DEPTH_ZERO);
			} else if (!overrideIncoming) {
				// Do not override the incoming deletion
				return;
			}
		} else if (base != null && remote != null) {
			boolean same = comparator.compare(base, remote);
			if (!synchronizer.isLocallyModified(localFile) && same) {
				// The base and remote are the same and there's no local changes
				// so nothing needs to be done
				return;
			}
			if (!same && !overrideIncoming) {
				// The remote has changed. Only override if specified
				return;
			}
		}
		
		// Handle an outgoing deletion
		File diskFile = provider.getFile(localFile);
		if (!localFile.exists()) { 
			diskFile.delete();
			synchronizer.flush(localFile, IResource.DEPTH_ZERO);
		} else {
			// Otherwise, upload the contents
			try {
				//Copy from the local file to the remote file:
				InputStream in = null;
				FileOutputStream out = null;
				try {
					in = localFile.getContents();
					out = new FileOutputStream(diskFile);
					//Copy the contents of the local file to the remote file:
					StreamUtil.pipe(in, out, diskFile.length(), progress, diskFile.getName());
					// Mark the file as read-only to require another checkout
					localFile.setReadOnly(true);
				} finally {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				}
				// Update the synchronizer base bytes
				remote = getResourceVariant(localFile);
				synchronizer.setBaseBytes(localFile, remote.asBytes());
			} catch (IOException e) {
				throw FileSystemPlugin.wrapException(e);
			} catch (CoreException e) {
				throw FileSystemPlugin.wrapException(e);
			}
		}
	}
	
	/*
	 * Get the folder and its children to the depth specified.
	 */
	private void internalPut(IContainer container, int depth, boolean overrideIncoming, IProgressMonitor progress) throws TeamException {
		try {
			ThreeWaySynchronizer synchronizer = FileSystemSubscriber.getInstance().getSynchronizer();
			// Make the local folder state match the remote folder state
			List toDelete = new ArrayList();
			if (container.getType() == IResource.FOLDER) {
				IFolder folder = (IFolder)container;
				File diskFile = provider.getFile(container);
				FileSystemResourceVariant remote = getResourceVariant(container);
				if (!folder.exists() && remote != null) {
					// Schedule the folder for removal but delay in
					// case the folder contains incoming changes
					toDelete.add(diskFile);
				} else if (folder.exists() && remote == null) {
					// Create the remote directory and sync up the local
					diskFile.mkdir();
					synchronizer.setBaseBytes(folder, provider.getResourceVariant(folder).asBytes());
				}
			}
			
			// Get the children
			IResource[] children = synchronizer.members(container);
			if (children.length > 0) {
				internalPut(children, depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, overrideIncoming, progress);
			}
		
			// Remove any empty folders
			for (Iterator iter = toDelete.iterator(); iter.hasNext(); ) {
				File diskFile = (File) iter.next();
				if (diskFile.listFiles().length == 0) {
					diskFile.delete();
					synchronizer.flush(container, IResource.DEPTH_INFINITE);
				}
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
}
