package org.eclipse.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
/**
 * Constant definitions for debug UI plug-in.
 * <p>
 * Popup menus in the debug UI support action contribution via the
 * <code>org.eclipse.ui.popupMenus</code>  extension. Actions may be
 * contributed to any group on the menu. To facilitate insertion of actions
 * inbetween existing groups, empty groups have been defined
 * in the menu. Each group prefixed by "empty" indicates an empty group.
 * </p>
 * <h3>Debug View Popup Menu</h3>
 * <ul>
 *   <li>Empty edit group</li>
 *   <li>Edit group</li>
 *   <li>Copy Stack action</li>
 *   <li>Empty Step group</li>
 *   <li>Step group</li>
 *   <li>Step Into action</li>
 *   <li>Step Over action</li>
 *   <li>Run to return action</li>
 *   <li>Empty thread group</li>
 *   <li>Thread group</li>
 *   <li>Suspend action</li>
 *   <li>Resume action</li>
 *   <li>Terminate action</li>
 *   <li>Empty launch group</li>
 *   <li>Launch group</li>
 *   <li>Terminate and Remove action</li>
 *   <li>Terminate All action</li>
 *   <li>Remove All Terminated action</li>
 *   <li>Relaunch action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 *   <li>Show qualified names action</li>
 *   <li>Property group</li>
 *   <li>Property dialog action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Process View Popup Menu</h3>
 * <ul>
 *   <li>Empty launch group</li>
 *   <li>Launch group</li>
 *   <li>Relaunch action</li>
 *   <li>Terminate action</li>
 *   <li>Terminate and Remove action</li>
 *   <li>Terminate All action</li>
 *   <li>Remove All Terminated action</li>
 *   <li>Property group</li>
 *   <li>Property dialog action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Variable View Popup Menu</h3>
 * <ul>
 *   <li>Empty variable group</li>
 *   <li>Variable group</li>
 *   <li>Add to Watch List action</li>
 *   <li>Change value action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 *   <li>Show qualified names action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Breakpoint View Popup Menu</h3>
 * <ul>
 *   <li>Empty Navigation group</li>
 *   <li>Navigation group</li>
 *   <li>Open action</li>
 *   <li>Empty Breakpoint goup</li>
 *   <li>Breakpoint group</li>
 *   <li>Enable/Disable action</li> 
 *   <li>Remove action</li>
 *   <li>Remove all action</li>
 *   <li>Empty render group</li>
 *   <li>Render group</li>
 *   <li>Show qualified names action</li>
 * 	 <li>Show breakpoints for model action</li>
 *   <li>Additions group</li>
 * </ul>
 * <h3>Expression View Popup Menu</h3>
 * <ul>
 *   <li>Empty Expression group</li>
 *   <li>Expression group</li>
 *	 <li>Change variable value action</li>
 * 	 <li>Copy to clipboard action</li>	 
 *   <li>Remove action</li>
 *   <li>Remove all action</li>
 *   <li>Empty Render group</li>
 *   <li>Render group</li>
 * 	 <li>Show type names action</li>
 *   <li>Show qualified names action</li>
 *   <li>Additions group</li>
 * </ul>
 * <p>
 * Constants only; not intended to be implemented or extended.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */

public interface IDebugUIConstants {
	
	/**
	 * Debug UI plug-in identifier (value <code>"org.eclipse.debug.ui"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.debug.ui"; //$NON-NLS-1$
	
	/**
	 * Debug perspective identifier (value <code>"org.eclipse.debug.ui.DebugPerspective"</code>).
	 */
	public static final String ID_DEBUG_PERSPECTIVE = PLUGIN_ID + ".DebugPerspective"; //$NON-NLS-1$
	
	/**
	 * Debug model presentation extension point identifier (value <code>"debugModelPresentations"</code>).
	 */
	public static final String ID_DEBUG_MODEL_PRESENTATION= "debugModelPresentations"; //$NON-NLS-1$
	
