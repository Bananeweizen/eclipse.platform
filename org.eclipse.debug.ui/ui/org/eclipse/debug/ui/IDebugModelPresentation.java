package org.eclipse.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

/**
 * A debug model presentation is responsible for providing labels, images,
 * and editors associated with debug elements in a specific debug model.
 * Extensions of type <code>org.eclipse.debug.ui.debugModelPresentations</code> implement
 * this interface. Generally, a debug model implementation will also provide a
 * debug model presentation extension to render and display its elements. A debug
 * model presentation is registered for a specific debug model, and is responsible
 * for the presentation of the following kinds of elements defined by that model:
 * <ul>
 * <li>debug targets</li>
 * <li>threads</li>
 * <li>stack frames</li>
 * <li>variables</li>
 * <li>values</li>
 * <li>breakpoint markers</li>
 * </ul>
 * <p>
 * A debug model presentation extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a debug model presentation extension.
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.debugModelPresentations"&gt;
 *   &lt;debugModelPresentation 
 *      id="com.example.debugModelIdentifier"
 *      class="com.example.ExmaplePresentation"
 *   &lt;/debugModelPresentation&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies the identifier of the debug model this presentation
 *    is responsible for. Corresponds to the model identifier returned from a debug
 *	element - see <code>IDebugElement.getModelIndentifier</code></li>
 * <li><code>class</code> specifies the fully qualified name of the Java class
 *   that implements this interface.</li>
 * </ul>
 * </p>
 * <p>
 * To allow for an extensible configuration, this interface defines
 * a <code>setAttribute</code> method. The debug UI plug-in defines
 * two presentation attributes:
 * <ul>
 *  <li><code>DISPLAY_QUALIFIED_NAMES</code> - This is a boolean attribute 
 *     indicating whether elements should be rendered with fully qualified names.
 *     For example, a Java debug model presentation would include package names
 *     when this attribute is true.</li>
 *  <li><code>DISPLAY_VARIABLE_TYPE_NAMES</code> - This is a boolean attribute 
 *     indicating whether variable elements should be rendered with the declared
 *     type of a variable. For example, a Java debug model presentation would render
 *     an integer as <code>"int x = 3"</code> when true, and <code>"x = 3"</code>
 *     when false.</li>
 * </ul>
 * </p>
 * <p>
 * Clients may define new presentation attributes. For example, a client may wish
 * to define a "hexidecimal" property to display numeric values in hexidecimal. Implementations
 * should honor the presentation attributes defined by this interface where possible,
 * but do not need to honor presentation attributes defined by other clients.
 * To access the debug model presentation for a debug view, clients should use the
 * <code>IDebugViewAdapter</code>.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.debug.core.model.IDebugElement
 * @see ILabelProvider
 * @see IDebugViewAdapter
 */

public interface IDebugModelPresentation extends ILabelProvider {
	/**
	 * Qualified names presentation property (value <code>"org.eclipse.debug.ui.displayQualifiedNames"</code>).
	 * When <code>DISPLAY_QUALIFIED_NAMES</code> is set to <code>True</code>,
	 * this label provider should use fully qualified type names when rendering elements.
	 * When set to <code>False</code>,this label provider should use simple names
	 * when rendering elements.
	 * @see #setAttribute(String, Object)
	 */
	public final static String DISPLAY_QUALIFIED_NAMES= IDebugUIConstants.PLUGIN_ID + ".displayQualifiedNames"; //$NON-NLS-1$
	/** 
	 * Variable type names presentation property (value <code>"org.eclipse.debug.ui.displayVariableTypeNames"</code>).
	 * When <code>DISPLAY_VARIABLE_TYPE_NAMES</code> is set to <code>True</code>,
	 * this label provider should include the reference type of a variable  when rendering
	 * variables. When set to <code>False</code>, this label provider 
	 * should not include the reference type of a variable when rendering
	 * variables.
	 * @see #setAttribute(String, Object)
	 */
	public final static String DISPLAY_VARIABLE_TYPE_NAMES= IDebugUIConstants.PLUGIN_ID + ".displayVariableTypeNames"; //$NON-NLS-1$
	/**
	 * Sets a presentation attribute of this label provider. For example,
	 * see the presentation attribute <code>DISPLAY_QUALIFIED_NAMES</code>
	 * defined by this interface.
	 *
	 * @param attribute the presentation attribute identifier
	 * @param value the value of the attribute
	 */
	void setAttribute(String attribute, Object value);
	/**
	 * Returns the image for the label of the given element. If an implementation
	 * of <code>IDebugModelPresentation</code> returns <code>null</code>, the debug
	 * UI will provide a default image for the given element.
	 *
	 * @param element the debug model element
	 * @return an image for the element, or <code>null</code> if a default
	 *    image should be used
	 * @see ILabelProvider
	 */
	public Image getImage(Object element);
	/**
	 * Returns the text for the label of the given element. If an implementation
	 * of <code>IDebugModelPresentation</code> returns <code>null</code>, the debug
	 * UI will provide a default text for the given element.
	 *
	 * @param element the debug model element
	 * @return a label for the element, or <code>null</code> if a default
	 *    label should be used
	 * @see ILabelProvider
	 */
	public String getText(Object element);
	/**
	 * Returns an editor input that should be used to display the given object
	 * in an editor or <code>null</code> if unable to provide an editor input
	 * for the given object.
	 *
	 * @param element a debug model object
	 * @return an editor input, or <code>null</code> if none
	 */
	IEditorInput getEditorInput(Object element);
	
	/**
	 * Returns the id of the editor to use to display the
	 * given editor input and object, or <code>null</code> if
	 * unable to provide an editor id.
	 *
	 * @param input an editor input that was previously retrieved from this
	 *    debug model presentation's <code>getEditorInput</code> method
	 * @param element the debug model object that was used in the call to
	 *    <code>getEditorInput</code>, that corresponds to the given input
	 * @return an editor id, or <code>null</code> if none
	 */
	String getEditorId(IEditorInput input, Object element);
	
	/**
	 * Computes a detailed description of the given value, reporting
	 * the result to the specified listener. This allows a presentation
	 * to provide extra details about a selected value in the variable detail
	 * portion of the variable view. Since this can be a long-running operation,
	 * the details are reported back to the specified listener asynchronously.
	 * If <code>null</code> is reported, the value's value string is displayed
	 * (<code>IValue.getValueString()</code>).
	 * 
	 * @param value the value for which a detailed description
	 * 	is required
	 */
	void computeDetail(IValue value, IValueDetailListener listener);	

}
