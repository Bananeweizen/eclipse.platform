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
package org.eclipse.update.internal.core;

import java.io.*;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IInstallConfiguration;
/**
 * A log writer that writes update manager log entries.  
 */
public class UpdateManagerLogWriter {
	private File logFile = null;
	private Writer log = null;

	private static final String CONFIGURATION = "!CONFIGURATION"; //$NON-NLS-1$
	private static final String ACTIVITY = "!ACTIVITY"; //$NON-NLS-1$

	private static final String SUCCESS = "success"; //$NON-NLS-1$
	private static final String FAILURE = "failure"; //$NON-NLS-1$

	private static final String FEATURE_INSTALL = "feature-install"; //$NON-NLS-1$
	private static final String FEATURE_REMOVE = "feature-remove"; //$NON-NLS-1$
	private static final String SITE_INSTALL = "site-install"; //$NON-NLS-1$
	private static final String SITE_REMOVE = "site-remove"; //$NON-NLS-1$
	private static final String UNCONFIGURE = "feature-disable"; //$NON-NLS-1$
	private static final String CONFIGURE = "feature-enable"; //$NON-NLS-1$
	private static final String REVERT = "revert"; //$NON-NLS-1$
	private static final String RECONCILIATION = "reconciliation"; //$NON-NLS-1$
	private static final String PRESERVED = "preserve-configuration"; //$NON-NLS-1$	
	private static final String UNKNOWN = "unknown"; //$NON-NLS-1$	

	private static final String LINE_SEPARATOR;

	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}

	/*
	 * 
	 */
	public UpdateManagerLogWriter(File file) {
		this.logFile = file;
		UpdateCore.warn("UPDATE MANAGER LOG Location: "+file.getAbsolutePath());
	}

	/*
	 * 
	 */
	private void closeLogFile() throws IOException {
		try {
			if (log != null) {
				log.flush();
				log.close();
			}
		} finally {
			log = null;
		}
	}
	
	/*
	 * 
	 */
	public synchronized void log(IInstallConfiguration installConfig) {
		// thread safety: (Concurrency003)
		if (logFile != null)
			openLogFile();
		if (log == null)
			log = logForStream(System.err);
		try {
			try {
				write(installConfig);
			} finally {
				if (logFile != null)
					closeLogFile();
				else
					log.flush();
			}
		} catch (Exception e) {
			System.err.println("An exception occurred while writing to the update manager log:"); //$NON-NLS-1$
			e.printStackTrace(System.err);
			System.err.println("Logging to the console instead."); //$NON-NLS-1$
			//we failed to write, so dump log entry to console instead
			try {
				log = logForStream(System.err);
				write(installConfig);
				log.flush();
			} catch (Exception e2) {
				System.err.println("An exception occurred while logging to the console:"); //$NON-NLS-1$
				e2.printStackTrace(System.err);
			}
		} finally {
			log = null;
		}
	}
	
	
	/*
	 * 
	 */
	private void openLogFile() {
		try {
			log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile.getAbsolutePath(), true), "UTF-8")); //$NON-NLS-1$
		} catch (IOException e) {
			// there was a problem opening the log file so log to the console
			log = logForStream(System.err);
		}
	}
	
    /*
     * 
     */
	private String getFormattedDate(Date date) {
		try {
			DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss.SS"); //$NON-NLS-1$
			return formatter.format(date);
		} catch (Exception e) {
			// If there were problems writing out the date, ignore and
			// continue since that shouldn't stop us from losing the rest
			// of the information
		}
		return Long.toString(System.currentTimeMillis());
	}
	
	/*
	 * 
	 */
	private Writer logForStream(OutputStream output) {
		try {
			return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new BufferedWriter(new OutputStreamWriter(output));
		}
	}
	
	
	/*
	 * Shuts down the update manager log.
	 */
	public synchronized void shutdown() {
		try {
			if (logFile != null) {
				closeLogFile();
				logFile = null;
			} else {
				if (log != null) {
					Writer old = log;
					log = null;
					old.flush();
					old.close();
				}
			}
		} catch (IOException e) {
			//we've shutdown the log, so not much else we can do!
			e.printStackTrace();
		}
	}


	/*
	 * !CONFIGURATION <label>
	 */
	private void write(IInstallConfiguration installConfig) throws IOException {
		writeln();
		write(CONFIGURATION);
		writeSpace();		
		write(installConfig.getLabel());
		writeln();
		IActivity[] activities = installConfig.getActivities();
		for (int i = 0; i < activities.length; i++) {
			write(activities[i]);
		}				
	}

	/*
	 * !ACTIVITY <date> <target> <action> <status>
	 */
	private void write(IActivity activity) throws IOException {
		write(ACTIVITY);
		writeSpace();		
		write(getFormattedDate(activity.getDate()));
		writeSpace();
		write(activity.getLabel());
		writeSpace();
		write(getAction(activity.getAction()));
		writeSpace();
		write((activity.getStatus()==IActivity.STATUS_OK)?SUCCESS:FAILURE);
		writeln();		
	}

	/*
	 * 
	 */
	private String getAction(int i) {
		switch (i) {
			case IActivity.ACTION_FEATURE_INSTALL :
				return FEATURE_INSTALL;
			case IActivity.ACTION_FEATURE_REMOVE :
				return FEATURE_REMOVE;
			case IActivity.ACTION_SITE_INSTALL :
				return SITE_INSTALL;
			case IActivity.ACTION_SITE_REMOVE :
				return SITE_REMOVE;
			case IActivity.ACTION_UNCONFIGURE :
				return UNCONFIGURE;
			case IActivity.ACTION_CONFIGURE :
				return CONFIGURE;
			case IActivity.ACTION_REVERT :
				return REVERT;
			case IActivity.ACTION_RECONCILIATION :
				return RECONCILIATION;
			case IActivity.ACTION_ADD_PRESERVED :
				return PRESERVED;
							
			default :
				return UNKNOWN+" ["+i+"]";
		}
	}


	/*
	 * 
	 */
	private void writeln() throws IOException {
		write(LINE_SEPARATOR);
	}
	
	/*
	 * 
	 */
	private void write(String message) throws IOException {
		if (message != null)
			log.write(message);
	}
	
	/*
	 * 
	 */
	private void writeSpace() throws IOException {
		write(" "); //$NON-NLS-1$
	}
}
