/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.content;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

public final class ContentDescription implements IContentDescription {
	private static final byte ALL_OPTIONS = 0x01;
	private static final byte IMMUTABLE = 0x02;
	private IContentType contentType;
	private byte flags;
	private Object keys;
	private Object values;

	public ContentDescription(QualifiedName[] requested) {
		if (requested == IContentDescription.ALL) {
			flags |= ALL_OPTIONS;
			return;
		}
		if (requested.length > 1) {
			keys = requested;
			values = new Object[requested.length];
		} else if (requested.length == 1)
			keys = requested[0];
		// if requested.length == 0 then keys == null (no options to describe!)
	}

	private void assertMutable() {
		if ((flags & IMMUTABLE) != 0)
			throw new IllegalStateException("Content description is immutable"); //$NON-NLS-1$
	}

	/**
	 * @see IContentDescription
	 */
	public IContentType getContentType() {
		return contentType;
	}

	/**
	 * @see IContentDescription
	 */
	public Object getProperty(QualifiedName key) {
		// no values have been set
		if (values == null)
			return null;
		// a single property may have been set
		if (keys instanceof QualifiedName)
			return keys.equals(key) ? values : null;
		// multiple properties may have been set
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (int i = 0; i < tmpKeys.length; i++)
			if (tmpKeys[i].equals(key))
				return ((Object[]) values)[i];
		return null;
	}

	/**
	 * @see IContentDescription
	 */
	public boolean isRequested(QualifiedName propertyKey) {
		// all options requested
		if ((flags & ALL_OPTIONS) != 0)
			return true;
		// no options requested
		if (keys == null)
			return false;
		// a single option requested
		if (keys instanceof QualifiedName)
			return keys.equals(propertyKey);
		// some (but not all) options requested
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (int i = 0; i < tmpKeys.length; i++)
			if (tmpKeys[i].equals(propertyKey))
				return true;
		return false;
	}

	boolean isSet() {
		if (keys == null || values == null)
			return false;
		if (keys instanceof QualifiedName)
			return true;
		Object[] tmpValues = (Object[]) this.values;
		for (int i = 0; i < tmpValues.length; i++)
			if (tmpValues[i] != null)
				return true;
		return false;
	}

	public void markImmutable() {
		assertMutable();
		flags |= IMMUTABLE;
	}

	void setContentType(IContentType contentType) {
		markImmutable();
		this.contentType = contentType;
	}

	/**
	 * @see IContentDescription
	 */
	public void setProperty(QualifiedName newKey, Object newValue) {
		assertMutable();
		if (keys == null) {
			if ((flags & ALL_OPTIONS) != 0) {
				keys = newKey;
				values = newValue;
			}
			return;
		}
		if (keys.equals(newKey)) {
			values = newValue;
			return;
		}
		if (keys instanceof QualifiedName) {
			if ((flags & ALL_OPTIONS) != 0) {
				keys = new QualifiedName[] {(QualifiedName) keys, newKey};
				values = new Object[] {values, newValue};
			}
			return;
		}
		QualifiedName[] tmpKeys = (QualifiedName[]) this.keys;
		for (int i = 0; i < tmpKeys.length; i++)
			if (tmpKeys[i].equals(newKey)) {
				((Object[]) values)[i] = newValue;
				return;
			}
		if ((flags & ALL_OPTIONS) == 0)
			return;
		// need to resize arrays 		
		int currentSize = tmpKeys.length;
		tmpKeys = new QualifiedName[currentSize + 1];
		System.arraycopy(keys, 0, tmpKeys, 0, currentSize);
		Object[] tmpValues = new Object[currentSize + 1];
		System.arraycopy(values, 0, tmpValues, 0, currentSize);
		tmpKeys[tmpKeys.length - 1] = newKey;
		tmpValues[tmpValues.length - 1] = newValue;
		keys = tmpKeys;
		values = tmpValues;
	}

	public String toString() {
		StringBuffer result = new StringBuffer("{"); //$NON-NLS-1$
		if (keys != null)
			if (keys instanceof QualifiedName)
				if (values != null)
					result.append(keys + "=" + values); //$NON-NLS-1$
				else
					;
			else {
				QualifiedName[] tmpKeys = (QualifiedName[]) keys;
				Object[] tmpValues = (Object[]) values;
				boolean any = false;
				for (int i = 0; i < tmpKeys.length; i++)
					if (tmpValues[i] != null) {
						result.append(tmpKeys[i] + "=" + tmpValues[i] + ","); //$NON-NLS-1$ //$NON-NLS-2$
						any = true;
					}
				if (any)
					result.deleteCharAt(result.length() - 1);
			}
		result.append("} : "); //$NON-NLS-1$
		result.append(contentType);
		return result.toString();
	}
}