/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.forms.widgets;

import java.util.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.IFormEntryListener;

/**
 *TODO Need to return to this class - not clear how to rework it.
 */

public class FormEntry {
	private Text text;
	private String value;
	private boolean dirty;
	private Vector listeners = new Vector();
	boolean ignoreModify = false;

	public FormEntry(Text text) {
		this.text = text;
		this.value = text.getText();
		addListeners();
	}
	public void addFormTextListener(IFormEntryListener listener) {
		listeners.addElement(listener);
	}
	private void addListeners() {
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				editOccured(e);
			}
		});
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (dirty)
					commit();
			}
		});
	}
	
	public void commit() {
		if (dirty) {
			value = text.getText();
			//notify
			for (Iterator iter = listeners.iterator(); iter.hasNext();) {
				((IFormEntryListener) iter.next()).textValueChanged(this);
			}
		}
		dirty = false;
	}
	
	protected void editOccured(ModifyEvent e) {
		if (ignoreModify)
			return;
		dirty = true;
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			((IFormEntryListener) iter.next()).textDirty(this);
		}
	}

	public Text getControl() {
		return text;
	}
	public java.lang.String getValue() {
		return value;
	}
	public boolean isDirty() {
		return dirty;
	}
	protected void keyReleaseOccured(KeyEvent e) {
		if (e.character == '\r') {
			// commit value
			if (dirty)
				commit();
		} else if (e.character == '\u001b') { // Escape character
			text.setText(value != null ? value : ""); // restore old
			dirty = false;
		}
	}
	
	public void removeFormTextListener(IFormEntryListener listener) {
		listeners.removeElement(listener);
	}

	public void setDirty(boolean newDirty) {
		dirty = newDirty;
	}
	
	public void setValue(String value) {
		if (text != null)
			text.setText(value != null ? value : "");
		this.value = value;
	}

	public void setValue(String value, boolean blockNotification) {
		ignoreModify = blockNotification;
		setValue(value);
		ignoreModify = false;
	}

}
