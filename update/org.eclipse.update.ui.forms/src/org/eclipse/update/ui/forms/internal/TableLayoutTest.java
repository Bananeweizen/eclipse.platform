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
package org.eclipse.update.ui.forms.internal;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class TableLayoutTest {

public static void main (String [] args) {
     Display display = new Display ();
     Shell shell = new Shell (display);
     shell.setLayout(new FillLayout());
     final ScrolledComposite sc = new ScrolledComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
    // sc.setAlwaysShowScrollBars(true);
     sc.setBackground(sc.getDisplay().getSystemColor(SWT.COLOR_WHITE));
     final Composite c = new Composite(sc, SWT.NONE);
     //c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_RED));
     sc.setContent(c);
     sc.addListener (SWT.Resize,  new Listener () {
		public void handleEvent (Event e) {
			Rectangle ssize = sc.getClientArea();
			int swidth = ssize.width;
			HTMLTableLayout layout = (HTMLTableLayout)c.getLayout();
			Point size = layout.computeSize(c, swidth, SWT.DEFAULT, true);
			//if (size.x < swidth) size.x = swidth;
			Rectangle trim = c.computeTrim(0, 0, size.x, size.y);
			size = new Point(trim.width, trim.height);
			/*
			Point size = c.computeSize(swidth, SWT.DEFAULT, true);
			*/
			//System.out.println("in: "+swidth+", out: "+size.x);
			c.setSize(size);
		}
	});
     c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
     HTMLTableLayout layout = new HTMLTableLayout();
     layout.numColumns = 2;
     //layout.horizontalSpacing = 0;
     layout.leftMargin = 0;
     layout.rightMargin = 0;
     layout.makeColumnsEqualWidth=false;
     //layout.marginWidth = layout.marginHeight = 0;
     //layout.horizontalSpacing = 0;
     c.setLayout(layout);
     
     Label label;
     Button b;
     TableData td;
/*     
     label = new Label(c, SWT.NULL);
     label.setText("Single line1");
     
     //label = new Label(c, SWT.WRAP);
     Text text = new Text(c, SWT.MULTI | SWT.WRAP);
     text.setEditable(false);
     text.setText("This is a much longer text that I want wrapped,"+
     " but depending on the window size it can be rendered completely.");
     
     b = new Button(c, SWT.PUSH);
     b.setText("Simple button");
     
     label = new Label(c, SWT.WRAP);
     label.setText("Another text that may or may not be wrapped");

     label = new Label(c, SWT.NULL);
     td = new TableData(TableData.RIGHT, TableData.BOTTOM);
     label.setLayoutData(td);
     label.setText("Fixed label");
     
     Composite nested = new Composite(c, SWT.NULL);
     layout = new HTMLTableLayout();
     layout.numColumns = 2;
     nested.setLayout(layout);
     b = new Button(nested, SWT.PUSH);
     b.setText("Button2");
     label = new Label(nested, SWT.WRAP);
     td = new TableData(TableData.LEFT, TableData.MIDDLE);
     label.setLayoutData(td);
     label.setText("Some text in the nested label that can be wrapped");
*/     
     label = new Label(c, SWT.NULL);
     label.setText("Text in the left column");
     
     b = new Button(c, SWT.CHECK);
     b.setText("Checkbox in the right column");

     
     label = new Label(c, SWT.WRAP);
     label.setText("This assignment step is then repeated for nested tables using the minimum and maximum widths derived for all such tables in the first pass. In this case, the width of the parent table cell plays the role of the current window size in the above description. This process is repeated recursively for all nested tables. The topmost table is then rendered using the assigned widths. Nested tables are subsequently rendered as part of the parent table's cell contents.");
     td = new TableData();
     td.colspan = 2;
     td.align = TableData.FILL;
     label.setLayoutData(td);
     
     ExpandableGroup exp = new ExpandableGroup (SWT.WRAP) {
     	public void fillExpansion(Composite container, FormWidgetFactory factory) {
     		HTMLTableLayout layout = new HTMLTableLayout();
     		container.setLayout(layout);
     		layout.leftMargin = layout.rightMargin = 0;
     		Button button = factory.createButton(container, null, SWT.PUSH);
     		button.setText("Button");
    	}
     	public void expanded() {
     		c.layout(true);
     		updateSize(sc, c);
     	}
     	public void collapsed() {
     		c.layout(true);
     		updateSize(sc, c);
     	}
     };
     exp.setText("Expandable Section");
     //exp.setExpandable(false);
     FormWidgetFactory factory = new FormWidgetFactory();
     exp.createControl(c, factory);
     //exp.getControl().setBackground(label.getDisplay().getSystemColor(SWT.COLOR_GREEN));
     td = new TableData();
     td.colspan = 2;
     td.align = TableData.FILL;
     exp.getControl().setLayoutData(td);
     
	 StatusLineManager manager = new StatusLineManager();
     //addFormEngine(c, factory, manager);
     //addFormEngine(c, factory, manager);
     //addFormEngine(c, factory, manager);
     
     addRow(c);
 
     Control mcontrol = manager.createControl(c);
     td = new TableData();
     td.colspan = 2;
     td.align = TableData.FILL;
     mcontrol.setLayoutData(td);
     
     factory.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_ROLLOVER);
     
     SelectableFormLabel ft = new SelectableFormLabel(c, SWT.WRAP);
     ft.setText("Some text in the form text that should also wrap");
     factory.turnIntoHyperlink(ft, new IHyperlinkListener() {
     	public void linkEntered(Control link) {
     		System.out.println("Link entered");
     	}
     	public void linkExited(Control link) {
    		System.out.println("Link exited");
     	}
     	public void linkActivated(Control link) {
     		System.out.println("Link activated");
     	}
     });

     ft = new SelectableFormLabel(c, SWT.WRAP);
     ft.setText("Some more form text here");
     factory.turnIntoHyperlink(ft, new IHyperlinkListener() {
     	public void linkEntered(Control link) {
     		System.out.println("Link entered");
     	}
     	public void linkExited(Control link) {
    		System.out.println("Link exited");
     	}
     	public void linkActivated(Control link) {
     		System.out.println("Link activated");
     	}
     });
   
     shell.open ();
     while (!shell.isDisposed ()) {
          if (!display.readAndDispatch ()) display.sleep ();
     }
     display.dispose ();
}

