/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.antsupport.logger.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.eclipse.ant.internal.ui.antsupport.logger.RemoteAntBuildLogger;

/**
 * Parts adapted from org.eclipse.jdt.internal.junit.runner.RemoteTestRunner
 * A build logger that reports via a socket connection.
 * See DebugMessageIds and MessageIds for more information about the protocol.
 */
public class RemoteAntDebugBuildLogger extends RemoteAntBuildLogger {
	
	private ServerSocket fServerSocket;
	private Socket fRequestSocket;
	
	private PrintWriter fRequestWriter;
	
	private BufferedReader fRequestReader;
	
	protected boolean fStepOverSuspend= false;
	protected boolean fStepIntoSuspend= false;
	
	protected boolean fClientSuspend= false;
	
	protected boolean fBuildStartedSuspend= false;
	
	private Stack fTasks= new Stack();
	private Task fCurrentTask;
	private Task fStepOverTask;
	private Task fLastTaskFinished;
	
	private List fBreakpoints= null;
	
	//properties set before execution
	private Map fInitialProperties= null;
	private Map fProperties= null;
	
	/**
	 * Request port to connect to.
	 * Used for debug connections
	 */
	protected int fRequestPort= -1;

	/**
	 * Reader thread that processes requests from the debug client.
	 */
	private class ReaderThread extends Thread {
		public ReaderThread() {
			super("ReaderThread"); //$NON-NLS-1$
			setDaemon(true);
		}

		public void run(){
			try { 
				String message= null; 
				while (fRequestReader != null) { 
					if ((message= fRequestReader.readLine()) != null) {
						
						if (message.startsWith(DebugMessageIds.STEP_INTO)){
							synchronized(RemoteAntDebugBuildLogger.this) {
							    fStepIntoSuspend= true;
								RemoteAntDebugBuildLogger.this.notifyAll();
							}
						} if (message.startsWith(DebugMessageIds.STEP_OVER)){
							synchronized(RemoteAntDebugBuildLogger.this) {
							    fStepOverSuspend= true;
								fStepOverTask= fCurrentTask;
								RemoteAntDebugBuildLogger.this.notifyAll();
							}
						} else if (message.startsWith(DebugMessageIds.SUSPEND)) {
							synchronized(RemoteAntDebugBuildLogger.this) {
								fClientSuspend= true;
							}
						} else if (message.startsWith(DebugMessageIds.RESUME)) {
							synchronized(RemoteAntDebugBuildLogger.this) {
								RemoteAntDebugBuildLogger.this.notifyAll();
							}
						} else if (message.startsWith(DebugMessageIds.TERMINATE)) {
						    sendRequestResponse(DebugMessageIds.TERMINATED);
							shutDown();
						} else if (message.startsWith(DebugMessageIds.STACK)) {
							marshallStack();
						} else if (message.startsWith(DebugMessageIds.ADD_BREAKPOINT)) {
							addBreakpoint(message);
						} else if (message.startsWith(DebugMessageIds.REMOVE_BREAKPOINT)) {
							removeBreakpoint(message);
						}  else if (message.startsWith(DebugMessageIds.PROPERTIES)) {
							marshallProperties();
						}
					}
				} 
			} catch (Exception e) {
				RemoteAntDebugBuildLogger.this.shutDown();
			}
		}
	}
	
