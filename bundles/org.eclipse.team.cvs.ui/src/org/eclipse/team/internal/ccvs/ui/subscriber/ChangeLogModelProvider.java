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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.progress.UIJob;

/**
 * This is a prototype model provider using *internal* team classes. It is not meant
 * to be an example or sanctioned use of team. This provider groups changes 
 * It would be very useful to support showing changes grouped logically
 * instead of grouped physically. This could be used for showing incoming
 * changes and also for showing the results of comparisons.
 * 
 * + 2003-12-09 Tuesday 6:04 jlemieux
 *   + Bug 3456: this was changed last night
 *     + org/eclipse/com/Main.java
 *     + org/blah/this/Other.txt
 * 
 * {date/time, comment, user} -> {*files}
 */
public class ChangeLogModelProvider extends SynchronizeModelProvider {
	
	private Map commentRoots = new HashMap();
	private RemoteLogOperation logOperation;
	private boolean shutdown = false;
	private FetchLogEntriesJob fetchLogEntriesJob;
	private ChangeLogActionGroup sortGroup;
	private CVSTag tag1;
	private CVSTag tag2;
	private final static String SORT_ORDER_GROUP = "changelog_sort"; //$NON-NLS-1$
	private static final String P_LAST_COMMENTSORT = TeamUIPlugin.ID + ".P_LAST_COMMENT_SORT"; //$NON-NLS-1$
	private static final String P_LAST_RESOURCESORT = TeamUIPlugin.ID + ".P_LAST_RESOURCE_SORT"; //$NON-NLS-1$
	
	/**
	 * Action that allows changing the model providers sort order.
	 */
	private class ToggleSortOrderAction extends Action {
		private int criteria;
		private int sortType;
		public final static int RESOURCE_NAME = 1;
		public final static int COMMENT = 2;
		protected ToggleSortOrderAction(String name, int criteria, int sortType, int defaultCriteria) {
			super(name, Action.AS_RADIO_BUTTON);
			this.criteria = criteria;
			this.sortType = sortType;
			setChecked(criteria == defaultCriteria);		
		}

		public void run() {
			StructuredViewer viewer = getViewer();
			if (viewer != null && !viewer.getControl().isDisposed()) {
				ChangeLogModelSorter sorter = (ChangeLogModelSorter) viewer.getSorter();
				if (isChecked() && sorter != null && getCriteria(sorter) != criteria) {
					viewer.setSorter(createSorter(sorter));
					String key = sortType == RESOURCE_NAME ? P_LAST_RESOURCESORT : P_LAST_COMMENTSORT;
					IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
					if(pageSettings != null) {
						pageSettings.put(key, criteria);
					}
					update();
				}
			}
		}
		
		public void update() {
			StructuredViewer viewer = getViewer();
			if (viewer != null && !viewer.getControl().isDisposed()) {
				ChangeLogModelSorter sorter = (ChangeLogModelSorter) viewer.getSorter();
				if (sorter != null) {
					setChecked(getCriteria(sorter) == criteria);		
				}
			}	
		}
		
		protected ChangeLogModelSorter createSorter(ChangeLogModelSorter sorter) {
			if(sortType == COMMENT) {
				return new ChangeLogModelSorter(criteria, sorter.getResourceCriteria());
			}	else {
				return new ChangeLogModelSorter(sorter.getCommentCriteria(), criteria);
			}
		}
		
		protected int getCriteria(ChangeLogModelSorter sorter) {
			if(sortType == COMMENT)
				return sorter.getCommentCriteria();
			else
				return sorter.getResourceCriteria();
		}
	}
	
