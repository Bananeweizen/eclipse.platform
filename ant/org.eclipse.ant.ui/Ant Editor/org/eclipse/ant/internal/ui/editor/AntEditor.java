/*******************************************************************************
 * Copyright (c) 2002, 2004 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 * 	   John-Mason P. Shackelford - bug 40255
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.util.ResourceBundle;

import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.outline.AntEditorContentOutlinePage;
import org.eclipse.ant.internal.ui.editor.outline.AntModel;
import org.eclipse.ant.internal.ui.editor.outline.XMLCore;
import org.eclipse.ant.internal.ui.editor.text.AnnotationAccess;
import org.eclipse.ant.internal.ui.editor.text.AntEditorDocumentProvider;
import org.eclipse.ant.internal.ui.editor.text.IAntEditorColorConstants;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * The actual editor implementation for Eclipse's Ant integration.
 */
public class AntEditor extends TextEditor {

	static class TabConverter {
		
		private int fTabRatio;
		private ILineTracker fLineTracker;
		
		public void setNumberOfSpacesPerTab(int ratio) {
			fTabRatio= ratio;
		}
		
		public void setLineTracker(ILineTracker lineTracker) {
			fLineTracker= lineTracker;
		}
		
		private int insertTabString(StringBuffer buffer, int offsetInLine) {
			
			if (fTabRatio == 0) {
				return 0;
			}
				
			int remainder= offsetInLine % fTabRatio;
			remainder= fTabRatio - remainder;
			for (int i= 0; i < remainder; i++) {
				buffer.append(' ');
			}
			return remainder;
		}
		
		public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
			String text= command.text;
			if (text == null) {
				return;
			}
				
