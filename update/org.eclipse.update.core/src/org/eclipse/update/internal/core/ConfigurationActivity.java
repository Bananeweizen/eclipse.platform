package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.PrintWriter;

import org.eclipse.update.configuration.*;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.internal.model.*;
import org.eclipse.update.internal.model.ConfigurationActivityModel;
import org.eclipse.update.internal.model.InstallConfigurationParser;

public class ConfigurationActivity extends ConfigurationActivityModel implements IActivity, IWritable {
	

	public ConfigurationActivity(){
	}

	/**
	 * Constructor for ConfigurationActivityModel.
	 */
	public ConfigurationActivity(int action) {
		super();
		setAction(action);
		setStatus(STATUS_NOK); 
	}

	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {
		String gap = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			gap += " "; //$NON-NLS-1$
		String increment = ""; //$NON-NLS-1$
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " "; //$NON-NLS-1$
		
		
		w.print(gap + "<" + InstallConfigurationParser.ACTIVITY + " "); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("action=\""+getAction()+"\" "); //$NON-NLS-1$ //$NON-NLS-2$
		if (getLabel()!=null){
			w.println(gap+"label=\""+Writer.xmlSafe(getLabel())+"\" "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		w.println("date=\"" + getDate().getTime() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		w.print("status=\""+getStatus()+"\""); //$NON-NLS-1$ //$NON-NLS-2$
		w.println(">"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$
		
		
		// end
		w.println(gap+"</"+InstallConfigurationParser.ACTIVITY+">"); //$NON-NLS-1$ //$NON-NLS-2$
		
		
		
	}

	/*
	 * @see IActivity#getInstallConfiguration()
	 */
	public IInstallConfiguration getInstallConfiguration() {
		return (IInstallConfiguration)getInstallConfigurationModel();
	}

}

