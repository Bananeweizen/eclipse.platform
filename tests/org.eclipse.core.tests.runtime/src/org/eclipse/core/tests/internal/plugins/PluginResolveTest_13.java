package org.eclipse.core.tests.internal.plugins;

import java.net.URL;
import junit.framework.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.internal.plugins.InternalFactory;
import org.eclipse.core.internal.plugins.PluginDescriptor;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.Policy;
/**
 */
public class PluginResolveTest_13 extends PluginResolveTest {
public PluginResolveTest_13() {
	super(null);
}
public PluginResolveTest_13(String name) {
	super(name);
}
public void baseTest() {
	
	MultiStatus problems = new MultiStatus(Platform.PI_RUNTIME, Platform.PARSE_PROBLEM, Policy.bind("registryTestProblems", new String[0]), null);
	InternalFactory factory = new InternalFactory(problems);
	PluginDescriptor tempPlugin = (PluginDescriptor)Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.core.tests.runtime");
	String pluginPath = tempPlugin.getLocation().concat("Plugin_Testing/plugins.resolve.13/");
	URL pluginURLs[] = new URL[1];
	URL pURL = null;
	try {
		pURL = new URL (pluginPath);
	} catch (java.net.MalformedURLException e) {
		assertTrue("Bad URL for " + pluginPath, true);
	}
	pluginURLs[0] = pURL;
	IPluginRegistry registry = ParseHelper.doParsing (factory, pluginURLs, true);

	IPluginDescriptor pluginDescriptor = null;

	PluginVersionIdentifier verIdB = new PluginVersionIdentifier("2.1.0");
	pluginDescriptor = registry.getPluginDescriptor("tests.b", verIdB);
	assertNotNull("0.0", pluginDescriptor);
	assertEquals("0.1", pluginDescriptor.getUniqueIdentifier(), "tests.b");
	assertEquals("0.2", pluginDescriptor.getVersionIdentifier(), verIdB);
	verIdB = null;

	PluginVersionIdentifier verIdC = new PluginVersionIdentifier("2.0.5");
	pluginDescriptor = registry.getPluginDescriptor("tests.c", verIdC);
	assertNotNull("1.0", pluginDescriptor);
	assertEquals("1.1", pluginDescriptor.getUniqueIdentifier(), "tests.c");
	assertEquals("1.2", pluginDescriptor.getVersionIdentifier(), verIdC);
	verIdC = null;

}
public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(new PluginResolveTest_13("baseTest"));
	return suite;
}
}
