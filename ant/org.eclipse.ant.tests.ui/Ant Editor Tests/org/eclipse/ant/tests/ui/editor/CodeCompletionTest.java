/*******************************************************************************
 * Copyright (c) 2002, 2003 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial implementation
 * 	   IBM Corporation - additional tests
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.ui.editor.AntEditorSaxDefaultHandler;
import org.eclipse.ant.internal.ui.editor.utils.ProjectHelper;
import org.eclipse.ant.tests.ui.editor.support.TestTextCompletionProcessor;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests everything about code completion and code assistance.
 * 
 */
public class CodeCompletionTest extends AbstractAntUITest {

    /**
     * Constructor for CodeCompletionTest.
     * @param name
     */
    public CodeCompletionTest(String name) {
        super(name);
    }
    
	public static Test suite() {
		return new TestSuite(CodeCompletionTest.class);
	}

    /**
     * Tests the code completion for attributes of tasks.
     */
    public void testAttributeProposals() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        ICompletionProposal[] proposals = processor.getAttributeProposals("contains", "ca");
        assertEquals(1, proposals.length);
        assertEquals("casesensitive - (true | false | on | off | yes | no)", proposals[0].getDisplayString());

        proposals = processor.getAttributeProposals("move", "");
        assertEquals(14, proposals.length);
        ICompletionProposal proposal = proposals[0];
        String displayString = proposal.getDisplayString();
        assertTrue(displayString.equals("id") 
        || displayString.equals("flatten - (true | false | on | off | yes | no)")
        || displayString.equals("encoding")
        || displayString.equals("tofile")
        || displayString.equals("todir")
        || displayString.equals("file")
        || displayString.equals("verbose - (true | false | on | off | yes | no)")
        || displayString.equals("includeemptydirs")
        || displayString.equals("overwrite - (true | false | on | off | yes | no)")
        || displayString.equals("taskname")
        || displayString.equals("failonerror - (true | false | on | off | yes | no)")
        || displayString.equals("description")
        || displayString.equals("preservelastmodified - (true | false | on | off | yes | no)")
        || displayString.equals("filtering - (true | false | on | off | yes | no)"));

        proposals = processor.getAttributeProposals("move", "to");
        assertEquals(2, proposals.length);

        proposals = processor.getAttributeProposals("reference", "idl");
        assertEquals(0, proposals.length);

        proposals = processor.getAttributeProposals("reference", "id");
        assertEquals(1, proposals.length);
        assertEquals("id", proposals[0].getDisplayString());

        proposals = processor.getAttributeProposals("reference", "i");
        assertEquals(1, proposals.length);
        assertEquals("id", proposals[0].getDisplayString());

        proposals = processor.getAttributeProposals("project", "de");
        assertEquals(1, proposals.length);
        
