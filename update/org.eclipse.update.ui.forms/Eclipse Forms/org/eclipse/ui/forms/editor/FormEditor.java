/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.forms.editor;
import java.util.Vector;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.*;
import org.eclipse.update.ui.forms.internal.FormsPlugin;
/**
 * This class forms a base of multi-page form editors that typically use one or
 * more pages with forms and one page for raw source of the editor input.
 * <p>
 * Pages are added 'lazily' i.e. adding a page reserves a tab for it but does
 * not cause the page control to be created. Page control is created when an
 * attempt is made to select the page in question. This allows editors with
 * several tabs and complex pages to open quickly.
 * 
 * @since 3.0
 */
public abstract class FormEditor extends MultiPageEditorPart {
	private FormToolkit toolkit;
	protected Vector pages;
	private IEditorPart sourcePage;
	private IEditorPart lastActiveEditor = null;
	private int currentPage = -1;
	/**
	 * The constructor.
	 */
	public FormEditor() {
		pages = new Vector();
	}
	/**
	 * Creates the common toolkit for this editor and adds pages to the editor.
	 * 
	 * @see #addPages
	 */
	protected void createPages() {
		toolkit = createToolkit(getContainer().getDisplay());
		addPages();
	}
	protected FormToolkit createToolkit(Display display) {
		return new FormToolkit(display);
	}
	/**
	 * Subclass should implement this method to add pages to the editor using
	 * 'addPage(IFormPage)' method.
	 */
	protected abstract void addPages();
	/**
	 * Overrides 'super' to create a multi-page key binding editor site for key
	 * binding delegation down to the editor nested sites.
	 */
	protected IEditorSite createSite(IEditorPart editor) {
		return new MultiPageKeyBindingEditorSite(this, editor);
	}
	/**
	 * Adds the form page to this editor.
	 * 
	 * @param page
	 *            the page to add
	 */
	public int addPage(IFormPage page) {
		int i = addPage(page.getPartControl());
		configurePage(i, page);
		return i;
	}
	public int addPage(IEditorPart editor, IEditorInput input)
			throws PartInitException {
		int index = super.addPage(editor, input);
		if (editor instanceof IFormPage) {
			configurePage(index, (IFormPage) editor);
		}
		return index;
	}
	protected void configurePage(int index, IFormPage page) {
		setPageText(index, page.getTitle());
		//setPageImage(index, page.getTitleImage());
		page.setIndex(index);
		registerPage(page);
	}
	/**
	 * Disposes the pages and the toolkit after disposing the editor itself.
	 * Subclasses must call 'super' when reimplementing the method.
	 */
	public void dispose() {
		super.dispose();
		for (int i = 0; i < pages.size(); i++) {
			IFormPage page = (IFormPage) pages.get(i);
			// don't dispose source pages because they will
			// be disposed as nested editors by the superclass
			if (!page.isSource()) page.dispose();
		}
		pages = null;
		toolkit.dispose();
		toolkit = null;
	}
	/**
	 * Returns the toolkit owned by this editor.
	 * 
	 * @return the toolkit object
	 */
	public FormToolkit getToolkit() {
		return toolkit;
	}
	/*
	 * Widens visibility for access by MultiPageKeyBindingEditorSite
	 */
	public IEditorPart getActiveEditor() {
		return super.getActiveEditor();
	}
	/**
	 * Returns the current page index. The value is identical to the value of
	 * 'getActivePage()' except during the page switch, when this method still
	 * has the old active page index.
	 * 
	 * @return
	 */
	protected int getCurrentPage() {
		return currentPage;
	}
	/**
	 * @see MultiPageEditorPart#pageChange(int)
	 */
	protected void pageChange(int newPageIndex) {
		// deactivate the old editor's site
		if (lastActiveEditor != null) {
			((MultiPageKeyBindingEditorSite) lastActiveEditor.getSite())
					.deactivate();
			lastActiveEditor = null;
		}
		// Now is the absolute last moment to create the page control.
		IFormPage page = (IFormPage) pages.get(newPageIndex);
		if (page.getPartControl() == null) {
			page.createPartControl(getContainer());
			setControl(newPageIndex, page.getPartControl());
		}
		// fix for windows handles
		int oldPage = getCurrentPage();
		if (pages.size() > newPageIndex
				&& pages.get(newPageIndex) instanceof IFormPage)
			((IFormPage) pages.get(newPageIndex)).setActive(true);
		if (oldPage != -1 && pages.size() > oldPage
				&& pages.get(oldPage) instanceof IFormPage)
			((IFormPage) pages.get(oldPage)).setActive(false);
		// Call super - this will cause pages to switch
		super.pageChange(newPageIndex);
		// activate the new editor's site
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor != null) {
			if (activeEditor.getSite() instanceof MultiPageKeyBindingEditorSite) {
				((MultiPageKeyBindingEditorSite) activeEditor.getSite())
						.activate();
				lastActiveEditor = activeEditor;
			}
		}
		this.currentPage = newPageIndex;
	}
	/**
	 * Sets the active page using the unique page identifier.
	 * 
	 * @param pageId
	 *            the id of the page to switch to
	 * @return page that was set active or <samp>null</samp> if not found.
	 */
	public IFormPage setActivePage(String pageId) {
		for (int i = 0; i < pages.size(); i++) {
			IFormPage page = (IFormPage) pages.get(i);
			if (page.getId().equals(pageId)) {
				setActivePage(i);
				return page;
			}
		}
		return null;
	}
	/**
	 * Finds the page instance that has the provided id.
	 * 
	 * @param pageId
	 *            the id of the page to find
	 * @return page with the matching id or <code>null</code> if not found.
	 */
	public IFormPage findPage(String pageId) {
		for (int i = 0; i < pages.size(); i++) {
			IFormPage page = (IFormPage) pages.get(i);
			if (page.getId().equals(pageId))
				return page;
		}
		return null;
	}
	/**
	 * Sets the active page using the unique page identifier and sets its input
	 * to the provided object.
	 * 
	 * @param pageId
	 *            the id of the page to switch to
	 * @param pageInput
	 *            the page input
	 * @return page that was set active or <samp>null</samp> if not found.
	 */
	public IFormPage setActivePage(String pageId, Object pageInput) {
		IFormPage page = setActivePage(pageId);
		if (page != null) {
			IManagedForm mform = page.getManagedForm();
			if (mform != null)
				mform.setInput(pageInput);
		}
		return page;
	}
	/**
	 * Returns active page instance if the currently selected page index is not
	 * -1, or <code>null</code> if it is.
	 * 
	 * @return active page instance if selected, or <code>null</code> if no
	 *         page is currently active.
	 */
	public IFormPage getActivePageInstance() {
		int index = getActivePage();
		if (index != -1)
			return (IFormPage) pages.get(index);
		return null;
	}
	/**
	 * @see MultiPageEditorPart#setActivePage(int)
	 */
	protected void setActivePage(int pageIndex) {
		// fix for window handles problem
		// this should be called only when the editor is first opened
		if (pages.size() > pageIndex
				&& pages.get(pageIndex) instanceof IFormPage) {
			pageChange(pageIndex);
			IFormPage activePage = (IFormPage) pages.get(pageIndex);
			activePage.setActive(true);
			super.setActivePage(pageIndex);
		} else
			super.setActivePage(pageIndex);
		// this is to enable the undo/redo actions before a page change has
		// occurred
		IEditorActionBarContributor contributor = getEditorSite()
				.getActionBarContributor();
		if (contributor != null
				&& contributor instanceof MultiPageEditorActionBarContributor) {
			((MultiPageEditorActionBarContributor) contributor)
					.setActivePage(getEditor(pageIndex));
		}
	}
	private void registerPage(IFormPage page) {
		if (!pages.contains(page))
			pages.add(page);
		if (page.isSource() == false) {
			try {
				page.init(getEditorSite(), getEditorInput());
			} catch (PartInitException e) {
				FormsPlugin.logException(e);
			}
		}
	}
	/**
	 * Closes the editor programmatically.
	 * 
	 * @param save
	 *            if <code>true</code>, the content should be saved before
	 *            closing.
	 */
	public void close(final boolean save) {
		Display display = getSite().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				if (toolkit != null) {
					getSite().getPage().closeEditor(FormEditor.this, save);
				}
			}
		});
	}
}