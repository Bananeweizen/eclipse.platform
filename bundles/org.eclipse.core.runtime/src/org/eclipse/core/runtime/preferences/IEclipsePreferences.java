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
package org.eclipse.core.runtime.preferences;

import java.util.EventObject;
import org.eclipse.core.runtime.IPath;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This interface describes Eclipse extensions to the preference
 * story. It provides means for both preference and node change
 * listeners as well as providing node navigation APIs which deal
 * in <code>IPath</code> objects.
 * 
 * @see org.osgi.service.prefs.Preferences
 * @since 3.0
 */
public interface IEclipsePreferences extends Preferences {

	/**
	 * An event object which describes the details of a change in the 
	 * preference node hierarchy. The child node is the one which 
	 * was added or removed.
	 * 
	 * @see INodeChangeListener
	 * @since 3.0
	 */
	public final class NodeChangeEvent extends EventObject {

		private Preferences child;

		/**
		 * Constructor for a new node change event object.
		 * 
		 * @param parent
		 * @param child
		 */
		public NodeChangeEvent(Preferences parent, Preferences child) {
			super(parent);
			this.child = child;
		}

		/**
		 * Return the parent node for this event. This is the parent
		 * of the node which was added or removed.
		 * 
		 * @return the parent node
		 */
		public Preferences getParent() {
			return (Preferences) getSource();
		}

		/**
		 * Return the child node for this event. This is the node
		 * which was added or removed.
		 * 
		 * @return the child node
		 */
		public Preferences getChild() {
			return child;
		}
	}

	/**
	 * A listener to be used to receive preference node change events.
	 * 
	 * @since 3.0
	 */
	public interface INodeChangeListener {

		/**
		 * Notification that a child node was added to the preference hierarchy.
		 * The given event must not be <code>null</code>.
		 * 
		 * @param event an event specifying the details about the new node
		 * @see NodeChangeEvent
		 * @see IEclipsePreferences#addNodeChangeListener(INodeChangeListener)
		 * @see IEclipsePreferences#removeNodeChangeListener(INodeChangeListener)
		 */
		public void added(NodeChangeEvent event);

		/**
		 * Notification that a child node was removed from the preference hierarchy.
		 * The given event must not be <code>null</code>.
		 * 
		 * @param event an event specifying the details about the removed node
		 * @see NodeChangeEvent
		 * @see IEclipsePreferences#addNodeChangeListener(INodeChangeListener)
		 * @see IEclipsePreferences#removeNodeChangeListener(INodeChangeListener)
		 */
		public void removed(NodeChangeEvent event);
	}

	/**
	 * An event object describing the details of a change to a preference
	 * in the preference store.
	 * 
	 * @see IPreferenceChangeListener
	 * @since 3.0
	 */
	public final class PreferenceChangeEvent extends EventObject {

		private String key;
		private Object newValue;
		private Object oldValue;

		/**
		 * Constructor for a new preference change event. The node and the
		 * key must not be <code>null</code>.
		 * 
		 * @param node the node on which the change occurred
		 * @param key the preference key
		 * @param oldValue the old preference value
		 * @param newValue the new preference value
		 */
		public PreferenceChangeEvent(Object node, String key, Object oldValue, Object newValue) {
			super(node);
			if (key == null || !(node instanceof Preferences))
				throw new IllegalArgumentException();
			this.key = key;
			this.newValue = newValue;
			this.oldValue = oldValue;
		}

		/**
		 * Return the preference node on which the change occurred.
		 * 
		 * @return the node
		 */
		public Preferences getNode() {
			return (Preferences) source;
		}

		/**
		 * Return the key of the preference which was changed.
		 * 
		 * @return the preference key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Return the new value for the preference or <code>null</code> if 
		 * the preference was removed.
		 * 
		 * @return the new value or <code>null</code>
		 */
		public Object getNewValue() {
			return newValue;
		}

		/**
		 * Return the old value for the preference or <code>null</code> if 
		 * the preference was removed or if it cannot be determined.
		 * 
		 * @return the old value or <code>null</code>
		 */
		public Object getOldValue() {
			return oldValue;
		}
	}

	/**
	 * A listener used to receive changes to preference values in the preference store.
	 * 
	 * @since 3.0
	 */
	public interface IPreferenceChangeListener {

		/**
		 * Notification that a preference value has changed in the preference store.
		 * The given event object describes the change details and must not
		 * be <code>null</code>.
		 * 
		 * @param event the event details
		 * @see PreferenceChangeEvent
		 * @see IEclipsePreferences#addPreferenceChangeListener(IPreferenceChangeListener)
		 * @see IEclipsePreferences#removePreferenceChangeListener(IPreferenceChangeListener)
		 */
		public void preferenceChange(PreferenceChangeEvent event);
	}

