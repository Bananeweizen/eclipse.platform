package org.eclipse.team.internal.ccvs.ui.sync;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.sync.ChangedTeamContainer;
import org.eclipse.team.internal.ui.sync.ITeamNode;
import org.eclipse.team.internal.ui.sync.SyncSet;
import org.eclipse.team.internal.ui.sync.UnchangedTeamContainer;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Applies merge related actions to the selected ITeamNodes.
 */
abstract class MergeAction extends Action {
	public static final int CHECKIN = 0;
	public static final int GET = 1;
	public static final int DELETE_REMOTE = 2;
	public static final int DELETE_LOCAL = 3;

	private CVSSyncCompareInput diffModel;
	private ISelectionProvider selectionProvider;

	// direction can be INCOMING or OUTGOING
	private int direction;

	private Shell shell;
	
	/**
	 * Creates a MergeAction which works on selection and doesn't commit changes.
	 */
	public MergeAction(CVSSyncCompareInput model, ISelectionProvider sp, int direction, String label, Shell shell) {
		super(label);
		this.diffModel = model;
		this.selectionProvider = sp;
		this.direction = direction;
		this.shell = shell;
	}
	
	protected Shell getShell() {
		return shell;
	}
	
	/**
	 * Returns true if at least one node can perform the specified action.
	 */
	private boolean isEnabled(Object[] nodes) {
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] instanceof ITeamNode) {
				ITeamNode node = (ITeamNode)nodes[i];
				if (isMatchingKind(node.getKind())) 
					return true;
			} else {
				if (nodes[i] instanceof IDiffContainer)
					if (isEnabled(((IDiffContainer)nodes[i]).getChildren()))
						return true;
			}
		}
		return false;
	}

	protected abstract boolean isMatchingKind(int kind);

	/**
	 * Perform the sychronization operation.
	 */
	public void run() {
		ISelection s = selectionProvider.getSelection();
		if (!(s instanceof IStructuredSelection) || s.isEmpty()) {
			return;
		}
		final SyncSet set = new SyncSet((IStructuredSelection)s, direction);
		set.removeNonApplicableNodes();
		final SyncSet[] result = new SyncSet[1];
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				result[0] = run(set, monitor);
			}
		};
		try {
			run(op, Policy.bind("MergeAction.problemsDuringSync"));
		} catch (InterruptedException e) {
		}
		if (result[0] != null) {
			removeNodes(result[0].getChangedNodes());
			diffModel.updateView();
		}
	}
	
	/**
	 * The given nodes have been synchronized.  Remove them from
	 * the sync set.
	 */
	private void removeNodes(final ITeamNode[] nodes) {
		// Update the model
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getClass() == UnchangedTeamContainer.class) {
				// Unchanged containers get removed automatically when all
				// children are removed
				continue;
			}
			if (nodes[i].getClass() == ChangedTeamContainer.class) {
				// If this node still has children, convert to an
				// unchanged container, then it will disappear when
				// all children have been removed.
				ChangedTeamContainer container = (ChangedTeamContainer)nodes[i];
				IDiffElement[] children = container.getChildren();
				if (children.length > 0) {
					IDiffContainer parent = container.getParent();
					parent.removeToRoot(container);
					UnchangedTeamContainer unchanged = new UnchangedTeamContainer(diffModel, parent, container.getResource());
					for (int j = 0; j < children.length; j++) {
						unchanged.add(children[j]);
					}
					continue;
				}
				// No children, it will get removed below.
			}
			nodes[i].getParent().removeToRoot(nodes[i]);	
		}
	}

	/**
	 * Updates the action with the latest selection, setting enablement
	 * as necessary.
	 */
	public void update() {
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		setEnabled(isEnabled(selection.toArray()));
	}
	
	/**
	 * Subclasses must implement this method, which performs action-specific code.
	 * 
	 * It may return the sync set which was passed in, or null.
	 */
	protected abstract SyncSet run(SyncSet syncSet, IProgressMonitor monitor);

	/**
	 * Helper method to run a runnable in a progress monitor dialog, and display any errors.
	 */
	protected void run(IRunnableWithProgress op, String problemMessage) throws InterruptedException {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, op);
		} catch (InvocationTargetException e) {
			Throwable throwable = e.getTargetException();
			IStatus error = null;
			if (throwable instanceof CoreException) {
				error = ((CoreException)throwable).getStatus();
			} else {
				error = new Status(IStatus.ERROR, CVSUIPlugin.ID, 1, Policy.bind("simpleInternal") , throwable);
			}
			ErrorDialog.openError(shell, problemMessage, null, error);
			CVSUIPlugin.log(error);
		}
	}
}
