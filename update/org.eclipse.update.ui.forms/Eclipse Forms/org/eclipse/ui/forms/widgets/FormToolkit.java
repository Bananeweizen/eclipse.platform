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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
/**
 * The toolkit is responsible for creating SWT controls adapted to work in
 * Eclipse forms. In addition to changing their presentation properties (fonts,
 * colors etc.), various listeners are attached to make them behave correctly
 * in the form context.
 * <p>
 * In addition to being the control factory, the toolkit is also responsible
 * for painting flat borders for select controls, managing hyperlink groups and
 * control colors.
 * <p>
 * The toolkit creates some of the most common controls used to populate
 * Eclipse forms. Controls that must be created using their constructors,
 * <samp>adapt </samp> method is available to change its properties in the same
 * way as with the supported toolkit controls.
 * <p>
 * Typically, one toolkit object is created per workbench part (for example, an
 * editor or a form wizard). The toolkit is disposed when the part is disposed.
 * To conserve resources, it is possible to create one color object for the
 * entire plug-in and share it between several toolkits. The plug-in is
 * responsible for disposing the colors (disposing the toolkit that uses shared
 * color object will not dispose the colors).
 * 
 * @since 3.0
 */
public class FormToolkit {
	public static final String KEY_DRAW_BORDER = "FormWidgetFactory.drawBorder";
	public static final String TREE_BORDER = "treeBorder";
	public static final String TEXT_BORDER = "textBorder";
	private int borderStyle = SWT.NULL;
	private FormColors colors;
	private KeyListener deleteListener;
	private BorderPainter borderPainter;
	private HyperlinkGroup hyperlinkGroup;
	/* default */
	VisibilityHandler visibilityHandler;
	/* default */
	KeyboardHandler keyboardHandler;
	private class BorderPainter implements PaintListener {
		public void paintControl(PaintEvent event) {
			Composite composite = (Composite) event.widget;
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control c = children[i];
				boolean inactiveBorder = false;
				boolean textBorder = false;
				if (!c.isVisible()) continue;
				if (c.getEnabled() == false && !(c instanceof CCombo))
					continue;
				if (c instanceof Hyperlink)
					continue;
				Object flag = c.getData(KEY_DRAW_BORDER);
				if (flag != null) {
					if (flag.equals(Boolean.FALSE))
						continue;
					if (flag.equals(TREE_BORDER))
						inactiveBorder = true;
					else if (flag.equals(TEXT_BORDER))
						textBorder = true;
				}
				if (!inactiveBorder
						&& (c instanceof Text || c instanceof Canvas
								|| c instanceof CCombo || textBorder)) {
					Rectangle b = c.getBounds();
					GC gc = event.gc;
					gc.setForeground(c.getBackground());
					gc.drawRectangle(b.x - 1, b.y - 1, b.width + 1,
							b.height + 1);
					gc.setForeground(colors.getForeground());
					if (c instanceof CCombo)
						gc.drawRectangle(b.x - 1, b.y - 1, b.width + 1,
								b.height + 1);
					else
						gc.drawRectangle(b.x - 1, b.y - 2, b.width + 1,
								b.height + 3);
				} else if (inactiveBorder || c instanceof Table
						|| c instanceof Tree || c instanceof TableTree) {
					Rectangle b = c.getBounds();
					GC gc = event.gc;
					gc.setForeground(colors.getBorderColor());
					gc.drawRectangle(b.x - 1, b.y - 1, b.width + 2,
							b.height + 2);
				}
			}
		}
	}
	private static class VisibilityHandler extends FocusAdapter {
		public void focusGained(FocusEvent e) {
			Widget w = e.widget;
			if (w instanceof Control) {
				FormUtil.ensureVisible((Control) w);
			}
		}
	}
	private static class KeyboardHandler extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			Widget w = e.widget;
			if (w instanceof Control) {
				FormUtil.processKey(e.keyCode, (Control) w);
			}
		}
	}
	/**
	 * Creates a toolkit that is self-sufficient (will manage its own colors).
	 *  
	 */
	public FormToolkit(Display display) {
		this(new FormColors(display));
	}
	/**
	 * Creates a toolkit that will use the provided (shared) colors. The
	 * toolkit will <b>not </b> dispose the provided colors.
	 * 
	 * @param colors
	 *            the shared colors
	 */
	public FormToolkit(FormColors colors) {
		this.colors = colors;
		initialize();
	}
	/**
	 * Creates a button as a part of the form.
	 * 
	 * @param parent
	 *            the button parent
	 * @param text
	 *            an optional text for the button (can be <code>null</code>)
	 * @param style
	 *            the button style (for example, <code>SWT.PUSH</code>)
	 * @return the button widget
	 */
	public Button createButton(Composite parent, String text, int style) {
		Button button = new Button(parent, style | SWT.FLAT);
		if (text != null)
			button.setText(text);
		adapt(button, true, true);
		return button;
	}
	/**
	 * Creates the composite as a part of the form.
	 * 
	 * @param parent
	 *            the composite parent
	 * @return the composite widget
	 */
	public Composite createComposite(Composite parent) {
		return createComposite(parent, SWT.NULL);
	}
	/**
	 * Creates the composite as part of the form using the provided style.
	 * 
	 * @param parent
	 *            the composite parent
	 * @param style
	 *            the composite style
	 * @return the composite widget
	 */
	public Composite createComposite(Composite parent, int style) {
		Composite composite = new LayoutComposite(parent, style);
		adapt(composite);
		return composite;
	}
	/**
	 * Creats the composite that can server as a separator between various
	 * parts of a form. Separator height should be controlled by setting the
	 * height hint on the layout data for the composite.
	 * 
	 * @param parent
	 *            the separator parent
	 * @return the separator widget
	 */
	public Composite createCompositeSeparator(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event e) {
				if (composite.isDisposed())
					return;
				Rectangle bounds = composite.getBounds();
				GC gc = e.gc;
				gc.setForeground(colors.getColor(FormColors.SEPARATOR));
				gc.setBackground(colors.getBackground());
				gc.fillGradientRectangle(0, 0, bounds.width, bounds.height,
						false);
			}
		});
		if (parent instanceof Section)
			((Section) parent).setSeparatorControl(composite);
		return composite;
	}
	/**
	 * Creates a label as a part of the form.
	 * 
	 * @param parent
	 *            the label parent
	 * @param text
	 *            the label text
	 * @return the label widget
	 */
	public Label createLabel(Composite parent, String text) {
		return createLabel(parent, text, SWT.NONE);
	}
	/**
	 * Creates a label as a part of the form.
	 * 
	 * @param parent
	 *            the label parent
	 * @param text
	 *            the label text
	 * @param style
	 *            the label style
	 * @return the label widget
	 */
	public Label createLabel(Composite parent, String text, int style) {
		Label label = new Label(parent, style);
		if (text != null)
			label.setText(text);
		adapt(label, false, false);
		return label;
	}
	/**
	 * Creates a hyperlink as a part of the form. The hyperlink will be added
	 * to the hyperlink group that belongs to this toolkit.
	 * 
	 * @param parent
	 *            the hyperlink parent
	 * @param text
	 *            the text of the hyperlink
	 * @param style
	 *            the hyperlink style
	 * @return the hyperlink widget
	 */
	public Hyperlink createHyperlink(Composite parent, String text, int style) {
		Hyperlink hyperlink = new Hyperlink(parent, style);
		if (text != null)
			hyperlink.setText(text);
		hyperlink.addFocusListener(visibilityHandler);
		hyperlink.addKeyListener(keyboardHandler);
		hyperlinkGroup.add(hyperlink);
		return hyperlink;
	}
	/**
	 * Creates an image hyperlink as a part of the form. The hyperlink will be
	 * added to the hyperlink group that belongs to this toolkit.
	 * 
	 * @param parent
	 *            the hyperlink parent
	 * @param style
	 *            the hyperlink style
	 * @return the image hyperlink widget
	 */
	public ImageHyperlink createImageHyperlink(Composite parent, int style) {
		ImageHyperlink hyperlink = new ImageHyperlink(parent, style);
		hyperlink.addFocusListener(visibilityHandler);
		hyperlink.addKeyListener(keyboardHandler);
		hyperlinkGroup.add(hyperlink);
		return hyperlink;
	}
	/**
	 * Creates a rich text as a part of the form.
	 * 
	 * @param parent
	 *            the rich text parent
	 * @param trackFocus
	 *            if <code>true</code>, the toolkit will monitor focus
	 *            transfers to ensure that the hyperlink in focus is visible in
	 *            the form.
	 * @return the rich text widget
	 */
	public RichText createRichText(Composite parent, boolean trackFocus) {
		RichText engine = new RichText(parent, SWT.WRAP);
		engine.marginWidth = 1;
		engine.marginHeight = 0;
		engine.setHyperlinkSettings(getHyperlinkGroup());
		adapt(engine, trackFocus, true);
		engine.setMenu(parent.getMenu());
		return engine;
	}
	/**
	 * Adapts a control to be used in a form that is associated with this
	 * toolkit. This involves adjusting colors and optionally adding handlers
	 * to ensure focus tracking and keyboard management.
	 * 
	 * @param control
	 *            a control to adapt
	 * @param trackFocus
	 *            if <code>true</code>, form will be scrolled horizontally
	 *            and/or vertically if needed to ensure that the control is
	 *            visible when it gains focus. Set it to <code>false</code>
	 *            if the control is not capable of gaining focus.
	 * @param trackKeyboard
	 *            if <code>true</code>, the control that is capable of
	 *            gaining focus will be tracked for certain keys that are
	 *            important to the underlying form (for example, PageUp,
	 *            PageDown, ScrollUp, ScrollDown etc.). Set it to <code>false</code>
	 *            if the control is not capable of gaining focus or these
	 *            particular key event are already used by the control.
	 */
	public void adapt(Control control, boolean trackFocus, boolean trackKeyboard) {
		control.setBackground(colors.getBackground());
		control.setForeground(colors.getForeground());
		if (trackFocus)
			control.addFocusListener(visibilityHandler);
		if (trackKeyboard)
			control.addKeyListener(keyboardHandler);
	}
	/**
	 * Adapts a composite to be used in a form associated with this toolkit.
	 * 
	 * @param composite
	 *            the composite to adapt
	 */
	public void adapt(Composite composite) {
		composite.setBackground(colors.getBackground());
		composite.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				((Control) e.widget).setFocus();
			}
		});
		composite.setMenu(composite.getParent().getMenu());
	}
	/**
	 * Creates a section as a part of the form.
	 * 
	 * @param parent
	 *            the section parent
	 * @param sectionStyle
	 *            the section style
	 * @return the section widget
	 */
	public Section createSection(Composite parent, int sectionStyle) {
		Section section = new Section(parent, sectionStyle);
		section.setBackground(colors.getBackground());
		section.setForeground(colors.getForeground());
		section.textLabel.addFocusListener(visibilityHandler);
		section.textLabel.addKeyListener(keyboardHandler);
		if (section.toggle != null) {
			section.toggle.addFocusListener(visibilityHandler);
			section.toggle.addKeyListener(keyboardHandler);
			section.toggle.setActiveDecorationColor(getHyperlinkGroup()
					.getActiveForeground());
			section.toggle.setDecorationColor(colors
					.getColor(FormColors.SEPARATOR));
		}
		section.setFont(JFaceResources.getFontRegistry().get(
				JFaceResources.BANNER_FONT));
		return section;
	}
	/**
	 * Creates an expandable composite as a part of the form.
	 * 
	 * @param parent
	 *            the expandable composite parent
	 * @param expansionStyle
	 *            the expandable composite style
	 * @return the expandable composite widget
	 */
	public ExpandableComposite createExpandableComposite(Composite parent,
			int expansionStyle) {
		ExpandableComposite ec = new ExpandableComposite(parent, SWT.NULL,
				expansionStyle);
		ec.setBackground(colors.getBackground());
		ec.setForeground(colors.getForeground());
		//hyperlinkGroup.add(ec.textLabel);
		if (ec.toggle != null) {
			ec.toggle.addFocusListener(visibilityHandler);
			ec.toggle.addKeyListener(keyboardHandler);
		}
		ec.textLabel.addFocusListener(visibilityHandler);
		ec.textLabel.addKeyListener(keyboardHandler);
		ec.setFont(JFaceResources.getFontRegistry().get(
				JFaceResources.BANNER_FONT));
		return ec;
	}
	/**
	 * Creates a separator label as a part of the form.
	 * 
	 * @param parent
	 *            the separator parent
	 * @param style
	 *            the separator style
	 * @return the separator label
	 */
	public Label createSeparator(Composite parent, int style) {
		Label label = new Label(parent, SWT.SEPARATOR | style);
		label.setBackground(colors.getBackground());
		label.setForeground(colors.getBorderColor());
		return label;
	}
	/**
	 * Creates a table as a part of the form.
	 * 
	 * @param parent
	 *            the table parent
	 * @param style
	 *            the table style
	 * @return the table widget
	 */
	public Table createTable(Composite parent, int style) {
		Table table = new Table(parent, style | borderStyle);
		adapt(table, false, false);
		hookDeleteListener(table);
		return table;
	}
	/**
	 * Creates a text as a part of the form.
	 * 
	 * @param parent
	 *            the text parent
	 * @param value
	 *            the text initial value
	 * @return the text widget
	 */
	public Text createText(Composite parent, String value) {
		return createText(parent, value, SWT.SINGLE);
	}
	/**
	 * Creates a text as a part of the form.
	 * 
	 * @param parent
	 *            the text parent
	 * @param value
	 *            the text initial value
	 * @param the
	 *            text style
	 * @return the text widget
	 */
	public Text createText(Composite parent, String value, int style) {
		Text text = new Text(parent, borderStyle | style);
		if (value != null)
			text.setText(value);
		adapt(text, true, false);
		return text;
	}
	/**
	 * Creates a tree widget as a part of the form.
	 * 
	 * @param parent
	 *            the tree parent
	 * @param style
	 *            the tree style
	 * @return the tree widget
	 */
	public Tree createTree(Composite parent, int style) {
		Tree tree = new Tree(parent, borderStyle | style);
		adapt(tree, false, false);
		hookDeleteListener(tree);
		return tree;
	}
	/**
	 * Creates a form widget in the provided parent.
	 * 
	 * @param parent
	 *            the form parent
	 * @return the form widget
	 */
	public Form createForm(Composite parent) {
		Form form = new Form(parent);
		form.setExpandHorizontal(true);
		form.setExpandVertical(true);
		form.setBackground(colors.getBackground());
		form.setForeground(colors.getColor(FormColors.TITLE));
		form.setFont(JFaceResources.getHeaderFont());
		return form;
	}
	public FormContent createFormContent(Composite parent) {
		FormContent formContent = new FormContent(parent, SWT.NULL);
		formContent.setBackground(colors.getBackground());
		formContent.setForeground(colors.getColor(FormColors.TITLE));
		formContent.setFont(JFaceResources.getHeaderFont());
		return formContent;
	}
	/**
	 * Creates a rich text as a part of the form.
	 * 
	 * @param parent
	 *            the rich text parent
	 * @param trackFocus
	 *            if <code>true</code>, the toolkit will monitor focus
	 *            transfers to ensure that the hyperlink in focus is visible in
	 *            the form.
	 * @return the rich text widget
	 */
	public ScrolledPageBook createPageBook(Composite parent, int style) {
		ScrolledPageBook book = new ScrolledPageBook(parent, style);
		adapt(book, true, true);
		book.setMenu(parent.getMenu());
		return book;
	}
	//TODO hook delete key
	/*
	 * private void deleteKeyPressed(Widget widget) { if (!(widget instanceof
	 * Control)) return; Control control = (Control) widget; for (Control
	 * parent = control.getParent(); parent != null; parent =
	 * parent.getParent()) { if (parent.getData() instanceof SectionPart) {
	 * SectionPart section = (SectionPart) parent.getData();
	 * section.doGlobalAction(ActionFactory.DELETE.getId()); break; } } }
	 */
	/**
	 * Disposes the toolkit.
	 */
	public void dispose() {
		if (colors.isShared() == false) {
			colors.dispose();
			colors = null;
		}
	}
	/**
	 * Returns the hyperlink group that manages hyperlinks for this toolkit.
	 * 
	 * @return the hyperlink group
	 */
	public HyperlinkGroup getHyperlinkGroup() {
		return hyperlinkGroup;
	}
	public void hookDeleteListener(Control control) {
		if (deleteListener == null) {
			deleteListener = new KeyAdapter() {
				public void keyPressed(KeyEvent event) {
					if (event.character == SWT.DEL && event.stateMask == 0) {
						//TODO hook delete key
						//deleteKeyPressed(event.widget);
					}
				}
			};
		}
		control.addKeyListener(deleteListener);
	}
