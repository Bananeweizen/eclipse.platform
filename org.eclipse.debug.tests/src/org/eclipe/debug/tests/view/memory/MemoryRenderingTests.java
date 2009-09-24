/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.view.memory;

import junit.framework.TestCase;

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;

/**
 * Tests memory rendering manager
 */
public class MemoryRenderingTests extends TestCase {
	
	public MemoryRenderingTests(String name) {
		super(name);
	}

	public void testRenderingTypes() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryRenderingType[] types = manager.getRenderingTypes();
		assertTrue("Wrong number of rendering types contributed", types.length > 6);
		assertTrue("Missing type 1", indexOf(manager.getRenderingType("rendering_type_1"), types) >= 0);
        assertTrue("Missing type 2", indexOf(manager.getRenderingType("rendering_type_2"), types) >= 0);
        assertTrue("Missing type 3", indexOf(manager.getRenderingType("rendering_type_3"), types) >= 0);
        assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.raw_memory"), types) >= 0);
        assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.ascii"), types) >= 0);
        assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.signedint"), types) >= 0);
        assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.unsignedint"), types) >= 0);
	}
	
	public void testRenderingTypeNames() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryRenderingType type = manager.getRenderingType("rendering_type_1");
		assertEquals("Wrong name", "Rendering One", type.getLabel());
		type = manager.getRenderingType("rendering_type_2");
		assertEquals("Wrong name", "Rendering Two", type.getLabel());
	}

	public void testSingleBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockOne();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length);
		assertEquals("Wrong binding", "rendering_type_1", types[0].getId());
	}
	
	public void testDoubleBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockTwo();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);
		assertEquals("Wrong number of bindings", 2, types.length);
		assertTrue("Missing binding", indexOf(manager.getRenderingType("rendering_type_1"), types) >= 0);
		assertTrue("Missing binding", indexOf(manager.getRenderingType("rendering_type_2"), types) >= 0);
	}	
    
    public void testDefaultBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockOne();
        IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
        assertEquals("Wrong number of bindings", 1, types.length);
        assertEquals("Wrong binding", "rendering_type_1", types[0].getId());
    }
	
    public void testNoDefaultBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockTwo();
        IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
        assertEquals("Wrong number of bindings", 0, types.length);
    }
    
    public void testPrimaryBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockOne();
        IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
        assertEquals("Wrong binding", "rendering_type_1", type.getId());
    }
    
    public void testNoPrimaryBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockTwo();
        IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
        assertNull("Wrong binding", type);
    }
    
    public void testDefaultWithoutPrimaryBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockThree();
        IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
        assertEquals("Wrong number of bindings", 1, types.length);
        assertEquals("Wrong binding", "rendering_type_3", types[0].getId());
    }
    
    public void testDynamicBinding() {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryBlock block = new MemoryBlockDynamic();
        IMemoryRenderingType[] types = manager.getRenderingTypes(block);
        assertEquals("Wrong number of bindings", 1, types.length);
        assertEquals("Wrong binding", "rendering_type_1", types[0].getId());
        types = manager.getDefaultRenderingTypes(block);
        assertEquals("Wrong number of bindings", 1, types.length);
        assertEquals("Wrong binding", "rendering_type_1", types[0].getId());
        IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
        assertEquals("Wrong bindings", manager.getRenderingType("rendering_type_1"), type);
    }
	
	public void testBindingChangeNotification() {
		final boolean[] changed = new boolean[1];
		IMemoryRenderingBindingsListener listener = new IMemoryRenderingBindingsListener() {
			public void memoryRenderingBindingsChanged() {
				changed[0] = true;
			}		
		};
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		try {
			manager.addListener(listener);
			assertFalse("Renderings should not have changed yet", changed[0]);
			DynamicRenderingBindings.setBinding("rendering_type_2");
			assertTrue("Renderings should have changed", changed[0]);
			IMemoryBlock block = new MemoryBlockDynamic();
	        IMemoryRenderingType[] types = manager.getRenderingTypes(block);
			assertEquals("Wrong number of bindings", 1, types.length);
	        assertEquals("Wrong binding", "rendering_type_2", types[0].getId());
		} finally {
			// restore original bindings
			DynamicRenderingBindings.setBinding("rendering_type_1");
			manager.removeListener(listener);
		}
	}
    
	protected int indexOf(Object thing, Object[] list) {
		for (int i = 0; i < list.length; i++) {
			Object object2 = list[i];
			if (object2.equals(thing)) {
				return i;
			}
		}
		return -1;
	}
}