	/**
	 * Launch configuration tab extension point identifier (value <code>"launchConfigurationTabs"</code>).
	 */
	public static final String ID_LAUNCH_CONFIGURATION_TABS= "launchConfigurationTabs"; //$NON-NLS-1$	
	
	// Preferences
	/**
	 * Boolean preference controlling automatic change to debug perspective when
	 * a debug session is launched, or when a debug session suspends
	 * (value <code>"org.eclipse.debug.ui.auto_show_debug_view"</code>). When this
	 * preference is <code>true</code> and a debug session is launched or suspends,
	 * and a launch view is not present in the current perspective, a debug perspective
	 * of the appropriate kind is created (or switched to if already created).
	 */
	public static final String PREF_AUTO_SHOW_DEBUG_VIEW= PLUGIN_ID + ".auto_show_debug_view"; //$NON-NLS-1$
	
	/**
	 * Boolean preference controlling automatic change to debug perspective when
	 * a program is launched in run mode (value <code>"org.eclipse.debug.ui.auto_show_process_view"</code>).
	 * When this preference is <code>true</code>
	 * and a program is launched, and a launch view is not present in the current
	 * perspective, a debug perspective of the appropriate kind is created (or switched
	 * to if already created).
	 */
	public static final String PREF_AUTO_SHOW_PROCESS_VIEW= PLUGIN_ID + ".auto_show_process_view";	 //$NON-NLS-1$

	/**
	 * Boolean preference controlling automatic build before
	 * launching a program.
	 */
	public static final String PREF_AUTO_BUILD_BEFORE_LAUNCH= PLUGIN_ID + ".auto_build_before_launch"; //$NON-NLS-1$
	/**
	 * Boolean preference controlling automatic removal of terminated launches
	 * when a new launch is registered.
	 * @since 2.0
	 */
	public static final String PREF_AUTO_REMOVE_OLD_LAUNCHES= PLUGIN_ID + ".auto_remove_old_launches"; //$NON-NLS-1$
		
	// Debug views
	
	/**
	 * Debug view identifier (value <code>"org.eclipse.debug.ui.DebugView"</code>).
	 */
	public static final String ID_DEBUG_VIEW= "org.eclipse.debug.ui.DebugView"; //$NON-NLS-1$

	/**
	 * Process view identifier (value <code>"org.eclipse.debug.ui.ProcessView"</code>).
	 * 
	 * @deprecated this view no longer exists
	 */
	public static final String ID_PROCESS_VIEW= "org.eclipse.debug.ui.ProcessView"; //$NON-NLS-1$
	
	/**
	 * Breakpoint view identifier (value <code>"org.eclipse.debug.ui.BreakpointView"</code>).
	 */
	public static final String ID_BREAKPOINT_VIEW= "org.eclipse.debug.ui.BreakpointView"; //$NON-NLS-1$
	
	/**
	 * Variable view identifier (value <code>"org.eclipse.debug.ui.VariableView"</code>).
	 */
	public static final String ID_VARIABLE_VIEW= "org.eclipse.debug.ui.VariableView"; //$NON-NLS-1$
	
	/**
	 * Expression view identifier (value <code>"org.eclipse.debug.ui.ExpressionView"</code>).
	 * @since 2.0
	 */
	public static final String ID_EXPRESSION_VIEW= "org.eclipse.debug.ui.ExpressionView"; //$NON-NLS-1$
		
	/**
	 * Console view identifier (value <code>"org.eclipse.debug.ui.ConsoleView"</code>).
	 */
	public static final String ID_CONSOLE_VIEW= "org.eclipse.debug.ui.ConsoleView"; //$NON-NLS-1$

	// Extension points
	
	/**
	 * Extension point for launch configuration type images.
	 */
	public static final String EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPE_IMAGES = "launchConfigurationTypeImages";
	
	// Debug Action images
	
	/**
	 * Debug action image identifier.
	 */
	public static final String IMG_ACT_DEBUG= "IMG_ACT_DEBUG"; //$NON-NLS-1$

	/**
	 * Run action image identifier.
	 */
	public static final String IMG_ACT_RUN= "IMG_ACT_RUN"; //$NON-NLS-1$
		
