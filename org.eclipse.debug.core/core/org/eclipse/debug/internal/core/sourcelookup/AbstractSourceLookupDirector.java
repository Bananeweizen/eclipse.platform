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
package org.eclipse.debug.internal.core.sourcelookup;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.core.sourcelookup.containers.DefaultSourceContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Directs source lookup among a collection of source lookup participants,
 * and a common collection of source containers.
 * Each source lookup participant is a source locator itself, which allows
 * more than one source locator to participate in source lookup for a
 * launch. Each source lookup participant searches for source in the source
 * containers managed by this director, and each participant is notified
 * of changes in the source containers (i.e. when the set of source
 * containers changes).
 * <p>
 * When a source director is intilaized, it adds it self as a launch listener,
 * and automatically disposes itself when its associated launch is removed
 * from the launch manager. If a source director is instantiated by a client
 * that is not part of a launch, that client is responsible for disposing
 * the source director.
 * </p>
 * <p>
 * This class is yet experimental.
 * </p>
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.0
 * @see org.eclipse.debug.core.model.ISourceLocator
 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainer
 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType
 * @see org.eclipse.debug.internal.core.sourcelookup.ISourcePathComputer
 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupParticipant
 */
public abstract class AbstractSourceLookupDirector implements ISourceLookupDirector, ILaunchConfigurationListener, ILaunchListener {
	
	//ISourceLocatorParticipants that are listening for container changes
	protected ArrayList fParticipants = new ArrayList();
	//list of current source containers
	protected ISourceContainer[] fSourceContainers = null;
	//the launch config associated with this director
	protected ILaunchConfiguration fConfig;
	//whether duplicates should be searched for or not
	protected boolean fDuplicates = false;
	
	// XML nodes & attributes for persistence
	protected static final String DIRECTOR_ROOT_NODE = "sourceLookupDirector"; //$NON-NLS-1$
	protected static final String CONTAINERS_NODE = "sourceContainers"; //$NON-NLS-1$
	protected static final String DUPLICATES_ATTR = "duplicates"; //$NON-NLS-1$
	protected static final String CONTAINER_NODE = "container"; //$NON-NLS-1$
	protected static final String CONTAINER_TYPE_ATTR = "typeId"; //$NON-NLS-1$
	protected static final String CONTAINER_MEMENTO_ATTR = "memento"; //$NON-NLS-1$
	  
	class SourceLookupQuery implements ISafeRunnable {
		
		private List fSourceElements = new ArrayList();
		private IStackFrame fFrame = null;
		
		SourceLookupQuery(IStackFrame frame) {
			fFrame = frame;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			DebugPlugin.log(exception);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			for(int i=0; i < fParticipants.size(); i++) {
				Object[] sourceArray;
				try {
					sourceArray = ((ISourceLookupParticipant)fParticipants.get(i)).findSourceElements(fFrame);
					if (sourceArray !=null && sourceArray.length > 0) {
						if (isFindDuplicates()) {
							for(int j=0; j<sourceArray.length; j++)
								if(!checkDuplicate(sourceArray[j], fSourceElements))
									fSourceElements.add(sourceArray[j]);
						} else {
							fSourceElements.add(sourceArray[0]);
							return;
						}
					}
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
			}	
		}
		
		public List getSourceElements() {
			return fSourceElements;
		}

		public void dispose() {
			fFrame = null;
			fSourceElements = null;
		}
		
	}
	
	/**
	 * Constructs source lookup director
	 */
	public AbstractSourceLookupDirector() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.IPersistableSourceLocator2#dispose()
	 */
	public void dispose() {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchConfigurationListener(this);
		launchManager.removeLaunchListener(this);
		Iterator iterator = fParticipants.iterator();
		while (iterator.hasNext()) {
			ISourceLookupParticipant participant = (ISourceLookupParticipant) iterator.next();
			//director may also be a participant
			if(participant != this)
				participant.dispose();
		}
		fParticipants.clear();
		if (fSourceContainers != null) {
			for (int i = 0; i < fSourceContainers.length; i++) {
				fSourceContainers[i].dispose();
			}
		}
		fSourceContainers = null;
	}
	
