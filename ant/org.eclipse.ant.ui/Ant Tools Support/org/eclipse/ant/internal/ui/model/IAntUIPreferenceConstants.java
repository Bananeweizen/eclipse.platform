/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

/**
 * Constants used to identify user preferences.
 */
public interface IAntUIPreferenceConstants {

	public static final String ANTVIEW_INCLUDE_ERROR_SEARCH_RESULTS = "antview.includeErrorSearchResults"; //$NON-NLS-1$
	public static final String ANTVIEW_LAST_SEARCH_STRING = "antview.lastSearchString"; //$NON-NLS-1$
	public static final String ANTVIEW_LAST_WORKINGSET_SEARCH_SCOPE = "antview.lastSearchScope"; //$NON-NLS-1$
	public static final String ANTVIEW_USE_WORKINGSET_SEARCH_SCOPE = "antview.useWorkingSetSearchScope"; //$NON-NLS-1$
	
	public static final String ANT_FIND_BUILD_FILE_NAMES = "ant.findBuildFileNames"; //$NON-NLS-1$
	
	public static final String CONSOLE_ERROR_RGB = "console.errorColor"; //$NON-NLS-1$
	public static final String CONSOLE_WARNING_RGB = "console.warningColor"; //$NON-NLS-1$
	public static final String CONSOLE_INFO_RGB = "console.infoColor"; //$NON-NLS-1$
	public static final String CONSOLE_VERBOSE_RGB = "console.verboseColor"; //$NON-NLS-1$
	public static final String CONSOLE_DEBUG_RGB = "console.debugColor"; //$NON-NLS-1$	
	
	public static final String ANT_TOOLS_JAR_WARNING= "toolsJAR"; //$NON-NLS-1$
	
	public static final String ANT_ERROR_DIALOG= "errorDialog"; //$NON-NLS-1$
	
	public static final String ANT_CLASSPATH_WARNING= "antClasspathInfo";	 //$NON-NLS-1$
	
	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should
	 * show internal targets in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_INTERNAL_TARGETS= "anteditor.filterInternalTargets"; //$NON-NLS-1$
	
	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should
	 * show imported elements in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_IMPORTED_ELEMENTS = "anteditor.filterImportedElements"; //$NON-NLS-1$
	
	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should
	 * show properties in the Outline.
	 */
	public static final String ANTEDITOR_FILTER_PROPERTIES= "anteditor.filterProperties"; //$NON-NLS-1$
	
	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should
	 * sort elements in the Outline.
	 */
	public static final String ANTEDITOR_SORT= "anteditor.sort"; //$NON-NLS-1$
}