	/**
	 * Actions for the compare particpant's toolbar
	 */
	public class ChangeLogActionGroup extends SynchronizePageActionGroup {
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			MenuManager sortByComment = new MenuManager(Policy.bind("ChangeLogModelProvider.0"));	 //$NON-NLS-1$
			MenuManager sortByResource = new MenuManager(Policy.bind("ChangeLogModelProvider.6"));	 //$NON-NLS-1$
			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					SORT_ORDER_GROUP, 
					sortByComment);
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					SORT_ORDER_GROUP, 
					sortByResource);
			
			ChangeLogModelSorter sorter = (ChangeLogModelSorter)getViewerSorter();
			
			sortByComment.add(new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.1"), ChangeLogModelSorter.COMMENT, ToggleSortOrderAction.COMMENT, sorter.getCommentCriteria())); //$NON-NLS-1$
			sortByComment.add(new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.2"), ChangeLogModelSorter.DATE, ToggleSortOrderAction.COMMENT, sorter.getCommentCriteria())); //$NON-NLS-1$
			sortByComment.add(new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.3"), ChangeLogModelSorter.USER, ToggleSortOrderAction.COMMENT, sorter.getCommentCriteria())); //$NON-NLS-1$

			sortByResource.add( new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.8"), ChangeLogModelSorter.PATH, ToggleSortOrderAction.RESOURCE_NAME, sorter.getResourceCriteria())); //$NON-NLS-1$
			sortByResource.add(new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.7"), ChangeLogModelSorter.NAME, ToggleSortOrderAction.RESOURCE_NAME, sorter.getResourceCriteria())); //$NON-NLS-1$
			sortByResource.add(new ToggleSortOrderAction(Policy.bind("ChangeLogModelProvider.9"), ChangeLogModelSorter.PARENT_NAME, ToggleSortOrderAction.RESOURCE_NAME, sorter.getResourceCriteria())); //$NON-NLS-1$
		}
	}
	
	public static class DateComment {
		Date date;
		String comment;
		private String user;
		
		DateComment(Date date, String comment, String user) {
			this.date = date;
			this.comment = comment;
			this.user = user;	
		}

		public boolean equals(Object obj) {
			if(obj == this) return true;
			if(! (obj instanceof DateComment)) return false;
			DateComment other = (DateComment)obj;
			
			Calendar c1 = new GregorianCalendar();
			c1.setTime(date);
			int year = c1.get(Calendar.YEAR);
			int day = c1.get(Calendar.DAY_OF_YEAR);
			
			Calendar c2 = new GregorianCalendar();
			c2.setTime(other.date);
			int yearOther = c2.get(Calendar.YEAR);
			int dayOther = c2.get(Calendar.DAY_OF_YEAR);
			
			return comment.equals(other.comment) && user.equals(other.user);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return comment.hashCode() + user.hashCode();
		}
	}
	
	public static class FullPathSyncInfoElement extends SyncInfoModelElement {
		public FullPathSyncInfoElement(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}
		public String getName() {
			IResource resource = getResource();
			return resource.getName() + " - " + resource.getFullPath().toString(); //$NON-NLS-1$
		}
	}
	
	public class CVSUpdatableSyncInfo extends CVSSyncInfo {
		public int kind;
		public CVSUpdatableSyncInfo(int kind, IResource local, IResourceVariant base, IResourceVariant remote, Subscriber s) {
			super(local, base, remote, s);
			this.kind = kind;
		}

		protected int calculateKind() throws TeamException {
			return kind;
		}
	}
	
	private class FetchLogEntriesJob extends Job {
		private Set syncSets = new HashSet();
		public FetchLogEntriesJob() {
			super(Policy.bind("ChangeLogModelProvider.4"));  //$NON-NLS-1$
			setUser(false);
		}
		public boolean belongsTo(Object family) {
			return family == ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION;
		}
		public IStatus run(IProgressMonitor monitor) {
			if (syncSets != null && !shutdown) {
				// Determine the sync sets for which to fetch comment nodes
				SyncInfoSet[] updates;
				synchronized(syncSets) {
					updates = (SyncInfoSet[])syncSets.toArray(new SyncInfoSet[syncSets.size()]);
					syncSets.clear();
				}
				
				for (int i = 0; i < updates.length; i++) {
					SyncInfoSet set = updates[i];
					calculateRoots(updates[i], monitor);
				}
								
				refreshViewer();				
			}
			return Status.OK_STATUS;
		}
		public void add(SyncInfoSet set) {
			synchronized(syncSets) {
				syncSets.add(set);
			}
			schedule();
		}
		public boolean shouldRun() {
			return !syncSets.isEmpty();
		}
	};
	
	public static class ChangeLogModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_cvs_changelog"; //$NON-NLS-1$
		public String getId() {
			return ID;
		}		
		public String getName() {
			return Policy.bind("ChangeLogModelProvider.5"); //$NON-NLS-1$
		}		
		public ImageDescriptor getImageDescriptor() {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_CHANGELOG);
		}
	};
	private static final ChangeLogModelProviderDescriptor descriptor = new ChangeLogModelProviderDescriptor();
	
	public ChangeLogModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set, CVSTag tag1, CVSTag tag2) {
		super(configuration, set);
		this.tag1 = tag1;
		this.tag2 = tag2;
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, SORT_ORDER_GROUP);
		this.sortGroup = new ChangeLogActionGroup();
		configuration.addActionContribution(sortGroup);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getDescriptor()
	 */
	public ISynchronizeModelProviderDescriptor getDescriptor() {
		return descriptor;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.HierarchicalModelProvider#buildModelObjects(org.eclipse.compare.structuremergeviewer.DiffNode)
	 */
	protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
		if (node == getModelRoot()) {
			// Cancel any existing fetching jobs
			try {
				if (fetchLogEntriesJob != null && fetchLogEntriesJob.getState() != Job.NONE) {
					fetchLogEntriesJob.cancel();
					fetchLogEntriesJob.join();
				}
			} catch (InterruptedException e) {
			}
			// Clear any cached state
			commentRoots.clear();
			// Start building the model from scratch
			startUpdateJob(getSyncInfoSet());
		}
		return new IDiffElement[0];
	}

	private void startUpdateJob(SyncInfoSet set) {
		if(fetchLogEntriesJob == null) {
			fetchLogEntriesJob = new FetchLogEntriesJob();
		}
		fetchLogEntriesJob.add(set);
	}
	
	private void refreshViewer() {
		UIJob updateUI = new UIJob("") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				StructuredViewer tree = getViewer();	
				tree.refresh();
				ISynchronizeModelElement root = getModelRoot();
				if(root instanceof SynchronizeModelElement)
					((SynchronizeModelElement)root).fireChanges();
				return Status.OK_STATUS;
			}
		};
		updateUI.setSystem(true);
		updateUI.schedule();
	}
	
	private void calculateRoots(SyncInfoSet set, IProgressMonitor monitor) {
		try {
			// Decide which nodes we have to fetch log histories
			SyncInfo[] infos = set.getSyncInfos();
			ArrayList commentNodes = new ArrayList();
			ArrayList resourceNodes = new ArrayList();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				if(isInterestingChange(info)) {
					commentNodes.add(info);
				} else {
					resourceNodes.add(info);
				}
			}	
			// Show elements that don't need their log histories retreived
			for (Iterator it = resourceNodes.iterator(); it.hasNext();) {
				SyncInfo info = (SyncInfo) it.next();
				addNewElementFor(info, null, null);
			}
			if(! resourceNodes.isEmpty())
				refreshViewer();
			
			// Fetch log histories then add elements
			SyncInfo[] commentInfos = (SyncInfo[]) commentNodes.toArray(new SyncInfo[commentNodes.size()]);
			RemoteLogOperation logs = getSyncInfoComment(commentInfos, monitor);
			if (logs != null) {
				for (int i = 0; i < commentInfos.length; i++) {
					addSyncInfoToCommentNode(commentInfos[i], logs);
				}
			}
		} catch (CVSException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Create a node for the given sync info object. The logs should contain the log for this info.
	 * 
	 * @param info the info for which to create a node in the model
	 * @param log the cvs log for this node
	 */
	private void addSyncInfoToCommentNode(SyncInfo info, RemoteLogOperation logs) {
		ICVSRemoteResource remoteResource = getRemoteResource((CVSSyncInfo)info);
		if(tag1 != null && tag2 != null) {
			ILogEntry[] logEntries = logs.getLogEntries(remoteResource);
			if(logEntries == null || logEntries.length == 0) {
				addNewElementFor(info, null, null);
			}
			for (int i = 0; i < logEntries.length; i++) {
				ILogEntry entry = logEntries[i];
				addNewElementFor(info, remoteResource, entry);
			}
		} else {
			ILogEntry logEntry = logs.getLogEntry(remoteResource);
			addNewElementFor(info, remoteResource, logEntry);
		}
	}
	
	/**
	 * @param info
	 * @param remoteResource
	 * @param logEntry
	 */
	private void addNewElementFor(SyncInfo info, ICVSRemoteResource remoteResource, ILogEntry logEntry) {
		ISynchronizeModelElement element;	
		// If the element has a comment then group with common comment
		if(remoteResource != null && logEntry != null && isInterestingChange(info)) {
			DateComment dateComment = new DateComment(logEntry.getDate(), logEntry.getComment(), logEntry.getAuthor());
			ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(dateComment);
			if (changeRoot == null) {
				changeRoot = new ChangeLogDiffNode(getModelRoot(), logEntry);
				commentRoots.put(dateComment, changeRoot);
				addToViewer(changeRoot);
			}
			if(info instanceof CVSSyncInfo) {
				info = new CVSUpdatableSyncInfo(info.getKind(), info.getLocal(), info.getBase(), (RemoteResource)logEntry.getRemoteFile(), ((CVSSyncInfo)info).getSubscriber());
			}
			element = new FullPathSyncInfoElement(changeRoot, info);
		} else {
			// For nodes without comments, simply parent with the root. These will be outgoing
			// additions.
			element = new FullPathSyncInfoElement(getModelRoot(), info);
		}	
		addToViewer(element);
	}

	private boolean isInterestingChange(SyncInfo info) {
		int kind = info.getKind();
		if(info.getComparator().isThreeWay()) {
			return (kind & SyncInfo.DIRECTION_MASK) != SyncInfo.OUTGOING;
		}
		return true;
	}

	/**
	 * How do we tell which revision has the interesting log message? Use the later
	 * revision, since it probably has the most up-to-date comment.
	 */
	private RemoteLogOperation getSyncInfoComment(SyncInfo[] infos, IProgressMonitor monitor) throws CVSException, InterruptedException {
		List remotes = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			CVSSyncInfo info = (CVSSyncInfo)infos[i];
			if (info.getLocal().getType() != IResource.FILE) {
				continue;
			}	
			ICVSRemoteResource remote = getRemoteResource(info);
			if(remote != null) {
				remotes.add(remote);
			}
		}
		ICVSRemoteResource[] remoteResources = (ICVSRemoteResource[]) remotes.toArray(new ICVSRemoteResource[remotes.size()]);
		if(! remotes.isEmpty()) {
			if(logOperation == null) {
				logOperation = new RemoteLogOperation(null, remoteResources, tag1, tag2);
			}
			logOperation.setRemoteResources(remoteResources);
			logOperation.execute(monitor);
			return logOperation;
		}
		return null;
	}
	
	private ICVSRemoteResource getRemoteResource(CVSSyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource) info.getRemote();
			ICVSRemoteResource local = (ICVSRemoteFile) CVSWorkspaceRoot.getRemoteResourceFor(info.getLocal());
			if(local == null) {
				local = (ICVSRemoteResource)info.getBase();
			}

			String remoteRevision = getRevisionString(remote);
			String localRevision = getRevisionString(local);

			boolean useRemote = true;
			if (local != null && remote != null) {
				useRemote = ResourceSyncInfo.isLaterRevision(remoteRevision, localRevision);
			} else if (remote == null) {
				useRemote = false;
			}
			if (useRemote) {
				return remote;
			} else if (local != null) {
				return local;
			}
			return null;
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}
	
	private String getRevisionString(ICVSRemoteResource remoteFile) {
		if(remoteFile instanceof RemoteFile) {
			return ((RemoteFile)remoteFile).getRevision();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.HierarchicalModelProvider#dispose()
	 */
	public void dispose() {
		shutdown = true;
		if(fetchLogEntriesJob != null && fetchLogEntriesJob.getState() != Job.NONE) {
			fetchLogEntriesJob.cancel();
		}
		getConfiguration().removeActionContribution(sortGroup);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#getViewerSorter()
	 */
	public ViewerSorter getViewerSorter() {
		int commentSort = ChangeLogModelSorter.USER;
		int resourceSort = ChangeLogModelSorter.PATH;
		try {
			IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
			if(pageSettings != null) {
				commentSort = pageSettings.getInt(P_LAST_COMMENTSORT);
				resourceSort = pageSettings.getInt(P_LAST_RESOURCESORT);
			}
		} catch(NumberFormatException e) {
			// ignore and use the defaults.
		}
		return new ChangeLogModelSorter(commentSort, resourceSort);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#doAdd(org.eclipse.team.ui.synchronize.viewers.SynchronizeModelElement, org.eclipse.team.ui.synchronize.viewers.SynchronizeModelElement)
	 */
	protected void doAdd(ISynchronizeModelElement parent, ISynchronizeModelElement element) {
		AbstractTreeViewer viewer = (AbstractTreeViewer)getViewer();
		viewer.add(parent, element);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#doRemove(org.eclipse.team.ui.synchronize.viewers.SynchronizeModelElement)
	 */
	protected void doRemove(ISynchronizeModelElement element) {
		AbstractTreeViewer viewer = (AbstractTreeViewer)getViewer();
		viewer.remove(element);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#handleResourceAdditions(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
	 */
	protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		startUpdateJob(new SyncInfoSet(event.getAddedResources()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#handleResourceChanges(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
	 */
	protected void handleResourceChanges(ISyncInfoTreeChangeEvent event) {
		//	Refresh the viewer for each changed resource
		SyncInfo[] infos = event.getChangedResources();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IResource local = info.getLocal();
			removeFromViewer(local);
		}
		startUpdateJob(new SyncInfoSet(event.getChangedResources()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SynchronizeModelProvider#handleResourceRemovals(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		IResource[] removedRoots = event.getRemovedSubtreeRoots();
		for (int i = 0; i < removedRoots.length; i++) {
			removeFromViewer(removedRoots[i]);
		}
		// We have to look for folders that may no longer be in the set
		// (i.e. are in-sync) but still have descendants in the set
		IResource[] removedResources = event.getRemovedResources();
		for (int i = 0; i < removedResources.length; i++) {
			removeFromViewer(removedResources[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SynchronizeModelProvider#removeFromViewer(org.eclipse.core.resources.IResource)
	 */
	protected void removeFromViewer(IResource resource) {
		// First clear the log history cache for the remote element
		if (logOperation != null) {
			ISynchronizeModelElement element = getModelObject(resource);
			if (element instanceof FullPathSyncInfoElement) {
				CVSSyncInfo info = (CVSSyncInfo) ((FullPathSyncInfoElement) element).getSyncInfo();
				if (info != null) {
					ICVSRemoteResource remote = getRemoteResource(info);
					logOperation.clearEntriesFor(remote);
				}
			}
		}
		// Remove the object now
		super.removeFromViewer(resource);
	}
}
