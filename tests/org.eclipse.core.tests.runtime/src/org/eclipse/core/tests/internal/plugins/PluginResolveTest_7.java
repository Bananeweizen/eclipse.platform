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
public class PluginResolveTest_7 extends PluginResolveTest {
public PluginResolveTest_7() {
	super(null);
}
public PluginResolveTest_7(String name) {
	super(name);
}
public void baseTest() {

	MultiStatus problems = new MultiStatus(Platform.PI_RUNTIME, Platform.PARSE_PROBLEM, Policy.bind("registryTestProblems", new String[0]), null);
	InternalFactory factory = new InternalFactory(problems);
	URL pluginURLs[] = new URL[1];
	URL pURL = null;
	try {
		PluginDescriptor tempPlugin = (PluginDescriptor)Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.core.tests.runtime");
		String pluginPath = tempPlugin.getLocation().concat("Plugin_Testing/plugins.resolve.7/");
		pURL = new URL (pluginPath);
	} catch (java.net.MalformedURLException e) {
	}
	pluginURLs[0] = pURL;
	IPluginRegistry registry = ParseHelper.doParsing (factory, pluginURLs, true);
	IPluginDescriptor pd = null;

	pd = checkResolved(registry, "0.0", "tests.a", "1.0.0", true);
	checkResolvedPrereqs("0.1", pd, new String[] { "tests.c" }, new String[] { "1.0.9" });

	pd = checkResolved(registry, "1.0", "tests.b", "1.0.0", false);

	pd = checkResolved(registry, "2.0", "tests.c", "1.0.9", true);
	checkResolvedPrereqs("2.1", pd, new String[] { }, new String[] { });

	pd = checkResolved(registry, "3.0", "tests.c", "2.5.0", false);
}
public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(new PluginResolveTest_7("baseTest"));
	return suite;
}
}
