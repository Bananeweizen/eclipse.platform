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
package org.eclipse.ui.forms;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;

/**
 * Form wizard page is a page that hosts a scrollable form. Subclasses
 * are supposed to implement 'fillFormBody' that 
 */
public abstract class FormWizardPage extends WizardPage {
	protected FormToolkit toolkit;
	protected ManagedForm managedForm;
	
	public FormWizardPage(String id, FormToolkit toolkit) {
		super(id);
		this.toolkit = toolkit;
	}

/**
 * Creates the form wizard page control. This method is final. Clients 
 * are expected to implement <code>createFormContents(Composite)</code> instead.
 */
	public final void createControl(Composite parent) {
		Form form = toolkit.createForm(parent);
		form.setExpandHorizontal(true);
		form.setExpandVertical(true);
		managedForm = new ManagedForm(toolkit, form);
		createFormContents(form.getBody());
		setControl(form);
	}
	
	public void dispose() {
		managedForm.dispose();
		super.dispose();
	}
	
	protected abstract void createFormContents(Composite form);
}
