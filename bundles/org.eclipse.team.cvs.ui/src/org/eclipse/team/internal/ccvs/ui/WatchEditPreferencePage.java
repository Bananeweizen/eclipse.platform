/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;

/**
 * This page contains preferences related to the cvs watch/edit commands
 */
public class WatchEditPreferencePage extends CVSFieldEditorPreferencePage {
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.CVSPreferencePage#getPageHelpContextId()
	 */
	protected String getPageHelpContextId() {
		return IHelpContextIds.WATCH_EDIT_PREFERENCE_PAGE;
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.CVSFieldEditorPreferencePage#getPageDescription()
	 */
	protected String getPageDescription() {
		return Policy.bind("WatchEditPreferencePage.description"); //$NON-NLS-1$;
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(
			ICVSUIConstants.PREF_CHECKOUT_READ_ONLY, 
			Policy.bind("WatchEditPreferencePage.checkoutReadOnly"),  //$NON-NLS-1$
			BooleanFieldEditor.DEFAULT, 
			getFieldEditorParent()));
		addField(new RadioGroupFieldEditor(
			ICVSUIConstants.PREF_PROMPT_ON_EDIT,
			Policy.bind("WatchEditPreferencePage.validateEditSaveAction"), //$NON-NLS-1$
			1,
			new String[][] {{Policy.bind("WatchEditPreferencePage.always"), CVSUIPlugin.ALWAYS_EDIT}, {Policy.bind("WatchEditPreferencePage.prompt"), CVSUIPlugin.PROMPT}}, //$NON-NLS-1$ //$NON-NLS-2$
			getFieldEditorParent(), true));
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (!super.performOk()) return false;
		pushPreferences();
		return true;
	}

	private void pushPreferences() {
		IPreferenceStore source = getCVSPreferenceStore();
		Preferences target = CVSProviderPlugin.getPlugin().getPluginPreferences();
		target.setValue(
			CVSProviderPlugin.READ_ONLY,
			source.getBoolean(ICVSUIConstants.PREF_CHECKOUT_READ_ONLY));
	}
	
}
