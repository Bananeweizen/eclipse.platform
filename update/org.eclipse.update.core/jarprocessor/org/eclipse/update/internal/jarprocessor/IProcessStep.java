/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.jarprocessor;

import java.io.File;

/**
 * @author aniefer
 *
 */
public interface IProcessStep {
	
	/**
	 * The effect of this processing step if the JarProcessor was to recurse on this entry.
	 * Return null if this step will not do anything with this entry.
	 * Return the new entryName if this step will modify this entry on recursion.
	 * @param entryName
	 * @return
	 */
	public String recursionEffect(String entryName);
	
	/**
	 * Perform some processing on the input file before the JarProcessor considers the entries for recursion.
	 *  return the file containing the result of the processing
	 * @param input
	 * @param workingDirectory
	 * @return
	 */
	public File preProcess(File input, File workingDirectory);
	
	/**
	 * Perform some processing on the input file after the JarProcessor returns from recursion
	 * return the file containing the result of the processing
	 * @param input
	 * @param workingDirectory
	 * @return
	 */
	public File postProcess(File input, File workingDirectory);
	
	/**
	 * Return the name of this process step
	 * @return
	 */
	public String getStepName();
}