	/**
	 * Constructs source containers from a list of container mementos.
	 * 
	 * @param list the list of nodes to be parsed
	 * @exception CoreException if parsing encounters an error
	 * @return a list of source containers
	 */
	private List parseSourceContainers(NodeList list) throws CoreException {
		List containers = new ArrayList();
		for (int i=0; i < list.getLength(); i++) {
			if(!(list.item(i).getNodeType() == Node.ELEMENT_NODE))
 				continue;
			Element element = (Element)list.item(i);
			String typeId = element.getAttribute(CONTAINER_TYPE_ATTR);
			if (typeId == null || typeId.equals("")) {	 //$NON-NLS-1$
				SourceLookupUtils.abort(SourceLookupMessages.getString("AbstractSourceLookupDirector.11"), null); //$NON-NLS-1$
			}
			ISourceContainerType type = SourceLookupUtils.getSourceContainerType(typeId);
			if(type == null) {
				SourceLookupUtils.abort(MessageFormat.format(SourceLookupMessages.getString("AbstractSourceLookupDirector.12"), new String[]{typeId}), null); //$NON-NLS-1$
			}			
			String memento = element.getAttribute(CONTAINER_MEMENTO_ATTR);
			if (memento == null || memento.equals("")) {	 //$NON-NLS-1$
				SourceLookupUtils.abort(SourceLookupMessages.getString("AbstractSourceLookupDirector.13"), null); //$NON-NLS-1$
			}
			ISourceContainer container = type.createSourceContainer(memento);
			containers.add(container);
		}	
		return containers;
	}
	
