/********************************************************************** * Copyright (c) 2002 IBM Corporation and others. * All rights reserved.   This program and the accompanying materials * are made available under the terms of the Common Public License v0.5 * which accompanies this distribution, and is available at * http://www.eclipse.org/legal/cpl-v05.html *  * Contributors:  * IBM - Initial API and implementation **********************************************************************/package org.eclipse.ant.internal.ui;import java.util.Iterator;import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.swt.graphics.*;import org.eclipse.swt.widgets.Display;
public class AntPropertyChangeListener implements IPropertyChangeListener {
		// unique instance	private static AntPropertyChangeListener instance = new AntPropertyChangeListener();	// private constructor to ensure the singletonprivate AntPropertyChangeListener() {}// access to the singletonpublic static AntPropertyChangeListener getInstance() {	return instance;}
/**
 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
 */
public void propertyChange(PropertyChangeEvent event) {
	String propertyName= event.getProperty();

	if (propertyName.equals(IAntPreferenceConstants.CONSOLE_ERROR_RGB)) {
		Color temp = AntConsole.ERROR_COLOR;
		AntConsole.ERROR_COLOR = AntConsolePreferencePage.getPreferenceColor(IAntPreferenceConstants.CONSOLE_ERROR_RGB);
		temp.dispose();		clearOutput();
	} else if (propertyName.equals(IAntPreferenceConstants.CONSOLE_WARNING_RGB)) {
		Color temp = AntConsole.WARN_COLOR;
		AntConsole.WARN_COLOR = AntConsolePreferencePage.getPreferenceColor(IAntPreferenceConstants.CONSOLE_WARNING_RGB);
		temp.dispose();		clearOutput();
	} else if (propertyName.equals(IAntPreferenceConstants.CONSOLE_INFO_RGB)) {
		Color temp = AntConsole.INFO_COLOR;
		AntConsole.INFO_COLOR = AntConsolePreferencePage.getPreferenceColor(IAntPreferenceConstants.CONSOLE_INFO_RGB);
		temp.dispose();		clearOutput();
	} else if (propertyName.equals(IAntPreferenceConstants.CONSOLE_VERBOSE_RGB)) {
		Color temp = AntConsole.VERBOSE_COLOR;
		AntConsole.VERBOSE_COLOR = AntConsolePreferencePage.getPreferenceColor(IAntPreferenceConstants.CONSOLE_VERBOSE_RGB);
		temp.dispose();		clearOutput();
	} else if (propertyName.equals(IAntPreferenceConstants.CONSOLE_DEBUG_RGB)) {
		Color temp = AntConsole.DEBUG_COLOR;
		AntConsole.DEBUG_COLOR = AntConsolePreferencePage.getPreferenceColor(IAntPreferenceConstants.CONSOLE_DEBUG_RGB);
		temp.dispose();		clearOutput();
	} else if (propertyName.equals(IAntPreferenceConstants.CONSOLE_FONT)) {		FontData data= AntConsolePreferencePage.getConsoleFontData();		Font temp= AntConsole.ANT_FONT;		AntConsole.ANT_FONT = new Font(Display.getCurrent(), data);		temp.dispose();		updateFont();		} else
		return;
}
/** * Clears the output of all the consoles */private void clearOutput(){	for (Iterator iterator = AntConsole.getInstances().iterator(); iterator.hasNext();)		((AntConsole) iterator.next()).clearOutput();}/** * Updates teh font in all the consoles */private void updateFont() {	for (Iterator iterator = AntConsole.getInstances().iterator(); iterator.hasNext();)		((AntConsole) iterator.next()).updateFont();	}
}