	private void requestConnect() {
		if (fDebugMode) {
			System.out.println("RemoteAntDebugBuildLogger: trying to connect" + fHost + ":" + fRequestPort); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try{
			fRequestSocket= fServerSocket.accept();
			fRequestWriter= new PrintWriter(fRequestSocket.getOutputStream(), true);
			fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
			
			ReaderThread readerThread= new ReaderThread();
			readerThread.setDaemon(true);
			readerThread.start();
			return;
		} catch(IOException e){
		}
		
		shutDown();
		//throw new BUildExection()
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.antsupport.logger.RemoteAntBuildLogger#shutDown()
	 */
	protected void shutDown() {
		if (fRequestWriter != null) {
			fRequestWriter.close();
			fRequestWriter= null;
		}
		
		if (fRequestReader != null) {
			try {
				fRequestReader.close();
			} catch (IOException e) {
			}
			fRequestReader= null;
		}
		
		if (fRequestSocket != null) {
			try {
				fRequestSocket.close();	
			} catch(IOException e) {
			}
		}
		fRequestSocket= null;
		
		super.shutDown();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		super.buildStarted(event);
		marshalMessage(-1, DebugMessageIds.BUILD_STARTED);
		String requestPortProperty= event.getProject().getProperty("eclipse.connect.request_port"); //$NON-NLS-1$
		if (requestPortProperty != null) {
			fRequestPort= Integer.parseInt(requestPortProperty);
			try {
				fServerSocket= new ServerSocket(fRequestPort);
			} catch (IOException ioe) {
				//throw new buildexection();
				shutDown();
			}
			requestConnect();
		} else {
			shutDown();
		}
		fBuildStartedSuspend= true;
		waitIfSuspended();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
		if (fInitialProperties == null) {//implicit or top level target does not fire targetStarted()
			fInitialProperties= event.getProject().getProperties();
		}
		super.taskStarted(event);
		fCurrentTask= event.getTask();
		fTasks.push(fCurrentTask);
		waitIfSuspended();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(BuildEvent event) {
		super.taskFinished(event);
		fLastTaskFinished= (Task)fTasks.pop();
		fCurrentTask= null;
		waitIfSuspended();
	}
	
	private synchronized void waitIfSuspended() {
	    if (fCurrentTask != null) {
	        String detail= null;
	        boolean shouldSuspend= true;
	        RemoteAntBreakpoint breakpoint= breakpointAtLineNumber(fCurrentTask.getLocation());
	        if (breakpoint != null) {
	            detail= breakpoint.toMarshallString();
	        } else if (fStepIntoSuspend) {
	            detail= DebugMessageIds.STEP;
	            fStepIntoSuspend= false;
	        } else if (fStepOverSuspend) {
	            if (fLastTaskFinished == fStepOverTask) {
	                detail= DebugMessageIds.STEP;
	                fStepOverSuspend= false;
	                fStepOverTask= null;
	            } else {
	                shouldSuspend= false;
	            }
	        } else if (fClientSuspend) {
	            detail= DebugMessageIds.CLIENT_REQUEST;
	            fClientSuspend= false;
	        } else {
	            shouldSuspend= false;
	        }
	        if (shouldSuspend) {
	            StringBuffer message= new StringBuffer(DebugMessageIds.SUSPENDED);
	            message.append(detail);
	            sendRequestResponse(message.toString());
	            try {
	                wait();
	            } catch (InterruptedException e) {
	            }
	        }
	    } else if (fBuildStartedSuspend) {
	        try {
	            fBuildStartedSuspend= false;
	            wait();
	        } catch (InterruptedException e) {
	        }
	    }
	}

	private RemoteAntBreakpoint breakpointAtLineNumber(Location location) {
		if (fBreakpoints == null) {
			return null;
		}
		String fileName= getFileName(location);
		int lineNumber= getLineNumber(location);
		for (int i = 0; i < fBreakpoints.size(); i++) {
			RemoteAntBreakpoint breakpoint = (RemoteAntBreakpoint) fBreakpoints.get(i);
			if (breakpoint.isAt(fileName, lineNumber)) {
				return breakpoint;
			}
		}
		return null;
	}

	private void sendRequestResponse(String message) {
		if (fRequestWriter == null) {
			return;
		}
		
		fRequestWriter.println(message);
	}
	
	protected void marshallStack() {
	    StringBuffer stackRepresentation= new StringBuffer();
	    stackRepresentation.append(DebugMessageIds.STACK);
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    
	    for (int i = fTasks.size() - 1; i >= 0 ; i--) {
	        Task task = (Task) fTasks.get(i);
	        stackRepresentation.append(task.getOwningTarget().getName());
	        stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	        stackRepresentation.append(task.getTaskName());
	        stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	        
	        Location location= task.getLocation();
	        stackRepresentation.append(getFileName(location));
	        stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	        stackRepresentation.append(getLineNumber(location));
	        stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    }	
	    sendRequestResponse(stackRepresentation.toString());
	}
	
	protected void marshallProperties() {
		
	    StringBuffer propertiesRepresentation= new StringBuffer();
	    propertiesRepresentation.append(DebugMessageIds.PROPERTIES);
	    propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    Map currentProperties= null;
	    Map currentUserProperties= null;
	    if (!fTasks.isEmpty()) {
	        currentProperties= ((Task)fTasks.peek()).getProject().getProperties();
	        currentUserProperties= ((Task)fTasks.peek()).getProject().getUserProperties();
	        if (fProperties != null && currentProperties.size() == fProperties.size()) {
	            //no new properties
	            sendRequestResponse(propertiesRepresentation.toString());
	            return;
	        }
	        
	        Iterator iter= currentProperties.keySet().iterator();
	        String propertyName;
	        String propertyValue;
	        while (iter.hasNext()) {
	            propertyName = (String) iter.next();
	            if (propertyName.equals("line.separator")) { //$NON-NLS-1$
	            	continue;
	            }
	            if (fProperties == null || fProperties.get(propertyName) == null) { //new property
	                propertiesRepresentation.append(propertyName.length());
	                propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	                propertiesRepresentation.append(propertyName);
	                propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	                propertyValue= (String) currentProperties.get(propertyName);
	                propertiesRepresentation.append(propertyValue.length());
	                propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	                propertiesRepresentation.append(propertyValue);
	                propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	                if (fInitialProperties.get(propertyName) != null) { //properties set before the start of the build
	                	if (currentUserProperties.get(propertyName) == null) {
	                		propertiesRepresentation.append(DebugMessageIds.PROPERTY_SYSTEM);
	                	} else {
	                		propertiesRepresentation.append(DebugMessageIds.PROPERTY_USER);
	                	}
	                } else if (currentUserProperties.get(propertyName) == null){
	                	propertiesRepresentation.append(DebugMessageIds.PROPERTY_RUNTIME);
	                } else {
	                	propertiesRepresentation.append(DebugMessageIds.PROPERTY_USER);
	                }
	                propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	            }
	        }
	    }
	    propertiesRepresentation.deleteCharAt(propertiesRepresentation.length() - 1);
	    fProperties= currentProperties;
	    sendRequestResponse(propertiesRepresentation.toString());
	}
	
	protected void addBreakpoint(String breakpointRepresentation) {
		if (fBreakpoints == null) {
			fBreakpoints= new ArrayList();
		}
		RemoteAntBreakpoint newBreakpoint= new RemoteAntBreakpoint(breakpointRepresentation);
		if (!fBreakpoints.contains(newBreakpoint)) {
			fBreakpoints.add(newBreakpoint);	
		}
	}
	
	protected void removeBreakpoint(String breakpointRepresentation) {
		if (fBreakpoints == null) {
			return;
		} 
		RemoteAntBreakpoint equivalentBreakpoint= new RemoteAntBreakpoint(breakpointRepresentation);
		for (Iterator iter = fBreakpoints.iterator(); iter.hasNext(); ) {
			RemoteAntBreakpoint breakpoint = (RemoteAntBreakpoint) iter.next();
			if (breakpoint.equals(equivalentBreakpoint)) {
				iter.remove();
				return;
			}
		}
	}
	
	private int getLineNumber(Location location) {
		try {
			return location.getLineNumber();
		} catch (NoSuchMethodError e) {
			//Ant before 1.6
			String locationString= location.toString();
			if (locationString.length() == 0) {
				return 0;
			}
			//filename: lineNumber: ("c:\buildfile.xml: 12: ")
			int lastIndex= locationString.lastIndexOf(':');
			int index =locationString.lastIndexOf(':', lastIndex - 1);
			if (index != -1) {
				try {
					return Integer.parseInt(locationString.substring(index + 1, lastIndex));
				} catch (NumberFormatException nfe) {
					return 0;
				}
			}
			return 0;
		}
	}
	
	private String getFileName(Location location) {
		try {
			return location.getFileName();
		} catch (NoSuchMethodError e) {
			//Ant before 1.6
			String locationString= location.toString();
			if (locationString.length() == 0) {
				return null;
			}
			//filename: lineNumber: ("c:\buildfile.xml: 12: ")			
			int lastIndex= locationString.lastIndexOf(':');
			int index =locationString.lastIndexOf(':', lastIndex-1);
			if (index == -1) {
				index= lastIndex; //only the filename is known
			}
			if (index != -1) {
				return locationString.substring(5, index);
			}
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		if (fInitialProperties == null) {
			fInitialProperties= event.getProject().getProperties();
		}
		super.targetStarted(event);
	}
}