/*

public static void addFormEngine(Composite c, FormWidgetFactory factory, IStatusLineManager manager) {
     new Label(c, SWT.NULL);
     RichText html = new RichText(c, SWT.WRAP);
     html.setHyperlinkSettings(factory.getHyperlinkHandler());
     RichTextHTTPAction action = new RichTextHTTPAction(manager);
     URL i1URL = TableLayoutTest.class.getResource("image1.gif");
     ImageDescriptor id1 = ImageDescriptor.createFromURL(i1URL);
     html.registerTextObject("urlHandler", action);
     html.registerTextObject("image1", id1.createImage());
     //html.setBackground(factory.getBackgroundColor());
     html.setForeground(factory.getForegroundColor());
     InputStream is = TableLayoutTest.class.getResourceAsStream("index.xml");
     //html.setParagraphsSeparated(false);
     html.setContents(is, true);
     TableData td = new TableData();
     td.colspan = 1;
     td.align = TableData.FILL;
     html.setLayoutData(td);
}
*/

public static void addRow(Composite c) {
	Composite row = new Composite(c, SWT.WRAP);
	RowLayout layout = new RowLayout();
	layout.wrap = true;
	row.setLayout(layout);
	
	for (int i=0; i<10; i++) {
		Button button = new Button(row, SWT.PUSH);
		button.setText("Button that should be wrapped");
	}
	TableData td = new TableData();
	td.colspan = 2;
	td.align = TableData.FILL;
	td.grabHorizontal = true;
	row.setLayoutData(td);
}

private static void updateSize(ScrolledComposite sc, Composite c) {
	Rectangle ssize = sc.getClientArea();
	int swidth = ssize.width;
	HTMLTableLayout layout = (HTMLTableLayout)c.getLayout();
	Point size = layout.computeSize(c, swidth, SWT.DEFAULT, true);
	Rectangle trim = c.computeTrim(0, 0, size.x, size.y);
	size = new Point(trim.width, trim.height);
	c.setSize(size);
}

}
