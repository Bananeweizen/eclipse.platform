package org.eclipse.debug.internal.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;

public class OpenBreakpointMarkerAction extends OpenMarkerAction {

	protected static DelegatingModelPresentation fgPresentation = new DelegatingModelPresentation();
	
	public OpenBreakpointMarkerAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, ActionMessages.getString("OpenBreakpointMarkerAction.&Go_to_File_1")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenBreakpointMarkerAction.Go_to_File_for_Breakpoint_2")); //$NON-NLS-1$
		ISharedImages images= DebugUIPlugin.getDefault().getWorkbench().getSharedImages();
		setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_OPEN_MARKER));
		WorkbenchHelp.setHelp(
			this,
			IDebugHelpContextIds.OPEN_BREAKPOINT_ACTION);
	}

	/**
	 * @see IAction
	 */
	public void run() {
		IWorkbenchWindow dwindow= DebugUIPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page= dwindow.getActivePage();
		if (page == null) {
			return;
		}
		IEditorPart part= null;
		// Get the resource.
		IStructuredSelection selection= (IStructuredSelection)getStructuredSelection();
		//Get the selected marker
		Iterator enum= selection.iterator();
		IBreakpoint breakpoint= (IBreakpoint)enum.next();
		IEditorInput input= fgPresentation.getEditorInput(breakpoint);
		String editorId= fgPresentation.getEditorId(input, breakpoint);
		if (input != null) {
			try {
				part= page.openEditor(input, editorId);
			} catch (PartInitException e) {
				DebugUIPlugin.logError(e);
			}
		}
		if (part != null) {
			// Bring editor to front.
			part.setFocus();

			// Goto the bookmark.
			part.gotoMarker(breakpoint.getMarker());
		}
	}
}