	/** Resume action image identifier. */
	public static final String IMG_LCL_RESUME= "IMG_LCL_RESUME"; //$NON-NLS-1$
	
	/** Suspend action image identifier. */
	public static final String IMG_LCL_SUSPEND= "IMG_LCL_SUSPEND"; //$NON-NLS-1$
	
	/** Terminate action image identifier. */
	public static final String IMG_LCL_TERMINATE= "IMG_LCL_TERMINATE"; //$NON-NLS-1$
	
	/** Terminate all action image identifier. */
	public static final String IMG_LCL_TERMINATE_ALL= "IMG_LCL_TERMINATE_ALL"; //$NON-NLS-1$
	
	/** Terminate and remove action image identifier. */
	public static final String IMG_LCL_TERMINATE_AND_REMOVE= "IMG_LCL_TERMINATE_AND_REMOVE"; //$NON-NLS-1$
	
	/** Disconnect action image identifier. */
	public static final String IMG_LCL_DISCONNECT= "IMG_LCL_DISCONNECT"; //$NON-NLS-1$
	
	/** Step into action image identifier. */
	public static final String IMG_LCL_STEPINTO= "IMG_LCL_STEPINTO"; //$NON-NLS-1$
	
	/** Step over action image identifier. */
	public static final String IMG_LCL_STEPOVER= "IMG_LCL_STEPOVER"; //$NON-NLS-1$
	
	/** Step return action image identifier. */
	public static final String IMG_LCL_STEPRETURN= "IMG_LCL_STEPRETURN"; //$NON-NLS-1$
	
	/** Clear action image identifier. */
	public static final String IMG_LCL_CLEAR= "IMG_LCL_CLEAR"; //$NON-NLS-1$
	
	/** Remove all terminated action image identifier. */
	public static final String IMG_LCL_REMOVE_TERMINATED= "IMG_LCL_REMOVE_TERMINATED"; //$NON-NLS-1$
	
	/** Display variable type names action image identifier. */
	public static final String IMG_LCL_TYPE_NAMES= "IMG_LCL_TYPE_NAMES"; //$NON-NLS-1$
	
	/** Remove action image identifier. */
	public static final String IMG_LCL_REMOVE= "IMG_LCL_REMOVE"; //$NON-NLS-1$
	
	/** Remove all action image identifier. */
	public static final String IMG_LCL_REMOVE_ALL= "IMG_LCL_REMOVE_ALL"; //$NON-NLS-1$

	/** Inspect action image identifier. */
	public static final String IMG_LCL_INSPECT= "IMG_LCL_INSPECT"; //$NON-NLS-1$
	
	/** Re-launch action image identifier.*/
	public static final String IMG_LCL_RELAUNCH= "IMG_LCL_RELAUNCH"; //$NON-NLS-1$

	/** Copy-to-clipboard action image identifier.*/
	public static final String IMG_LCL_COPY= "IMG_LCL_COPY"; //$NON-NLS-1$

	
	// Debug element images
	
	/** Debug mode launch image identifier. */
	public static final String IMG_OBJS_LAUNCH_DEBUG= "IMG_OBJS_LAUNCH_DEBUG"; //$NON-NLS-1$
	
	/** Run mode launch image identifier. */
	public static final String IMG_OBJS_LAUNCH_RUN= "IMG_OBJS_LAUNCH_RUN"; //$NON-NLS-1$
	
	/** Running debug target image identifier. */
	public static final String IMG_OBJS_DEBUG_TARGET= "IMG_OBJS_DEBUG_TARGET"; //$NON-NLS-1$
	
	/** Terminated debug target image identifier. */
	public static final String IMG_OBJS_DEBUG_TARGET_TERMINATED= "IMG_OBJS_DEBUG_TARGET_TERMINATED"; //$NON-NLS-1$
	
	/** Running thread image identifier. */
	public static final String IMG_OBJS_THREAD_RUNNING= "IMG_OBJS_THREAD_RUNNING"; //$NON-NLS-1$
	
