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
package org.eclipse.core.tests.internal.plugin.a;

import org.eclipse.core.runtime.*;

public class ExtensionDirectCompilerReferenceToPluginClass extends ConfigurableExtension {

public Object run(Object o) {	
	super.run(o);
	// make direct compiler reference to other plugin class
	Plugin p = org.eclipse.core.tests.internal.plugin.b.PluginClass.plugin;
	return p;
}
}
