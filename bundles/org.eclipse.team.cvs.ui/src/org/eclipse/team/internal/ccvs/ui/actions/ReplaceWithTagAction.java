package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.CVSTeamProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.TagSelectionDialog;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Action for replace with tag.
 */
public class ReplaceWithTagAction extends ReplaceWithAction {
	/*
	 * Method declared on IActionDelegate.
	 */
	public void run(IAction action) {
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					IResource[] resources = getSelectedResources();
					if (resources.length != 1) return;
					IResource resource = resources[0];
		
					CVSTeamProvider provider = (CVSTeamProvider)TeamPlugin.getManager().getProvider(resource.getProject());
					if (isDirty(resource)) {
						final Shell shell = getShell();
						final boolean[] result = new boolean[] { false };
						shell.getDisplay().syncExec(new Runnable() {
							public void run() {
								result[0] = MessageDialog.openQuestion(getShell(), Policy.bind("question"), Policy.bind("localChanges"));
							}
						});
						if (!result[0]) return;
					}
					
					TagSelectionDialog dialog = new TagSelectionDialog(getShell(), resource);
					dialog.setBlockOnOpen(true);
					if (dialog.open() == Dialog.CANCEL) {
						return;
					}
					CVSTag tag = dialog.getResult();
					if (tag == null) {
						return;
					}
					provider.get(new IResource[] { resource }, IResource.DEPTH_INFINITE, tag, monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		}, Policy.bind("ReplaceWithTagAction.replace"), this.PROGRESS_BUSYCURSOR);			
	}
	
	protected boolean isEnabled() {
		return getSelectedResources().length == 1;
	}
}
