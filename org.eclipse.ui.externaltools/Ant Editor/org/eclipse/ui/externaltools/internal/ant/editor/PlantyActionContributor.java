package org.eclipse.ui.externaltools.internal.ant.editor;
//
// PlantyActionContributor.java
//
// Copyright:
// GEBIT Gesellschaft fuer EDV-Beratung
// und Informatik-Technologien mbH, 
// Berlin, Duesseldorf, Frankfurt (Germany) 2002
// All rights reserved.
//
import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

/**
 * Contributes interesting Java actions to the desktop's Edit menu and the toolbar.
 * 
 * @author Alf Schiefelbein
 */
public class PlantyActionContributor extends TextEditorActionContributor {

	protected RetargetTextEditorAction fContentAssistProposal;
//    protected RetargetTextEditorAction fContentAssistTip;

	/**
	 * Default constructor.
	 */
	public PlantyActionContributor() {
		super();
		fContentAssistProposal= new RetargetTextEditorAction(ResourceBundle.getBundle("org.eclipse.ui.externaltools.internal.ant.editor.PlantyMessages"), "ContentAssistProposal."); //$NON-NLS-1$ //$NON-NLS-2$
//        fContentAssistTip= new RetargetTextEditororg.eclipse.ui.externaltools.internal.ant.editorBundle.getBundle("org.eclipse.ui.externaltools.internal.ant.editor.PlantyMessages"), "ContentAssistTip."); //$NON-NLS-1$
	}
	
	private void doSetActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor editor= null;
		if (part instanceof ITextEditor)
			editor= (ITextEditor) part;

		fContentAssistProposal.setAction(getAction(editor, "ContentAssistProposal")); //$NON-NLS-1$
//        fContentAssistTip.setAction(getAction(editor, "ContentAssistTip")); //$NON-NLS-1$
	}
	
    /*
     * @see IEditorActionBarContributor#init(IActionBars)
     */
    public void init(IActionBars bars) {
        super.init(bars);
        
        IMenuManager menuManager= bars.getMenuManager();
        IMenuManager editMenu= menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            editMenu.add(new org.eclipse.jface.action.Separator());
            editMenu.add(fContentAssistProposal);
//            editMenu.add(fContentAssistTip);
        }   
        
    }
    
	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		doSetActiveEditor(part);
	}
	
	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}
}
