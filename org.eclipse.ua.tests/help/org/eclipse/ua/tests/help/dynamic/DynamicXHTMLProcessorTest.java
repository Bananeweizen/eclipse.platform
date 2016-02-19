/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.dynamic;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.help.internal.xhtml.DynamicXHTMLProcessor;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.ua.tests.plugin.UserAssistanceTestPlugin;
import org.eclipse.ua.tests.util.XMLUtil;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

public class DynamicXHTMLProcessorTest {

	@Before
	public void setUp() throws Exception {
		// activate the UI plug-in for UI filtering ability
		HelpUIPlugin.getDefault();
	}

	private String process(String path) throws Exception {
		Bundle bundle = UserAssistanceTestPlugin.getDefault().getBundle();
		InputStream is = getProcessedInput(path, bundle);
		return readStream(is);
	}

	protected InputStream getProcessedInput(String path, Bundle bundle)
			throws IOException, SAXException, ParserConfigurationException,
			TransformerException, TransformerConfigurationException {
		InputStream in = bundle.getEntry(path).openStream();
		String href = '/' + bundle.getBundleId() +path;
		return DynamicXHTMLProcessor.process(href, in, "en", true);
	}

	private String readStream(InputStream is) throws Exception {
		try (InputStreamReader inputStreamReader = new InputStreamReader(is, "UTF-8")) {
			StringBuffer buffer = new StringBuffer();
			char[] cbuf = new char[256];
			int len;
			do {
				len = inputStreamReader.read(cbuf);
				if (len > 0) {
					buffer.append(cbuf, 0, len);
				}
			} while (len >= 0);
			return buffer.toString();
		}
	}

	@Test
	public void testXhtmlNoCollapseAnchor() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertTrue("Anchor collapsed in " + processed, processed.indexOf("</a>") > 0);
	}

	@Test
	public void testXhtmlNoCollapseParagraph() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertTrue("Paragraph collapsed in " + processed, processed.indexOf("</p>") > 0);
	}

	@Test
	public void testXhtmlNoCollapseAnchorIC() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchorWithComment.xhtml");
		assertTrue("Anchor collapsed in " + processed, processed.indexOf("</a>") > 0);
	}

	@Test
	public void testXhtmlNoCollapseParagraphIC() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchorWithComment.xhtml");
		assertTrue("Paragraph collapsed in " + processed, processed.indexOf("</p>") > 0);
	}

	@Test
	public void testXhtmlNoCollapseDiv() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyDiv.xhtml");
		assertTrue("Div collapsed in " + processed, processed.indexOf("</div>") > 0);
	}

	@Test
	public void testXhtmlNoCollapseScript() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertTrue("Div collapsed in " + processed, processed.indexOf("</script>") > 0);
	}

	@Test
	public void testValidXML() throws Exception {
	    String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
	    XMLUtil.assertParseableXML(processed);
	}

}
