package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.pages.*;
import org.eclipse.update.internal.ui.preferences.UpdateColors;
import org.eclipse.update.ui.forms.internal.*;

public abstract class UpdateSection extends FormSection {
	private UpdateFormPage page;
	
	public UpdateSection(UpdateFormPage page) {
		this.page = page;
		IPreferenceStore pstore = UpdateUI.getDefault().getPreferenceStore();
		pstore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getProperty().equals(UpdateColors.P_TOPIC_COLOR))
					updateHeaderColor();
			}
		});
	}
	
	public UpdateFormPage getPage() {
		return page;
	}
	
	protected void updateHeaderColor() {
		header.setForeground(UpdateColors.getTopicColor(header.getDisplay()));
	}
}