/**
 * Sets the background color for the entire toolkit. The method
 * delegates the call to the FormColors object and also updates
 * the hyperlink group so that hyperlinks and other objects are
 * in sync.
 * @param bg the new background color
 */
	public void setBackground(Color bg) {
		hyperlinkGroup.setBackground(bg);
		colors.setBackground(bg);
	}
	/**
	 * Refreshes the hyperlink colors by loading from JFace settings.
	 */
	public void refreshHyperlinkColors() {
		hyperlinkGroup.initializeDefaultForegrounds(colors.getDisplay());
	}
	/**
	 * Paints flat borders for widgets created by this toolkit within the
	 * provided parent. Borders will not be painted if the global border style
	 * is SWT.BORDER (i.e. if native borders are used). Call this method during
	 * creation of a form composite to get the borders of its children painted.
	 * Care should be taken when selection layout margins. At least one pixel
	 * pargin width and height must be chosen to allow the toolkit to paint the
	 * border on the parent around the widgets.
	 * <p>
	 * Borders are painted for some controls that are selected by the toolkit
	 * by default. If a control needs a border but is not on its list, it is
	 * possible to force border in the following way:
	 * 
	 * <pre>
	 *  widget.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
	 *  
	 *  or
	 *  
	 *  widget.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
	 * </pre>
	 * 
	 * @param parent
	 *            the parent that owns the children for which the border needs
	 *            to be painted.
	 */
	public void paintBordersFor(Composite parent) {
		if (borderStyle == SWT.BORDER)
			return;
		if (borderPainter == null)
			borderPainter = new BorderPainter();
		parent.addPaintListener(borderPainter);
	}
	/**
	 * Returns the colors used by this toolkit.
	 * 
	 * @return the color object
	 */
	public FormColors getColors() {
		return colors;
	}
	/**
	 * Returns the border style used for various widgets created by this
	 * toolkit. The intent of the toolkit is to create controls with styles
	 * that yield a 'flat' appearance. On systems where the native borders are
	 * already flat, we set the style to SWT.BORDER and don't paint the borders
	 * ourselves. Otherwise, the style is set to SWT.NULL, and borders are
	 * painted by the toolkit.
	 * 
	 * @return the global border style
	 */
	public int getBorderStyle() {
		return borderStyle;
	}
	private void initialize() {
		String osname = System.getProperty("os.name");
		if (osname.equals("Windows XP"))
			borderStyle = SWT.BORDER;
		hyperlinkGroup = new HyperlinkGroup(colors.getDisplay());
		hyperlinkGroup.setBackground(colors.getBackground());
		visibilityHandler = new VisibilityHandler();
		keyboardHandler = new KeyboardHandler();
	}
}