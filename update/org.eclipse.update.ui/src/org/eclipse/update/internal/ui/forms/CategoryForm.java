package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.model.SiteCategory;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.ui.forms.internal.*;


public class CategoryForm extends UpdateWebForm {
	private final static String KEY_TITLE = "CategoryPage.title";
	private final static String KEY_MORE_INFO = "CategoryPage.moreInfo";
	SiteCategory currentCategory;
	Label textLabel;
	SelectableFormLabel link;
	
public CategoryForm(UpdateFormPage page) {
	super(page);
}

public void dispose() {
	super.dispose();
}

public void initialize(Object modelObject) {
	setHeadingText(UpdateUI.getString(KEY_TITLE));
	super.initialize(modelObject);
	//((Composite)getControl()).layout(true);
}

protected void createContents(Composite parent) {
	HTMLTableLayout layout = new HTMLTableLayout();
	parent.setLayout(layout);
	layout.leftMargin = layout.rightMargin = 10;
	layout.topMargin = 10;
	layout.verticalSpacing = 20;
	layout.numColumns = 1;
	// defect 17123
	//layout.horizontalSpacing = 0;
	
	FormWidgetFactory factory = getFactory();
	textLabel = factory.createLabel(parent, null, SWT.WRAP);
	
	IHyperlinkListener listener;

	listener = new HyperlinkAdapter() {
		public void linkActivated(Control link) {
			if (currentCategory==null) return;
			BusyIndicator.showWhile(getControl().getDisplay(),
			new Runnable() {
				public void run() {
					ICategory category = currentCategory.getCategory();
					if (category!=null) {
						IURLEntry info = category.getDescription();
						URL infoURL = info.getURL();
						if (infoURL!=null) {
							DetailsView.showURL(infoURL.toString());
						}
					}
				}
			});
		}
	};
	link = new SelectableFormLabel(parent, SWT.NULL);
	link.setText(UpdateUI.getString(KEY_MORE_INFO));
	factory.turnIntoHyperlink(link, listener);
	link.setVisible(false);
	setFocusControl(link);
	WorkbenchHelp.setHelp(parent, "org.eclipse.update.ui.CategoryForm");
}

public void expandTo(Object obj) {
	if (obj instanceof SiteCategory) {
		inputChanged((SiteCategory)obj);
	}
}
//
// Defect 17123 - commented out this implementation of inputChanged(...)
/*private void inputChanged(SiteCategory category) {
	setHeadingText(category.getCategory().getLabel());
	IURLEntry info = category.getCategory().getDescription();
	if (info!=null) {
		String text = info.getAnnotation();
		if (text!=null)
		   textLabel.setText(text);
		else
		   textLabel.setText("");
		link.setVisible(info.getURL()!=null);
	}
	else {
		textLabel.setText("");
		link.setVisible(false);
	}
	textLabel.getParent().layout();
	((Composite)getControl()).layout();
	getControl().redraw();
	currentCategory = category;
}*/

// defect 17123: new implementation of inputChanged(...)
private void inputChanged(SiteCategory category) {
	setHeadingText(category.getCategory().getLabel());
	IURLEntry info = category.getCategory().getDescription();
	String text = null;
	if (info != null) {
		text = info.getAnnotation();
		if (text != null) {
			Composite parent = textLabel.getParent();
			textLabel.dispose();
			parent.layout();
			textLabel = getFactory().createLabel(parent,text,SWT.WRAP);
			link.setVisible(info.getURL()!= null);
		}
	}
	if (text == null) {
	 textLabel.setText("");
	 link.setVisible(false);
	}
	
	textLabel.getParent().layout();
	((Composite)getControl()).layout();
	getControl().redraw();
	currentCategory = category;
}

}