package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.ui.forms.internal.*;


public class MyComputerForm extends UpdateWebForm {
	private SiteBookmark currentBookmark;
	private static final String KEY_TITLE = "MyComputerPage.title";
	private static final String KEY_NTITLE = "MyComputerPage.ntitle";
	private static final String KEY_DESC = "MyComputerPage.desc";
	
public MyComputerForm(UpdateFormPage page) {
	super(page);
}

public void dispose() {
	super.dispose();
}

public void initialize(Object modelObject) {
	setHeadingText(UpdateUI.getResourceString(KEY_TITLE));
	super.initialize(modelObject);
}

protected void createContents(Composite parent) {
	HTMLTableLayout layout = new HTMLTableLayout();
	parent.setLayout(layout);
	layout.leftMargin = layout.rightMargin = 10;
	layout.topMargin = 10;
	layout.verticalSpacing = 20;
	// defect 13686
	//layout.horizontalSpacing = 0;
	layout.numColumns = 3;
	
	FormWidgetFactory factory = getFactory();
	
	Label text = factory.createLabel(parent, null, SWT.WRAP);
	text.setText(UpdateUI.getResourceString(KEY_DESC));
	WorkbenchHelp.setHelp(parent, "org.eclipse.update.ui.MyComputerForm");
}

public void expandTo(Object obj) {
	if (obj instanceof MyComputer) {
		inputChanged((MyComputer)obj);
	}
}

private void inputChanged(MyComputer myComputer) {
	String pattern = UpdateUI.getResourceString(KEY_TITLE);
	String name = System.getProperty("user.name");
	String message = MessageFormat.format(pattern, new Object[] {name});
	setHeadingText(message);
}

}