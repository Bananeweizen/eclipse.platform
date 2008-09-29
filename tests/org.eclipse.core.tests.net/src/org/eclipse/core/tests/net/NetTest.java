/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.net;

import java.util.*;

import junit.framework.*;

import org.eclipse.core.internal.net.ProxyType;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;

public class NetTest extends TestCase {

	private boolean isSetEnabled;
	private boolean isProxiesDefault;
	private boolean isSystemProxiesDefault;
	private Map dataCache = new HashMap();

	public NetTest() {
		super();
	}

	public NetTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(NetTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		isSystemProxiesDefault = isSystemProxiesEnabled();
		setSystemProxiesEnabled(false);
		isProxiesDefault = isProxiesEnabled();
		setProxiesEnabled(true);
		isSetEnabled = true;
		dataCache.clear();
		ProxyType.socksSystemPropertySetting = ProxyType.ALWAYS_SET;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		setProxiesEnabled(isProxiesDefault);
		setSystemProxiesEnabled(isSystemProxiesDefault);
		IProxyData[] data = getProxyManager().getProxyData();
		for (int i = 0; i < data.length; i++) {
			IProxyData proxyData = data[i];
			proxyData.disable();
		}
		getProxyManager().setProxyData(data);
	}
	
	private IProxyService getProxyManager() {
		return Activator.getDefault().getProxyService();
	}

	private void assertProxyDataEqual(IProxyData expectedData) {
		IProxyData data = getProxyManager().getProxyData(expectedData.getType());
		assertEquals(expectedData.getType(), data.getType());
		assertEquals(expectedData.getHost(), data.getHost());
		assertEquals(expectedData.getPort(), data.getPort());
		assertEquals(expectedData.getUserId(), data.getUserId());
		assertEquals(expectedData.getPassword(), data.getPassword());
		assertEquals(expectedData.isRequiresAuthentication(), data.isRequiresAuthentication());
		assertEquals(expectedData.getSource(), data.getSource());
		assertSystemPropertiesMatch(data);
	}
	
	public void assertSystemPropertiesMatch(IProxyData proxyData) {
		if (proxyData.getType().equals(IProxyData.HTTP_PROXY_TYPE)) {
			assertHttpSystemProperties(proxyData, "http");
		} else if (proxyData.getType().equals(IProxyData.HTTPS_PROXY_TYPE)) {
			assertHttpSystemProperties(proxyData, "https");
		} else if (proxyData.getType().equals(IProxyData.SOCKS_PROXY_TYPE)) {
			assertSocksSystemProperties(proxyData);
		}
	}

	private void assertHttpSystemProperties(IProxyData proxyData, String keyPrefix) {
		Properties sysProps = System.getProperties();
		
		if (this.getProxyManager().isProxiesEnabled()) {
			boolean isSet = Boolean.getBoolean(keyPrefix + ".proxySet");
			assertEquals(proxyData.getHost() != null, isSet); //$NON-NLS-1$
			assertEquals(proxyData.getHost(), sysProps.get(keyPrefix + ".proxyHost")); //$NON-NLS-1$
			String portString = (String)sysProps.get(keyPrefix + ".proxyPort"); //$NON-NLS-1$
			int port = -1;
			if (portString != null)
				port = Integer.parseInt(portString);
			assertEquals(proxyData.getPort(), port);
			if (isSet)
				assertEquals(ProxyType.convertHostsToPropertyString(this.getProxyManager().getNonProxiedHosts()), sysProps.get(keyPrefix + ".nonProxyHosts")); //$NON-NLS-1$
			else 
				assertNull(sysProps.get(keyPrefix + ".nonProxyHosts"));
			assertEquals(proxyData.getUserId(), sysProps.get(keyPrefix + ".proxyUser")); //$NON-NLS-1$
			assertEquals(proxyData.getUserId(), sysProps.get(keyPrefix + ".proxyUserName")); //$NON-NLS-1$
			assertEquals(proxyData.getPassword(), sysProps.get(keyPrefix + ".proxyPassword")); //$NON-NLS-1$
		} else {
			assertNull(sysProps.get(keyPrefix + ".proxySet"));
			assertNull(sysProps.get(keyPrefix + ".proxyHost"));
			assertNull(sysProps.get(keyPrefix + ".proxyPort"));
			assertNull(sysProps.get(keyPrefix + ".nonProxyHosts"));
			assertNull(sysProps.get(keyPrefix + ".proxyUser"));
			assertNull(sysProps.get(keyPrefix + ".proxyUserName"));
			assertNull(sysProps.get(keyPrefix + ".proxyPassword"));
		}
	}