	/** Suspended thread image identifier. */
	public static final String IMG_OBJS_THREAD_SUSPENDED= "IMG_OBJS_THREAD_SUSPENDED"; //$NON-NLS-1$
	
	/** Terminated thread image identifier. */
	public static final String IMG_OBJS_THREAD_TERMINATED= "IMG_OBJS_THREAD_TERMINATED"; //$NON-NLS-1$
	
	/** Stack frame (suspended) image identifier. */
	public static final String IMG_OBJS_STACKFRAME= "IMG_OBJS_STACKFRAME"; //$NON-NLS-1$
	
	/** Stack frame (running) image identifier. */
	public static final String IMG_OBJS_STACKFRAME_RUNNING= "IMG_OBJS_STACKFRAME_RUNNING"; //$NON-NLS-1$
	
	/** Enabled breakpoint image identifier. */
	public static final String IMG_OBJS_BREAKPOINT= "IMG_OBJS_BREAKPOINT"; //$NON-NLS-1$
	
	/** Disabled breakpoint image identifier. */
	public static final String IMG_OBJS_BREAKPOINT_DISABLED= "IMG_OBJS_BREAKPOINT_DISABLED"; //$NON-NLS-1$
		
	/** Running system process image identifier. */
	public static final String IMG_OBJS_OS_PROCESS= "IMG_OBJS_OS_PROCESS"; //$NON-NLS-1$
	
	/** Terminated system process image identifier. */
	public static final String IMG_OBJS_OS_PROCESS_TERMINATED= "IMG_OBJS_OS_PROCESS_TERMINATED"; //$NON-NLS-1$

	/** Expression image identifier. */
	public static final String IMG_OBJS_EXPRESSION= "IMG_OBJS_EXPRESSION"; //$NON-NLS-1$
	
	// wizard banners
	/** Debug wizard banner image identifier. */
	public static final String IMG_WIZBAN_DEBUG= "IMG_WIZBAN_DEBUG"; //$NON-NLS-1$
	
	/** Run wizard banner image identifier. */
	public static final String IMG_WIZBAN_RUN= "IMG_WIZBAN_RUN"; //$NON-NLS-1$
	
	/** OK wizard banner image identifier. */
	public static final String IMG_WIZBAN_OK= "IMG_WIZBAN_OK"; //$NON-NLS-1$
	
	/** FAIL wizard banner image identifier. */
	public static final String IMG_WIZBAN_FAIL= "IMG_WIZBAN_FAIL"; //$NON-NLS-1$
	
	/**
	 * Debug action set identifier (value <code>"org.eclipse.debug.ui.debugActionSet"</code>).
	 */
	public static final String DEBUG_ACTION_SET= PLUGIN_ID + ".debugActionSet"; //$NON-NLS-1$
	
	/**
	 * Launch action set identifier (value <code>"org.eclipse.debug.ui.LaunchActionSet"</code>).
	 */
	public static final String LAUNCH_ACTION_SET= PLUGIN_ID + ".launchActionSet"; //$NON-NLS-1$
	
	// menus 
	
