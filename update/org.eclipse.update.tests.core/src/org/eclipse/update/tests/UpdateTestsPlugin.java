package org.eclipse.update.tests;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.appserver.WebappManager;
import org.eclipse.update.internal.core.UpdateCORE;

/**
 * manages the startuo and shutown of the 
 * web server
 */
public class UpdateTestsPlugin extends Plugin {

	private static String appServerHost = null;
	private static int appServerPort = 0;
	private static UpdateTestsPlugin plugin;
	private static boolean initialized=false;

	public UpdateTestsPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
	}

	public static UpdateTestsPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Called by Platform after loading the plugin
	 */
	public void startup() throws CoreException {

	}

	/**
	 * Shuts down this plug-in and discards all plug-in state.
	 * @exception CoreException if this method fails to shut down
	 *   this plug-in 
	 */
	public void shutdown() throws CoreException {
		WebappManager.stop("org.eclipse.update.tests.core.updatetests");
		super.shutdown();
	}

	/**
	 * Returns the host identifier for the web app server
	 */
	public static String getWebAppServerHost() {
		if (!initialized) initialize();
		return appServerHost;
	}

	/**
	 * Returns the port identifier for the web app server
	 */
	public static int getWebAppServerPort() {
		if (!initialized) initialize();		
		return appServerPort;
	}
	/**
	 * Method initialize.
	 */
	private static void initialize() {
		String text = null;
		try {
			WebappManager.start("org.eclipse.update.tests.core.updatetests", "org.eclipse.update.tests.core", new Path("webserver"));
			appServerHost = WebappManager.getHost();
			appServerPort = WebappManager.getPort();

			text = "The webServer did start ip:" + appServerHost + ":" + appServerPort;
		} catch (CoreException e) {
			text = "The webServer didn't start ";
			IStatus status = new Status(IStatus.ERROR, "org.eclipse.update.tests.core", IStatus.OK, "WebServer not started. Update Tests results are invalid", null);
			UpdateCORE.warn("",new CoreException(status));
		}finally {
			System.out.println(text);
			initialized = true;
		}
	}

}