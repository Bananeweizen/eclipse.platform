package org.eclipse.team.tests.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import junit.framework.TestCase;
import junit.framework.TestResult;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.actions.AddToWorkspaceAction;
import org.eclipse.team.internal.ccvs.ui.actions.CommitAction;
import org.eclipse.team.internal.ccvs.ui.actions.ReplaceWithRemoteAction;
import org.eclipse.team.internal.ccvs.ui.actions.TagAction;
import org.eclipse.team.internal.ccvs.ui.actions.UpdateAction;
import org.eclipse.team.internal.ccvs.ui.sync.CVSSyncCompareInput;
import org.eclipse.team.internal.ccvs.ui.sync.CommitSyncAction;
import org.eclipse.team.internal.ccvs.ui.sync.UpdateSyncAction;
import org.eclipse.team.internal.ccvs.ui.wizards.SharingWizard;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.ui.sync.ITeamNode;
import org.eclipse.team.ui.sync.SyncSet;
import org.eclipse.team.ui.sync.SyncView;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class CVSUITestCase extends TestCase {
	private List testWindows;
	protected IWorkbenchWindow testWindow;
	protected LoggingTestResult logResult;
	protected CVSRepositoryLocation testRepository;

	public CVSUITestCase(String name) {
		super(name);
		testWindows = new ArrayList(3);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		testRepository = CVSTestSetup.repository;
		testWindow = openTestWindow();

		// disable auto-build
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		// wait for UI to settle
		processEventsUntil(100);
	}
	
	protected void tearDown() throws Exception {
		// wait for UI to settle
		processEventsUntil(100);
		closeAllTestWindows();
		super.tearDown();
	}

	public void run(TestResult result) {
		logResult = (result instanceof LoggingTestResult) ? (LoggingTestResult) result : null;
		super.run(result);
	}

	/**
	 * Marks the beginning of a new task group.
	 * @param groupName the name for the group
	 */
	protected void startGroup(String groupName) {
		if (logResult != null) logResult.startGroup(groupName);		
	}
	
	/**
	 * Marks the ends of the active task group.
	 */
	protected void endGroup() {
		if (logResult != null) logResult.endGroup();		
	}

	/**
	 * Marks the beginning of a new task.
	 * @param taskName the name for the task
	 */
	protected void startTask(String taskName) {
		if (logResult != null) logResult.startTask(taskName);		
	}
	
	/**
	 * Marks the ends of the active task.
	 */
	protected void endTask() {
		if (logResult != null) logResult.endTask();		
	}

	/**
	 * Returns the name of the active group, or null if none.
	 */
	protected String getGroupName() {
		return logResult == null ? "unknown" : logResult.getGroupName();
	}
	
	/**
	 * Returns the name of the active task, or null if none.
	 */
	protected String getTaskName() {
		return logResult == null ? "unknown" : logResult.getTaskName();
	}	

 	/** 
	 * Open a test window with the empty perspective.
	 */
	public IWorkbenchWindow openTestWindow() {
		try {
			IWorkbenchWindow win = PlatformUI.getWorkbench().openWorkbenchWindow(
				EmptyPerspective.PERSP_ID, ResourcesPlugin.getWorkspace());
			testWindows.add(win);
			return win;
		} catch (WorkbenchException e) {
			fail();
			return null;
		}
	}

 	/**
	 * Close all test windows.
	 */
	public void closeAllTestWindows() {
		Iterator iter = testWindows.iterator();
		IWorkbenchWindow win;
		while (iter.hasNext()) {
			win = (IWorkbenchWindow) iter.next();
			win.close();
		}
		testWindows.clear();
	}

	/**
	 * Process pending events until at least the specified number of milliseconds elapses.
	 */
	public void processEventsUntil(int hiatus) {
		if (testWindow == null) return;
		Display display = testWindow.getShell().getDisplay();
		final boolean done[] = new boolean[] { hiatus == 0 };
		if (hiatus != 0) display.timerExec(hiatus, new Runnable() {
			public void run() { done[0] = true; }
		});
		for (;;) {
			while (display.readAndDispatch());
			if (done[0]) return;
			display.sleep();
		}
	}
	
	/**
	 * Process pending events until the tests are resumed by the user.
	 * Very useful for inspecting intermediate results while debugging.
	 */
	public void processEventsUntilResumed() {
		if (testWindow == null) return;
		Display display = testWindow.getShell().getDisplay();
		Shell shell = new Shell(testWindow.getShell(), SWT.CLOSE);
		shell.setText("Close me to resume tests");
		shell.setBounds(0, 0, 300, 30);
		shell.open();
		while (! shell.isDisposed()) {
			while (! display.readAndDispatch()) display.sleep();
		}
	}

	/**
	 * Checks out the projects with the specified tags from the test repository.
	 */
	protected void actionCheckoutProjects(String[] projectNames, CVSTag[] tags) throws Exception {		
		ICVSRemoteFolder[] projects = lookupRemoteProjects(projectNames, tags);
		runActionDelegate(new AddToWorkspaceAction(), projects, "Repository View Checkout action");
		processEventsUntil(100); // let the UI settle before continuing
	}

	/**
	 * Replaces the specified resources with the remote contents using the action contribution.
	 */
	protected void actionReplaceWithRemote(IResource[] resources) {
		ReplaceWithRemoteAction action = new ReplaceWithRemoteAction() {
			protected boolean confirmOverwrite(String message) {
				return true;
			}
		};
		runActionDelegate(action, resources, "Replace with Remote action");
		processEventsUntil(100); // let the UI settle before continuing
	}
		
	/**
	 * Shares the specified project with the test repository.
	 * @param project the project to share
	 */
	protected void actionShareProject(IProject project) {
		final SharingWizard wizard = new SharingWizard();
		wizard.init(PlatformUI.getWorkbench(), project);
		Util.waitForWizardToOpen(testWindow.getShell(), wizard, new Waiter() {
			public boolean notify(Object object) {
				WizardDialog dialog = (WizardDialog) object;
				startTask("set sharing, pop up sync viewer");
				wizard.performFinish();
				endTask();
				dialog.close();
				return false;
			}
		});
		processEventsUntil(100); // let the UI settle before continuing
	}

	/**
	 * Updates the specified resources using the action contribution.
	 */
	protected void actionCVSCommit(IResource[] resources, final String comment) {
		CommitAction action = new CommitAction() {
			protected String promptForComment() {
				return comment;
			}
		};
		runActionDelegate(action, resources, "CVS Commit action");
		processEventsUntil(100); // let the UI settle before continuing
	}	
	
	/**
	 * Tags the specified resources using the action contribution.
	 */
	protected void actionCVSTag(IResource[] resources, final String name) {
		TagAction action = new TagAction() {
			protected String promptForTag() {
				return name;
			}
		};
		runActionDelegate(action, resources, "CVS Tag action");
		processEventsUntil(100); // let the UI settle before continuing
	}

	/**
	 * Updates the specified resources using the action contribution.
	 */
	protected void actionCVSUpdate(IResource[] resources, final String name) {
		runActionDelegate(new UpdateAction(), resources, "CVS Update action");
		processEventsUntil(100); // let the UI settle before continuing
	}	
	
	/**
	 * Pops up the synchronizer view for the specified resources.
	 * @param resources the resources to sync
	 * @return the compare input used
	 */
	protected CVSSyncCompareInput syncResources(IResource[] resources) {
		startTask("Synchronize with Repository action");
		SyncView syncView = getSyncView();
		CVSSyncCompareInput input = new CVSSyncCompareInput(resources) {
			// overridden to prevent "nothing to synchronize" dialog from popping up
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				super.run(monitor);
				DiffNode result = getDiffRoot(); // (DiffNode) getCompareResult()
				if (result == null || Util.isEmpty(result)) throw new InterruptedException();
			}
		};
		syncView.showSync(input);
		endTask();
		return input;
	}
	
	/**
	 * Commits the specified resources using the synchronizer view.
	 * @param resources the resources to commit
	 * @param input the compare input for the sync view, or null to create a new one
	 * @param comment the comment string, or ""
	 */
	protected void syncCommitResources(IResource[] resources, CVSSyncCompareInput input, String comment) {
		if (input == null) input = syncResources(resources);
		IDiffContainer diffRoot = input.getDiffRoot();
		if (Util.isEmpty(diffRoot)) {
			startTask("Nothing to Commit");
		} else {
			ITeamNode[] nodes = getTeamNodesForResources(diffRoot, resources);
			startTask("Sync View Commit action");
			syncCommitInternal(input, nodes, comment);
		}
		endTask();
		processEventsUntil(100); // let the UI settle before continuing
	}
	
	/**
	 * Updates the specified resources using the synchronizer view.
	 * @param resources the resources to update
	 * @param input the compare input for the sync view, or null to create a new one
	 * @param comment the comment string, or ""
	 */
	protected void syncUpdateResources(IResource[] resources, CVSSyncCompareInput input) {
		if (input == null) input = syncResources(resources);
		IDiffContainer diffRoot = input.getDiffRoot();
		if (Util.isEmpty(diffRoot)) {
			startTask("Nothing to Update");
		} else {
			ITeamNode[] nodes = getTeamNodesForResources(diffRoot, resources);
			startTask("Sync View Update action");
			syncGetInternal(input, nodes);
		}
		endTask();
		processEventsUntil(100); // let the UI settle before continuing
	}
	
	/**
	 * Creates and imports project contents from a zip file.
	 */
	protected IProject createAndImportProject(String prefix, File zipFile) throws Exception {
//		beginTask("create test project and import initial contents");
		IProject project = Util.createUniqueProject(prefix);
		Util.importZipIntoProject(project, zipFile);
//		endTask();
		processEventsUntil(100); // let the UI settle before continuing
		return project;
	}
	
	/**
	 * Deletes a project safely.
	 */
	protected void deleteProject(IProject project) {
//		beginTask("delete test project");
		try {
			processEventsUntil(250); // wait for things to settle before deleting
			Util.deleteProject(project);
		} catch (CoreException e) {
			System.err.println("Error occurred while deleting project, disregarding it");
			e.printStackTrace();
		}
//		endTask();
		processEventsUntil(100); // let the UI settle before continuing
	}

	/**
	 * Looks up handles for remote projects by name.
	 */
	protected ICVSRemoteFolder[] lookupRemoteProjects(String[] projectNames, CVSTag[] tags) throws Exception {
		ICVSRemoteFolder[] folders = new ICVSRemoteFolder[projectNames.length];
		for (int i = 0; i < projectNames.length; ++i) {
			folders[i] = testRepository.getRemoteFolder(projectNames[i], tags[i]);
		}
		return folders;
	}
	
	/**
	 * Gets an instance of the Synchronize view
	 */
	protected SyncView getSyncView() {
		// based on org.eclipse.team.internal.ccvs.ui.wizards.SharingWizard
		SyncView view = (SyncView)CVSUIPlugin.getActivePage().findView(SyncView.VIEW_ID);
		if (view == null) {
			view = SyncView.findInActivePerspective();
		}
		assertNotNull("Could not obtain a Sync View.", view);
		try {
			CVSUIPlugin.getActivePage().showView(SyncView.VIEW_ID);
		} catch (PartInitException e) {
			CVSUIPlugin.log(e.getStatus());
		}
		return view;
	}
	
	/**
	 * Runs an IActionDelegate prototype instance on a given selection.
	 */
	protected void runActionDelegate(IActionDelegate delegate, Object[] selection, String taskName) {
		Action action = new Action() { };
		if (delegate instanceof IObjectActionDelegate) {
			((IObjectActionDelegate) delegate).setActivePart(action,
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart());
		}
		delegate.selectionChanged(action, new StructuredSelection(selection));
		startTask(taskName);
		delegate.run(action);
		endTask();
	}
	
	/**
	 * Commits the resources represented by an array of synchronizer nodes.
	 */
	private void syncCommitInternal(CVSSyncCompareInput input, ITeamNode[] nodes, final String comment) {
		FakeSelectionProvider selectionProvider = new FakeSelectionProvider(nodes);
		CommitSyncAction action = new CommitSyncAction(input, selectionProvider, "Commit",
			testWindow.getShell()) {
			protected int promptForConflicts(SyncSet syncSet) {
				return 0; // yes! sync conflicting changes
			}
			protected String promptForComment(RepositoryManager manager) {
				return comment; // use our comment
			}
		};
		action.run();
	}

	/**
	 * Commits the resources represented by an array of synchronizer nodes.
	 */
	private void syncGetInternal(CVSSyncCompareInput input, ITeamNode[] nodes) {
		FakeSelectionProvider selectionProvider = new FakeSelectionProvider(nodes);
		UpdateSyncAction action = new UpdateSyncAction(input, selectionProvider, "Get",
			testWindow.getShell()) {
			protected boolean promptForConflicts() {
				return true;
			}
			protected int promptForMergeableConflicts() {
				return 2;
			}
		};
		action.run();
	}

	/**
	 * Gets an array of synchronizer nodes corresponding to an array of resouces.
	 */
	protected static ITeamNode[] getTeamNodesForResources(IDiffContainer root, IResource[] resources) {
		ITeamNode[] nodes = new ITeamNode[resources.length];
		for (int i = 0; i < resources.length; ++i) {
			nodes[i] = findTeamNodeForResource(root, resources[i]);
			assertNotNull(nodes[i]);
		}
		return nodes;
	}
	
	private static ITeamNode findTeamNodeForResource(IDiffElement root, IResource resource) {
		if (root instanceof ITeamNode) {
			ITeamNode node = (ITeamNode) root;
			if (resource.equals(node.getResource())) return node;
			// prune the backtracking tree
			IResource parent = resource.getParent();
			do {
				if (parent == null) return null; // can't possibly be child of this node
			} while (! resource.equals(parent));
		}
		if (root instanceof IDiffContainer) {
			IDiffContainer container = (IDiffContainer) root;
			if (container.hasChildren()) {
				IDiffElement[] children = container.getChildren();
				for (int i = 0; i < children.length; ++i) {
					ITeamNode node = findTeamNodeForResource(children[i], resource);
					if (node != null) return node;
				}
			}
		}
		return null;
	}
}
