/*******************************************************************************
 * Copyright (c) 2000, 2003 Matt Conway and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Matt Conway - initial implementation
 *     IBM Corporation - integration and code cleanup
 *******************************************************************************/
package org.eclipse.debug.internal.ui.stringsubstitution;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.core.stringsubstitution.IContextVariable;
import org.eclipse.debug.internal.core.stringsubstitution.IContextVariableResolver;
import org.eclipse.debug.internal.ui.DebugUIPlugin;

/**
 * Base implementation for variable resolvers that prompt the user
 * for their value.
 */
abstract class PromptingResolver implements IContextVariableResolver {

	/**
	 * A hint that helps the user choose their input. If a prompt
	 * hint is provider the user will be prompted:
	 * 	Please input a value for <code>promptHint</code>
	 */
	protected String promptHint = null;
	/**
	 * The prompt displayed to the user.
	 */
	protected String dialogMessage = null;
	/**
	 * The default value selected when the prompt is displayed
	 */
	protected String defaultValue = null;
	/**
	 * The last value chosen by the user for this variable 
	 */
	protected String lastValue = null;
	/**
	 * The result returned from the prompt dialog
	 */
	protected String dialogResultString = null;
	
	/**
	 * Presents the user with the appropriate prompt for the variable to be expanded
	 * and sets the <code>dialogResultString</code> based on the user's selection.
	 */
	public abstract void prompt();

	/**
	 * Initializes values displayed when the user is prompted. If
	 * a prompt hint and default value are supplied in the given
	 * variable value, these are extracted for presentation
	 * 
	 * @param varValue the value of the variable from which the prompt
	 * hint and default value will be extracted
	 */
	protected void setupDialog(String varValue) {
		promptHint = null;
		defaultValue = null;
		dialogResultString = null;
		if (varValue != null) {
			int idx = varValue.indexOf(':');
			if (idx != -1) {
				promptHint = varValue.substring(0, idx);
				defaultValue = varValue.substring(idx + 1);
			} else {
				promptHint = varValue;
			}
		}

		if (promptHint != null) {
			dialogMessage = MessageFormat.format(StringSubstitutionMessages.getString("PromptExpanderBase.0"), new String[] {promptHint}); //$NON-NLS-1$
		} else {
			dialogMessage = StringSubstitutionMessages.getString("PromptExpanderBase.1"); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.stringsubstitution.IContextVariableResolver#resolveValue(org.eclipse.debug.internal.core.stringsubstitution.IContextVariable, java.lang.String)
	 */
	public String resolveValue(IContextVariable variable, String argument) throws CoreException {
		String value = null;
		setupDialog(argument);

		DebugUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				prompt();
			}
		});
		if (dialogResultString != null) {
			value = dialogResultString;
			lastValue = dialogResultString;
		}
		return value;
	}

}
