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
package org.eclipse.core.runtime.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.core.internal.runtime.Policy;
import org.eclipse.core.runtime.*;

/**
 * A content describer for binary formats that present some 
 * simple signature at a known, fixed offset.
 * <p>
 * This executable extension supports three parameters:
 * "signature", "offset" and "required", the first one being mandatory. 
 * If the
 * <code>":-"</code> method is used, then the value is treated as the
 * "signature".
 * </p> 
 * <p>
 * This class is not intended to be subclassed or instantiated by clients, 
 * only to be referenced in the "describer" configuration element of
 * extensions to the <code>org.eclipse.core.runtime.contentTypes</code>
 * extension-pont.
 * </p>
 * 
 * @since 3.0
 */
public final class BinarySignatureDescriber implements IContentDescriber, IExecutableExtension {
	private final static String SIGNATURE = "signature"; //$NON-NLS-1$
	private final static String OFFSET = "offset"; //$NON-NLS-1$
	private static final Object REQUIRED = "required"; //$NON-NLS-1$

	private byte[] signature;
	private int offset;
	private boolean required = true;

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#describe(InputStream, IContentDescription)
	 */
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		byte[] buffer = new byte[signature.length];
		int notValid = required ? INVALID : INDETERMINATE;
		if (contents.skip(offset) < offset)
			return notValid;
		if (contents.read(buffer) != buffer.length)
			return notValid;
		for (int i = 0; i < signature.length; i++)
			if (signature[i] != buffer[i])
				return notValid;
		return VALID;
	}

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#getSupportedOptions
	 */
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

	/* (Intentionally not included in javadoc)
	 * @see IExecutableExtension#setInitializationData
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		try {
			if (data instanceof String)
				signature = parseSignature((String) data);
			else if (data instanceof Hashtable) {
				Hashtable parameters = (Hashtable) data;
				if (!parameters.containsKey(SIGNATURE)) {
					String message = Policy.bind("content.badInitializationData", XMLRootElementContentDescriber.class.getName()); //$NON-NLS-1$
					throw new CoreException(new Status(IStatus.ERROR, Platform.PI_RUNTIME, 0, message, null));
				}
				signature = parseSignature((String) parameters.get(SIGNATURE));
				if (parameters.containsKey(OFFSET))
					offset = Integer.parseInt((String) parameters.get(OFFSET));
				if (parameters.containsKey(REQUIRED))
					required = Boolean.valueOf((String) parameters.get(REQUIRED)).booleanValue();
			}
		} catch (NumberFormatException nfe) {
			String message = Policy.bind("content.badInitializationData", XMLRootElementContentDescriber.class.getName()); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, Platform.PI_RUNTIME, 0, message, nfe));
		}
	}

	private byte[] parseSignature(String data) {
		List bytes = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(data, " \t\n\r\f,"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			bytes.add(new Byte(Byte.parseByte(tokenizer.nextToken().trim(), 16)));
		byte[] signature = new byte[bytes.size()];
		for (int i = 0; i < signature.length; i++)
			signature[i] = ((Byte) bytes.get(i)).byteValue();
		return signature;
	}
}