	/** 
	 * Identifier for an empty group preceeding an
	 * edit group in a menu (value <code>"emptyEditGroup"</code>).
	 */
	public static final String EMPTY_EDIT_GROUP = "emptyEditGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for an edit group in a menu (value <code>"editGroup"</code>).
	 */
	public static final String EDIT_GROUP = "editGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * step group in a menu (value <code>"emptyStepGroup"</code>).
	 */
	public static final String EMPTY_STEP_GROUP = "emptyStepGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a step group in a menu (value <code>"stepGroup"</code>).
	 */
	public static final String STEP_GROUP = "stepGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * thread group in a menu (value <code>"emptyThreadGroup"</code>).
	 */
	public static final String EMPTY_THREAD_GROUP = "emptyThreadGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a thread group in a menu (value <code>"threadGroup"</code>).
	 */
	public static final String THREAD_GROUP = "threadGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * launch group in a menu (value <code>"emptyLaunchGroup"</code>).
	 */
	public static final String EMPTY_LAUNCH_GROUP = "emptyLaunchGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a launch group in a menu (value <code>"launchGroup"</code>).
	 */
	public static final String LAUNCH_GROUP = "launchGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * variable group in a menu (value <code>"emptyVariableGroup"</code>).
	 */
	public static final String EMPTY_VARIABLE_GROUP = "emptyVariableGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a variable group in a menu (value <code>"variableGroup"</code>).
	 */
	public static final String VARIABLE_GROUP = "variableGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * navigation group in a menu (value <code>"emptyNavigationGroup"</code>).
	 */
	public static final String EMPTY_NAVIGATION_GROUP = "emptyNavigationGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a navigation group in a menu (value <code>"navigationGroup"</code>).
	 */
	public static final String NAVIGATION_GROUP = "navigationGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding a
	 * breakpoint group in a menu (value <code>"emptyBreakpointGroup"</code>).
	 */
	public static final String EMPTY_BREAKPOINT_GROUP = "emptyBreakpointGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a breakpoint group in a menu (value <code>"breakpointGroup"</code>).
	 */
	public static final String BREAKPOINT_GROUP = "breakpointGroup"; //$NON-NLS-1$
	
	/** 
	 * Identifier for an empty group preceeding an
	 * expression group in a menu (value <code>"emptyExpressionGroup"</code>).
	 */
	public static final String EMPTY_EXPRESSION_GROUP = "emptyExpressionGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for an expression group in a menu (value <code>"expressionGroup"</code>).
	 */

	public static final String EXPRESSION_GROUP = "expressionGroup"; //$NON-NLS-1$
	/** 
	 * Identifier for an empty group preceeding a
	 * render group in a menu (value <code>"emptyRenderGroup"</code>).
	 */
	public static final String EMPTY_RENDER_GROUP = "emptyRenderGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a render group in a menu (value <code>"renderGroup"</code>).
	 */
	public static final String RENDER_GROUP = "renderGroup"; //$NON-NLS-1$
	
	/**
	 * Identifier for a property group in a menu (value <code>"propertyGroup"</code>).
	 */
	public static final String PROPERTY_GROUP = "propertyGroup"; //$NON-NLS-1$
	
	/**
	 * Id for the popup menu associated with the variables (tree viewer) part of the VariableView
	 */
	public static final String VARIABLE_VIEW_VARIABLE_ID = "org.eclipse.debug.ui.VariableView.variables"; //$NON-NLS-1$
	
	/**
	 * Id for the popup menu associated with the detail (text viewer) part of the VariableView
	 */
	public static final String VARIABLE_VIEW_DETAIL_ID = "org.eclipse.debug.ui.VariableView.detail"; //$NON-NLS-1$
	
	// status codes
	/**
	 * Status indicating an invalid extension definition.
	 */
	public static final int STATUS_INVALID_EXTENSION_DEFINITION = 100;
	
	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 120;		
	
	// launch configuration attribute keys
	/**
	 * Launch configuartion attribute - the perspective to
	 * switch to when a launch configuration is launched in
	 * run mode (value <code>org.eclipse.debug.ui.target_run_perspective</code>).
	 * Value is a string corresponding to a perspective identifier,
	 * or <code>null</code> indicating no perspective change.
	 * 
	 * @since 2.0
	 */
	public static final String ATTR_TARGET_RUN_PERSPECTIVE = PLUGIN_ID + ".target_run_perspective";	 //$NON-NLS-1$
	
	/**
	 * Launch configuartion attribute - the perspective to
	 * switch to when a launch configuration is launched in
	 * debug mode (value <code>org.eclipse.debug.ui.target_run_perspective</code>).
	 * Value is a string corresponding to a perspective identifier,
	 * or <code>null</code> indicating no perspective change.
	 * 
	 * @since 2.0
	 */
	public static final String ATTR_TARGET_DEBUG_PERSPECTIVE = PLUGIN_ID + ".target_debug_perspective";		 //$NON-NLS-1$
	
}