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
package org.eclipse.update.standalone;
import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;


/**
 * Adds a new site
 */
public class RemoveSiteCommand extends ScriptedCommand {
	private IConfiguredSite csite;
	private File sitePath;
	
	/**
	 * @param toSite if specified, list only the features from the specified local install site
	 */
	public RemoveSiteCommand(String toSite) throws Exception {
		try {
			if (toSite != null) {
				sitePath = new File(toSite);
				if (!sitePath.getName().equals("eclipse"))
					sitePath = new File(sitePath, "eclipse");
				if (!sitePath.exists())
					throw new Exception("Cannot find site: " + toSite);
					
				IConfiguredSite[] csites = SiteManager.getLocalSite().getCurrentConfiguration().getConfiguredSites();
				for (int i=0; i<csites.length; i++) {
					File f = new File(csites[i].getSite().getURL().getFile());
					if (f.equals(sitePath)) {
						csite = csites[i];
						break;
					}
				}
				
				if (csite == null)
					throw new Exception("Site is not configured " + toSite);
			} else {
				throw new Exception("No site specified");
			}
		
		} catch (Exception e) {
			throw e;
		} 
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
			
			try {
				getConfiguration().removeConfiguredSite(csite);
				// update the sites array
				getConfiguration().getConfiguredSites();
				SiteManager.getLocalSite().save();
				return true;
			} catch (CoreException e) {
				return false;
			}
	}
}
