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
package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.IAntEditorColorConstants;
import org.eclipse.jdt.internal.ui.preferences.CHyperLink;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

/*
 * The page for setting the editor options.
 */
public class AntEditorPreferencePage extends AbstractAntEditorPreferencePage {
	
	protected static class ControlData {
		private String fKey;
		private String[] fValues;
		
		public ControlData(String key, String[] values) {
			fKey= key;
			fValues= values;
		}
		
		public String getKey() {
			return fKey;
		}
		
		public String getValue(boolean selection) {
			int index= selection ? 0 : 1;
			return fValues[index];
		}
		
		public String getValue(int index) {
			return fValues[index];
		}		
		
		public int getSelection(String value) {
			if (value != null) {
				for (int i= 0; i < fValues.length; i++) {
					if (value.equals(fValues[i])) {
						return i;
					}
				}
			}
			return fValues.length -1; // assume the last option is the least severe
		}
	}
	
	/**
	 * Item in the highlighting color list.
	 * 
	 * @since 3.0
	 */
	private class HighlightingColorListItem {
		/** Display name */
		private String fDisplayName;
		/** Color preference key */
		private String fColorKey;
		/** Bold preference key */
		private String fBoldKey;
		/** Italic preference key */
		private String fItalicKey;
		/** Item color */
		private Color fItemColor;
		
