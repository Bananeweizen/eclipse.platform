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
package org.eclipse.welcome.internal.webbrowser;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.welcome.internal.*;

public class WebBrowserEditor extends EditorPart implements IEmbeddedWebBrowser {
	// NL
	private static final String KEY_NOT_AVAILABLE =
		"WebBrowserView.notAvailable";
	private static final String KEY_ADDRESS = "WebBrowserView.address";
	private static final String KEY_STOP = "WebBrowserView.stop";
	private static final String KEY_GO = "WebBrowserView.go";
	private static final String KEY_REFRESH = "WebBrowserView.refresh";
	private static final String KEY_BACKWARD = "WebBrowserView.backward";
	private static final String KEY_FORWARD = "WebBrowserView.forward";

	private int ADDRESS_SIZE = 10;
	private WebBrowser browser;
	private Control control;
	private Combo addressCombo;
	private Object input;
	private ToolBarManager toolBarManager;
	private Action refreshAction;
	private Action stopAction;
	private Action goAction;
	private GlobalActionHandler globalActionHandler;
	private IWebBrowserListener listener;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#gotoMarker(org.eclipse.core.resources.IMarker)
	 */
	public void gotoMarker(IMarker marker) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		if (!(input instanceof WebBrowserEditorInput))
			throw new PartInitException("Invalid Input: Must be WelcomeEditorInput");
		setSite(site);
		setInput(input);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	public WebBrowserEditor() {
	}

	/**
	 * @see IFormPage#createControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		control = container;
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		Composite navContainer = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 1;
		navContainer.setLayout(layout);
		createNavBar(navContainer);
		navContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		final WebBrowser winBrowser = new WebBrowser(container);
		browser = winBrowser;

		Control c = browser.getControl();
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		final BrowserControlSite site = winBrowser.getControlSite();
		IStatusLineManager smng =
			getEditorSite().getActionBars().getStatusLineManager();
		site.setStatusLineManager(smng);

		site.addEventListener(WebBrowser.DownloadComplete, new OleListener() {
			public void handleEvent(OleEvent event) {
				String url = winBrowser.getLocationURL();
				if (url != null) {
					addressCombo.setText(url);
					downloadComplete(url);
				}
			}
		});
		site.addEventListener(WebBrowser.DownloadBegin, new OleListener() {
			public void handleEvent(OleEvent event) {
				stopAction.setEnabled(true);
				refreshAction.setEnabled(false);
			}
		});
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.WebBrowserView");
		WebBrowserEditorInput input = (WebBrowserEditorInput)getEditorInput();
		openTo(input.getURL());
	}

	public void openTo(final String url) {
		addressCombo.setText(url);
		control.getDisplay().asyncExec(new Runnable() {
			public void run() {
				navigate(url);
			}
		});
	}

	private void downloadComplete(String url) {
		stopAction.setEnabled(false);
		refreshAction.setEnabled(true);
		if (listener!=null)
			listener.stateChanged();
	}
	
	public void setListener(IWebBrowserListener listener) {
		this.listener = listener;
	}

	private void createNavBar(Composite parent) {
		Label addressLabel = new Label(parent, SWT.NONE);
		addressLabel.setText(WelcomePortal.getString(KEY_ADDRESS));

		addressCombo = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER);
		addressCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String text = addressCombo.getText();
				goAction.setEnabled(text.length() > 0);
			}
		});
		addressCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String text =
					addressCombo.getItem(addressCombo.getSelectionIndex());
				if (text.length() > 0)
					navigate(text);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				navigate(addressCombo.getText());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		addressCombo.setLayoutData(gd);
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
		toolBarManager = new ToolBarManager(toolbar);
		makeActions();
		IActionBars bars = getEditorSite().getActionBars();
		globalActionHandler = new GlobalActionHandler(bars, addressCombo);
	}

	private void navigate(String url) {
		browser.navigate(url);
		String[] items = addressCombo.getItems();
		int loc = -1;
		String normURL = getNormalizedURL(url);
		for (int i = 0; i < items.length; i++) {
			String normItem = getNormalizedURL(items[i]);
			if (normURL.equals(normItem)) {
				// match 
				loc = i;
				break;
			}
		}
		if (loc != -1) {
			addressCombo.remove(loc);
		}
		addressCombo.add(url, 0);
		if (addressCombo.getItemCount() > ADDRESS_SIZE) {
			addressCombo.remove(addressCombo.getItemCount() - 1);
		}
		addressCombo.getParent().layout(true);
	}
	
	void back() {
		browser.back();
	}
	void forward() {
		browser.forward();
	}
	boolean isBackwardEnabled() {
		return browser.isBackwardEnabled();
	}
	boolean isForwardEnabled() {
		return browser.isForwardEnabled();
	}

	private void makeActions() {
		goAction = new Action() {
			public void run() {
				navigate(addressCombo.getText());
			}
		};
		goAction.setEnabled(false);
		goAction.setToolTipText(WelcomePortal.getString(KEY_GO));
		goAction.setImageDescriptor(WelcomePortalImages.DESC_GO_NAV);
		goAction.setDisabledImageDescriptor(WelcomePortalImages.DESC_GO_NAV_D);
		goAction.setHoverImageDescriptor(WelcomePortalImages.DESC_GO_NAV_H);

		stopAction = new Action() {
			public void run() {
				browser.stop();
			}
		};
		stopAction.setToolTipText(WelcomePortal.getString(KEY_STOP));
		stopAction.setImageDescriptor(WelcomePortalImages.DESC_STOP_NAV);
		stopAction.setDisabledImageDescriptor(
			WelcomePortalImages.DESC_STOP_NAV_D);
		stopAction.setHoverImageDescriptor(
			WelcomePortalImages.DESC_STOP_NAV_H);
		stopAction.setEnabled(false);

		refreshAction = new Action() {
			public void run() {
				browser.refresh();
			}
		};
		refreshAction.setToolTipText(
			WelcomePortal.getString(KEY_REFRESH));
		refreshAction.setImageDescriptor(WelcomePortalImages.DESC_REFRESH_NAV);
		refreshAction.setDisabledImageDescriptor(
			WelcomePortalImages.DESC_REFRESH_NAV_D);
		refreshAction.setHoverImageDescriptor(
			WelcomePortalImages.DESC_REFRESH_NAV_H);
		refreshAction.setEnabled(false);

		toolBarManager.add(goAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(stopAction);
		toolBarManager.add(refreshAction);
		toolBarManager.update(true);
	}

	private String getNormalizedURL(String url) {
		url = url.toLowerCase();
		if (url.indexOf("://") == -1) {
			url = "http://" + url;
		}
		return url;
	}

	public void dispose() {
		if (browser != null)
			browser.dispose();
		globalActionHandler.dispose();
		super.dispose();
	}

	public void setFocus() {
		if (control != null)
			control.setFocus();
	}
}
