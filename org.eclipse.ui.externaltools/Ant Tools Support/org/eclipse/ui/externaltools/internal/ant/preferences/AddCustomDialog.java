package org.eclipse.ui.externaltools.internal.ant.preferences;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;
import org.eclipse.ui.externaltools.internal.model.IHelpContextIds;
import org.eclipse.ui.externaltools.internal.ui.*;
import org.eclipse.ui.externaltools.internal.ui.StatusDialog;
import org.eclipse.ui.externaltools.internal.ui.StatusInfo;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public class AddCustomDialog extends StatusDialog {
	private ZipFileStructureProvider providerCache;
	private ZipFileStructureProvider currentProvider;
	
	//A boolean to indicate if the user has typed anything
	private boolean entryChanged = false;

	protected Combo sourceNameField;
	
	// dialog store id constants
	private final static String STORE_SOURCE_NAMES_ID = "WizardZipFileImportPage1.STORE_SOURCE_NAMES_ID"; //$NON-NLS-1$
	private final static String STORE_IMPORT_ALL_RESOURCES_ID = "WizardZipFileImportPage1.STORE_IMPORT_ALL_ENTRIES_ID"; //$NON-NLS-1$
	private final static String STORE_OVERWRITE_EXISTING_RESOURCES_ID = "WizardZipFileImportPage1.STORE_OVERWRITE_EXISTING_RESOURCES_ID"; //$NON-NLS-1$
	private final static String STORE_SELECTED_TYPES_ID = "WizardZipFileImportPage1.STORE_SELECTED_TYPES_ID"; //$NON-NLS-1$

	private String title;
	private String description;
	private List libraryUrls;
	private List existingNames;
	
	private TreeAndListGroup selectionGroup;
	
	protected Button sourceBrowseButton;
	
	private Text nameField;
	
	private String customLabel;
	
	private String name=""; //$NON-NLS-1$
	private URL library= null;
	private String className=""; //$NON-NLS-1$

	/**
	 * Creates a new dialog with the given shell and title.
	 */
	public AddCustomDialog(Shell parent, List libraryUrls, List existingNames, String title, String description, String customLabel) {
		super(parent);
		this.title = title;
		this.description = description;
		this.libraryUrls = libraryUrls;
		this.existingNames= existingNames;
		this.customLabel= customLabel;
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = new Composite(parent, SWT.NULL);
		dialogArea.setLayout(new GridLayout());
		dialogArea.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		dialogArea.setSize(dialogArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		dialogArea.setFont(parent.getFont());

		createNameGroup(dialogArea);
		new Label(dialogArea, SWT.NULL);
		createRootDirectoryGroup(dialogArea);
		createFileSelectionGroup(dialogArea);
		
		if (library != null) {
			setSourceName(library.getFile());
		}
		return dialogArea;
	}
	
	private void createNameGroup(Composite dialogArea) {
		Composite nameContainerGroup = new Composite(dialogArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		nameContainerGroup.setLayout(layout);
		nameContainerGroup.setFont(dialogArea.getFont());
		nameContainerGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		
		Label label = new Label(nameContainerGroup, SWT.NONE);
		label.setFont(dialogArea.getFont());
		label.setText(AntPreferencesMessages.getString("AddCustomDialog.&Name__3")); //$NON-NLS-1$
		
		nameField = new Text(nameContainerGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
	
		nameField.setLayoutData(data);
		nameField.setFont(dialogArea.getFont());
		nameField.setText(name);
		nameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});
	}

	/* (non-Javadoc)
	 * Method declared on Window.
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
		WorkbenchHelp.setHelp(newShell, IHelpContextIds.ADD_TASK_DIALOG);
	}
	/**
	 * Clears the cached structure provider after first finalizing
	 * it properly.
	 */
	protected void clearProviderCache() {
		if (providerCache != null) {
			closeZipFile(providerCache.getZipFile());
			providerCache = null;
		}
	}
	/**
	 * Attempts to close the passed zip file, and answers a boolean indicating success.
	 */
	protected boolean closeZipFile(ZipFile file) {
		try {
			file.close();
		} catch (IOException e) {
			ExternalToolsPlugin.getDefault().log(MessageFormat.format(AntPreferencesMessages.getString("AddCustomDialog.Could_not_close_zip_file_{0}_4"), new String[]{file.getName()}), e); //$NON-NLS-1$
			return false;
		}

		return true;
	}

	/**
	 *	Create the group for creating the root directory
	 */
	private void createRootDirectoryGroup(Composite parent) {
		Composite sourceContainerGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight=0;
		layout.marginWidth=0;
		
		sourceContainerGroup.setLayout(layout);
		sourceContainerGroup.setFont(parent.getFont());
		sourceContainerGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		Label groupLabel = new Label(sourceContainerGroup, SWT.NONE);
		groupLabel.setText(AntPreferencesMessages.getString("AddCustomDialog.&Library__5")); //$NON-NLS-1$
		groupLabel.setFont(parent.getFont());

		// source name entry field
		sourceNameField = new Combo(sourceContainerGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		sourceNameField.setLayoutData(data);
		sourceNameField.setFont(parent.getFont());

		Iterator libraries= libraryUrls.iterator();
		while (libraries.hasNext()) {
			URL library = (URL) libraries.next();
			sourceNameField.add(library.getFile());
		}
		sourceNameField.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateFromSourceField();
			}
		});

		sourceNameField.addKeyListener(new KeyListener() {
			/*
			 * @see KeyListener.keyPressed
			 */
			public void keyPressed(KeyEvent e) {
				//If there has been a key pressed then mark as dirty
				entryChanged = true;
			}

			/*
			 * @see KeyListener.keyReleased
			 */
			public void keyReleased(KeyEvent e) {
			}
		});

		sourceNameField.addFocusListener(new FocusListener() {
			/*
			 * @see FocusListener.focusGained(FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
				//Do nothing when getting focus
			}

			/*
			 * @see FocusListener.focusLost(FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
				//Clear the flag to prevent constant update
				if (entryChanged) {
					entryChanged = false;
					updateFromSourceField();
				}
			}
		});
	}
	
	/**
	 * Update the receiver from the source name field.
	 */
	private void updateFromSourceField(){
		setSourceName(sourceNameField.getText());
		updateStatus();
	}
	
	
	/**
	 * Check the field values and display a message in the status if needed.
	 */
	private void updateStatus() {
		StatusInfo status= new StatusInfo();
		String name= nameField.getText().trim();
		if (name.length() == 0) {
			status.setError(MessageFormat.format(AntPreferencesMessages.getString("AddCustomDialog.A_name_must_be_provided_for_the_new_{0}_6"), new String[]{customLabel})); //$NON-NLS-1$
		} else {
			Iterator names= existingNames.iterator();
			while (names.hasNext()) {
				String aName = (String) names.next();
				if(aName.equals(name)) {
					status.setError(MessageFormat.format(AntPreferencesMessages.getString("AddCustomDialog.A_{0}_with_the_name_{1}_already_exists_7"), new String[]{customLabel, name})); //$NON-NLS-1$
					updateStatus(status);
					return;
				}
			}
		} 
		if (selectionGroup.getListTableSelection().isEmpty()) {
			status.setError(AntPreferencesMessages.getString("AddCustomDialog.A_class_file_must_be_selected_from_the_library_8")); //$NON-NLS-1$
		}
		updateStatus(status);
	}

	
	/**
	 * Sets the source name of the import to be the supplied path.
	 * Adds the name of the path to the list of items in the
	 * source combo and selects it.
	 *
	 * @param path the path to be added
	 */
	protected void setSourceName(String path) {

		if (path.length() > 0) {

			String[] currentItems = this.sourceNameField.getItems();
			int selectionIndex = -1;
			for (int i = 0; i < currentItems.length; i++) {
				if (currentItems[i].equals(path)) {
					selectionIndex = i;
					break;
				}
			}
			if (selectionIndex < 0) {
				int oldLength = currentItems.length;
				String[] newItems = new String[oldLength + 1];
				System.arraycopy(currentItems, 0, newItems, 0, oldLength);
				newItems[oldLength] = path;
				this.sourceNameField.setItems(newItems);
				selectionIndex = oldLength;
			}
			this.sourceNameField.select(selectionIndex);

			resetSelection();
		}
	}

	/**
	*	Create the import source selection widget
	*/
	protected void createFileSelectionGroup(Composite parent) {
		//Just create with a dummy root.
		FileSystemElement dummyRoot= new FileSystemElement("Dummy", null, true); //$NON-NLS-1$
		this.selectionGroup = new TreeAndListGroup(parent, dummyRoot, 
			getFolderProvider(), new WorkbenchLabelProvider(), getFileProvider(), new WorkbenchLabelProvider(), SWT.NONE, 400, 150);

		ISelectionChangedListener listener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatus();
			}
		};

		WorkbenchViewerSorter sorter = new WorkbenchViewerSorter();
		this.selectionGroup.setTreeSorter(sorter);
		this.selectionGroup.setListSorter(sorter);
		this.selectionGroup.addSelectionChangedListener(listener);

	}
	
	/**
	 *	Answer a boolean indicating whether the specified source currently exists
	 *	and is valid (ie.- proper format)
	 */
	protected boolean ensureSourceIsValid() {
		ZipFile specifiedFile = getSpecifiedSourceFile();

		if (specifiedFile == null)
			return false;

		return closeZipFile(specifiedFile);
	}
	/**
	*	Answer the root FileSystemElement that represents the contents of the
	*	currently-specified .zip file.  If this FileSystemElement is not
	*	currently defined then create and return it.
	*/
	protected MinimizedFileSystemElement getFileSystemTree() {

		ZipFile sourceFile = getSpecifiedSourceFile();
		if (sourceFile == null) {
			//Clear out the provider as well
			this.currentProvider = null;
			return null;
		}

		ZipFileStructureProvider provider = getStructureProvider(sourceFile);
		this.currentProvider = provider;
		return selectFiles(provider.getRoot(), provider);
	}
	
	/**
	 * Invokes a file selection operation using the specified file system and
	 * structure provider.  If the user specifies files then this selection is
	 * cached for later retrieval and is returned.
	 */
	protected MinimizedFileSystemElement selectFiles(final Object rootFileSystemObject, final ZipFileStructureProvider structureProvider) {

		final MinimizedFileSystemElement[] results = new MinimizedFileSystemElement[1];

		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				//Create the root element from the supplied file system object
				results[0] = createRootElement(rootFileSystemObject, structureProvider);
			}
		});

		return results[0];
	}
	
	/**
	 * Creates and returns a <code>MinimizedFileSystemElement</code> if the specified
	 * file system object merits one.
	 */
	protected MinimizedFileSystemElement createRootElement(Object fileSystemObject, ZipFileStructureProvider provider) {
		boolean isContainer = provider.isFolder(fileSystemObject);
		String elementLabel = provider.getLabel(fileSystemObject);

		// Use an empty label so that display of the element's full name
		// doesn't include a confusing label
		MinimizedFileSystemElement dummyParent =
			new MinimizedFileSystemElement("", null, true);//$NON-NLS-1$
		dummyParent.setPopulated();
		MinimizedFileSystemElement result =
			new MinimizedFileSystemElement(elementLabel, dummyParent, isContainer);
		result.setFileSystemObject(fileSystemObject);

		//Get the files for the element so as to build the first level
		result.getFiles(provider);

		return dummyParent;
	}
	
	/**
	 *	Answer a handle to the zip file currently specified as being the source.
	 *	Return null if this file does not exist or is not of valid format.
	 */
	protected ZipFile getSpecifiedSourceFile() {
		try {
			return new ZipFile(sourceNameField.getText());
		} catch (ZipException e) {
			StatusInfo status= new StatusInfo();
			status.setError("Specified zip file is not in the correct format");
			updateStatus(status);
		} catch (IOException e) {
			StatusInfo status= new StatusInfo();
			status.setError("Specified zip file could not be read");
			updateStatus(status);
		}

		sourceNameField.setFocus();
		return null;
	}
	/**
	 * Returns a structure provider for the specified zip file.
	 */
	protected ZipFileStructureProvider getStructureProvider(ZipFile targetZip) {
		if (providerCache == null)
			providerCache = new ZipFileStructureProvider(targetZip);
		else if (!providerCache.getZipFile().getName().equals(targetZip.getName())) {
			clearProviderCache();
			// ie.- new value, so finalize & remove old value
			providerCache = new ZipFileStructureProvider(targetZip);
		} else if (!providerCache.getZipFile().equals(targetZip)) {
			closeZipFile(targetZip); // ie.- duplicate handle to same .zip
		}

		return providerCache;
	}

	/**
	 *	Repopulate the view based on the currently entered directory.
	 */
	protected void resetSelection() {
		MinimizedFileSystemElement currentRoot = getFileSystemTree();
		selectionGroup.setRoot(currentRoot);
		
		if (className != null) {
			StringTokenizer tokenizer= new StringTokenizer(className, "."); //$NON-NLS-1$
			if (selectClass(currentRoot, tokenizer)) {
				//getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		}
	}
	
	private boolean selectClass(MinimizedFileSystemElement currentParent, StringTokenizer tokenizer) {
		List folders= currentParent.getFolders(currentProvider);
		if (folders.size() == 1) {
			MinimizedFileSystemElement element = (MinimizedFileSystemElement)folders.get(0);
			if (element.getLabel(null).equals("/")) { //$NON-NLS-1$
				selectionGroup.selectAndRevealFolder(element);
				return selectClass(element, tokenizer);
			}
		}
		String currentName= tokenizer.nextToken();
		if (tokenizer.hasMoreTokens()) {
			Iterator allFolders= folders.iterator();
			while (allFolders.hasNext()) {
				MinimizedFileSystemElement folder = (MinimizedFileSystemElement) allFolders.next();
				if (folder.getLabel(null).equals(currentName)) {
					selectionGroup.selectAndRevealFolder(folder);
					return selectClass(folder, tokenizer);
				}
			}	
		} else {
			List files= currentParent.getFiles(currentProvider);
			Iterator iter= files.iterator();
			while (iter.hasNext()) {
				MinimizedFileSystemElement file = (MinimizedFileSystemElement) iter.next();
				if (file.getLabel(null).equals(currentName + ".class")) { //$NON-NLS-1$
					selectionGroup.selectAndRevealFile(file);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns a content provider for <code>MinimizedFileSystemElement</code>s that returns
	 * only files as children.
	 */
	protected ITreeContentProvider getFileProvider() {
		return new WorkbenchContentProvider() {
			public Object[] getChildren(Object o) {
				if (o instanceof MinimizedFileSystemElement) {
					MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
					return element.getFiles(currentProvider).toArray();
				}
				return new Object[0];
			}
		};
	}

	/**
	 * Returns a content provider for <code>MinimizedFileSystemElement</code>s that returns
	 * only folders as children.
	 */
	protected ITreeContentProvider getFolderProvider() {
		return new WorkbenchContentProvider() {
			public Object[] getChildren(Object o) {
				if (o instanceof MinimizedFileSystemElement) {
					MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
					return element.getFolders(currentProvider).toArray();
				}
				return new Object[0];
			}
			public boolean hasChildren(Object o) {
				if (o instanceof MinimizedFileSystemElement) {
					MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
					if (element.isPopulated())
						return getChildren(element).length > 0;
					else {
						//If we have not populated then wait until asked
						return true;
					}
				}
				return false;
			}
		};
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	protected void cancelPressed() {
		clearProviderCache();
		super.cancelPressed();
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		clearProviderCache();
		name=nameField.getText().trim();
		library= (URL)libraryUrls.get(sourceNameField.getSelectionIndex());
		IStructuredSelection selection= this.selectionGroup.getListTableSelection();
		MinimizedFileSystemElement element= (MinimizedFileSystemElement)selection.getFirstElement();
		ZipEntry entry= (ZipEntry)element.getFileSystemObject();
		className= entry.getName();
		int index= className.lastIndexOf('.');
		className= className.substring(0, index);
		className= className.replace('/', '.'); 
		super.okPressed();
	}
	
	protected String getName() {
		return name;
	}
	
	protected void setName(String name) {
			this.name= name;
		} 
	
	protected void setLibrary(URL library) {
		this.library= library;
	}
	
	protected URL getLibrary() {
		return this.library;
	}
	
	protected String getClassName() {
		return className;
	}
	
	protected void setClassName(String className) {
		this.className= className;
	}
	
	/**
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		getButton(IDialogConstants.OK_ID).setEnabled(!(library == null));
	}
}