	/**
	 * Registers the given source lookup participant. Has no effect if an identical
	 * participant is already registered. Paticipants receive notification
	 * when the source containers associated with this source director change. 
	 * 
	 * @param participant the particiapant to register
	 */
	public void addSourceLookupParticipant(ISourceLookupParticipant participant) {
		if (!fParticipants.contains(participant)) {
			fParticipants.add(participant);
			participant.init(this);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#getSourceContainers()
	 */
	public ISourceContainer[] getSourceContainers() {
		if (fSourceContainers == null) {
			return new ISourceContainer[0];
		}
		ISourceContainer[] copy = new ISourceContainer[fSourceContainers.length];
		System.arraycopy(fSourceContainers, 0, copy, 0, fSourceContainers.length);
		return copy;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#isFindDuplicates()
	 */
	public boolean isFindDuplicates() {			
		return fDuplicates;			
	}
	
	/**
	 * Sets whether source containers should be searched exhaustively for
	 * applicable source elements or if only the first match should be located.
	 * 
	 * @param duplicates whether source containers should be searched exhaustively for
	 *  applicable source elements or if only the first match should be located
	 */
	public void setFindDuplicates(boolean duplicates) {			
		fDuplicates = duplicates;			
	}	
	
	/**
	 * Removes the given participant from the list of registered partipants.
	 * Has no effect if an identical participant is not already registered.
	 * 
	 * @param participant the participant to remove
	 */
	public void removeSourceLookupParticipant(ISourceLookupParticipant participant) {
		fParticipants.remove(participant);		
	}	

	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
	}
	
	/* (non-Javadoc)
	 * 
	 * Updates source containers in repsonse to changes in underlying launch
	 * configuration. A source lookup director can be initialized from a
	 * working copy launch configuration or a persisted launch configuration.
	 * This director responds to changes in the launch configuration it was
	 * initialized from.
	 * 
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		if (fConfig == null) {
			return;
		}
		if(fConfig.equals(configuration)) {
			try{
				String locatorMemento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO,(String)null);
				if (locatorMemento == null) {
					initializeDefaults(configuration);
				} else {
					initializeFromMemento(locatorMemento, configuration);
				}
			} catch (CoreException e){
			}
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator#getMemento()
	 */
	public String getMemento() throws CoreException {
		Document doc = SourceLookupUtils.newDocument();
		Element rootNode = doc.createElement(DIRECTOR_ROOT_NODE);
		doc.appendChild(rootNode);
				
		Element pathNode = doc.createElement(CONTAINERS_NODE);		
		if(fDuplicates) {
			pathNode.setAttribute(DUPLICATES_ATTR, "true"); //$NON-NLS-1$
		} else {
			pathNode.setAttribute(DUPLICATES_ATTR, "false"); //$NON-NLS-1$
		}
		rootNode.appendChild(pathNode);
		if(fSourceContainers !=null){
			for(int i=0; i<fSourceContainers.length; i++){
				Element node = doc.createElement(CONTAINER_NODE);
				ISourceContainer container = fSourceContainers[i];
				ISourceContainerType type = container.getType();
				node.setAttribute(CONTAINER_TYPE_ATTR, type.getId());
				node.setAttribute(CONTAINER_MEMENTO_ATTR, type.getMemento(container));
				pathNode.appendChild(node);
			}
		}
		return SourceLookupUtils.serializeDocument(doc);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator#initializeFromMemento(java.lang.String)
	 */
	public void initializeFromMemento(String memento) throws CoreException {
		dispose();
		Element rootElement = SourceLookupUtils.parseDocument(memento);		
		if (!rootElement.getNodeName().equalsIgnoreCase(DIRECTOR_ROOT_NODE)) { 
			SourceLookupUtils.abort(SourceLookupMessages.getString("AbstractSourceLookupDirector.14"), null); //$NON-NLS-1$
		}
		NodeList list = rootElement.getChildNodes();
		int length = list.getLength();
		for (int i = 0; i < length; ++i) {
			Node node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element entry = (Element) node;
				if(entry.getNodeName().equalsIgnoreCase(CONTAINERS_NODE)){
					setFindDuplicates("true".equals(entry.getAttribute(DUPLICATES_ATTR))); //$NON-NLS-1$
					NodeList children = entry.getChildNodes();
					List containers = parseSourceContainers(children);
					setSourceContainers((ISourceContainer[]) containers.toArray(new ISourceContainer[containers.size()]));
					return;
				}
			}
		}
	}
	
	/**
	 * Sets the source containers used by this source lookup
	 * director, and notifies participants of the change.
	 * 
	 * @param containers source containers to use
	 */
	public void setSourceContainers(ISourceContainer[] containers) {
		fSourceContainers = containers;
		for (int i = 0; i < containers.length; i++) {
			ISourceContainer container = containers[i];
			container.init(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISourceLocator#getSourceElement(org.eclipse.debug.core.model.IStackFrame)
	 * Would be better to accept Object so this can be used for breakpoints and other objects.
	 */
	public Object getSourceElement(IStackFrame stackFrame) {
		SourceLookupQuery query = new SourceLookupQuery(stackFrame);
		Platform.run(query);
		List sources = query.getSourceElements();
		query.dispose();
		if(sources.size() == 1) {
			return sources.get(0);
		} else if(sources.size() > 1) {
			return resolveSourceElement(stackFrame, sources);
		} else { 
			return null;
		}
	}
	
	/**
	 * Returns the source element to associate with the given stack frame.
	 * This method is called when more than one source element has been found
	 * for a stack frame, and allows the source director to select a single
	 * source element to associate with the stack frame.
	 * <p>
	 * Subclasses should override this method as appropriate. For example,
	 * to prompt the user to choose a source element.
	 * </p>
	 * @param frame the frame for which source is being searched for
	 * @param sources the source elements found for the given stack frame
	 * @return a single source element for the given stack frame
	 */
	public Object resolveSourceElement(IStackFrame frame, List sources) {
		return sources.get(0);
	}

	/**
	 * Checks if the object being added to the list of sources is a duplicate of what's already in the list
	 * @param sourceToAdd the new source file to be added 
	 * @param sources the list that the source will be compared against
	 * @return true if it is already in the list, false if it is a new object
	 */
	private boolean checkDuplicate(Object sourceToAdd, List sources){
		if(sources.size() == 0)
			return false;
		Iterator iterator = sources.iterator();
		while(iterator.hasNext())
			if(iterator.next().equals(sourceToAdd))
				return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.IPersistableSourceLookupDirector#initializeFromMemento(java.lang.String, org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFromMemento(String memento, ILaunchConfiguration configuration) throws CoreException {
		setLaunchConfiguration(configuration);
		initializeFromMemento(memento);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator#initializeDefaults(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
		dispose();
		setLaunchConfiguration(configuration);
		setSourceContainers(new ISourceContainer[]{new DefaultSourceContainer()});				
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#getLaunchConfiguration()
	 */
	public ILaunchConfiguration getLaunchConfiguration() {
		return fConfig;
	}
	
	/**
	 * Sets the launch configuration associated with this source lookup
	 * director. If the given configuration is a working copy, this director
	 * will respond to changes the working copy. If the given configuration
	 * is a persisted launch configration, this director will respond to changes
	 * in the persisted launch configuration.
	 * 
	 * @param configuration launch configuration to associate with this
	 *  source lookup director, or <code>null</code> if none
	 */
	protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfig = configuration;
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchConfigurationListener(this);
		launchManager.addLaunchListener(this);		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		if (this.equals(launch.getSourceLocator())) {
			dispose();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#getParticipants()
	 */
	public ISourceLookupParticipant[] getParticipants() {
		return (ISourceLookupParticipant[]) fParticipants.toArray(new ISourceLookupParticipant[fParticipants.size()]);
	}
}
