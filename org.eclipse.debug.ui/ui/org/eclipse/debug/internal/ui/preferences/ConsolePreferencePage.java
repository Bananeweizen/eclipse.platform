package org.eclipse.debug.internal.ui.preferences;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

/**
 * A page to set the preferences for the console
 */
public class ConsolePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	private BooleanFieldEditor2 fWrapEditor = null;
	private IntegerFieldEditor fWidthEditor = null;
	/**
	 * Create the console page.
	 */
	public ConsolePreferencePage() {
		super(GRID);
		setDescription(DebugPreferencesMessages.getString("ConsolePreferencePage.Console_settings")); //$NON-NLS-1$
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(
			parent,
			IDebugHelpContextIds.CONSOLE_PREFERENCE_PAGE );
	}
	
	/**
	 * Create all field editors for this page
	 */
	public void createFieldEditors() {
		
		fWrapEditor = new BooleanFieldEditor2(IDebugPreferenceConstants.CONSOLE_WRAP, DebugPreferencesMessages.getString("ConsolePreferencePage.Wrap_text_1"), SWT.NONE, getFieldEditorParent()); //$NON-NLS-1$
		addField(fWrapEditor);
		
		fWidthEditor = new IntegerFieldEditor(IDebugPreferenceConstants.CONSOLE_WIDTH, DebugPreferencesMessages.getString("ConsolePreferencePage.Console_width"), getFieldEditorParent()); //$NON-NLS-1$
		addField(fWidthEditor);
		fWidthEditor.setValidRange(1, Integer.MAX_VALUE);
		
		fWrapEditor.getChangeControl(getFieldEditorParent()).addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateWidthEditor();
				}
			}
		);
		
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, DebugPreferencesMessages.getString("ConsolePreferencePage.Show_&Console_View_when_there_is_program_output_3"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, DebugPreferencesMessages.getString("ConsolePreferencePage.Show_&Console_View_when_there_is_program_error_3"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$

		// Note: first String value is the key for the preference bundle and second the
		// second String value is the label displayed in front of the editor.
		ColorFieldEditor sysout= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_RGB, DebugPreferencesMessages.getString("ConsolePreferencePage.Standard_Out__2"), getFieldEditorParent()); //$NON-NLS-1$
		ColorFieldEditor syserr= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_RGB, DebugPreferencesMessages.getString("ConsolePreferencePage.Standard_Error__3"), getFieldEditorParent()); //$NON-NLS-1$
		ColorFieldEditor sysin= new ColorFieldEditor(IDebugPreferenceConstants.CONSOLE_SYS_IN_RGB, DebugPreferencesMessages.getString("ConsolePreferencePage.Standard_In__4"), getFieldEditorParent()); //$NON-NLS-1$
		
		WorkbenchChainedTextFontFieldEditor editor= new WorkbenchChainedTextFontFieldEditor(IDebugPreferenceConstants.CONSOLE_FONT,
				DebugPreferencesMessages.getString("ConsolePreferencePage.Console_font_setting___5"), getFieldEditorParent()); //$NON-NLS-1$
		
		addField(sysout);
		addField(syserr);
		addField(sysin);
		addField(editor);
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
		
	/**
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean ok= super.performOk();
		DebugUIPlugin.getDefault().savePluginPreferences();
		return ok;
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
	 */
	protected void initialize() {
		super.initialize();
		updateWidthEditor();
	}
	
	/**
	 * Update enablement of width editor based on enablement of 'fixed width' editor.
	 */
	protected void updateWidthEditor() {
		Button b = fWrapEditor.getChangeControl(getFieldEditorParent());
		fWidthEditor.getTextControl(getFieldEditorParent()).setEnabled(b.getSelection());
		fWidthEditor.getLabelControl(getFieldEditorParent()).setEnabled(b.getSelection());				
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
		updateWidthEditor();
	}
}