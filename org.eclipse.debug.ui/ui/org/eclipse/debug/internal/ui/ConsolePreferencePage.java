package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

/**
 * A page to set the preferences for the console
 */
public class ConsolePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IDebugPreferenceConstants {
	/**
	 * Create the console page.
	 */
	public ConsolePreferencePage() {
		super(GRID);
		setDescription(DebugUIMessages.getString("ConsolePreferencePage.Console_text_color_settings._1")); //$NON-NLS-1$
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(
			parent,
			new Object[] { IDebugHelpContextIds.CONSOLE_PREFERENCE_PAGE });
	}
	
	/**
	 * Create all field editors for this page
	 */
	public void createFieldEditors() {

		// Note: first String value is the key for the preference bundle and second the
		// second String value is the label displayed in front of the editor.
		ColorFieldEditor sysout= new ColorFieldEditor(CONSOLE_SYS_OUT_RGB, DebugUIMessages.getString("ConsolePreferencePage.Standard_Out__2"), getFieldEditorParent()); //$NON-NLS-1$
		ColorFieldEditor syserr= new ColorFieldEditor(CONSOLE_SYS_ERR_RGB, DebugUIMessages.getString("ConsolePreferencePage.Standard_Error__3"), getFieldEditorParent()); //$NON-NLS-1$
		ColorFieldEditor sysin= new ColorFieldEditor(CONSOLE_SYS_IN_RGB, DebugUIMessages.getString("ConsolePreferencePage.Standard_In__4"), getFieldEditorParent()); //$NON-NLS-1$
		
		WorkbenchChainedTextFontFieldEditor editor= new WorkbenchChainedTextFontFieldEditor(CONSOLE_FONT,
				DebugUIMessages.getString("ConsolePreferencePage.Console_font_setting___5"), getFieldEditorParent()); //$NON-NLS-1$
		
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
	 * Returns the a color based on the type.
	 */
	public static Color getPreferenceColor(String type) {
		IPreferenceStore pstore= DebugUIPlugin.getDefault().getPreferenceStore();
		RGB outRGB= PreferenceConverter.getColor(pstore, type);
		ColorManager colorManager= DebugUIPlugin.getDefault().getColorManager();
		return colorManager.getColor(outRGB);
	}
	
	/**
	 * Returns the font data that describes the font to use for the console
	 */
	public static FontData getConsoleFontData() {
		IPreferenceStore pstore= DebugUIPlugin.getDefault().getPreferenceStore();
		FontData fontData= PreferenceConverter.getFontData(pstore, CONSOLE_FONT);
		return fontData;
	}
	
	protected static void initDefaults(IPreferenceStore store) {
		WorkbenchChainedTextFontFieldEditor.startPropagate(store, CONSOLE_FONT);
		
		PreferenceConverter.setDefault(store, CONSOLE_SYS_OUT_RGB, new RGB(0, 0, 255));
		PreferenceConverter.setDefault(store, CONSOLE_SYS_IN_RGB, new RGB(0, 200, 125));
		PreferenceConverter.setDefault(store, CONSOLE_SYS_ERR_RGB, new RGB(255, 0, 0));
	}	
}