		/**
		 * Initialize the item with the given values.
		 * 
		 * @param displayName the display name
		 * @param colorKey the color preference key
		 * @param boldKey the bold preference key
		 * @param italicKey the italic preference key
		 * @param itemColor the item color
		 */
		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, Color itemColor) {
			fDisplayName= displayName;
			fColorKey= colorKey;
			fBoldKey= boldKey;
			fItalicKey= italicKey;
			fItemColor= itemColor;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getBoldKey() {
			return fBoldKey;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getItalicKey() {
			return fItalicKey;
		}
		
		/**
		 * @return the color preference key
		 */
		public String getColorKey() {
			return fColorKey;
		}
		
		/**
		 * @return the display name
		 */
		public String getDisplayName() {
			return fDisplayName;
		}
		
		/**
		 * @return the item color
		 */
		public Color getItemColor() {
			return fItemColor;
		}
	}
	
	/**
	 * Color list label provider.
	 * 
	 * @since 3.0
	 */
	private class ColorListLabelProvider extends LabelProvider implements IColorProvider {

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return ((HighlightingColorListItem)element).getDisplayName();
		}
		
		/*
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			return ((HighlightingColorListItem)element).getItemColor();
		}

		/*
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			return null;
		}
	}
	
	/**
	 * Color list content provider.
	 * 
	 * @since 3.0
	 */
	private class ColorListContentProvider implements IStructuredContentProvider {

		/*
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return ((java.util.List)inputElement).toArray();
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	/** The keys of the overlay store. */
	private String[][] fSyntaxColorListModel;
	
	private final String[] fProblemPreferenceKeys= new String[] {
			AntEditorPreferenceConstants.PROBLEM_CLASSPATH,
			AntEditorPreferenceConstants.PROBLEM_PROPERTIES,
			AntEditorPreferenceConstants.PROBLEM_IMPORTS,
			AntEditorPreferenceConstants.PROBLEM_TASKS
		};
	
	private ColorEditor fSyntaxForegroundColorEditor;
	private Button fBoldCheckBox;
	private Button fItalicCheckBox;
	
	private TableViewer fHighlightingColorListViewer;
	private final java.util.List fHighlightingColorList= new ArrayList(5);
	
	private SourceViewer fPreviewViewer;
	private AntPreviewerUpdater fPreviewerUpdater;
	
	private SelectionListener fSelectionListener;
	protected Map fWorkingValues;
	protected ArrayList fComboBoxes;
	
	public AntEditorPreferencePage() {
		super();
		setDescription(AntPreferencesMessages.getString("AntEditorPreferencePage.description")); //$NON-NLS-1$
	}
	
	protected OverlayPreferenceStore createOverlayStore() {
		fSyntaxColorListModel= new String[][] {
				{AntPreferencesMessages.getString("AntEditorPreferencePage.Ant_editor_text_1"), IAntEditorColorConstants.TEXT_COLOR, null}, //$NON-NLS-1$
				{AntPreferencesMessages.getString("AntEditorPreferencePage.Ant_editor_processing_instuctions_2"),  IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR, null}, //$NON-NLS-1$
				{AntPreferencesMessages.getString("AntEditorPreferencePage.Ant_editor_constant_strings_3"),  IAntEditorColorConstants.STRING_COLOR, null},  //$NON-NLS-1$
				{AntPreferencesMessages.getString("AntEditorPreferencePage.Ant_editor_tags_4"),    IAntEditorColorConstants.TAG_COLOR, null},  //$NON-NLS-1$
				{AntPreferencesMessages.getString("AntEditorPreferencePage.Ant_editor_comments_5"), IAntEditorColorConstants.XML_COMMENT_COLOR, null}, //$NON-NLS-1$
				{AntPreferencesMessages.getString("AntEditorPreferencePage.26"), IAntEditorColorConstants.XML_DTD_COLOR, null} //$NON-NLS-1$
			};
		ArrayList overlayKeys= new ArrayList();			
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS));
			
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.CODEASSIST_AUTOINSERT));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AntEditorPreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND));		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AntEditorPreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS));
	
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_COMMENTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_DTD));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_DEFINING));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_FOLDING_TARGETS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AntEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE));
		
		for (int i= 0; i < fSyntaxColorListModel.length; i++) {
			String colorKey= fSyntaxColorListModel[i][1];
			addTextKeyToCover(overlayKeys, colorKey);
		}
		
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return new OverlayPreferenceStore(getPreferenceStore(), keys);
	}

	private void addTextKeyToCover(ArrayList overlayKeys, String mainKey) {
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, mainKey + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX));
	}
	
	private Control createAppearancePage(Composite parent) {
		Font font= parent.getFont();

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		appearanceComposite.setFont(font);
		GridLayout layout= new GridLayout(); 
		layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		String labelText= AntPreferencesMessages.getString("AntEditorPreferencePage.40"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, labelText, AntEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, 1);
		
		labelText= AntPreferencesMessages.getString("AntEditorPreferencePage.32"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, labelText, AntEditorPreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE, 0);

		return appearanceComposite;
	}
			
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IAntUIHelpContextIds.ANT_EDITOR_PREFERENCE_PAGE);
		getOverlayStore().load();
		getOverlayStore().start();
		
		createHeader(parent);
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.general")); //$NON-NLS-1$
		item.setControl(createAppearancePage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.1")); //$NON-NLS-1$
		item.setControl(createSyntaxPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.10")); //$NON-NLS-1$
		item.setControl(createProblemsTabContent(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.19")); //$NON-NLS-1$
		item.setControl(createFoldingTabContent(folder));
		
		initialize();
		
		applyDialogFont(parent);
		return folder;
	}
	
	private Control createFoldingTabContent(TabFolder folder) {
		Composite composite= new Composite(folder, SWT.NULL);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		addCheckBox(composite, AntPreferencesMessages.getString("AntEditorPreferencePage.20"), AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, 0);  //$NON-NLS-1$
		
		Label label= new Label(composite, SWT.LEFT);
		label.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.21")); //$NON-NLS-1$
		
		addCheckBox(composite, AntPreferencesMessages.getString("AntEditorPreferencePage.22"), AntEditorPreferenceConstants.EDITOR_FOLDING_DTD, 0); //$NON-NLS-1$
		addCheckBox(composite, AntPreferencesMessages.getString("AntEditorPreferencePage.23"), AntEditorPreferenceConstants.EDITOR_FOLDING_COMMENTS, 0); //$NON-NLS-1$
		addCheckBox(composite, AntPreferencesMessages.getString("AntEditorPreferencePage.24"), AntEditorPreferenceConstants.EDITOR_FOLDING_DEFINING, 0); //$NON-NLS-1$
		addCheckBox(composite, AntPreferencesMessages.getString("AntEditorPreferencePage.25"), AntEditorPreferenceConstants.EDITOR_FOLDING_TARGETS, 0); //$NON-NLS-1$
		return composite;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0, n= fSyntaxColorListModel.length; i < n; i++) {
			fHighlightingColorList.add(
				new HighlightingColorListItem (fSyntaxColorListModel[i][0], fSyntaxColorListModel[i][1],
						fSyntaxColorListModel[i][1] + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX, 
						fSyntaxColorListModel[i][1] + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX, null));
		}
		fHighlightingColorListViewer.setInput(fHighlightingColorList);
		fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(0)));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.AbstractAntEditorPreferencePage#handleDefaults()
	 */
	protected void handleDefaults() {
		handleSyntaxColorListSelection();
		restoreWorkingValuesToDefaults();
	}
	
	private Control createSyntaxPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NONE);
		colorComposite.setLayout(new GridLayout());

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.5")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fHighlightingColorListViewer= new TableViewer(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fHighlightingColorListViewer.setLabelProvider(new ColorListLabelProvider());
		fHighlightingColorListViewer.setContentProvider(new ColorListContentProvider());
		fHighlightingColorListViewer.setSorter(new WorkbenchViewerSorter());
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fHighlightingColorListViewer.getControl().setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.6")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fSyntaxForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.7")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fBoldCheckBox.setLayoutData(gd);
		
		fItalicCheckBox= new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.8")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fItalicCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.9")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(20);
		gd.heightHint= convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		fHighlightingColorListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSyntaxColorListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				PreferenceConverter.setValue(getOverlayStore(), item.getColorKey(), fSyntaxForegroundColorEditor.getColorValue());
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				getOverlayStore().setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
			}
		});
				
		fItalicCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				getOverlayStore().setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		fPreviewViewer = new SourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        
		AntSourceViewerConfiguration configuration = new AntSourceViewerConfiguration();        
	
		fPreviewViewer.configure(configuration);
		fPreviewViewer.setEditable(false);	
		Font font= JFaceResources.getFont(JFaceResources.TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);    
		
		IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] { getOverlayStore(), EditorsUI.getPreferenceStore() });
		fPreviewerUpdater= new AntPreviewerUpdater(fPreviewViewer, configuration, store);
		
		String content= loadPreviewContentFromFile("SyntaxPreviewCode.txt"); //$NON-NLS-1$
		IDocument document = new Document(content);       
		new AntDocumentSetupParticipant().setup(document);
		fPreviewViewer.setDocument(document);
		
		return fPreviewViewer.getControl();
	}
	
	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item= getHighlightingColorListItem();
		RGB rgb= PreferenceConverter.getColor(getOverlayStore(), item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(getOverlayStore().getBoolean(item.getBoldKey()));
		fItalicCheckBox.setSelection(getOverlayStore().getBoolean(item.getItalicKey()));
	}
	
	/**
	 * Returns the current highlighting color list item.
	 * 
	 * @return the current highlighting color list item
	 * @since 3.0
	 */
	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection= (IStructuredSelection) fHighlightingColorListViewer.getSelection();
		return (HighlightingColorListItem) selection.getFirstElement();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (fPreviewerUpdater != null) {
			fPreviewerUpdater.dispose();
		}
	}
	
	private Composite createProblemsTabContent(TabFolder folder) {
		fComboBoxes= new ArrayList();
		initializeWorkingValues();
		
		String[] errorWarningIgnoreLabels= new String[] {
				AntPreferencesMessages.getString("AntEditorPreferencePage.11"), AntPreferencesMessages.getString("AntEditorPreferencePage.12"), AntPreferencesMessages.getString("AntEditorPreferencePage.13")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String[] errorWarningIgnore= new String[] { 
				AntEditorPreferenceConstants.BUILDFILE_ERROR, 
				AntEditorPreferenceConstants.BUILDFILE_WARNING, 
				AntEditorPreferenceConstants.BUILDFILE_IGNORE };
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;

		Composite othersComposite= new Composite(folder, SWT.NULL);
		othersComposite.setLayout(layout);
		
		Label description= new Label(othersComposite, SWT.WRAP);
		description.setText(AntPreferencesMessages.getString("AntEditorPreferencePage.14")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan= nColumns;
		description.setLayoutData(gd);
				
		String label= AntPreferencesMessages.getString("AntEditorPreferencePage.18"); //$NON-NLS-1$
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_TASKS, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label= AntPreferencesMessages.getString("AntEditorPreferencePage.15"); //$NON-NLS-1$
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_CLASSPATH, errorWarningIgnore, errorWarningIgnoreLabels, 0);	
		
		label= AntPreferencesMessages.getString("AntEditorPreferencePage.16"); //$NON-NLS-1$
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_PROPERTIES, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label= AntPreferencesMessages.getString("AntEditorPreferencePage.17"); //$NON-NLS-1$
		addComboBox(othersComposite, label, AntEditorPreferenceConstants.PROBLEM_IMPORTS, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		return othersComposite;
	}
	
	private void initializeWorkingValues() {
		fWorkingValues= new HashMap(fProblemPreferenceKeys.length);
		for (int i = 0; i < fProblemPreferenceKeys.length; i++) {
			String key = fProblemPreferenceKeys[i];
			fWorkingValues.put(key, getPreferenceStore().getString(key));
		}
	}
	
	private void restoreWorkingValuesToDefaults() {
		fWorkingValues= new HashMap(fProblemPreferenceKeys.length);
		for (int i = 0; i < fProblemPreferenceKeys.length; i++) {
			String key = fProblemPreferenceKeys[i];
			fWorkingValues.put(key, getPreferenceStore().getDefaultString(key));
		}
		updateControls();
	}

	protected Combo addComboBox(Composite parent, String label, String key, String[] values, String[] valueLabels, int indent) {
		ControlData data= new ControlData(key, values);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indent;
				
		Label labelControl= new Label(parent, SWT.LEFT | SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);
		
		Combo comboBox= new Combo(parent, SWT.READ_ONLY);
		comboBox.setItems(valueLabels);
		comboBox.setData(data);
		comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		comboBox.addSelectionListener(getSelectionListener());
		
		Label placeHolder= new Label(parent, SWT.NONE);
		placeHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		String currValue= (String)fWorkingValues.get(key);	
		comboBox.select(data.getSelection(currValue));
		
		fComboBoxes.add(comboBox);
		return comboBox;
	}
	
	protected SelectionListener getSelectionListener() {
		if (fSelectionListener == null) {
			fSelectionListener= new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {}
	
				public void widgetSelected(SelectionEvent e) {
					controlChanged(e.widget);
				}
			};
		}
		return fSelectionListener;
	}
	
	protected void controlChanged(Widget widget) {
		ControlData data= (ControlData) widget.getData();
		String newValue= null;
		if (widget instanceof Button) {
			newValue= data.getValue(((Button)widget).getSelection());			
		} else if (widget instanceof Combo) {
			newValue= data.getValue(((Combo)widget).getSelectionIndex());
		} else {
			return;
		}
		fWorkingValues.put(data.getKey(), newValue);
	}
	
	protected void updateControls() {
		// update the UI
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Combo curr= (Combo) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			
			String currValue= (String) fWorkingValues.get(data.getKey());	
			curr.select(data.getSelection(currValue));			
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		Iterator iter= fWorkingValues.keySet().iterator();
		IPreferenceStore store= getPreferenceStore();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			store.putValue(key, (String)fWorkingValues.get(key));
		}
		if (store.needsSaving()) {
			store.putValue(AntEditorPreferenceConstants.PROBLEM, "changed"); //$NON-NLS-1$
		}
		return super.performOk();
	}
	
	private void createHeader(Composite contents) {
		String before= AntPreferencesMessages.getString("AntEditorPreferencePage.0"); //$NON-NLS-1$
		String linktext= AntPreferencesMessages.getString("AntEditorPreferencePage.2"); //$NON-NLS-1$
		String linktooltip= AntPreferencesMessages.getString("AntEditorPreferencePage.3"); //$NON-NLS-1$
		String after= AntPreferencesMessages.getString("AntEditorPreferencePage.4"); //$NON-NLS-1$
		Control description= createLinkText(contents, new Object[] {
				before, 
				new String[] {linktext, "org.eclipse.ui.preferencePages.GeneralTextEditor", linktooltip }, //$NON-NLS-1$
				after});
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= 150; // only expand further if anyone else requires it
		description.setLayoutData(gridData);
	}
	
	private Control createLinkText(Composite contents, Object[] tokens) {
		Composite description= new Composite(contents, SWT.NONE);
		RowLayout rowLayout= new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify= false;
		rowLayout.fill= true;
		rowLayout.marginBottom= 0;
		rowLayout.marginHeight= 0;
		rowLayout.marginLeft= 0;
		rowLayout.marginRight= 0;
		rowLayout.marginTop= 0;
		rowLayout.marginWidth= 0;
		rowLayout.spacing= 0;
		description.setLayout(rowLayout);
		
		for (int i= 0; i < tokens.length; i++) {
			String text;
			if (tokens[i] instanceof String[]) {
				String[] strings= (String[]) tokens[i];
				text= strings[0];
				final String target= strings[1];
				final CHyperLink link= new CHyperLink(description, SWT.NONE);
				link.setText(text);
				link.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						PreferencesUtil.createPreferenceDialogOn(link.getShell(), target, null, null);
					}
				});
				if (strings.length > 2)
					link.setToolTipText(strings[2]);
				continue;
			}
			
			text= (String) tokens[i];
			StringTokenizer tokenizer= new StringTokenizer(text, " ", true); //$NON-NLS-1$
			boolean addSpace= false;
			while (tokenizer.hasMoreTokens()) {
				String token= tokenizer.nextToken();
				if (token.trim().length() == 0 && tokenizer.hasMoreTokens()) {
					addSpace= true;
					continue;
				}
					
				Label label= new Label(description, SWT.NONE);
				label.setText((addSpace ? " " : "") + token); //$NON-NLS-1$ //$NON-NLS-2$
				addSpace= false;
			}
		}
		
		return description;
	}

}