	private void assertSocksSystemProperties(IProxyData proxyData) {
		Properties sysProps = System.getProperties();
		if (this.getProxyManager().isProxiesEnabled()) {
			assertEquals(proxyData.getHost(), sysProps.get("socksProxyHost")); //$NON-NLS-1$
			String portString = (String)sysProps.get("socksProxyPort"); //$NON-NLS-1$
			int port = -1;
			if (portString != null)
				port = Integer.parseInt(portString);
			assertEquals(proxyData.getPort(), port);
		} else {
			assertNull(sysProps.get("socksProxyHost"));
			assertNull(sysProps.get("socksProxyPort"));
		}
	}
	
	private IProxyData getProxyData(String type) {
		IProxyData data = (IProxyData)dataCache.get(type);
		if (data == null) {
			data = this.getProxyManager().getProxyData(type);
			assertProxyDataEqual(data);
		}
		return data;
	}
	
	private void setProxyData(IProxyData data) throws CoreException {
		if (isSetEnabled) {
			this.getProxyManager().setProxyData(new IProxyData[] { data });
			assertProxyDataEqual(data);
		} else {
			dataCache.put(data.getType(), data);
		}
	}
	
	private void disableProxy(IProxyData proxyData) throws CoreException {
		proxyData.disable();
		setProxyData(proxyData);
	}
	
	private void changeProxyData(IProxyData oldData, IProxyData data)
			throws CoreException {
		// Make sure that setting the host doesn't change the persisted settings
		if (isSetEnabled)
			assertProxyDataEqual(oldData);
		// Now set it in the manager and assert that it is set
		setProxyData(data);
	}
	
	private void setHost(String type) throws CoreException {
		String host = "www.eclipse.org";
		setHost(type, host);
	}

