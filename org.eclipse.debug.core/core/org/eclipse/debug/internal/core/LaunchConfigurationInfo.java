package org.eclipse.debug.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
 
/**
 * The information associated with a launch configuration
 * handle.
 */
public class LaunchConfigurationInfo {

	/**
	 * This configurations attribute table.
	 * Keys are <code>String</code>s and values
	 * are one of <code>String</code>, <code>Integer</code>,
	 * or <code>Boolean</code>.
	 */
	private HashMap fAttributes;
	
	/**
	 * This launch configuration's type
	 */
	private ILaunchConfigurationType fType;
	
	/**
	 * Constructs a new empty info
	 */
	protected LaunchConfigurationInfo() {
		setAttributeTable(new HashMap(10));
	}
	
	/**
	 * Returns this configuration's attribute table.
	 * 
	 * @return attribute table
	 */
	private HashMap getAttributeTable() {
		return fAttributes;
	}

	/**
	 * Sets this configuration's attribute table.
	 * 
	 * @param table attribute table
	 */	
	private void setAttributeTable(HashMap table) {
		fAttributes = table;
	}
	
	/**
	 * Returns the <code>String</code> attribute with the
	 * given key or the given default value if undefined.
	 * 
	 * @return attribute specified by given key or the defaultValue
	 *  if undefined
	 * @exception if the attribute with the given key exists
	 *  but is not a <code>String</code>
	 */
	protected String getStringAttribute(String key, String defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof String) {
				return (String)attr;
			} else {
				throw new DebugException(
					new Status(
					 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.getString("LaunchConfigurationInfo.Attribute_{0}_is_not_of_type_java.lang.String._1"), new String[] {key}), null //$NON-NLS-1$
					)
				);
			}
		}
		return defaultValue;
	}
	
	/**
	 * Returns the <code>int</code> attribute with the
	 * given key or the given default value if undefined.
	 * 
	 * @return attribute specified by given key or the defaultValue
	 *  if undefined
	 * @exception if the attribute with the given key exists
	 *  but is not an <code>int</code>
	 */
	protected int getIntAttribute(String key, int defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Integer) {
				return ((Integer)attr).intValue();
			} else {
				throw new DebugException(
					new Status(
					 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.getString("LaunchConfigurationInfo.Attribute_{0}_is_not_of_type_int._2"), new String[] {key}), null //$NON-NLS-1$
					)
				);
			}
		}
		return defaultValue;
	}
	
	/**
	 * Returns the <code>boolean</code> attribute with the
	 * given key or the given default value if undefined.
	 * 
	 * @return attribute specified by given key or the defaultValue
	 *  if undefined
	 * @exception if the attribute with the given key exists
	 *  but is not a <code>boolean</code>
	 */
	protected boolean getBooleanAttribute(String key, boolean defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Boolean) {
				return ((Boolean)attr).booleanValue();
			} else {
				throw new DebugException(
					new Status(
					 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.getString("LaunchConfigurationInfo.Attribute_{0}_is_not_of_type_boolean._3"), new String[] {key}), null //$NON-NLS-1$
					)
				);
			}
		}
		return defaultValue;
	}
	
	/**
	 * Returns the <code>java.util.List</code> attribute with the
	 * given key or the given default value if undefined.
	 * 
	 * @return attribute specified by given key or the defaultValue
	 *  if undefined
	 * @exception if the attribute with the given key exists
	 *  but is not a <code>java.util.List</code>
	 */
	protected List getListAttribute(String key, List defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof List) {
				return (List)attr;
			} else {
				throw new DebugException(
					new Status(
					 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.getString("LaunchConfigurationInfo.Attribute_{0}_is_not_of_type_java.util.List._1"), new String[] {key}), null //$NON-NLS-1$
					)
				);
			}
		}
		return defaultValue;
	}
	
	/**
	 * Returns the <code>java.util.Map</code> attribute with the
	 * given key or the given default value if undefined.
	 * 
	 * @return attribute specified by given key or the defaultValue
	 *  if undefined
	 * @exception if the attribute with the given key exists
	 *  but is not a <code>java.util.Map</code>
	 */
	protected Map getMapAttribute(String key, Map defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Map) {
				return (Map)attr;
			} else {
				throw new DebugException(
					new Status(
					 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
					 DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.getString("LaunchConfigurationInfo.Attribute_{0}_is_not_of_type_java.util.Map._1"), new String[] {key}), null //$NON-NLS-1$
					)
				);
			}
		}
		return defaultValue;
	}
	
	/** 
	 * Sets this configuration's type.
	 * 
	 * @param type launch configuration type
	 */
	protected void setType(ILaunchConfigurationType type) {
		fType = type;
	}
	
	/** 
	 * Returns this configuration's type.
	 * 
	 * @return launch configuration type
	 */
	protected ILaunchConfigurationType getType() {
		return fType;
	}	
	
	
	/**
	 * Returns a copy of this info object
	 * 
	 * @return copy of this info
	 */
	protected LaunchConfigurationInfo getCopy() {
		LaunchConfigurationInfo copy = new LaunchConfigurationInfo();
		copy.setType(getType());
		copy.setAttributeTable((HashMap)getAttributeTable().clone());
		return copy;
	}
	
	/**
	 * Sets the given attribute to the given value. Only
	 * working copy's should use this API.
	 * 
	 * @param key attribute key
	 * @param value attribuet value
	 */
	protected void setAttribute(String key, Object value) {
		getAttributeTable().put(key, value);
	}
	
	/**
	 * Returns the content of this info as XML
	 * 
	 * @return the content of this info as XML
	 * @exception IOException if an exception occurrs creating the XML
	 */
	protected String getAsXML() throws IOException {

		Document doc = new DocumentImpl();
		Element configRootElement = doc.createElement("launchConfiguration"); //$NON-NLS-1$
		doc.appendChild(configRootElement);
		
		configRootElement.setAttribute("type", getType().getIdentifier()); //$NON-NLS-1$
		
		Iterator keys = getAttributeTable().keySet().iterator();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			Object value = getAttributeTable().get(key);
			if (value == null) {
				continue;
			}
			Element element = null;
			String valueString = null;
			if (value instanceof String) {
				valueString = (String)value;
				element = createKeyValueElement(doc, "stringAttribute", key, valueString); //$NON-NLS-1$
			} else if (value instanceof Integer) {
				valueString = ((Integer)value).toString();
				element = createKeyValueElement(doc, "intAttribute", key, valueString); //$NON-NLS-1$
			} else if (value instanceof Boolean) {
				valueString = ((Boolean)value).toString();
				element = createKeyValueElement(doc, "booleanAttribute", key, valueString); //$NON-NLS-1$
			} else if (value instanceof List) {				
				element = createListElement(doc, "listAttribute", key, (List)value); //$NON-NLS-1$
			} else if (value instanceof Map) {				
				element = createMapElement(doc, "mapAttribute", key, (Map)value); //$NON-NLS-1$
			}			
			configRootElement.appendChild(element);
		}

		// produce a String output
		StringWriter writer = new StringWriter();
		OutputFormat format = new OutputFormat();
		format.setIndenting(true);
		Serializer serializer =
			SerializerFactory.getSerializerFactory(Method.XML).makeSerializer(
				writer,
				format);
		serializer.asDOMSerializer().serialize(doc);
		return writer.toString();
			
	}
	
	/**
	 * Helper method that creates a 'key value' element of the specified type with the 
	 * specified attribute values.
	 */
	protected Element createKeyValueElement(Document doc, String elementType, String key, String value) {
		Element element = doc.createElement(elementType);
		element.setAttribute("key", key);
		element.setAttribute("value", value);
		return element;
	}
	
	protected Element createListElement(Document doc, String elementType, String listKey, List list) {
		Element listElement = doc.createElement(elementType);
		listElement.setAttribute("key", listKey);
		Iterator iterator = list.iterator();
		while (iterator.hasNext()) {
			String value = (String) iterator.next();
			Element element = doc.createElement("listEntry");
			element.setAttribute("value", value);
			listElement.appendChild(element);
		}		
		return listElement;
	}
	
	protected Element createMapElement(Document doc, String elementType, String mapKey, Map map) {
		Element mapElement = doc.createElement(elementType);
		mapElement.setAttribute("key", mapKey);
		Iterator iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			String value = (String) map.get(key);
			Element element = doc.createElement("mapEntry");
			element.setAttribute("key", key);
			element.setAttribute("value", value);
			mapElement.appendChild(element);
		}		
		return mapElement;		
	}
	
	protected void initializeFromXML(Element root) throws CoreException {
		if (!root.getNodeName().equalsIgnoreCase("launchConfiguration")) { //$NON-NLS-1$
			throw getInvalidFormatDebugException();
		}
		
		// read type
		String id = root.getAttribute("type"); //$NON-NLS-1$
		if (id == null) {
			throw getInvalidFormatDebugException();
		} else {
			ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(id);
			if (type == null) {
				throw getInvalidFormatDebugException();
			}
			setType(type);
		}
		
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		for (int i = 0; i < length; ++i) {
			Node node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				
				if (nodeName.equalsIgnoreCase("stringAttribute")) { //$NON-NLS-1$
					setStringAttribute(element);
				} else if (nodeName.equalsIgnoreCase("intAttribute")) { //$NON-NLS-1$
					setIntegerAttribute(element);
				} else if (nodeName.equalsIgnoreCase("booleanAttribute"))  { //$NON-NLS-1$
					setBooleanAttribute(element);
				} else if (nodeName.equalsIgnoreCase("listAttribute")) {   //$NON-NLS-1$
					setListAttribute(element);					
				} else if (nodeName.equalsIgnoreCase("mapAttribute")) {    //$NON-NLS-1$
					setMapAttribute(element);										
				}
			}
		}
	}	
	
	protected void setStringAttribute(Element element) throws CoreException {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, value);
	}
	
	protected void setIntegerAttribute(Element element) throws CoreException {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, new Integer(value));
	}
	
	protected void setBooleanAttribute(Element element) throws CoreException {
		String key = getKeyAttribute(element);
		String value = getValueAttribute(element);
		setAttribute(key, new Boolean(value));
	}
	
	protected void setListAttribute(Element element) throws CoreException {
		String listKey = element.getAttribute("key");  //$NON-NLS-1$
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		List list = new ArrayList(entryCount);
		for (int i = 0; i < entryCount; i++) {
			Node node = nodeList.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element subElement = (Element) node;
				String nodeName = subElement.getNodeName();				
				if (!nodeName.equalsIgnoreCase("listEntry")) { //$NON-NLS-1$
					throw getInvalidFormatDebugException();
				}
				String value = getValueAttribute(subElement);
				list.add(value);
			}
		}
		setAttribute(listKey, list);
	}
		
	protected void setMapAttribute(Element element) throws CoreException {
		String mapKey = element.getAttribute("key");  //$NON-NLS-1$
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		Map map = new HashMap(entryCount);
		for (int i = 0; i < entryCount; i++) {
			Node node = nodeList.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element subElement = (Element) node;
				String nodeName = subElement.getNodeName();				
				if (!nodeName.equalsIgnoreCase("mapEntry")) { //$NON-NLS-1$
					throw getInvalidFormatDebugException();
				}
				String key = getKeyAttribute(subElement);
				String value = getValueAttribute(subElement);
				map.put(key, value);
			}
		}
		setAttribute(mapKey, map);
	}
		
	protected String getKeyAttribute(Element element) throws CoreException {
		String key = element.getAttribute("key");   //$NON-NLS-1$
		if (key == null) {
			throw getInvalidFormatDebugException();
		}
		return key;
	}
	
	protected String getValueAttribute(Element element) throws CoreException {
		String value = element.getAttribute("value");   //$NON-NLS-1$
		if (value == null) {
			throw getInvalidFormatDebugException();
		}
		return value;
	}
	
	protected DebugException getInvalidFormatDebugException() {
		return 
			new DebugException(
				new Status(
				 Status.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
				 DebugException.REQUEST_FAILED, DebugCoreMessages.getString("LaunchConfigurationInfo.Invalid_launch_configuration_XML._10"), null //$NON-NLS-1$
				)
			);
	}
	
	/**
	 * Two <code>LaunchConfigurationInfo</code> objects are equal if and only if they have the
	 * same type and they have the same set of attributes with the same values.
	 * 
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		
		// Make sure it's a LaunchConfigurationInfo object
		if (!(obj instanceof LaunchConfigurationInfo)) {
			return false;
		}
		
		// Make sure the types are the same
		LaunchConfigurationInfo other = (LaunchConfigurationInfo) obj;
		if (!fType.getIdentifier().equals(other.getType().getIdentifier())) {
			return false;
		}
		
		// Make sure the attributes are the same
		if (!fAttributes.equals(other.getAttributeTable())) {
			return false;
		}
		
		// They're equal
		return true;
	}
	
}

