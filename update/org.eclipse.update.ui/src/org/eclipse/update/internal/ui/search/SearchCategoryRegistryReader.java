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
package org.eclipse.update.internal.ui.search;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;

public class SearchCategoryRegistryReader {
	private ArrayList descriptors;

	static SearchCategoryRegistryReader instance;

	SearchCategoryRegistryReader() {
	}

	public static SearchCategoryRegistryReader getDefault() {
		if (instance == null)
			instance = new SearchCategoryRegistryReader();
		return instance;
	}

	public SearchCategoryDescriptor[] getCategoryDescriptors() {
		if (descriptors == null)
			load();
		return (SearchCategoryDescriptor[]) descriptors.toArray(
			new SearchCategoryDescriptor[descriptors.size()]);
	}

	public SearchCategoryDescriptor getDescriptor(String categoryId) {
		if (descriptors == null)
			load();
		for (int i = 0; i < descriptors.size(); i++) {
			SearchCategoryDescriptor desc = (SearchCategoryDescriptor) descriptors.get(i);
			if (desc.getId().equals(categoryId))
				return desc;
		}
		return null;
	}

	private void load() {
		descriptors = new ArrayList();
		IPluginRegistry registry = Platform.getPluginRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor("org.eclipse.update.ui.searchCategory");
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("search"))
				descriptors.add(new SearchCategoryDescriptor(element));
		}
	}
}