	/**
	 * Register the given listener for changes to this node. Duplicate calls
	 * to this method with the same listener will have no effect. The given
	 * listener argument must not be <code>null</code>.
	 *  
	 * @param listener the node change listener to add
	 * @throws IllegalStateException if this node or an ancestor has been removed
	 * @see #removeNodeChangeListener(INodeChangeListener)
	 * @see INodeChangeListener
	 */
	public void addNodeChangeListener(INodeChangeListener listener);

	/**
	 * De-register the given listener from receiving event change notifications
	 * for this node. Calling this method with a listener which is not registered
	 * has no effect. The given listener argument must not be <code>null</code>.
	 * 
	 * @param listener the node change listener to remove
	 * @throws IllegalStateException if this node or an ancestor has been removed
	 * @see #addNodeChangeListener(INodeChangeListener)
	 * @see INodeChangeListener
	 */
	public void removeNodeChangeListener(INodeChangeListener listener);

	/**
	 * Register the given listener for notification of preference changes to this node.
	 * Calling this method multiple times with the same listener has no effect. The
	 * given listener argument must not be <code>null</code>.
	 * 
	 * @param listener the preference change listener to register
	 * @throws IllegalStateException if this node or an ancestor has been removed
	 * @see #removePreferenceChangeListener(IPreferenceChangeListener)
	 * @see IPreferenceChangeListener
	 */
	public void addPreferenceChangeListener(IPreferenceChangeListener listener);

	/**
	 * De-register the given listner from receiving notification of preference changes
	 * to this node. Calling this method multiple times with the same listener has no
	 * effect. The given listener argument must not be <code>null</code>.
	 * 
	 * @param listener the preference change listener to remove
	 * @throws IllegalStateException if this node or an ancestor has been removed
	 * @see #addPreferenceChangeListener(IPreferenceChangeListener)
	 * @see IPreferenceChangeListener
	 */
	public void removePreferenceChangeListener(IPreferenceChangeListener listener);

	/**
	 * Return the preferences node with the given path. The given path must
	 * not be <code>null</code>.
	 * <p>
	 * Functionally equivalent to calling "<code>return node(path.toString());</code>". 
	 * See the spec of <code>Preferences#node(String)<code> for more details. 
	 * </p>
	 * <p>
	 * Note that if the node does not yet exist and is created, then the appropriate
	 * <code>INodeChangeEvent</code> must be sent to listeners who are
	 * registered at this node.
	 * </p>
	 * @param path the path of the node
	 * @return the node
	 * @see org.osgi.service.prefs.Preferences#node(String)
	 */
	public IEclipsePreferences node(IPath path);

	/**
	 * Return the boolean value <code>true</code> if a node with the
	 * given path exists in the preference node hierarchy. The given path
	 * must not be <code>null</code>.
	 * <p>
	 * Functionally equivalent to calling "<code>return nodeExists(path.toString());</code>". 
	 * See the spec of <code>Preferences#node(String)</code> for more details. 
	 * </p>
	 * @param path the path to the node
	 * @return <code>true</code> if the node exists and <code>false</code> otherwise
	 * @throws BackingStoreException if this method fails
	 * @see org.osgi.service.prefs.Preferences#nodeExists(String)
	 */
	public boolean nodeExists(IPath path) throws BackingStoreException;

	/**
	 * Remove this node from the preference hierarchy. If this node is the scope
	 * root, then do not remove this node, only remove this node's children.
	 * <p>
	 * Functionally equivalent to calling "<code>removeNode();</code>". 
	 * See the spec of <code>Preferences#removeNode()<code> for more details. 
	 * </p>
	 * <p>
	 * Implementors must send the appropriate <code>INodeChangeEvent</code>
	 * to listeners who are registered on this node's parent.
	 * </p>
	 * @throws BackingStoreException if there was a problem removing this node
	 * @see org.osgi.service.prefs.Preferences#removeNode()
	 */
	public void removeNode() throws BackingStoreException;

	/**
	 * Return the preferences node with the given path. The given path must
	 * not be <code>null</code>.
	 * <p>
	 * Functionally equivalent to calling "<code>return node(path.toString());</code>". 
	 * See the spec of <code>Preferences#node(String)<code> for more details. 
	 * </p>
	 * <p>
	 * Note that if the node does not yet exist and is created, then the appropriate
	 * <code>INodeChangeEvent</code> must be sent to listeners who are
	 * registered at this node.
	 * </p>
	 * @param path the path of the node
	 * @return the node
	 * @see org.osgi.service.prefs.Preferences#node(String)
	 */
	public Preferences node(String name);

	/**
	 * Accepts the given visitor. The visitor's <code>visit</code> method 
	 * is called with this node. If the visitor returns <code>true</code>, 
	 * this method visits this node's children.
	 *
	 * @param visitor the visitor
	 * @see IPreferenceNodeVisitor#visit
	 * @throws BackingStoreException
	 */
	public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException;
}