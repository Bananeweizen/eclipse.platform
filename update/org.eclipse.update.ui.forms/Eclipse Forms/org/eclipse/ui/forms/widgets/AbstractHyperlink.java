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
import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.internal.widgets.FormsResources;
/**
 * This is the base class for custom hyperlink widget. It is responsible for
 * processing mouse and keyboard events, and converting them into unified
 * hyperlink events. Subclasses are responsible for rendering the hyperlink in
 * the client area.
 * 
 * @since 3.0
 */
public abstract class AbstractHyperlink extends Canvas {
	private boolean hasFocus;
	private Vector listeners;
	/**
	 * Amount of the margin width around the hyperlink (default is 1).
	 */
	protected int marginWidth = 1;
	/**
	 * Amount of the margin height around the hyperlink (default is 1).
	 */
	protected int marginHeight = 1;
	/**
	 * Creates a new hyperlink in the provided parent.
	 * 
	 * @param parent
	 *            the control parent
	 * @param style
	 *            the widget style
	 */
	public AbstractHyperlink(Composite parent, int style) {
		super(parent, style);
		addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event e) {
				if (e.character == '\r') {
					handleActivate();
				}
			}
		});
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				switch (e.detail) {
					case SWT.TRAVERSE_PAGE_NEXT :
					case SWT.TRAVERSE_PAGE_PREVIOUS :
					case SWT.TRAVERSE_ARROW_NEXT :
					case SWT.TRAVERSE_ARROW_PREVIOUS :
					case SWT.TRAVERSE_RETURN :
						e.doit = false;
						return;
				}
				e.doit = true;
			}
		});
		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
					case SWT.FocusIn :
						hasFocus = true;
						handleEnter();
						break;
					case SWT.FocusOut :
						hasFocus = false;
						handleExit();
						break;
					case SWT.DefaultSelection :
						handleActivate();
						break;
					case SWT.MouseEnter :
						handleEnter();
						break;
					case SWT.MouseExit :
						handleExit();
						break;
					case SWT.MouseUp :
						handleMouseUp(e);
						break;
				}
			}
		};
		addListener(SWT.MouseEnter, listener);
		addListener(SWT.MouseExit, listener);
		addListener(SWT.MouseUp, listener);
		addListener(SWT.FocusIn, listener);
		addListener(SWT.FocusOut, listener);
		setCursor(FormsResources.getHandCursor());
	}
	/**
	 * Adds the event listener to this hyperlink.
	 * 
	 * @param listener
	 *            the event listener to add
	 */
	public void addHyperlinkListener(HyperlinkListener listener) {
		if (listeners == null)
			listeners = new Vector();
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	/**
	 * Removes the event listener from this hyperlink.
	 * 
	 * @param listener
	 *            the event listener to remove
	 */
	public void removeHyperlinkListener(HyperlinkListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
	}
	/**
	 * Returns the selection state of the control. When focus is gained, the
	 * state will be <samp>true </samp>; it will switch to <samp>false </samp>
	 * when the control looses focus.
	 * 
	 * @return <code>true</code> if the widget has focus, <code>false</code>
	 *         otherwise.
	 */
	public boolean getSelection() {
		return hasFocus;
	}
	/**
	 * Called when hyperlink is entered. Subclasses that override this method
	 * must call 'super'.
	 */
	protected void handleEnter() {
		redraw();
		if (listeners == null)
			return;
		int size = listeners.size();
		HyperlinkEvent e = new HyperlinkEvent(this, getHref(), getText());
		for (int i = 0; i < size; i++) {
			HyperlinkListener listener = (HyperlinkListener) listeners.get(i);
			listener.linkEntered(e);
		}
	}
	/**
	 * Called when hyperlink is exited. Subclasses that override this method
	 * must call 'super'.
	 */
	protected void handleExit() {
		redraw();
		if (listeners == null)
			return;
		int size = listeners.size();
		HyperlinkEvent e = new HyperlinkEvent(this, getHref(), getText());
		for (int i = 0; i < size; i++) {
			HyperlinkListener listener = (HyperlinkListener) listeners.get(i);
			listener.linkExited(e);
		}
	}
	/**
	 * Called when hyperlink has been activated. Subclasses that override this
	 * method must call 'super'.
	 */
	protected void handleActivate() {
		if (listeners == null)
			return;
		int size = listeners.size();
		setCursor(FormsResources.getBusyCursor());
		HyperlinkEvent e = new HyperlinkEvent(this, getHref(), getText());
		for (int i = 0; i < size; i++) {
			HyperlinkListener listener = (HyperlinkListener) listeners.get(i);
			listener.linkActivated(e);
		}
		if (!isDisposed())
			setCursor(FormsResources.getHandCursor());
	}
	/**
	 * Sets the object associated with this hyperlink. Concrete implementation
	 * of this class can use if to store text, URLs or model objects that need
	 * to be processed on hyperlink events.
	 * 
	 * @param href
	 *            the hyperlink object reference
	 */
	public void setHref(Object href) {
		setData("href", href);
	}
	/**
	 * Returns the object associated with this hyperlink.
	 * 
	 * @see #setHref
	 * @return the hyperlink object reference
	 */
	public Object getHref() {
		return getData("href");
	}
	/**
	 * Returns the textual representation of this hyperlink suitable for
	 * showing in tool tips or on the status line.
	 * 
	 * @return the hyperlink text
	 */
	public String getText() {
		return getToolTipText();
	}
	/**
	 * Paints the hyperlink as a reaction to the provided paint event.
	 * 
	 * @param e
	 *            the paint event
	 */
	protected abstract void paintHyperlink(PaintEvent e);
	/**
	 * Paints the control as a reaction to the provided paint event.
	 * 
	 * @param e
	 *            the paint event
	 */
	protected void paint(PaintEvent e) {
		paintHyperlink(e);
		if (hasFocus) {
			GC gc = e.gc;
			Rectangle carea = getClientArea();
			gc.setForeground(getForeground());
			gc.drawFocus(0, 0, carea.width, carea.height);
		}
	}
	private void handleMouseUp(Event e) {
		if (e.button != 1)
			return;
		Point size = getSize();
		// Filter out mouse up events outside
		// the link. This can happen when mouse is
		// clicked, dragged outside the link, then
		// released.
		if (e.x < 0)
			return;
		if (e.y < 0)
			return;
		if (e.x >= size.x)
			return;
		if (e.y >= size.y)
			return;
		handleActivate();
	}
}