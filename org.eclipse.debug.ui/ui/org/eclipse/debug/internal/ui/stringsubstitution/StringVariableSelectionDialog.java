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
package org.eclipse.debug.internal.ui.stringsubstitution;

import org.eclipse.core.variables.IStringVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * A dialog that prompts the user to choose and configure a string
 * substitution variable.
 * 
 * @since 3,0 
 */
public class StringVariableSelectionDialog extends ElementListSelectionDialog {
	
	// button to configure variable's argument
	private Button fArgumentButton;
	// variable description
	private Text fDescriptionText;
	// the argument value
	private Text fArgumentText;
	private String fArgumentValue;

	/**
	 * Constructs a new string substitution variable selection dialog.
	 *  
	 * @param parent parent shell
	 */
	public StringVariableSelectionDialog(Shell parent) {
		super(parent, new StringVariableLabelProvider());
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setTitle(StringSubstitutionMessages.getString("StringVariableSelectionDialog.2")); //$NON-NLS-1$
		setMessage(StringSubstitutionMessages.getString("StringVariableSelectionDialog.3")); //$NON-NLS-1$
		setMultipleSelection(false);
		setElements(VariablesPlugin.getDefault().getStringVariableManager().getVariables());
	}
	
	/**
	 * Returns the variable expression the user generated from this
	 * dialog, or <code>null</code> if none.
	 *  
	 * @return variable expression the user generated from this
	 * dialog, or <code>null</code> if none
	 */
	public String getVariableExpression() {
		Object[] selected = getResult();
		if (selected != null && selected.length == 1) {
			IStringVariable variable = (IStringVariable)selected[0];
			StringBuffer buffer = new StringBuffer();
			buffer.append("${"); //$NON-NLS-1$
			buffer.append(variable.getName());
			if (fArgumentValue != null && fArgumentValue.length() > 0) {
				buffer.append(":"); //$NON-NLS-1$
				buffer.append(fArgumentValue);
			}
			buffer.append("}"); //$NON-NLS-1$
			return buffer.toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		createArgumentArea((Composite)control);
		return control;
	}

	/**
	 * Creates an area to display a description of the selected variable
	 * and a button to configure the variable's argument.
	 * 
	 * @param parent parnet widget
	 */
	private void createArgumentArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		container.setFont(parent.getFont());
				
		Label desc = new Label(container, SWT.NONE);
		desc.setFont(parent.getFont());
		desc.setText(StringSubstitutionMessages.getString("StringVariableSelectionDialog.6")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		desc.setLayoutData(gd);		
		
		Composite args = new Composite(container, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		args.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		args.setLayoutData(gd);
		args.setFont(container.getFont());
		
		fArgumentText = new Text(args, SWT.BORDER);
		fArgumentText.setFont(container.getFont());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fArgumentText.setLayoutData(gd);		
		
		fArgumentButton = new Button(args, SWT.PUSH);
		fArgumentButton.setFont(parent.getFont());
		fArgumentButton.setText(StringSubstitutionMessages.getString("StringVariableSelectionDialog.7")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.widthHint = SWTUtil.getButtonWidthHint(fArgumentButton);
		fArgumentButton.setLayoutData(gd);
		fArgumentButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				configureArgument();
			}
		});

		
		desc = new Label(container, SWT.NONE);
		desc.setFont(parent.getFont());
		desc.setText(StringSubstitutionMessages.getString("StringVariableSelectionDialog.8")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		desc.setLayoutData(gd);
		
		fDescriptionText = new Text(container, SWT.BORDER | SWT.WRAP);
		fDescriptionText.setFont(container.getFont());
		fDescriptionText.setEditable(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.heightHint = 50;
		fDescriptionText.setLayoutData(gd);		
		
	}

	/**
	 * Configures the argument for the selected variable.
	 */
	protected void configureArgument() {
		Object[] objects = getSelectedElements();
		IStringVariable variable = (IStringVariable)objects[0];
		IArgumentSelector selector = StringVariablePresentationManager.getDefault().getArgumentSelector(variable);
		String value = selector.selectArgument(variable, getShell());
		if (value != null) {
			fArgumentText.setText(value);
		}
	}

	/**
	 * Update variable description and argument button enablement.
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractElementListSelectionDialog#handleSelectionChanged()
	 */
	protected void handleSelectionChanged() {
		super.handleSelectionChanged();
		Object[] objects = getSelectedElements();
		boolean enabled = false;
		String text = null;
		if (objects.length == 1) {
			 IStringVariable variable = (IStringVariable)objects[0];
			 IArgumentSelector selector = StringVariablePresentationManager.getDefault().getArgumentSelector(variable);
			 enabled = selector != null;
			 text = variable.getDescription();
		}
		if (text == null) {
			text = ""; //$NON-NLS-1$
		}
		fArgumentButton.setEnabled(enabled);
		fDescriptionText.setText(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		fArgumentValue = fArgumentText.getText().trim();
		super.okPressed();
	}

}
