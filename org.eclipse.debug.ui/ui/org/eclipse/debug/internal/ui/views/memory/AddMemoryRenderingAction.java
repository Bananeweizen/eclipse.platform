/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Toolbar "Add Memory Rendering Action" from Memory Rendering Pane
 */
public class AddMemoryRenderingAction extends AddMemoryBlockAction {
	
	public static final String PREFIX = "AddMemoryRenderingAction."; //$NON-NLS-1$
	public static final String TITLE = PREFIX + "title"; //$NON-NLS-1$
	public static final String ADD_RENDERING_FAILED = PREFIX + "add_rendering_failed"; //$NON-NLS-1$
	public static final String FAILED_TO_ADD_THE_SELECTED_RENDERING = PREFIX + "failed_to_add_the_selected_rendering"; //$NON-NLS-1$
	
	private IMemoryRenderingContainer fContainer;
	
	public AddMemoryRenderingAction(IMemoryRenderingContainer container)
	{
		super(DebugUIMessages.AddMemoryRenderingAction_Add_renderings, AS_PUSH_BUTTON); //$NON-NLS-1$
		setToolTipText(DebugUIMessages.AddMemoryRenderingAction_Add_renderings); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DebugUIPlugin.getUniqueIdentifier() + ".AddRenderingContextAction_context"); //$NON-NLS-1$
		fContainer = container;
		fSite = container.getMemoryRenderingSite();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		
		// pop up dialog and ask what renderings to create
		Shell shell = DebugUIPlugin.getShell();
		
		IDebugElement elem = getDebugElement(fCurrentSelection);
		
		if (elem != null)
		{	
			AddMemoryRenderingDialog dialog = new AddMemoryRenderingDialog(shell, fSite);
			dialog.open();
			
			// get a list of renderings to create
			Object[] renderings = dialog.getResult();
			
			IMemoryBlock blk = dialog.getMemoryBlock();
			
			if (blk == null)
				return;
			
			// ask for debug target and memory block retrieval
			IDebugTarget debugTarget = elem.getDebugTarget();
			IMemoryBlockRetrieval standardMemRetrieval = (IMemoryBlockRetrieval)elem.getAdapter(IMemoryBlockRetrieval.class);
			
			if (standardMemRetrieval == null)
			{	
				standardMemRetrieval = debugTarget;
			}
			
			if (standardMemRetrieval == null)
				return;
			
			// add memory renderings to Memory Rendering Manager
			for (int i=0; i<renderings.length; i++)
			{	
				if (renderings[i] instanceof IMemoryRenderingType)
				{
					try {
						IMemoryRendering rendering = ((IMemoryRenderingType)renderings[i]).createRendering();
						if (rendering != null)
						{
							rendering.init(fContainer, blk);
							fContainer.addMemoryRendering(rendering);
						}
					} catch (CoreException e) {
						MemoryViewUtil.openError(ADD_RENDERING_FAILED, FAILED_TO_ADD_THE_SELECTED_RENDERING, e);  //$NON-NLS-1$ //$NON-NLS-2$
					}
				}					
			}
		}
	}
	
	private IDebugElement getDebugElement(ISelection selection)
	{
		if (!(selection instanceof IStructuredSelection))
			return null;

		//only single selection of PICLDebugElements is allowed for this action
		if (selection == null || selection.isEmpty() || ((IStructuredSelection)selection).size() > 1)
		{
			return null;
		}

		Object elem = ((IStructuredSelection)selection).getFirstElement();

		// if not debug element
		if (!(elem instanceof IDebugElement))
			return null;
        return (IDebugElement)elem;
	}
}