	private void setHost(String type, String host) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setHost(host);
		changeProxyData(oldData, data);
	}
	
	private void setPort(String type, int port) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setPort(port);
		changeProxyData(oldData, data);
	}
	
	private void setUser(String type, String user, String password) throws CoreException {
		IProxyData data = getProxyData(type);
		IProxyData oldData = getProxyData(type);
		data.setUserid(user);
		data.setPassword(password);
		changeProxyData(oldData, data);
	}
	
	private void setDataTest(String type) throws CoreException {
		setHost(type, "www.eclipse.org");
		setPort(type, 1024);
		setUser(type, "me", "passw0rd");
	}

	private boolean isProxiesEnabled() {
		return this.getProxyManager().isProxiesEnabled();
	}

	private boolean isSystemProxiesEnabled() {
		return this.getProxyManager().isProxiesEnabled();
	}

	private void setProxiesEnabled(boolean enabled) {
		this.getProxyManager().setProxiesEnabled(enabled);
		if (enabled && this.getProxyManager().isSystemProxiesEnabled()
				&& !this.getProxyManager().hasSystemProxies()) {
			assertEquals(false, this.getProxyManager().isProxiesEnabled());
		} else {
			assertEquals(enabled, this.getProxyManager().isProxiesEnabled());
		}
	}

	private void setSystemProxiesEnabled(boolean enabled) {
		this.getProxyManager().setSystemProxiesEnabled(enabled);
		assertEquals(enabled, this.getProxyManager().isSystemProxiesEnabled());
	}

	private void delaySettingData() {
		isSetEnabled = false;
	}
	
	private void performSettingData() throws CoreException {
		IProxyData[] data = (IProxyData[]) dataCache.values().toArray(new IProxyData[dataCache.size()]);
		this.getProxyManager().setProxyData(data);
		for (int i = 0; i < data.length; i++) {
			IProxyData proxyData = data[i];
			assertProxyDataEqual(proxyData);
		}
		isSetEnabled = true;
		dataCache.clear();
	}
	
	public void testIndividualSetAndClear() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		IProxyData[] data = this.getProxyManager().getProxyData();
		for (int i = 0; i < data.length; i++) {
			IProxyData proxyData = data[i];
			disableProxy(proxyData);
		}
	}
	
	public void testAllSetAndClear() throws CoreException {
		delaySettingData();
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		performSettingData();
		
		delaySettingData();
		IProxyData[] data = this.getProxyManager().getProxyData();
		for (int i = 0; i < data.length; i++) {
			IProxyData proxyData = data[i];
			disableProxy(proxyData);
		}
		performSettingData();
	}

	public void testSetWhenDisabled() throws CoreException {
		setProxiesEnabled(false);
		String type = IProxyData.HTTP_PROXY_TYPE;
		setHost(type);
	}

	public void testDisableAfterSet() throws CoreException {
		String type = IProxyData.HTTP_PROXY_TYPE;
		setHost(type);
		IProxyData data = this.getProxyManager().getProxyData(type);
		setProxiesEnabled(false);
		assertProxyDataEqual(data);
	}
	
	public void testSimpleHost() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		
		IProxyData[] allData = this.getProxyManager().getProxyDataForHost("www.randomhot.com");
		assertEquals(3, allData.length);
		
		IProxyData data = this.getProxyManager().getProxyDataForHost("www.randomhot.com", IProxyData.HTTP_PROXY_TYPE);
		assertNotNull(data);
		
		allData = this.getProxyManager().getProxyDataForHost("localhost");
		assertEquals(0, allData.length);
		
		data = this.getProxyManager().getProxyDataForHost("localhost", IProxyData.HTTP_PROXY_TYPE);
		assertNull(data);
	}
	
	public void testHostPattern() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);
		
		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();
		this.getProxyManager().setNonProxiedHosts(new String[] { "*ignore.com" });
		
		IProxyData[] allData = this.getProxyManager().getProxyDataForHost("www.randomhot.com");
		assertEquals(3, allData.length);
		
		IProxyData data = this.getProxyManager().getProxyDataForHost("www.randomhot.com", IProxyData.HTTP_PROXY_TYPE);
		assertNotNull(data);
		
		allData = this.getProxyManager().getProxyDataForHost("www.ignore.com");
		assertEquals(0, allData.length);
		
		data = this.getProxyManager().getProxyDataForHost("www.ignore.com", IProxyData.HTTP_PROXY_TYPE);
		assertNull(data);
		
		allData = this.getProxyManager().getProxyDataForHost("ignore.com");
		assertEquals(0, allData.length);
		
		data = this.getProxyManager().getProxyDataForHost("ignore.com", IProxyData.HTTP_PROXY_TYPE);
		assertNull(data);
		
		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

	public void testBug238796() throws CoreException {
		setDataTest(IProxyData.HTTP_PROXY_TYPE);
		setDataTest(IProxyData.HTTPS_PROXY_TYPE);
		setDataTest(IProxyData.SOCKS_PROXY_TYPE);

		String[] oldHosts = this.getProxyManager().getNonProxiedHosts();

		this.getProxyManager().setNonProxiedHosts(
				new String[] { "nonexisting.com" });

		IProxyData[] allData = this.getProxyManager().getProxyDataForHost(
				"NONEXISTING.COM");
		assertEquals(0, allData.length);
		IProxyData data = this.getProxyManager().getProxyDataForHost(
				"NONEXISTING.COM", IProxyData.HTTP_PROXY_TYPE);
		assertNull(data);

		this.getProxyManager().setNonProxiedHosts(oldHosts);
	}

}
