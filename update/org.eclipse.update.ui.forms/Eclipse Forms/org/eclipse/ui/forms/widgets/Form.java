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
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
/**
 * Form is a control that is capable of scrolling an instance of
 * the FormContent class. It should be created in a parent that 
 * will allow it to use all the available area (for example, a shell, 
 * a view or an editor).
 * <p>
 * Children of the form should typically be created using FormToolkit to match
 * the appearance and behaviour. When creating children, use a form body as a
 * parent by calling 'getBody()' on the form instance. Example:
 * 
 * <pre>
 *  Form form = new Form(parent);
 *  FormToolkit toolkit = new FormToolkit(parent.getDisplay());
 *  form.setText(&quot;Sample form&quot;);
 *  form.getBody().setLayout(new GridLayout());
 *  toolkit.createButton(form.getBody(), &quot;Checkbox&quot;, SWT.CHECK);
 * </pre>
 * 
 * <p>
 * No layout manager has been set on the body. Clients are required to set the
 * desired layout manager explicitly.
 * 
 * @since 3.0
 */
public class Form extends SharedScrolledComposite {
	private FormContent content;

	public Form(Composite parent) {
		this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
	}
	/**
	 * Creates the form control as a child of the provided parent.
	 * 
	 * @param parent
	 *            the parent widget
	 */
	public Form(Composite parent, int style) {
		super(parent, style);
		content = new FormContent(this, SWT.NULL);
		super.setContent(content);
		content.setMenu(getMenu());
	}
	/**
	 * Returns the title text that will be rendered at the top of the form.
	 * 
	 * @return the title text
	 */
	public String getText() {
		return content.getText();
	}
	/**
	 * Sets the foreground color of the form. This color will also be used for
	 * the body.
	 */
	public void setForeground(Color fg) {
		super.setForeground(fg);
		content.setForeground(fg);
	}
	/**
	 * Sets the background color of the form. This color will also be used for
	 * the body.
	 */
	public void setBackground(Color bg) {
		super.setBackground(bg);
		content.setBackground(bg);
	}
	/**
	 * The form sets the content widget. This method should not be called by
	 * classes that instantiate this widget.
	 */
	public final void setContent(Control c) {
	}
	/**
	 * Sets the text to be rendered at the top of the form above the body as a
	 * title.
	 * 
	 * @param text
	 *            the title text
	 */
	public void setText(String text) {
		content.setText(text);
		reflow(true);
	}
	/**
	 * Returns the optional background image of this form. The image is
	 * rendered starting at the position 0,0 and is painted behind the title.
	 * 
	 * @return Returns the background image.
	 */
	public Image getBackgroundImage() {
		return content.getBackgroundImage();
	}
	/**
	 * Sets the optional background image to be rendered behind the title
	 * starting at the position 0,0.
	 * 
	 * @param backgroundImage
	 *            The backgroundImage to set.
	 */
	public void setBackgroundImage(Image backgroundImage) {
		content.setBackgroundImage(backgroundImage);
	}
	/**
	 * Returns the tool bar manager that is used to manage tool items in the
	 * form's title area.
	 * 
	 * @return form tool bar manager
	 */
	public IToolBarManager getToolBarManager() {
		return content.getToolBarManager();
	}
	/**
	 * Updates the local tool bar manager if used. Does nothing if local tool
	 * bar manager has not been created yet.
	 */
	public void updateToolBar() {
		content.updateToolBar();
	}
	/**
	 * Recomputes the body layout and form scroll bars. The method should be
	 * used when changes somewhere in the form body invalidate the current
	 * layout and/or scroll bars.
	 * 
	 * @param flushCache
	 *            if <samp>true </samp>, drop any cached layout information and
	 *            compute new one.
	 */
	public void reflow(boolean flushCache) {
		content.layout();
		super.reflow(flushCache);
	}
	/**
	 * Returns the container that occupies the body of the form (the form area
	 * below the title). Use this container as a parent for the controls that
	 * should be in the form. No layout manager has been set on the form body.
	 * 
	 * @return Returns the body of the form.
	 */
	public Composite getBody() {
		return content.getBody();
	}
}