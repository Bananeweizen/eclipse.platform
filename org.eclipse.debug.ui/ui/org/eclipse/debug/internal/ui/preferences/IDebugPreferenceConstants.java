package org.eclipse.debug.internal.ui.preferences;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.debug.ui.IDebugUIConstants;
 
/**
 * Constants defining the keys to be used for accessing preferences
 * inside the debug ui plugin's preference bundle.
 *
 * In descriptions (of the keys) below describe the preference 
 * stored at the given key. The type indicates type of the stored preferences
 *
 * The preference store is loaded by the plugin (DebugUIPlugin).
 * @see DebugUIPlugin.initializeDefaultPreferences(IPreferenceStore) - for initialization of the store
 */
public interface IDebugPreferenceConstants {

	/**
	 * RGB colors for displaying the content in the Console
	 */
	public static final String CONSOLE_SYS_ERR_RGB= "Console.stdErrColor"; //$NON-NLS-1$
	public static final String CONSOLE_SYS_OUT_RGB= "Console.stdOutColor"; //$NON-NLS-1$
	public static final String CONSOLE_SYS_IN_RGB= "Console.stdInColor"; //$NON-NLS-1$
	
	/**
	 * (boolean) Whether or not the text in the console will wrap
	 */
	public static final String CONSOLE_WRAP= "Console.wrap"; //$NON-NLS-1$
	
	/**
	 * (int) The maximum console character width, if wrapping. 
	 */ 
	public static final String CONSOLE_WIDTH = "Console.width"; //$NON-NLS-1$
	
	/**
	 * (boolean) Whether or not the console view is shown 
	 * when there is program output.
  	 */
	public static final String CONSOLE_OPEN_ON_OUT= "DEBUG.consoleOpenOnOut"; //$NON-NLS-1$
	/**
	 * (boolean) Whether or not the console view is shown 
	 * when there is program error.
  	 */
	public static final String CONSOLE_OPEN_ON_ERR= "DEBUG.consoleOpenOnErr"; //$NON-NLS-1$
	
	/**
	 * Console buffer high and low water marks
	 */
	public static final String CONSOLE_LIMIT_CONSOLE_OUTPUT = "Console.limitConsoleOutput"; //$NON-NLS-1$
	public static final String CONSOLE_LOW_WATER_MARK = "Console.lowWaterMark"; //$NON-NLS-1$ 
	public static final String CONSOLE_HIGH_WATER_MARK = "Console.highWaterMark"; //$NON-NLS-1$
	
	/**
	 * The orientation of the detail view in the VariablesView
	 */
	public static final String VARIABLES_DETAIL_PANE_ORIENTATION = "Variables.detail.orientation"; //$NON-NLS-1$
	public static final String VARIABLES_DETAIL_PANE_RIGHT = "Variables.detail.orientation.right"; //$NON-NLS-1$
	public static final String VARIABLES_DETAIL_PANE_UNDERNEATH = "Variables.detail.orientation.underneath"; //$NON-NLS-1$
	
	/**
	 * The RGB for the color to be used to indicate changed variables
	 */
	public static final String CHANGED_VARIABLE_RGB= "Changed.Variable.RGB"; //$NON-NLS-1$
	
	/**
	 * Memento for the last selected launch config in the
	 * launch config dialog.
	 */
	public static final String PREF_LAST_LAUNCH_CONFIGURATION_SELECTION = IDebugUIConstants.PLUGIN_ID + ".lastLaunchConfigSelection"; //$NON-NLS-1$

	/**
	 * Serialized list of the enabled debug action groups
	 */	
	public static final String PREF_ENABLED_DEBUG_ACTION_GROUPS = IDebugUIConstants.PLUGIN_ID + "enabledDebugActionGroups"; //$NON-NLS-1$
	
	/**
	 * Serialized list of the disabled debug action groups
	 */	
	public static final String PREF_DISABLED_DEBUG_ACTION_GROUPS = IDebugUIConstants.PLUGIN_ID + "disabledDebugActionGroups"; //$NON-NLS-1$
		
	/**
	 * The maximum size of the launch history list
	 */
	public static int MAX_LAUNCH_HISTORY_SIZE= 20;
	
	/**
	 * Common dialog settings
	 */
	public static final String DIALOG_ORIGIN_X = IDebugUIConstants.PLUGIN_ID + ".DIALOG_ORIGIN_X"; //$NON-NLS-1$
	public static final String DIALOG_ORIGIN_Y = IDebugUIConstants.PLUGIN_ID + ".DIALOG_ORIGIN_Y"; //$NON-NLS-1$
	public static final String DIALOG_WIDTH = IDebugUIConstants.PLUGIN_ID + ".DIALOG_WIDTH"; //$NON-NLS-1$
	public static final String DIALOG_HEIGHT = IDebugUIConstants.PLUGIN_ID + ".DIALOG_HEIGHT"; //$NON-NLS-1$
	public static final String DIALOG_SASH_WEIGHTS_1 = IDebugUIConstants.PLUGIN_ID + ".DIALOG_SASH_WEIGHTS_1"; //$NON-NLS-1$
	public static final String DIALOG_SASH_WEIGHTS_2 = IDebugUIConstants.PLUGIN_ID + ".DIALOG_SASH_WEIGHTS_2"; //$NON-NLS-1$
	
}


