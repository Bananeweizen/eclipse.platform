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
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Available;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.PathConvert;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.UpToDate;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.eclipse.ant.internal.ui.dtd.IAttribute;
import org.eclipse.ant.internal.ui.dtd.IDfm;
import org.eclipse.ant.internal.ui.dtd.IElement;
import org.eclipse.ant.internal.ui.dtd.ISchema;
import org.eclipse.ant.internal.ui.dtd.ParseError;
import org.eclipse.ant.internal.ui.dtd.Parser;
import org.eclipse.ant.internal.ui.editor.utils.ProjectHelper;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * The text completion processor for the Ant Editor.
 */
public class AntEditorCompletionProcessor implements IContentAssistProcessor {       
 
 	private Comparator proposalComparator= new Comparator() {
		public int compare(Object o1, Object o2) {
			String string1 = ((ICompletionProposal)o1).getDisplayString();
			String string2 = ((ICompletionProposal)o2).getDisplayString();
			return string1.compareToIgnoreCase(string2);
		}
 	};
	
	private final static int PROPOSAL_MODE_NONE = 0;
	private final static int PROPOSAL_MODE_TASK_PROPOSAL = 1;
	private final static int PROPOSAL_MODE_ATTRIBUTE_PROPOSAL = 2;
	private final static int PROPOSAL_MODE_TASK_PROPOSAL_CLOSING = 3;
	private final static int PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL = 4;
	private final static int PROPOSAL_MODE_PROPERTY_PROPOSAL = 5;
    
    /**
     * The line where the cursor sits now.
     * <P>
     * The first line has index '1'.
     */
	protected int lineNumber = -1;

    /**
     * The startingColumn where the cursor sits now.
     * <P>
     * The first startingColumn has index '1'.
     */
	protected int columnNumber = -1;
    
	/**
	 * The additional offset required from a required attribute to
	 * place the cursor for the current proposal
	 */
	private int additionalProposalOffset = -1;
    
    private static final String ANT_1_5_DTD_FILENAME = "/ant1.5b.dtd"; //$NON-NLS-1$

    /**
     * The dtd.
     */
	private static ISchema dtd;

    /**
     * Cursor position, counted from the beginning of the document.
     * <P>
     * The first position has index '0'.
     */
	protected int cursorPosition = -1;
	
    /**
     * The text viewer.
     */
	private ITextViewer viewer;
	
	/**
	 * The set of characters that will trigger the activation of the
	 * completion proposal computation.
	 */
	private char[] autoActivationChars= null;
	
    /**
     * The provider for all task and attribute descriptions.
     */
	private TaskDescriptionProvider descriptionProvider = new TaskDescriptionProvider();
	private AntEditorSaxDefaultHandler lastDefaultHandler;
	
	private String errorMessage;
	
	private ProjectHelper projectHelper;
	
	/**
	 * Constructor for AntEditorCompletionProcessor.
	 */
	public AntEditorCompletionProcessor() {
		super();
		if(dtd == null) {
	        try {
	     	  	dtd = parseDtd();
	        } catch (IOException e) {
	        	AntUIPlugin.log(e);
	        } catch (ParseError e) {
				AntUIPlugin.log(e);
			}
		}
		projectHelper= new ProjectHelper();
	}

