/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.forms;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.IVolume;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.parts.VolumeLabelProvider;
import org.eclipse.update.ui.forms.internal.*;

public class UnknownObjectForm extends UpdateWebForm {
	private Object currentObj;
	private VolumeLabelProvider volumeProvider;

	public UnknownObjectForm(UpdateFormPage page) {
		super(page);
	}

	public void dispose() {
		if (volumeProvider!=null) volumeProvider.dispose();
		super.dispose();
	}
	
	public void objectChanged(Object object, String property) {
		if (object.equals(currentObj)) {
			expandTo(object);
		}
	}

	public void initialize(Object modelObject) {
		setHeadingText("");
		super.initialize(modelObject);
	}

	protected void createContents(Composite parent) {
		HTMLTableLayout layout = new HTMLTableLayout();
		parent.setLayout(layout);
		layout.leftMargin = layout.rightMargin = 10;
		layout.topMargin = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 20;
		layout.numColumns = 1;

		FormWidgetFactory factory = getFactory();
		Composite client = factory.createComposite(parent);
		TableData td = new TableData();
		td.align = TableData.FILL;
		WorkbenchHelp.setHelp(client, "org.eclipse.update.ui.UnknownObjectForm");
	}

	public void expandTo(Object obj) {
		String name = "";

		if (obj != null && obj instanceof UIModelObject) {
			if (obj instanceof MyComputerDirectory) {
				MyComputerDirectory dir = (MyComputerDirectory)obj;
				IVolume volume = dir.getVolume();
				if (volume!=null) name = getVolumeName(volume);
			}
			if (name.length()==0)
				name = obj.toString();
		}
		setHeadingText(name);
		if (getControl() != null) {
			((Composite) getControl()).layout();
			getControl().redraw();
		}
		currentObj = obj;
	}
	private String getVolumeName(IVolume volume) {
		if (volumeProvider==null)
			volumeProvider = new VolumeLabelProvider();
		return volumeProvider.getText(volume);
	}
}