        // assertEquals("default - #REQUIRED", tempProposals[0].getDisplayString());

    }
    
    /**
     * Test the code completion for properties.
     */
    public void testPropertyProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        String documentText = "<project default=\"test\"><property name=\"prop1\" value=\"val1\" />\n";
        documentText += "<property name=\"prop2\" value=\"val2\" />\n";
        documentText += "<property name=\"alf\" value=\"horst\" />\n";
        documentText += "<test name=\"$";
		processor.setLineNumber(4);
		processor.setColumnNumber(14);
        int cursorPos = documentText.length();
        ICompletionProposal[] proposals = processor.getPropertyProposals(documentText, "", cursorPos);
        assertTrue(proposals.length >= 3);
        assertContains("alf", proposals);
        documentText = "<project default=\"test\"><property name=\"prop1\" value=\"val1\" />\n";
        documentText += "<property name=\"prop2\" value=\"val2\" />\n";
        documentText += "<property name=\"alf\" value=\"horst\" />\n";
        documentText += "<test name=\"$a}";
		processor.setLineNumber(4);
		processor.setColumnNumber(15);
        cursorPos = documentText.length()-1;
        proposals = processor.getPropertyProposals(documentText, "a", cursorPos);
        assertTrue(proposals.length >= 1);
        assertContains("alf", proposals);
    }
    
    /**
     * Test the code completion for the depend attribute of a target.
     */
    public void testTargetDependProposals() {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
    	//simple depends
    	String documentText = "<project default=\"test\">\n";
    	documentText += "<property name=\"prop1\" value=\"val1\" />\n";
    	documentText += "<property name=\"prop2\" value=\"val2\" />\n";
    	documentText += "<property name=\"alf\" value=\"horst\" />\n";
    	documentText += "<target name=\"pretest\"></target>\n";
    	documentText += "<target name=\"test\" depends=\"\">\n";
    	documentText +="<echo>depends</echo>\n";
    	documentText +="</target>\n";
    	documentText += "<target name=\"test2\"></target>\n";
    	documentText += "<target name=\"test3\"></target>\n";
    	processor.setLineNumber(5);
    	processor.setColumnNumber(31);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(documentText, documentText.substring(0, 202), "", "depends");
    	assertTrue(proposals.length == 3);
    	assertContains("pretest", proposals);
    	
    	//comma separated depends
    	documentText = "<project default=\"test\">\n";
    	documentText += "<property name=\"prop1\" value=\"val1\" />\n";
    	documentText += "<property name=\"prop2\" value=\"val2\" />\n";
    	documentText += "<property name=\"alf\" value=\"horst\" />\n";
    	documentText += "<target name=\"pretest\"></target>\n";
    	documentText += "<target name=\"test\" depends=\"pretest ,\"></target>\n";
    	documentText += "<target name=\"test2\"></target>\n";
    	documentText += "<target name=\"test3\"></target>\n";
    	processor.setLineNumber(5);
    	processor.setColumnNumber(41);
    	proposals = processor.getTargetAttributeValueProposals(documentText, documentText.substring(0, 210), "te", "depends");
    	assertTrue(proposals.length == 2);
    	assertContains("test2", proposals);
    }
    
    /**
     * Test the code completion for the if attribute of a target.
     */
    public void testTargetIfProposals() {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

    	String documentText = "<project default=\"test\">\n";
    	documentText += "<property name=\"prop1\" value=\"val1\" />\n";
    	documentText += "<property name=\"prop2\" value=\"val2\" />\n";
    	
    	documentText += "<target name=\"pretest\"></target>\n";
    	documentText += "<target name=\"test\" if=\"\">\n";
    	documentText +="<echo>depends</echo>\n";
    	documentText +="</target>\n";
    	documentText += "<target name=\"test2\"></target>\n";
    	documentText += "<property name=\"alf\" value=\"horst\" />\n";
    	documentText += "<target name=\"test3\"></target>\n";
    	processor.setLineNumber(6);
    	processor.setColumnNumber(26);
    	processor.setCursorPosition(160);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(documentText, documentText.substring(0, 160), "a", "if");
    	assertTrue(proposals.length >= 1);
    	assertContains("alf", proposals);
    }
    
    /**
     * Test the code completion for the unless attribute of a target.
     */
    public void testTargetUnlessProposals() {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

    	String documentText = "<project default=\"test\">\n";
    	documentText += "<property name=\"prop1\" value=\"val1\" />\n";
    	documentText += "<property name=\"prop2\" value=\"val2\" />\n";
    	
    	documentText += "<target name=\"pretest\"></target>\n";
    	documentText += "<target name=\"test\" if=\"\">\n";
    	documentText +="<echo>depends</echo>\n";
    	documentText +="</target>\n";
    	documentText += "<target name=\"test2\"></target>\n";
    	documentText += "<property name=\"alf\" value=\"horst\" />\n";
    	documentText += "<target name=\"test3\"></target>\n";
    	processor.setLineNumber(6);
    	processor.setColumnNumber(26);
    	processor.setCursorPosition(164);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(documentText, documentText.substring(0, 164), "prop", "unless");
    	assertTrue(proposals.length >= 2);
    	assertContains("prop2", proposals);

    }
    
	/**
	* Test the code completion for "unquoted" properties.
	* See bug 40871
	*/
   public void testUnquotedPropertyProposals() {
	   TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

	   String documentText = "<project default=\"test\"><property name=\"prop1\" value=\"val1\" />\n";
	   documentText += "<property name=\"prop2\" value=\"val2\" />\n";
	   documentText += "<property name=\"alf\" value=\"horst\" />\n";
	   documentText += "<echo>${</echo>";
	   processor.setLineNumber(4);
	   processor.setColumnNumber(8);
	   int cursorPos = documentText.length() - 7;
	   ICompletionProposal[] proposals = processor.getPropertyProposals(documentText, "", cursorPos);
	   assertTrue(proposals.length >= 3);
	   assertContains("alf", proposals);
	  }
 
 	/**
 	 * Asserts that <code>displayString</code> is in one of the 
 	 * completion proposals.
 	 */
    private void assertContains(String displayString, ICompletionProposal[] proposalArray) {
        boolean found = false;
        for (int i = 0; i < proposalArray.length; i++) {
            ICompletionProposal proposal = proposalArray[i];
            String proposalDisplayString = proposal.getDisplayString();
            if(displayString.equals(proposalDisplayString)) {
                found = true;
                break;
            }
        }
        assertEquals(true, found);
    }        
    
	/**
	 * Tests the property proposals including properties defined in 
	 * a seperate property file.
	 */
    public void testProperties() {
        Project antProject = new Project();
		antProject.init();
        
        File file = getBuildFile("buildtest1.xml");
      
		antProject.setUserProperty("ant.file", file.getAbsolutePath());

        // File will be parsed here
		org.apache.tools.ant.ProjectHelper helper = org.apache.tools.ant.ProjectHelper.getProjectHelper();
        antProject.addReference("ant.projectHelper", helper); //$NON-NLS-1$
        helper.parse(antProject, file);
        Map map = antProject.getProperties();
        assertEquals("valD", map.get("propD"));
        assertEquals("val1", map.get("prop1"));
        assertEquals("val2", map.get("prop2"));
        assertEquals("valV", map.get("propV"));
       // assertEquals("val", map.get("property_in_target"));
    }

	/**
	 * Tests the property proposals for the case that they are defined in
	 * a dependent targets.
	 */
    public void testPropertyProposalDefinedInDependantTargets() throws FileNotFoundException {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        File file= getBuildFile("dependencytest.xml");
        processor.setEditedFile(file);
		String documentText = getFileContentAsString(file);

		processor.setLineNumber(35);
		processor.setColumnNumber(41);
		int cursorPosition = documentText.lastIndexOf("${");
		assertTrue(cursorPosition != -1);
        ICompletionProposal[] proposals = processor.getPropertyProposals(documentText, "", cursorPosition+2);
		assertContains("init_prop", proposals);
		assertContains("main_prop", proposals);
		assertContains("prop_prop", proposals);
		assertContains("do_not_compile", proposals);
		assertContains("adit_prop", proposals);
		assertContains("compile_prop", proposals);
    }
    
    /**
     * Tests the code completion for tasks having parent tasks.
     */
    public void testTaskProposals() throws ParserConfigurationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        ICompletionProposal[] proposals = processor.getTaskProposals("         <", doc.createElement("rename"), "");
        assertEquals(0, proposals.length);

        proposals = processor.getTaskProposals("       <cl", createTestPropertyElement(doc), "cl");
        assertEquals(1, proposals.length);
        ICompletionProposal proposal = proposals[0];
        assertEquals("classpath", proposal.getDisplayString());
        
        //case insensitivity
		proposals = processor.getTaskProposals("       <CL", createTestPropertyElement(doc), "cl");
	   	assertEquals(1, proposals.length);
	   	proposal = proposals[0];
	   	assertEquals("classpath", proposal.getDisplayString());

        proposals = processor.getTaskProposals("       <cl", doc.createElement("property"), "cl");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("classpath", proposal.getDisplayString());

        proposals = processor.getTaskProposals("       <pr", doc.createElement("property"), "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("classpath", proposal.getDisplayString());

        proposals = processor.getTaskProposals("       <pr", createTestProjectElement(doc), "pr");
        assertEquals(1, proposals.length); // is choice and already used with classpath
        proposal = proposals[0];
        assertEquals("property", proposal.getDisplayString());
        
        proposals = processor.getTaskProposals("       <fi", createTestProjectElement(doc), "fi");
        assertEquals(5, proposals.length); // is choice and already used with classpath
        
        proposals = processor.getTaskProposals("          ", doc.createElement("project"), "");
        assertEquals(22, proposals.length);

        proposals = processor.getTaskProposals("          ", null, "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("project", proposal.getDisplayString());

        proposals = processor.getTaskProposals("            jl", null, "jl");
        assertEquals(0, proposals.length);

        proposals = processor.getTaskProposals("             ", doc.createElement("projexxx"), "");
        assertEquals(0, proposals.length);

        proposals = processor.getTaskProposals("              ", doc.createElement("filelist"), "");
        assertEquals(0, proposals.length);

        // "<project><target><mk"
        String string = "<project><target><mk";
        proposals = processor.getTaskProposals("             <mk", processor.findParentElement(string, 0, 20), "mk");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("mkdir", proposal.getDisplayString());
        
        proposals = processor.getTaskProposals("", null, "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("project", proposal.getDisplayString());

        proposals = processor.getTaskProposals("    \n<project></project>", null, "");
        assertEquals(1, proposals.length);
    }

	/**
	 * Test for bug 40951
	 */
	public void testMixedElements() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
		String string = "<project><target><sql driver=\"\" password=\"\" url=\"\" userid=\"\"><T</sql>";
		ICompletionProposal[] proposals = processor.getTaskProposals(string, processor.findParentElement(string, 0, 64), "t");
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("transaction", proposal.getDisplayString());
		
		
		string = "<project><target><concat></concat>";
		proposals = processor.getTaskProposals(string, processor.findParentElement(string, 0, 25), "");
		assertEquals(2, proposals.length);
		proposal = proposals[0];
		assertEquals("filelist", proposal.getDisplayString());
	}
	
    /**
     * Tests the algorithm for finding a child as used by the processor.
     */
    public void testFindChildElement() throws ParserConfigurationException {
        
        // Create the test data
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element parentElement = doc.createElement("parent");
        Attr attribute = doc.createAttribute("att1");
        parentElement.setAttributeNode(attribute);
        Comment comment = doc.createComment("lakjjflsakdfj");
        parentElement.appendChild(comment);
        Element childElement = doc.createElement("child");
        parentElement.appendChild(childElement);
        childElement = doc.createElement("secondchild");
        parentElement.appendChild(childElement);
        
        // Create the processor
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
        
        // Test it!
        childElement = processor.findChildElementNamedOf(parentElement, "jkl");
        assertNull(childElement);
        childElement = processor.findChildElementNamedOf(parentElement, "secondchild");
        assertNotNull(childElement);
        assertEquals("secondchild", childElement.getTagName());
    }
    
    private Element createTestProjectElement(Document aDocument) {
        Element parentElement = aDocument.createElement("project");
        Element childElement = aDocument.createElement("property");
        parentElement.appendChild(childElement);
        Element classpathElement = aDocument.createElement("classpath");
        childElement.appendChild(classpathElement);
        
        return parentElement;
    }

    private Element createTestPropertyElement(Document aDocument) {
        Element childElement = aDocument.createElement("property");
        Element classpathElement = aDocument.createElement("classpath");
        childElement.appendChild(classpathElement);

        return childElement;
    }
    
    /**
     * Tests how the processor determines the proposal mode.
     */
    public void testDeterminingAttributeProposalMode() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // Modes:
        // 0 None
        // 1 Task Proposal
        // 2 Attribute Proposal
        // 3 Task Closing Proposal
        // 4 Attribute Value Proposal
        // 5 Property Proposal
        
        int mode = processor.determineProposalMode("<project><property ta", 21, "ta");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<project><property ", 19, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<project><property   ", 21, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property id=\"hu\" ", 18, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property id=\"hu\" \r\n ", 21, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property\n", 10, "");
        assertEquals(2, mode);
    }
    
	/**
	* Tests how the processor determines the proposal mode.
	*/
   public void testDeterminingPropertyProposalMode() {
	   TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

	   // Modes:
	   // 0 None
	   // 1 Task Proposal
	   // 2 Attribute Proposal
	   // 3 Task Closing Proposal
	   // 4 Attribute Value Proposal
	   // 5 Property Proposal
    
	   int mode =processor.determineProposalMode("<project><target name=\"$\"", 24, "");
	   assertEquals(5, mode);
	   mode = processor.determineProposalMode("<project><target name=\"${\"", 25, "");
	   assertEquals(5, mode);
	   mode = processor.determineProposalMode("<project><target name=\"${ja.bl\"", 30, "ja.bl");
	   assertEquals(5, mode);
	  
		mode = processor.determineProposalMode("<project><target><echo>${", 25, "");
		assertEquals(5, mode);
   }
    
	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		// Modes:
		// 0 None
		// 1 Task Proposal
		// 2 Attribute Proposal
		// 3 Task Closing Proposal
		// 4 Attribute Value Proposal
		// 5 Property Proposal
    
		int mode = processor.determineProposalMode("<project><prop", 14, "prop");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk<", 14, "");
		assertEquals(1, mode); // allow this case though it is not valid with Ant
		mode = processor.determineProposalMode("<project>", 9, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> ", 10, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project></", 11, "");
		assertEquals(3, mode);
		mode = processor.determineProposalMode("<project>< </project>", 10, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\">a</target></project>", 44, "a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"></target></project>", 43, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><a</target></project>", 45, "<a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, "");
		assertEquals(3, mode);
		mode = processor.determineProposalMode("", 0, "");
		assertEquals(1, mode);
		mode= processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><javac>a</javac></target></project>", 51, "a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, "");
		assertEquals(1, mode);
	}
	
	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskClosingProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
	
		// Modes:
		// 0 None
		// 1 Task Proposal
		// 2 Attribute Proposal
		// 3 Task Closing Proposal
		// 4 Attribute Value Proposal
		// 5 Property Proposal
	
		int mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, "");
		assertEquals(3, mode);
	}

    /**
     * Tests how the prefix will be determined.
     */
    public void testDeterminingPrefix() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // cursor after ${
        String prefix = processor.getPrefixFromDocument("<project><target name=\"${}\"", 25);
        assertEquals("", prefix);

        // cursor after $
        prefix = processor.getPrefixFromDocument("<project><target name=\"${\"", 24);
        assertEquals("", prefix);

        // cursor after ${ja.
        prefix = processor.getPrefixFromDocument("<project><target name=\"${ja.\"", 28);
        assertEquals("ja.", prefix);
        
        // cursor after <
		prefix = processor.getPrefixFromDocument("<project><", 10);
		assertEquals("", prefix);
		
		prefix = processor.getPrefixFromDocument("<project name= \"test\"><tar", 26);
		assertEquals("tar", prefix);
    }    

    /**
     * Tests parsing an XML file with the use of our AntEditorSaxDefaultHandler.
     */
    public void testXMLParsingWithAntEditorDefaultHandler() throws ParserConfigurationException, IOException {
        SAXParser parser = getSAXParser();
		File file= getBuildFile("test1.xml");
		String fileContent= getFileContentAsString(file);
        AntEditorSaxDefaultHandler handler = new AntEditorSaxDefaultHandler(new org.eclipse.jface.text.Document(fileContent), file.getParentFile(), 4, 8);
        InputStream stream = new FileInputStream(file);
		parse(stream, parser, handler, file);
        Element element = handler.getParentElement(true);
        assertNotNull(element);
        assertEquals("klick", element.getTagName());
        NodeList childNodes = element.getChildNodes();
        assertEquals(1, childNodes.getLength());
        assertEquals("gurgel", ((Element)childNodes.item(0)).getTagName());
		stream.close();
		
		file= getBuildFile("test2.xml");
		fileContent= getFileContentAsString(file);
        handler = new AntEditorSaxDefaultHandler(new org.eclipse.jface.text.Document(fileContent), file.getParentFile(), 4, 8);
        stream = new FileInputStream(file);
		parse(stream, parser, handler, file);
        element = handler.getParentElement(false);
        assertNotNull(element);
        assertEquals("klick", element.getTagName());
        childNodes = element.getChildNodes();
        assertEquals(4, childNodes.getLength());
        assertEquals("gurgel", ((Element)childNodes.item(0)).getTagName());
        assertEquals("hal", ((Element)childNodes.item(1)).getTagName());
        assertEquals("klack", ((Element)childNodes.item(2)).getTagName());
        assertEquals("humpf", ((Element)childNodes.item(3)).getTagName());
		stream.close();
		
		file= getBuildFile("test3.xml");
		fileContent= getFileContentAsString(file);
        handler = new AntEditorSaxDefaultHandler(new org.eclipse.jface.text.Document(fileContent), file.getParentFile(), 3, 1);
        stream = new FileInputStream(file);
		parse(stream, parser, handler, file);
        element = handler.getParentElement(true);
        assertNotNull(element);
        assertEquals("bla", element.getTagName());
		stream.close();

		file= getBuildFile("test4.xml");
		fileContent= getFileContentAsString(file);
        handler = new AntEditorSaxDefaultHandler(new org.eclipse.jface.text.Document(fileContent), file.getParentFile(), 0, 46);
        stream = new FileInputStream(file);
		parse(stream, parser, handler, file);
        element = handler.getParentElement(true);
        assertNotNull(element);
        assertEquals("target", element.getTagName());
        stream.close();
    }
    
    /**
     * Tests how the processor determines the proposal mode.
     */
    public void testDeterminingNoneProposalMode() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // Modes:
        // 0 None
        // 1 Task Proposal
        // 2 Attribute Proposal
        // 3 Task Closing Proposal
        // 4 Attribute Value Proposal
        // 5 Property Proposal
        
        int mode = processor.determineProposalMode("<project><prop bla", 18, "bla");
        assertEquals(0, mode);
		mode= processor.determineProposalMode("<project default=\"hey\"><target name=", 37, "name=");
		assertEquals(0, mode);
	}
	
	public void testPropertiesWithWholeDocString() {
		// test with not valid whole document string
		Project antProject = new Project();
		antProject.init();
		File file= getBuildFile("empty.xml");
		antProject.setUserProperty("ant.file", file.getAbsolutePath());
		StringBuffer buff = new StringBuffer();
		buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buff.append("<project name=\"testproject\" basedir=\".\" default=\"main\">");
		buff.append("<property name=\"propA\" value=\"valA\" />\n");
		buff.append("<property file=\"buildtest1.properties\" />\n");
		buff.append("<target name=\"main\" depends=\"properties\">\n");
	   try {
	   		ProjectHelper projectHelper= new ProjectHelper();
	   		projectHelper.setBuildFile(file);
	   		projectHelper.parse(antProject, buff.toString());  // File will be parsed here
	   }
	   catch (BuildException e) {
		   	//ignore a build exception on purpose
	   		//as the document does not start and end within the same entity
	   }    
	   Map map = antProject.getProperties();
	   assertEquals("valA", map.get("propA"));
	   assertEquals("val2", map.get("prop2"));
	}
}