    /**
     * Parses the dtd.
     */
    private ISchema parseDtd() throws ParseError, IOException {
        InputStream stream = getClass().getResourceAsStream(ANT_1_5_DTD_FILENAME);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8"); //$NON-NLS-1$
        Parser parser = new Parser();
        ISchema schema= parser.parseDTD(reader, "project"); //$NON-NLS-1$
        reader.close();
        return schema;
    }
    
	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer refViewer, int documentOffset) {
		this.viewer = refViewer;
		return determineProposals();
	}
	
	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer refViewer, int documentOffset) {
		return new IContextInformation[0];
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
        return autoActivationChars;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
        return null;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
    /**
     * Returns the new determined proposals.
     */ 
	private ICompletionProposal[] determineProposals() {
		
		cursorPosition = ((ITextSelection)viewer.getSelectionProvider().getSelection()).getOffset();
        
        IDocument doc = viewer.getDocument();
        try {
            lineNumber = doc.getLineOfOffset(cursorPosition);
            columnNumber = cursorPosition - doc.getLineOffset(lineNumber);
        } catch (BadLocationException e) {
            AntUIPlugin.log(e);
        }
		
		String prefix = getCurrentPrefix();
		if (prefix == null || cursorPosition == -1) {
			AntUIPlugin.getStandardDisplay().beep();
			return null;
		}
	
		ICompletionProposal[] proposals = getProposalsFromDocument(doc, prefix);
		return proposals;
		
	}

    /**
     * Returns the proposals for the specified document.
     */
    private ICompletionProposal[] getProposalsFromDocument(IDocument document, String prefix) {
        String taskString = null;
		ICompletionProposal[] proposals= null;
        /*
         * Completions will be determined depending on the proposal mode.
         */
        switch (determineProposalMode(document, cursorPosition, prefix)) {

            case PROPOSAL_MODE_ATTRIBUTE_PROPOSAL:
                taskString = getTaskStringFromDocumentStringToPrefix(document.get().substring(0, cursorPosition-prefix.length()));
                proposals= getAttributeProposals(taskString, prefix);
                if (proposals.length == 0) {
                	errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.28"); //$NON-NLS-1$
                }
                break;
            case PROPOSAL_MODE_TASK_PROPOSAL:
				proposals= getTaskProposals(document, findParentElement(document, lineNumber, columnNumber), prefix);
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.29"); //$NON-NLS-1$
            	}
				break;
            case PROPOSAL_MODE_TASK_PROPOSAL_CLOSING:
                proposals= getClosingTaskProposals(findNotClosedParentElement(document, lineNumber, columnNumber), prefix);
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.30"); //$NON-NLS-1$
            	}
                break;
            case PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL:
            	String textToSearch= document.get().substring(0, cursorPosition-prefix.length());
                taskString = getTaskStringFromDocumentStringToPrefix(textToSearch);
                String attributeString = getAttributeStringFromDocumentStringToPrefix(textToSearch);
                if ("target".equalsIgnoreCase(taskString)) { //$NON-NLS-1$
                	proposals= getTargetAttributeValueProposals(document, textToSearch, prefix, attributeString);
                } else {
                	proposals=getAttributeValueProposals(taskString, attributeString, prefix);
                }
				if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.31"); //$NON-NLS-1$
				}
				break;
            case PROPOSAL_MODE_PROPERTY_PROPOSAL:
				proposals= getPropertyProposals(document, prefix, cursorPosition);
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.32"); //$NON-NLS-1$
            	}
				break;
			case PROPOSAL_MODE_NONE :
            default :
                proposals= new ICompletionProposal[0];
            	if (proposals.length == 0) {
				   errorMessage= AntEditorMessages.getString("AntEditorCompletionProcessor.33"); //$NON-NLS-1$
            	}
        } 
        Arrays.sort(proposals, proposalComparator);
        if (proposals.length > 0) {
        	errorMessage= ""; //$NON-NLS-1$
        }
        return proposals;

    }
    
	protected ICompletionProposal[] getTargetAttributeValueProposals(IDocument document, String textToSearch, String prefix, String attributeName) {
		if (attributeName.equalsIgnoreCase("depends")) { //$NON-NLS-1$
			return getDependsValueProposals(document, prefix);
		} else if (attributeName.equalsIgnoreCase("if") || attributeName.equalsIgnoreCase("unless")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (!textToSearch.trim().endsWith(",")) { //$NON-NLS-1$
				return getPropertyProposals(document, prefix, cursorPosition);
			}
		}
		
		return new ICompletionProposal[0];
	}

	private ICompletionProposal[] getDependsValueProposals(IDocument document, String prefix) {
		List possibleDependencies = new ArrayList();
		Element element = findEnclosingTargetElement(document, lineNumber, columnNumber);
		String currentTargetName = null;
		if(element == null 
				|| (currentTargetName = element.getAttribute("name")) == null //$NON-NLS-1$
				|| currentTargetName.length() == 0) {
			return new ICompletionProposal[0];
		}
			
		Map targets= findTargets(document);
		Set targetNames= targets.keySet();
		Iterator itr= targetNames.iterator();
		Enumeration dependencies= null;
		while (itr.hasNext()) {
			String targetName = (String) itr.next();
			if (targetName.equals(currentTargetName)) {
				Target currentTarget= (Target)targets.get(targetName);
				dependencies= currentTarget.getDependencies();
				continue;
			}
			if (targetName.toLowerCase().startsWith(prefix) && targetName.length() > 0){
				possibleDependencies.add(targetName);
			}
		}
		
		if (dependencies != null) {
			while (dependencies.hasMoreElements()) {
				possibleDependencies.remove(dependencies.nextElement());
			}
		}
		
		ICompletionProposal[] proposals= new ICompletionProposal[possibleDependencies.size()];
		int i= 0;
		for (Iterator iter = possibleDependencies.iterator(); iter.hasNext(); i++) {
			String targetName = (String) iter.next();
			ICompletionProposal proposal = new AntCompletionProposal(targetName, cursorPosition - prefix.length(), prefix.length(), targetName.length(), null, targetName, null, AntCompletionProposal.TASK_PROPOSAL);
			proposals[i]= proposal;
		}
		return proposals;
	}

	/**
     * Returns all possible attributes for the specified task.
     * 
     * @param aTaskName the name of the task for that the attribute shall be 
     * completed
     * @param aPrefix prefix, that all proposals should start with. The prefix
     * may be an empty string.
     */
    protected ICompletionProposal[] getAttributeProposals(String aTaskName, String aPrefix) {
        List proposals = new ArrayList();
        IElement element = dtd.getElement(aTaskName);
        if (element != null) {
        	Iterator keys = element.getAttributes().keySet().iterator();
        	while (keys.hasNext()) {
        		String attrName = (String) keys.next();
        		if (attrName.toLowerCase().startsWith(aPrefix)) {
        			IAttribute dtdAttributes = (IAttribute) element.getAttributes().get(attrName);
					String replacementString = attrName+"=\"\""; //$NON-NLS-1$
					String displayString = attrName;
					String[] items = dtdAttributes.getEnum();
					if (items != null) {					        			
                        if(items.length > 1) {
                            displayString += " - ("; //$NON-NLS-1$
                        }
                        for (int i = 0; i < items.length; i++) {
                            displayString += items[i];
                            if(i+1 < items.length) {
                                displayString += " | "; //$NON-NLS-1$
                            } else {
                                displayString += ")"; //$NON-NLS-1$
                            }
                        }
                    }
                    
                    String proposalInfo = null;
                    String required = descriptionProvider.getRequiredAttributeForTaskAttribute(aTaskName, attrName);
                    if(required != null && required.length() > 0) {
                        proposalInfo = AntEditorMessages.getString("AntEditorCompletionProcessor.Required___4") + required; //$NON-NLS-1$
                        proposalInfo += "<BR><BR>"; //$NON-NLS-1$
                    }
                    String description = descriptionProvider.getDescriptionForTaskAttribute(aTaskName, attrName);
                    if(description != null) {
                        proposalInfo = (proposalInfo == null ? "" : proposalInfo); //$NON-NLS-1$
                        proposalInfo += description;
                    }
                    
                    ICompletionProposal proposal = new AntCompletionProposal(replacementString, cursorPosition - aPrefix.length(), aPrefix.length(), attrName.length()+2, null, displayString, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
                    proposals.add(proposal);
                }       
            }
        }
        return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
    }


    /**
     * Returns all possible values for the specified attribute of the specified 
     * task.
     * 
     * @param aTaskName the name of the task that the specified attribute 
     * belongs to.
     * 
     * @param anAttributeName the name of the attribute for that the value
     * shall be completed
     * 
     * @param aPrefix prefix, that all proposals should start with. The prefix
     * may be an empty string.
     */
    private ICompletionProposal[] getAttributeValueProposals(String aTaskName, String anAttributeName, String aPrefix) {
        List proposals = new ArrayList();
        IElement taskElement = dtd.getElement(aTaskName);
        if (taskElement != null) {
        	IAttribute attribute = (IAttribute) taskElement.getAttributes().get(anAttributeName);
        	if (attribute != null) {
        		String[] items = attribute.getEnum();
        		if (items != null) {
					String item;
                    for (int i = 0; i < items.length; i++) {
                        item= items[i];
                        if(item.toLowerCase().startsWith(aPrefix)) {
                            ICompletionProposal proposal = new AntCompletionProposal(item, cursorPosition - aPrefix.length(), aPrefix.length(), item.length(), null, item, null, AntCompletionProposal.TASK_PROPOSAL);
                            proposals.add(proposal);
                        }
                    }
        		}
            }
        }
        return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    
    /**
     * Returns all possible properties for the specified prefix.
     * <P>
     * Note that the completion mode must be property mode, otherwise it is not
     * safe to call this method.
     */
    protected ICompletionProposal[] getPropertyProposals(IDocument document, String aPrefix, int aCursorPosition) {
        List proposals = new ArrayList();
        Map displayStringToProposals= new HashMap();
        Map properties = findPropertiesFromDocument(document);
		String propertyName;
		Image image = AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
		// Determine replacement length and offset
	   // String from beginning to the beginning of the prefix
	   int replacementLength = aPrefix.length();
	   int replacementOffset = 0;
	   String text= document.get();
	   String stringToPrefix = text.substring(0, aCursorPosition - aPrefix.length());
	   // Property proposal
	   String lastTwoCharacters = stringToPrefix.substring(stringToPrefix.length()-2, stringToPrefix.length());
	   boolean appendBraces= true;
	   if(lastTwoCharacters.equals("${")) { //$NON-NLS-1$
		   replacementLength += 2;
		   replacementOffset = aCursorPosition - aPrefix.length() - 2;
	   } else if(lastTwoCharacters.endsWith("$")) { //$NON-NLS-1$
		   replacementLength += 1;
		   replacementOffset = aCursorPosition - aPrefix.length() - 1;                
	   } else {
	   		replacementOffset= aCursorPosition - aPrefix.length();
	   		appendBraces= false;
	   }
	   
	   if(text.length() > aCursorPosition && text.charAt(aCursorPosition) == '}') {
		   replacementLength += 1;
	   }
        for(Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
            propertyName= (String)i.next();
            if(propertyName.toLowerCase().startsWith(aPrefix)) {
                String additionalPropertyInfo = (String)properties.get(propertyName);
                
                StringBuffer replacementString = new StringBuffer();
                if (appendBraces) {
                	replacementString.append("${"); //$NON-NLS-1$
                }
                replacementString.append(propertyName);
                if (appendBraces) {
                	replacementString.append('}');
                }
                
				if (displayStringToProposals.get(propertyName) == null) {
                	ICompletionProposal proposal = 
		                new AntCompletionProposal(
		                    replacementString.toString(), replacementOffset, replacementLength, 
		                    replacementString.length(), image, propertyName,
		                    additionalPropertyInfo, AntCompletionProposal.PROPERTY_PROPOSAL);
					proposals.add(proposal);
					displayStringToProposals.put(propertyName, proposal);
				}
            }
        }      
		return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);          
    }


    /**
     * Returns all possible attributes for the specified parent task element.
     * <P>
     * No completions will be returned if <code>aParentTaskElement</code> is 
     * not known.
     * 
     * @param aWholeDocumentString the entire document up to the cursor position 
     * 
     * @param aParentTaskName name of the parent(surrounding) task or 
     * <code>null</code> if completion should be done for the root element.
     * 
     * @param aPrefix prefix, that all proposals should start with. The prefix
     * may be an empty string.
     */
    protected ICompletionProposal[] getTaskProposals(IDocument document, Element aParentTaskElement, String aPrefix) {
		// The code this replaced assumed that child elements
		// are unordered; there was no provision for walking
		// through a child sequence. This works for the Ant
		// 1.5 DTD but not in general. I kept the assumption. bf
        List proposals = new ArrayList();
     
        if (aParentTaskElement == null) {
        	// DTDs do not designate a root element.
        	// The previous code must have looked for an element that
        	// was not in the content model of any other element.
        	// This test doesn't work with a DTD used to validate
        	// document partitions, a DTD with multiple candidate
        	// roots, etc. The right answer is to get
        	// the root element from the document. If there isn't
        	// one, we assume "project". bf
            String rootElementName = null;
	       	if (lastDefaultHandler != null) {
                rootElementName = lastDefaultHandler.rootElementName;
        	}
			if (rootElementName == null) {
				rootElementName = "project"; //$NON-NLS-1$
			}
			IElement rootElement = dtd.getElement(rootElementName);
			if(rootElement != null && rootElementName.toLowerCase().startsWith(aPrefix)) {
				additionalProposalOffset= 0;
				ICompletionProposal proposal = newCompletionProposal(document, aPrefix, rootElementName);
				proposals.add(proposal);
			}
        } else {
			IElement parent = dtd.getElement(aParentTaskElement.getTagName());
			if (parent != null) {
				IDfm dfm = parent.getDfm();
				String[] accepts = dfm.getAccepts();
				String elementName;
				ICompletionProposal proposal;
				for (int i = 0; i < accepts.length; i++) {
					additionalProposalOffset= 0;
					elementName = accepts[i];
					if(elementName.toLowerCase().startsWith(aPrefix)) {
						proposal = newCompletionProposal(document, aPrefix, elementName);
						proposals.add(proposal);
					}
				}
			}
        }
        
       return (ICompletionProposal[])proposals.toArray(new ICompletionProposal[proposals.size()]);
   }

    private ICompletionProposal newCompletionProposal(IDocument document, String aPrefix, String elementName) {
		Image proposalImage = AntUIImages.getImage(IAntUIConstants.IMG_TASK_PROPOSAL);
		String proposalInfo = descriptionProvider.getDescriptionForTask(elementName);
		String replacementString = getTaskProposalReplacementString(elementName);
		int replacementOffset = cursorPosition - aPrefix.length();
		int replacementLength = aPrefix.length();
		if(replacementOffset > 0 && document.get().charAt(replacementOffset-1) == '<') {
			replacementOffset--;
			replacementLength++;
		}
		return new AntCompletionProposal(replacementString, replacementOffset, 
			replacementLength, elementName.length() + 2 + additionalProposalOffset, 
			proposalImage, elementName, proposalInfo, AntCompletionProposal.TASK_PROPOSAL);
	}

	/**
     * Returns the one possible completion for the specified unclosed task 
     * element.
     * 
     * @param unclosedTaskElement the task element that hasn't been closed 
     * last
     * 
     * @param aPrefix prefix, that the one possible proposals should start 
     * with. The prefix may be an empty string.
     * 
     * @return array which may contain either one or none proposals
     */
    private ICompletionProposal[] getClosingTaskProposals(Element unclosedTaskElement, String prefix) {
		ICompletionProposal[] proposals= null;
        if(unclosedTaskElement != null) {
            if(unclosedTaskElement.getTagName().toLowerCase().startsWith(prefix)) {
                String replaceString = unclosedTaskElement.getTagName();
                proposals= new ICompletionProposal[1];
                proposals[0]= new AntCompletionProposal(replaceString + '>', cursorPosition - prefix.length(), prefix.length(), replaceString.length()+1, null, replaceString, null, AntCompletionProposal.TASK_PROPOSAL);
            }
        }
        if (proposals == null) {
        	proposals= new ICompletionProposal[0];
        }
        return proposals;
    }

    /**
     * Returns the replacement string for the specified task name.
     */
    private String getTaskProposalReplacementString(String aTaskName) {
        StringBuffer replacement = new StringBuffer("<"); //$NON-NLS-1$
        replacement.append(aTaskName); 
        Node attributeNode= descriptionProvider.getAttributesNode(aTaskName);
		
        if (attributeNode != null) {
			appendRequiredAttributes(replacement, attributeNode);
        } else if ("project".equals(aTaskName)){ //$NON-NLS-1$
        	replacement.append(" default=\"\""); //$NON-NLS-1$
			additionalProposalOffset= 9;
        }
        
        if (isEmpty(aTaskName)) {
            replacement.append("/>"); //$NON-NLS-1$
        } else {
            replacement.append("></"); //$NON-NLS-1$
            replacement.append(aTaskName);
            replacement.append('>');
        }
        return replacement.toString();               
    }

    private void appendRequiredAttributes(StringBuffer replacement, Node attributeNode) {
		boolean requiredAdded= false;
		NodeList attributes= attributeNode.getChildNodes();
		String required;
		Node attribute;
		for (int i = 0; i < attributes.getLength(); i++) {
			attribute = attributes.item(i);
			required= descriptionProvider.getRequiredOfNode(attribute);
			if (required.equalsIgnoreCase("yes")) { //$NON-NLS-1$
				String attributeName= descriptionProvider.getTaskAttributeName(attribute);
				replacement.append(' ');
				replacement.append(attributeName);
				replacement.append("=\"\""); //$NON-NLS-1$
				if (!requiredAdded){
					additionalProposalOffset= attributeName.length() + 2;
					requiredAdded= true;
				}	
			}
		}
	}

	/**
     * Returns whether the named element is empty, thus may not have any child
     * elements.
     */
    private boolean isEmpty(String aDTDElementName) {
        IElement element = dtd.getElement(aDTDElementName);
        return element.isEmpty();
    }
    
    
    /**
     * Finds a direct child element with <code>aChildElementName</code> of 
     * <code>anElement</code>.
     * <P>
     * The child will not be searched for in the whole hierarchy but only in
     * the hierarchy step below.
     * 
     * @return the found child or <code>null</code> if not found.
     */
    protected Element findChildElementNamedOf(Element anElement, String aChildElementName) {
        NodeList nodeList = anElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                if(childNode.getNodeName().equals(aChildElementName)) {
                    return (Element)childNode;
                }
            }   
        }
        return null;
    }

	/**
     * Determines the current prefix, that should be used for completion.
     */
	private String getCurrentPrefix() {

		ITextSelection selection = (ITextSelection)viewer.getSelectionProvider().getSelection();
		
		if (selection.getLength() > 0) {
			return null;
		}
		
		IDocument doc = viewer.getDocument();

        return getPrefixFromDocument(doc.get(), selection.getOffset()).toLowerCase();
	}


    /**
     * Returns the prefix in the specified document text with respect to the 
     * specified offset.
     * 
     * @param aDocumentText the whole content of the edited file as String
     * @param anOffset the cursor position
     */
    protected String getPrefixFromDocument(String aDocumentText, int anOffset) {
        
        int startOfWordToken = anOffset;
        
        char token= 'a';
        if (startOfWordToken > 0) {
			token= aDocumentText.charAt(startOfWordToken - 1);
        }
        
        while (startOfWordToken > 0 
                && (Character.isJavaIdentifierPart(token) 
                    || '.' == token
					|| '-' == token
        			|| ';' == token)
                && !('$' == token)) {
            startOfWordToken--;
			token= aDocumentText.charAt(startOfWordToken - 1);
        }
        
        if (startOfWordToken != anOffset) {
            return aDocumentText.substring(startOfWordToken, anOffset);
        } else {
            return ""; //$NON-NLS-1$
        }
    }
 
 
    /**
     * Returns the current proposal mode.
     */
    protected int determineProposalMode(IDocument document, int aCursorPosition, String aPrefix) {

        // String from beginning of document to the beginning of the prefix
    	String text= document.get();
        String stringToPrefix = text.substring(0, aCursorPosition - aPrefix.length());

        // Is trimmable from behind
        String trimmedString = stringToPrefix.trim();
        char lastChar = 0;
        if(trimmedString.length() > 0) {
	        lastChar = trimmedString.charAt(trimmedString.length()-1);
        } else {
        	return PROPOSAL_MODE_TASK_PROPOSAL;
        }
        if(stringToPrefix.charAt(stringToPrefix.length()-1) != lastChar && lastChar != '>' && lastChar != ',') {
            /*
             * Substring must be trimmable from behind in case of attribute 
             * proposal because a space or a new line must be used as delimiter 
             * between task name and attribute or attribute and attribute.
             * Example: '<property id="bla" name="hups"'
             */
             
            // Attribute proposal
            if(lastChar != '>' && lastChar != '<') {
                String taskString =
                    getTaskStringFromDocumentStringToPrefix(
                        trimmedString);
                if(taskString != null && isNamedTaskKnown(taskString)) {
                    return PROPOSAL_MODE_ATTRIBUTE_PROPOSAL;
                }
            }                
        } else if(stringToPrefix.charAt(stringToPrefix.length()-1) == '"' || trimmedString.charAt(trimmedString.length()-1) == ',') {
			// Attribute value proposal
            String taskString =
                getTaskStringFromDocumentStringToPrefix(
                    trimmedString);
            if(taskString != null && isNamedTaskKnown(taskString)) {
                return PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL;
            }
        } else {  // Possibly a Task proposal
            int spaceIndex = stringToPrefix.lastIndexOf(' ');
            int lessThanIndex = stringToPrefix.lastIndexOf('<');
            int greaterThanIndex = stringToPrefix.lastIndexOf('>');
            
            // Task proposal
            if(lessThanIndex > spaceIndex && greaterThanIndex < lessThanIndex) {
                int slashIndex = stringToPrefix.lastIndexOf('/');
                if(slashIndex == lessThanIndex +1) {
                    return PROPOSAL_MODE_TASK_PROPOSAL_CLOSING; // ... </
                }
                return PROPOSAL_MODE_TASK_PROPOSAL;
            }
            if(lessThanIndex < greaterThanIndex) {
            	if (isPropertyProposalMode(stringToPrefix)) {
				   return PROPOSAL_MODE_PROPERTY_PROPOSAL;
			   }
               return PROPOSAL_MODE_TASK_PROPOSAL;
            }
        }

        // Property proposal
		if (isPropertyProposalMode(stringToPrefix)) {
			return PROPOSAL_MODE_PROPERTY_PROPOSAL;
        }
        	            
        return PROPOSAL_MODE_NONE;
    }


	private boolean isPropertyProposalMode(String stringToPrefix) {
		if(stringToPrefix.length() >= 2) {
			String lastTwoChars = stringToPrefix.substring(stringToPrefix.length()-2, stringToPrefix.length());
			if(lastTwoChars.equals("${") || //$NON-NLS-1$
				stringToPrefix.charAt(stringToPrefix.length()-1) == '$') {
					return true;
			}
		}
		return false;
	}
    /**
     * Returns the last occuring task string in the specified string.
     * <P>
     * The returned string must not necessarily be a valid Ant task string.
     * This can be tested with the method <code>inNamedTaskKnown(String)</code>
     * after invoking this method.
     * 
     * @param aDocumentStringToPrefix the String that contains the whole string
     * of the currently edited file from the beginning up to the prefix for code
     * completion. Example: '<project default="name"><property '.
     * 
     * @return the extracted task string or <code>null</code> if no string could
     * be extracted.
     */
    private String getTaskStringFromDocumentStringToPrefix(String aDocumentStringToPrefix) {
            
        int lessThanIndex = aDocumentStringToPrefix.lastIndexOf('<');

        if(lessThanIndex > -1) {
            String taskString = aDocumentStringToPrefix.trim();
            taskString = taskString.substring(lessThanIndex+1, taskString.length());
            int index = taskString.indexOf(' ');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            index = taskString.indexOf('\n');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            index = taskString.indexOf('\r');
            if(index > 0) {
                taskString = taskString.substring(0, index);
            }
            return taskString;
        }
        
        return null;
    }
    

    /**
     * Returns the last occuring attribute string in the specified string.
     * <P>
     * Calling this method is only safe if the current proposal mode is really
     * <code>PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL</code>.
     */
    private String getAttributeStringFromDocumentStringToPrefix(String docStringToPrefix) {
        int index = docStringToPrefix.lastIndexOf('=');
        String subString = docStringToPrefix.substring(0, index);
        subString = subString.trim();
        
        index = subString.lastIndexOf(' ');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        index = subString.lastIndexOf('\n');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        index = subString.lastIndexOf('\r');
        if(index > 0) {
            subString = subString.substring(index+1, subString.length());
        }
        return subString;
    }


    /**
     * Returns whether the specified task name is known according to the DTD.
     */
    private boolean isNamedTaskKnown(String aTaskName) {
        return dtd.getElement(aTaskName) != null;
    }


    /**
     * Finds the parent task element in respect to the cursor position.
     * 
     * @return the parent task element or <code>null</code> if not found.
     */
    protected Element findParentElement(IDocument document, int aLineNumber, int aColumnNumber) {
        return parseEditedFileSearchingForParent(document, aLineNumber, aColumnNumber).getParentElement(true);      
    }
    

    /**
     * Parses the actually edited file as far as possible.
     * <P>
     * The returned handler can be asked about what happened while parsing
     * the document.
     * 
     * @return the handler that has been used for parsing or <code>null</code>
     * if parsing couldn't be done because of some error.
     */
    private AntEditorSaxDefaultHandler parseEditedFileSearchingForParent(IDocument document, int aLineNumber, int aColumnNumber) {
		SAXParser parser = getSAXParser();
		if(parser == null){
			return null;
		}
        
		//Set the handler
		AntEditorSaxDefaultHandler handler = null;
		File editedFile= getEditedFile();
		try {
			File parent = null;
			if(editedFile != null) {
				parent = editedFile.getParentFile();
			}
			handler = new AntEditorSaxDefaultHandler(document, parent, aLineNumber, aColumnNumber);
		} catch (ParserConfigurationException e) {
			AntUIPlugin.log(e);
		}
        
	    parse(document, parser, handler, editedFile);
	    
        lastDefaultHandler = handler;
        return handler;
    }    



	private void parse(IDocument document, SAXParser parser, AntEditorSaxDefaultHandler handler, File editedFile) {
		InputSource inputSource = new InputSource(new StringReader(document.get()));
		if (editedFile != null) {
			//needed for resolving relative external entities
			inputSource.setSystemId(editedFile.getAbsolutePath());
		}
		
		try {
			parser.parse(inputSource, handler);
		} catch(SAXParseException e) {
		   // Ignore since that happens always if the edited file is not valid. We try to handle that.
		} catch (SAXException e) {
			AntUIPlugin.log(e);
		} catch (IOException e) {
			//ignore since can happen when user has incorrect paths / protocols for external entities
		}
	}

	private SAXParser getSAXParser() {
		SAXParser parser = null;
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException e) {
			AntUIPlugin.log(e);
		} catch (SAXException e) {
			AntUIPlugin.log(e);
		}
		return parser;
	}


    /**
     * Parses the actually edited file as far as possible.
     * <P>
     * We use the parsing facilities of the ant plug-in here.
     * 
     * @return a map with all the found properties
     */
    private Map findPropertiesFromDocument(IDocument document) {
		/*
		 * What is implemented here:
		 * - We first use the ant plug-in to create a Project instance.
		 * - We determine the enclosing parent task element
		 * - We determine the dependency Vector for the parent task element
		 * - We work our way through the dependency Vector and execute the
		 *   Property relevant tasks.
		 */

    	Project project= parseAndConfigureProject(document);
    	Map properties = project.getProperties();
        
        // Determine the parent
        Element element = findEnclosingTargetElement(document, lineNumber, columnNumber);
        String targetName = null;
        if(element == null 
        		|| (targetName = element.getAttribute("name")) == null //$NON-NLS-1$
        		|| targetName.length() == 0) {
        	return properties;
        }
        List sortedTargets = null;
        try {
        	sortedTargets= project.topoSort(targetName, project.getTargets());
        } catch (BuildException be) {
			return project.getProperties();
        }

        int curidx = 0;
        Target curtarget;

        do {
            curtarget = (Target) sortedTargets.get(curidx++);
			Task[] tasks = curtarget.getTasks();
			
			for (int i = 0; i < tasks.length; i++) {
				Task task = tasks[i];                

				if (task instanceof UnknownElement) {
					try {
						task.maybeConfigure();
					} catch (BuildException be) {
						continue;
					}
					task= ((UnknownElement)task).getTask();
				}
				
				// sequential
				if(task instanceof Sequential) {
					// (T)
				}
				
				// parallel
				if(task instanceof Parallel) {
					// (T)
				}
				
				// waitfor (@since Ant 1.5)
	//			if(tempTask instanceof WaitFor) { 
	//				// (T)
	//			}
				
				if(task instanceof Property 
					|| task instanceof PathConvert
					|| task instanceof Available
					|| task instanceof UpToDate
					|| task instanceof Condition) {
						task.perform();					
				}
			
			
				// Ant 1.5
//				if(tempTask instanceof LoadFile) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof XmlProperty) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof Basename) {
//				
//				}
			
				
				// Ant 1.5
//				if(tempTask instanceof Dirname) {
//				
//				}
			
				// Ant 1.5
//				if(tempTask instanceof LoadProperties) {
//				
//				}
            }
        } while (!curtarget.getName().equals(targetName));

        
        // Need to reget it since tempTable hasn't been updated with Ant 1.5
        return project.getProperties();
    }
    
    /**
     * Parses the actually edited file as far as possible.
     * <P>
     * We use the parsing facilities of the ant plug-in here.
     * 
     * @return a map with all the found properties
     */
    private Map findTargets(IDocument document) {
    	Project project = parseAndConfigureProject(document);    
    	return project.getTargets();
    }

    /**
	 * @param document
	 * @return
	 */
	private Project parseAndConfigureProject(IDocument document) {
		// Create an initialized project
    	Project project = new Project();
    	project.init();

    	/* 
    	 * Ant's parsing facilities always works on a file, therefore we need
    	 * to determine the actual location of the file. Though the file 
    	 * contents will not be parsed. We parse the passed document string.
    	 */
    	File file = getEditedFile();
    	String filePath= ""; //$NON-NLS-1$
    	if (file != null) {
    		filePath= file.getAbsolutePath();
    	}
    	project.setUserProperty("ant.file", filePath); //$NON-NLS-1$

    	try {
    		projectHelper.setBuildFile(file);
    		projectHelper.parse(project, document.get());  // File will be parsed here
    		projectHelper.getImportStack().removeAllElements();
    		projectHelper.setBuildFile(null);
    	}
    	catch(BuildException e) {
    		// ignore a build exception on purpose, since we also parse invalid
    		// build files.
    	}
		return project;
	}

    protected File getEditedFile() {
    	IWorkbenchPage page= AntUIPlugin.getActivePage();
    	if (page == null) {
    		return null;
    	}
		IEditorPart editor= page.getActiveEditor();
		if (editor == null) {
			return null;
		}
        FileEditorInput input = (FileEditorInput) editor.getEditorInput();
        String projectPath = input.getFile().getProject().getLocation().toFile().getAbsolutePath();
        String  projectRelativeFilePath = input.getFile().getFullPath().removeFirstSegments(1).makeRelative().toString();
        return new File(projectPath + File.separator + projectRelativeFilePath);
    }

    /**
     * Finds the parent task element in respect to the cursor position which
     * that has not been closed yet.
     * 
     * @return the not closed parent task element or <code>null</code> if not 
     * found.
     */
    private Element findNotClosedParentElement(IDocument document, int aLineNumber, int aColumnNumber) {
        AntEditorSaxDefaultHandler handler = parseEditedFileSearchingForParent(document, aLineNumber, aColumnNumber);
        if(handler != null) {
            // A not closed parent element can only be found by guessing.
            if(handler.getParentElement(false) == null) {
                return handler.getParentElement(true);
            }
        }
        return null;
    }
 

    /**
     * Finds the enclosing target element in respect to the cursor position. 
     * 
     * @return the enclosing target element or <code>null</code> if not found.
     */
 	private Element findEnclosingTargetElement(IDocument document, int aLineNumber, int aColumnNumber) {

        // Get a new SAX Parser
        SAXParser parser = getSAXParser();
        if (parser == null) {
        	return null;
        }
        
        // Set the handler
        EnclosingTargetSearchingHandler handler = null;
		File editedFile= getEditedFile();
        try {
		   File parent = null;
		   if(editedFile != null) {
			   parent = editedFile.getParentFile();
		   }
            handler = new EnclosingTargetSearchingHandler(document, parent, aLineNumber, aColumnNumber);
        } catch (ParserConfigurationException e) {
            AntUIPlugin.log(e);
        }
        
		parse(document, parser, handler, editedFile);

		return handler.getParentElement(true);
 	}
	
	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 * 
	 * @param activationSet the activation set
	 */
	public void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		autoActivationChars= activationSet;
	}
}