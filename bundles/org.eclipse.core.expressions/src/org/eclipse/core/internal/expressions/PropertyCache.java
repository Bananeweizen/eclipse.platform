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
package org.eclipse.core.internal.expressions;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/* package */ class PropertyCache {
	
	private LinkedHashMap fCache;
	
	public PropertyCache(final int cacheSize) {
		// start with 100 elements but be able to grow until cacheSize
		fCache= new LinkedHashMap(100, 0.75f, true) {
			protected boolean removeEldestEntry(Entry eldest) {
				return size() > cacheSize;
			}
		};
	}
	
	public Property get(Property key) {
		synchronized (fCache) {
			return (Property)fCache.get(key);
		}
	}
	
	public void put(Property method) {
		synchronized (fCache) {
			fCache.put(method, method);
		}
	}
	
	public void remove(Property method) {
		synchronized(fCache) {
			fCache.remove(method);
		}
	}
}
