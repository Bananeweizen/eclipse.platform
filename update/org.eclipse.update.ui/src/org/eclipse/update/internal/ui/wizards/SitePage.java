/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.net.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class SitePage extends BannerPage implements ISearchProvider {

	class TreeContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getElements(Object parent) {
			return getAllSiteBookmarks();
		}

		public Object[] getChildren(final Object parent) {
//			if (parent instanceof SiteBookmark) {
//				final SiteBookmark bookmark = (SiteBookmark) parent;
//				if (bookmark.isUnavailable())
//					return new Object[0];
//				final Object[] children =
//					getSiteCatalogWithIndicator(
//						bookmark,
//						!bookmark.isSiteConnected());
//				treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
//					public void run() {
//						if (children.length > 0)
//							handleSiteExpanded(bookmark, children);
//					}
//				});
//				return children;
//			}
			return new Object[0];
		}

		public Object getParent(Object element) {
//			if (element instanceof SiteCategory)
//				return ((SiteCategory) element).getBookmark();
			return null;
		}

		public boolean hasChildren(Object element) {
            return false;
//			return (element instanceof SiteBookmark);
		}

	}

	class TreeLabelProvider extends LabelProvider {

		public Image getImage(Object obj) {
			if (obj instanceof SiteBookmark)
				return UpdateUI.getDefault().getLabelProvider().get(
					UpdateUIImages.DESC_SITE_OBJ);
			if (obj instanceof SiteCategory)
				return UpdateUI.getDefault().getLabelProvider().get(
					UpdateUIImages.DESC_CATEGORY_OBJ);
			return super.getImage(obj);
		}

		public String getText(Object obj) {
			if (obj instanceof SiteBookmark) {
				return ((SiteBookmark) obj).getLabel();
			}
			return super.getText(obj);
		}
	}

	class ModelListener implements IUpdateModelChangedListener {
		public void objectChanged(Object object, String property) {
			treeViewer.refresh();
			checkItems();
		}

		public void objectsAdded(Object parent, Object[] children) {
            treeViewer.refresh();
			checkItems();
		}

		public void objectsRemoved(Object parent, Object[] children) {
			treeViewer.refresh();
			checkItems();
		}
	}

	private static DiscoveryFolder discoveryFolder = new DiscoveryFolder();
	private CheckboxTreeViewer treeViewer;
	private ScrolledFormText descLabel;
	private Button addSiteButton;
	private Button addLocalButton;
	private Button addLocalZippedButton;
	private Button editButton;
	private Button removeButton;
	private Button exportButton;
	private Button importButton;
	private Button envFilterCheck;
	private EnvironmentFilter envFilter;
	private UpdateSearchRequest searchRequest;
	private ModelListener modelListener;

	public SitePage(UpdateSearchRequest searchRequest) {
		super("SitePage"); //$NON-NLS-1$
        this.searchRequest = searchRequest;
		setTitle(UpdateUI.getString("SitePage.title")); //$NON-NLS-1$
		setDescription(UpdateUI.getString("SitePage.desc")); //$NON-NLS-1$
		UpdateUI.getDefault().getLabelProvider().connect(this);
    	envFilter = new EnvironmentFilter();

		modelListener = new ModelListener();
		UpdateUI.getDefault().getUpdateModel().addUpdateModelChangedListener(
			modelListener);
	}

	private void toggleEnvFilter(boolean add) {
		if (add)
			searchRequest.addFilter(envFilter);
		else
			searchRequest.removeFilter(envFilter);
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		UpdateUI
			.getDefault()
			.getUpdateModel()
			.removeUpdateModelChangedListener(
			modelListener);
		super.dispose();
	}

	/*
	 * (non-Javadoc) @see
	 * org.eclipse.update.internal.ui.wizards.BannerPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		client.setLayout(layout);

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getString("SitePage.label")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createTreeViewer(client);

		Composite buttonContainer = new Composite(client, SWT.NULL);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);

		addSiteButton = new Button(buttonContainer, SWT.PUSH);
		addSiteButton.setText(UpdateUI.getString("SitePage.addUpdateSite")); //$NON-NLS-1$
		addSiteButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addSiteButton);
		addSiteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddSite();
			}
		});

		addLocalButton = new Button(buttonContainer, SWT.PUSH);
		addLocalButton.setText(UpdateUI.getString("SitePage.addLocalSite")); //$NON-NLS-1$
		addLocalButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addLocalButton);
		addLocalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddLocal();
			}
		});

		addLocalZippedButton = new Button(buttonContainer, SWT.PUSH);
		addLocalZippedButton.setText(UpdateUI.getString("SitePage.addLocalZippedSite")); //$NON-NLS-1$
		addLocalZippedButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addLocalZippedButton);
		addLocalZippedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddLocalZipped();
			}
		});
		
		// separator
		new Label(buttonContainer, SWT.None);
		
		editButton = new Button(buttonContainer, SWT.PUSH);
		editButton.setText(UpdateUI.getString("SitePage.edit")); //$NON-NLS-1$
		editButton.setEnabled(false);
		editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(editButton);
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});

		removeButton = new Button(buttonContainer, SWT.PUSH);
		removeButton.setText(UpdateUI.getString("SitePage.remove")); //$NON-NLS-1$
		removeButton.setEnabled(false);
		removeButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(removeButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		
		// separator
		new Label(buttonContainer, SWT.None);
		
		importButton = new Button(buttonContainer, SWT.PUSH);
		importButton.setText(UpdateUI.getString("SitePage.import")); //$NON-NLS-1$
		importButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(importButton);
		importButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleImport();
			}
		});
		
		exportButton = new Button(buttonContainer, SWT.PUSH);
		exportButton.setText(UpdateUI.getString("SitePage.export")); //$NON-NLS-1$
		exportButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(exportButton);
		exportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExport();
			}
		});

		descLabel = new ScrolledFormText(client, true);
		descLabel.setText("");
		descLabel.setBackground(parent.getBackground());
		HyperlinkSettings settings = new HyperlinkSettings(parent.getDisplay());
		descLabel.getFormText().setHyperlinkSettings(settings);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
        gd.heightHint = 30;
		descLabel.setLayoutData(gd);
		
		envFilterCheck = new Button(client, SWT.CHECK);
		envFilterCheck.setText(UpdateUI.getString("SitePage.ignore")); //$NON-NLS-1$
		envFilterCheck.setSelection(true);
		toggleEnvFilter(true);
		envFilterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleEnvFilter(envFilterCheck.getSelection());
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 2;
        gd.verticalAlignment = SWT.BOTTOM;
		envFilterCheck.setLayoutData(gd);

		Dialog.applyDialogFont(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.SitePage"); //$NON-NLS-1$

		return client;
	}

	private void createTreeViewer(Composite parent) {
		treeViewer =
			new CheckboxTreeViewer(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());

		initializeItems();

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				Object element = e.getElement();
				if (element instanceof SiteBookmark)
					handleSiteChecked((SiteBookmark) element, e.getChecked());
//				else if (element instanceof SiteCategory) {
//					handleCategoryChecked(
//						(SiteCategory) element,
//						e.getChecked());
//				}
			}
		});

		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged((IStructuredSelection) e.getSelection());
			}
		});

	}

	private void initializeItems() {
		checkItems();
		updateSearchRequest();
	}

	private void checkItems() {
		TreeItem[] items = treeViewer.getTree().getItems();
		for (int i = 0; i < items.length; i++) {
			SiteBookmark bookmark = (SiteBookmark) items[i].getData();
			treeViewer.setChecked(bookmark, bookmark.isSelected());
//			String[] ignoredCats = bookmark.getIgnoredCategories();
//			treeViewer.setGrayed(bookmark, ignoredCats.length > 0
//					&& bookmark.isSelected());
		}
	}

	private void handleAddSite() {
		NewUpdateSiteDialog dialog = new NewUpdateSiteDialog(getShell());
		dialog.create();
		dialog.getShell().setText(UpdateUI.getString("SitePage.new")); //$NON-NLS-1$
		if (dialog.open() == NewUpdateSiteDialog.OK)
			updateSearchRequest();
	}

	private void handleAddLocal() {
		SiteBookmark siteBookmark = LocalSiteSelector.getLocaLSite(getShell());
		if (siteBookmark != null) {
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
            siteBookmark.setSelected(true);
			model.addBookmark(siteBookmark);
			model.saveBookmarks();
			updateSearchRequest();
		}
		return;
	}

	private void handleAddLocalZipped() {
		SiteBookmark siteBookmark =
			LocalSiteSelector.getLocaLZippedSite(getShell());
		if (siteBookmark != null) {
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
            siteBookmark.setSelected(true);
			model.addBookmark(siteBookmark);
			model.saveBookmarks();
			updateSearchRequest();
		}
		return;
	}

	private void handleRemove() {
		BusyIndicator
			.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				UpdateModel updateModel =
					UpdateUI.getDefault().getUpdateModel();
				IStructuredSelection ssel =
					(IStructuredSelection) treeViewer.getSelection();
				SiteBookmark bookmark = (SiteBookmark) ssel.getFirstElement();
				String selName = bookmark.getLabel();
				boolean answer = MessageDialog
								.openQuestion(
										getShell(),
										UpdateUI
												.getString("SitePage.remove.location.conf.title"), //$NON-NLS-1$
										UpdateUI
												.getString("SitePage.remove.location.conf")	//$NON-NLS-1$
												+ " " + selName); //$NON-NLS-1$

				if (answer && !bookmark.isReadOnly()) {
					updateModel.removeBookmark(bookmark);
					updateSearchRequest();
				}
			}
		});
	}

	private void handleEdit() {
		IStructuredSelection ssel =
			(IStructuredSelection) treeViewer.getSelection();
		SiteBookmark bookmark = (SiteBookmark) ssel.getFirstElement();
		URL oldURL = bookmark.getURL();
		EditSiteDialog dialog = new EditSiteDialog(getShell(), bookmark);
		dialog.create();
		String title = bookmark.isLocal() ? UpdateUI.getString("SitePage.dialogEditLocal") : UpdateUI.getString("SitePage.dialogEditUpdateSite"); //$NON-NLS-1$ //$NON-NLS-2$
																																				  // //$NON-NLS-2$
		dialog.getShell().setText(title);
		if (dialog.open() == EditSiteDialog.OK ) {
			URL newURL = bookmark.getURL();
			if (!UpdateManagerUtils.sameURL(oldURL, newURL)) {
				UpdateModel model = UpdateUI.getDefault().getUpdateModel();
				model.fireObjectChanged(bookmark, null);
				updateSearchRequest();	
			}
		}
	}

	private void handleImport() {
		SiteBookmark[] siteBookmarks = SitesImportExport.getImportedBookmarks(getShell());
		if (siteBookmarks != null && siteBookmarks.length > 0) {
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
			SiteBookmark[] currentBookmarks = getAllSiteBookmarks();
			for (int i=0; i<siteBookmarks.length; i++) {
				boolean siteExists = false;
				for (int j=0; !siteExists && j<currentBookmarks.length; j++)
					if (currentBookmarks[j].getURL().equals(siteBookmarks[i].getURL()))
						siteExists = true;
				if (!siteExists)
					model.addBookmark(siteBookmarks[i]);
			}
			model.saveBookmarks();
			updateSearchRequest();
		}
		return;
	}
	
	private void handleExport() {
		SitesImportExport.exportBookmarks(getShell(), getAllSiteBookmarks());
	}
	
	private void handleSiteChecked(SiteBookmark bookmark, boolean checked) {
		if (bookmark.isUnavailable()) {
			bookmark.setSelected(false);
			treeViewer.setChecked(bookmark, false);
			return;
		}
		
		bookmark.setSelected(checked);
		updateSearchRequest();
	}


	private void handleSelectionChanged(IStructuredSelection ssel) {
		boolean enable = false;
		Object item = ssel.getFirstElement();
		String description = null;
		if (item instanceof SiteBookmark) {
			enable = !((SiteBookmark) item).isReadOnly();
			description = ((SiteBookmark)item).getDescription();
		} else if (item instanceof SiteCategory) {
//			IURLEntry descEntry = ((SiteCategory)item).getCategory().getDescription();
//			if (descEntry != null)
//				description = descEntry.getAnnotation();
		}
		editButton.setEnabled(enable);
		removeButton.setEnabled(enable);

		if (description == null)
			description = ""; //$NON-NLS-1$
		descLabel.setText(UpdateManagerUtils.getWritableXMLString(description));
	}

	private void updateSearchRequest() {
		Object[] checked = treeViewer.getCheckedElements();

		UpdateSearchScope scope = new UpdateSearchScope();
		int nsites = 0;

		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof SiteBookmark) {
				SiteBookmark bookmark = (SiteBookmark) checked[i];
				scope.addSearchSite(
					bookmark.getLabel(),
					bookmark.getURL(),
					bookmark.getIgnoredCategories());
				nsites++;
			}
		}
		searchRequest.setScope(scope);
		setPageComplete(nsites > 0);
	}

	public UpdateSearchRequest getSearchRequest() {
		return searchRequest;
	}
    
	public void setVisible(boolean value) {
		super.setVisible(value);
		if (value) {
			// Reset all unavailable sites, so they can be tried again if the user wants it
			SiteBookmark[] bookmarks = getAllSiteBookmarks();
			for (int i=0; i<bookmarks.length; i++) {
				if (bookmarks[i].isUnavailable())
					bookmarks[i].setUnavailable(false);
			}
		}
	}
	
	private SiteBookmark[] getAllSiteBookmarks() {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		Object[] bookmarks = model.getBookmarkLeafs();
		Object[] sitesToVisit =
			discoveryFolder.getChildren(discoveryFolder);
		SiteBookmark[] all = new SiteBookmark[bookmarks.length + sitesToVisit.length];
		System.arraycopy(bookmarks, 0, all, 0, bookmarks.length);
		System.arraycopy(
			sitesToVisit,
			0,
			all,
			bookmarks.length,
			sitesToVisit.length);
		return all;
	}
}