			int index= text.indexOf('\t');
			if (index > -1) {
				
				StringBuffer buffer= new StringBuffer();
				
				fLineTracker.set(command.text);
				int lines= fLineTracker.getNumberOfLines();
				
				try {
						
					for (int i= 0; i < lines; i++) {
						
						int offset= fLineTracker.getLineOffset(i);
						int endOffset= offset + fLineTracker.getLineLength(i);
						String line= text.substring(offset, endOffset);
						
						int position= 0;
						if (i == 0) {
							IRegion firstLine= document.getLineInformationOfOffset(command.offset);
							position= command.offset - firstLine.getOffset();	
						}
						
						int length= line.length();
						for (int j= 0; j < length; j++) {
							char c= line.charAt(j);
							if (c == '\t') {
								position += insertTabString(buffer, position);
							} else {
								buffer.append(c);
								++ position;
							}
						}
						
					}
						
					command.text= buffer.toString();
						
				} catch (BadLocationException x) {
				}
			}
		}
	}	
	class StatusLineSourceViewer extends SourceViewer{
		
		private boolean fIgnoreTextConverters= false;
		
		public StatusLineSourceViewer(Composite composite, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, int styles) {
			super(composite, verticalRuler, overviewRuler, isOverviewRulerVisible(), styles);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.ITextOperationTarget#doOperation(int)
		 */
		public void doOperation(int operation) {
			if (getTextWidget() == null || !redraws()) {
				return;
			}

			switch (operation) {
				case CONTENTASSIST_PROPOSALS:
					String msg= fContentAssistant.showPossibleCompletions();
					setStatusLineErrorMessage(msg);
					return;
				case UNDO:
					fIgnoreTextConverters= true;
					break;
				case REDO:
					fIgnoreTextConverters= true;
					break;
				default :
					super.doOperation(operation);
			}
		}
		
		public void setTextConverter(TabConverter tabConverter) {
			fTabConverter= tabConverter;
		}
		
		public void updateIndentationPrefixes() {
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			String[] types= configuration.getConfiguredContentTypes(this);
			for (int i= 0; i < types.length; i++) {
				String[] prefixes= configuration.getIndentPrefixes(this, types[i]);
				if (prefixes != null && prefixes.length > 0) {
					setIndentPrefixes(prefixes, types[i]);
				}
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.TextViewer#customizeDocumentCommand(org.eclipse.jface.text.DocumentCommand)
		 */
		protected void customizeDocumentCommand(DocumentCommand command) {
			super.customizeDocumentCommand(command);
			if (!fIgnoreTextConverters && fTabConverter != null) {
				fTabConverter.customizeDocumentCommand(getDocument(), command);
			}
			fIgnoreTextConverters= false;
		}
	}

	/**
	 * Selection changed listener for the outline view.
	 */
    protected SelectionChangedListener selectionChangedListener = new SelectionChangedListener();
    class SelectionChangedListener  implements ISelectionChangedListener {
        public void selectionChanged(SelectionChangedEvent event) {
            doSelectionChanged(event);
        }
    }
    
    /**
     * The page that shows the outline.
     */
    protected AntEditorContentOutlinePage page;
    
    /** The editor's tab to spaces converter */
	private TabConverter fTabConverter;
  
    /**
     * Constructor for AntEditor.
     */
    public AntEditor() {
        super();
		setSourceViewerConfiguration(new AntEditorSourceViewerConfiguration(this));
		setDocumentProvider(new AntEditorDocumentProvider(XMLCore.getDefault()));
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     */
    protected void createActions() {
        super.createActions();

        ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.ant.internal.ui.editor.AntEditorMessages"); //$NON-NLS-1$
        IAction action = new ContentAssistAction(bundle, "ContentAssistProposal.", this); //$NON-NLS-1$
        
        // This action definition is associated with the accelerator Ctrl+Space
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction("ContentAssistProposal", action); //$NON-NLS-1$
        
		action = new TextOperationAction(bundle, "ContentFormat.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("ContentFormat", action); //$NON-NLS-1$
        
		//TODO set help
		//WorkbenchHelp.setHelp(action, IJavaHelpContextIds.FORMAT_ACTION);
    }

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextEditor#initializeEditor()
	 * Called from TextEditor.<init>
	 */
    protected void initializeEditor() {
		super.initializeEditor();
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
		setCompatibilityMode(false);
		setHelpContextId(IAntUIHelpContextIds.ANT_EDITOR);	
    }
   
	/* (non-Javadoc)
     * Method declared on IAdaptable
     */
    public Object getAdapter(Class key) {
        if (key.equals(IContentOutlinePage.class)) {
			return getOutlinePage();
        }
        return super.getAdapter(key);
    }

	private AntEditorContentOutlinePage getOutlinePage() {
		if (page == null) {
			page= new AntEditorContentOutlinePage(XMLCore.getDefault());
			page.addPostSelectionChangedListener(selectionChangedListener);
			setOutlinePageInput(getEditorInput());
		}
		return page;
	}

    private void doSelectionChanged(SelectionChangedEvent aSelectionChangedEvent) {
        IStructuredSelection selection= (IStructuredSelection)aSelectionChangedEvent.getSelection();

        if (!isActivePart() && AntUIPlugin.getActivePage() != null) {
			AntUIPlugin.getActivePage().bringToTop(this);
        }
        
        /*
         * Here the according ISourceReference should be determined and
         * then passed to setSelection.
         */
        AntElementNode selectedXmlElement = (AntElementNode)selection.getFirstElement(); 
        if(selectedXmlElement != null) {
			setSelection(selectedXmlElement, !isActivePart());
        }
    }

    /*
     * Returns whether the editor is active.
     */
    private boolean isActivePart() {
        IWorkbenchWindow window= getSite().getWorkbenchWindow();
        IPartService service= window.getPartService();
        IWorkbenchPart part= service.getActivePart();
        return part != null && part.equals(this);
    }
    
    private void setSelection(AntElementNode reference, boolean moveCursor) {
        if (reference != null) {
        	if (reference.isExternal()) {
        		while (!reference.isRootExternal() || (reference.getParentNode() != null && reference.getParentNode().isExternal())) {
					//no possible selection for this external element
					//find the root external entity actually in the document
        			reference= reference.getParentNode();
        		}
        	}
            
            StyledText  textWidget= null;
            
            ISourceViewer sourceViewer= getSourceViewer();
            if (sourceViewer != null) {
                textWidget= sourceViewer.getTextWidget();
            }
            
            if (textWidget == null) {
                return;
            }
                
            try {
                
                int offset= reference.getOffset();
                int length= reference.getSelectionLength();
                
                if (offset < 0) {
                    return;
                }
                    
                textWidget.setRedraw(false);
                
                if(length > 0) {
	                setHighlightRange(offset, length, moveCursor);
                }
                
                if (!moveCursor) {
                    return;
                }
                                            
                if (offset > -1 && length > 0) {
                    sourceViewer.revealRange(offset, length);
                    // Selected region begins one index after offset
                    sourceViewer.setSelectedRange(offset, length); 
                }
            } catch (IllegalArgumentException x) {
            	AntUIPlugin.log(x);
            } finally {
                if (textWidget != null) {
                    textWidget.setRedraw(true);
                }
            }
            
        } else if (moveCursor) {
            resetHighlightRange();
        }
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#affectsTextPresentation(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property= event.getProperty();
		return property.equals(IAntEditorColorConstants.P_DEFAULT) ||
		property.equals(IAntEditorColorConstants.P_PROC_INSTR) ||
		property.equals(IAntEditorColorConstants.P_STRING) ||
		property.equals(IAntEditorColorConstants.P_TAG) ||
		property.equals(IAntEditorColorConstants.P_XML_COMMENT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#handlePreferenceStoreChanged(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property == AntEditorPreferenceConstants.VALIDATE_BUILDFILES) {
			setResolveFully(((Boolean)event.getNewValue()).booleanValue());
			return;
		}
		
		if (ExtendedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
			Object value= event.getNewValue();
			StatusLineSourceViewer viewer= (StatusLineSourceViewer) getSourceViewer();
			if (value instanceof Integer) {
				viewer.getTextWidget().setTabs(((Integer) value).intValue());
			} else if (value instanceof String) {
				viewer.getTextWidget().setTabs(Integer.parseInt((String) value));
			}
			return;
		}
		
		if (AntEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS.equals(property)) {
			if (isTabConversionEnabled()) {
				startTabConversion();
			} else {
				stopTabConversion();
			}
			return;
		}
		
		AntEditorSourceViewerConfiguration sourceViewerConfiguration= (AntEditorSourceViewerConfiguration)getSourceViewerConfiguration();
		if (affectsTextPresentation(event)) {
			sourceViewerConfiguration.updateScanners();
		}
		
		sourceViewerConfiguration.changeConfiguration(event);
							
		super.handlePreferenceStoreChanged(event);
	}
	
	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#doSetInput(org.eclipse.ui.IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		setOutlinePageInput(input);
	}

	private void setOutlinePageInput(IEditorInput input) {
		if (page != null) {
			IDocumentProvider provider= getDocumentProvider();
			if (provider instanceof AntEditorDocumentProvider) {
				AntEditorDocumentProvider documentProvider= (AntEditorDocumentProvider) provider;
				AntModel model= documentProvider.getAntModel(input);
				page.setPageInput(model);
			}
		}
	}
	
	/**
	 * Returns the Ant model for the current editor input of this editor.
	 * @return the Ant model for this editor or <code>null</code>
	 */
	public AntModel getAntModel() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof AntEditorDocumentProvider) {
			AntEditorDocumentProvider documentProvider= (AntEditorDocumentProvider) provider;
			return documentProvider.getAntModel(getEditorInput());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createAnnotationAccess()
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new AnnotationAccess();
	}
	
	/**
	 * Creates the source viewer to be used by this editor.
	 * Subclasses may re-implement this method.
	 *
	 * @param parent the parent control
	 * @param ruler the vertical ruler
	 * @param styles style bits
	 * @return the source viewer
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		fOverviewRuler= createOverviewRuler(getSharedColors());
		ISourceViewer viewer= new StatusLineSourceViewer(parent, ruler, getOverviewRuler(), styles);
		//ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}
	
	/**
	 * Ses the given message as error message to this editor's status line.
	 * @param msg message to be set
	 */
	protected void setStatusLineErrorMessage(String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, msg, null);	
	}

	private void setResolveFully(boolean resolveFully) {
		getAntModel().setResolveFully(resolveFully);
		
	}

	public void openReferenceElement() {
		ISelection selection= getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection)selection;
			String text= textSelection.getText();
			AntElementNode node= getAntModel().getReferenceNode(text);
			if (node != null) {
				setSelection(node, true);
				return;
			} 
		}
		setStatusLineErrorMessage(AntEditorMessages.getString("AntEditor.3")); //$NON-NLS-1$
		getSite().getShell().getDisplay().beep();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		
		IAction formatAction= getAction("ContentFormat"); //$NON-NLS-1$
		if (formatAction == null) {
			return;
		}
		
		if (formatAction.isEnabled()) {
			menu.add(formatAction);
		}
	}
	
	private void startTabConversion() {
		if (fTabConverter == null) {
			fTabConverter= new TabConverter();
			fTabConverter.setLineTracker(new DefaultLineTracker());
			fTabConverter.setNumberOfSpacesPerTab(getTabSize());
			StatusLineSourceViewer viewer= (StatusLineSourceViewer) getSourceViewer();
			viewer.setTextConverter(fTabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			viewer.updateIndentationPrefixes();
		}
	}
	
	private void stopTabConversion() {
		if (fTabConverter != null) {
			StatusLineSourceViewer viewer= (StatusLineSourceViewer) getSourceViewer();
			viewer.setTextConverter(null);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			viewer.updateIndentationPrefixes();
			fTabConverter= null;
		}
	}
	
	private int getTabSize() {
		IPreferenceStore preferences= getPreferenceStore();
		return preferences.getInt(ExtendedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		if (isTabConversionEnabled()) {
			startTabConversion();		
		}
	}
	
	private boolean isTabConversionEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(AntEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
	}
}