/*******************************************************************************
 * Copyright (c) 2004 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.formatter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.model.AntProjectNode;
import org.eclipse.ant.internal.ui.editor.outline.AntModel;
import org.eclipse.ant.internal.ui.editor.templates.AntContext;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateVariable;

public class XmlDocumentFormatter {

    private static class CommentReader extends TagReader {

        private boolean complete = false;

        protected void clear() {
            this.complete = false;
        }

        public String getStartOfTag() {
            return "<!--"; //$NON-NLS-1$
        }

        protected String readTag() throws IOException {
            int intChar;
            char c;
            StringBuffer node = new StringBuffer();

            while (!complete && (intChar = reader.read()) != -1) {
                c = (char) intChar;

                node.append(c);

                if (c == '>' && node.toString().endsWith("-->")) { //$NON-NLS-1$
                    complete = true;
                }
            }
            return node.toString();
        }
    }

    private static class DoctypeDeclarationReader extends TagReader {

        private boolean complete = false;

        protected void clear() {
            this.complete = false;
        }

        public String getStartOfTag() {
            return "<!"; //$NON-NLS-1$
        }

        protected String readTag() throws IOException {
            int intChar;
            char c;
            StringBuffer node = new StringBuffer();

            while (!complete && (intChar = reader.read()) != -1) {
                c = (char) intChar;

                node.append(c);

                if (c == '>') {
                    complete = true;
                }
            }
            return node.toString();
        }

    }

    private static class ProcessingInstructionReader extends TagReader {

        private boolean complete = false;

        protected void clear() {
            this.complete = false;
        }

        public String getStartOfTag() {
            return "<?"; //$NON-NLS-1$
        }

        protected String readTag() throws IOException {
            int intChar;
            char c;
            StringBuffer node = new StringBuffer();

            while (!complete && (intChar = reader.read()) != -1) {
                c = (char) intChar;

                node.append(c);

                if (c == '>' && node.toString().endsWith("?>")) { //$NON-NLS-1$
                    complete = true;
                }
            }
            return node.toString();
        }
    }

    private static abstract class TagReader {

        protected Reader reader;

        private String tagText;

        protected abstract void clear();

        public int getPostTagDepthModifier() {
            return 0;
        }

        public int getPreTagDepthModifier() {
            return 0;
        }

        abstract public String getStartOfTag();

        public String getTagText() {
            return this.tagText;
        }

        public boolean isTextNode() {
            return false;
        }

        protected abstract String readTag() throws IOException;

        public boolean requiresInitialIndent() {
            return true;
        }

        public void setReader(Reader reader) throws IOException {
            this.reader = reader;
            this.clear();
            this.tagText = readTag();
        }

        public boolean startsOnNewline() {
            return true;
        }
    }

    private static class TagReaderFactory {

        // Warning: the order of the Array is important!
        private static TagReader[] tagReaders = new TagReader[]{new CommentReader(),
                                                                new DoctypeDeclarationReader(),
                                                                new ProcessingInstructionReader(),
                                                                new XmlElementReader()};

        private static TagReader textNodeReader = new TextReader();

        public static TagReader createTagReaderFor(Reader reader)
                                                                 throws IOException {

            char[] buf = new char[10];
            reader.mark(10);
            reader.read(buf, 0, 10);
            reader.reset();

            String startOfTag = String.valueOf(buf);

            for (int i = 0; i < tagReaders.length; i++) {
                if (startOfTag.startsWith(tagReaders[i].getStartOfTag())) {
                    tagReaders[i].setReader(reader);
                    return tagReaders[i];
                }
            }
            // else
            textNodeReader.setReader(reader);
            return textNodeReader;
        }
    }

    private static class TextReader extends TagReader {

        private boolean complete;

        private boolean isTextNode;

        protected void clear() {
            this.complete = false;
        }

        /* (non-Javadoc)
         * @see org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter.TagReader#getStartOfTag()
         */
        public String getStartOfTag() {
            return ""; //$NON-NLS-1$
        }

        /* (non-Javadoc)
         * @see org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter.TagReader#isTextNode()
         */
        public boolean isTextNode() {
            return this.isTextNode;
        }

        protected String readTag() throws IOException {

            StringBuffer node = new StringBuffer();

            while (!complete) {

                reader.mark(1);
                int intChar = reader.read();
                if (intChar == -1) break;

                char c = (char) intChar;
                if (c == '<') {
                    reader.reset();
                    complete = true;
                } else {
                    node.append(c);
                }
            }

            // if this text node is just whitespace
            // remove it, except for the newlines.
            if (node.length() < 1) {
                this.isTextNode = false;

            } else if (node.toString().trim().length() == 0) {
                String whitespace = node.toString();
                node = new StringBuffer();
                for (int i = 0; i < whitespace.length(); i++) {
                    char whitespaceCharacter = whitespace.charAt(i);
                    if (whitespaceCharacter == '\n')
                            node.append(whitespaceCharacter);
                }
                this.isTextNode = false;

            } else {
                this.isTextNode = true;
            }
            return node.toString();
        }

        /* (non-Javadoc)
         * @see org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter.TagReader#requiresInitialIndent()
         */
        public boolean requiresInitialIndent() {
            return false;
        }

        /* (non-Javadoc)
         * @see org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter.TagReader#startsOnNewline()
         */
        public boolean startsOnNewline() {
            return false;
        }
    }

    private static class XmlElementReader extends TagReader {

        private boolean complete = false;

        protected void clear() {
            this.complete = false;
        }

        public int getPostTagDepthModifier() {
            if (getTagText().endsWith("/>") || getTagText().endsWith("/ >")) { //$NON-NLS-1$ //$NON-NLS-2$
                return 0;
            } else if (getTagText().startsWith("</")) { //$NON-NLS-1$
                return 0;
            } else {
                return +1;
            }
        }

        public int getPreTagDepthModifier() {
            if (getTagText().startsWith("</")) { //$NON-NLS-1$
                return -1;
            } else {
                return 0;
            }
        }

        public String getStartOfTag() {
            return "<"; //$NON-NLS-1$
        }

        protected String readTag() throws IOException {

            StringBuffer node = new StringBuffer();

            boolean insideQuote = false;
            int intChar;

            while (!complete && (intChar = reader.read()) != -1) {
                char c = (char) intChar;

                node.append(c);
                // TODO logic incorrectly assumes that " is quote character
                // when it could also be '
                if (c == '"') {
                    insideQuote = !insideQuote;
                }
                if (c == '>' && !insideQuote) {
                    complete = true;
                }
            }
            return node.toString();
        }
    }

    private int depth;

    private StringBuffer formattedXml;

    private boolean lastNodeWasText;

    public XmlDocumentFormatter() {
		super();
		depth= -1;
	}

    private void copyNode(Reader reader, StringBuffer out, FormattingPreferences prefs) throws IOException {

        TagReader tag = TagReaderFactory.createTagReaderFor(reader);

        depth = depth + tag.getPreTagDepthModifier();

        if (!lastNodeWasText) {

            if (tag.startsOnNewline() && !hasNewlineAlready(out)) {
                out.append("\n"); //$NON-NLS-1$
            }

            if (tag.requiresInitialIndent()) {
                out.append(indent(prefs.getCanonicalIndent()));
            }
        }

        out.append(tag.getTagText());

        depth = depth + tag.getPostTagDepthModifier();

        lastNodeWasText = tag.isTextNode();

    }

    public void format(TemplateBuffer templateBuffer, AntContext antContext, FormattingPreferences prefs) {
    	
		//IPreferenceStore prefs= AntUIPlugin.getDefault().getPreferenceStore();
		//boolean useCodeFormatter= prefs.getBoolean(IAntUIPreferenceConstants.TEMPLATES_USE_CODEFORMATTER);		
    	
    	String templateString= templateBuffer.getString();
    	IDocument fullDocument= antContext.getDocument();
    	
    	int completionOffset= antContext.getCompletionOffset();
    	try {
			IRegion lineRegion= fullDocument.getLineInformationOfOffset(completionOffset);
			String lineString= fullDocument.get(lineRegion.getOffset(), lineRegion.getLength());
			lineString.trim();
			fullDocument.replace(lineRegion.getOffset(), completionOffset, lineString);
		} catch (BadLocationException e1) {
			return;
		}
    	TemplateVariable[] variables= templateBuffer.getVariables();
		int[] offsets= variablesToOffsets(variables, completionOffset);
		
		IDocument origTemplateDoc= new Document(fullDocument.get());
		try {
			origTemplateDoc.replace(completionOffset, antContext.getCompletionLength(), templateString);
		} catch (BadLocationException e) {
			return; // don't format if the document has changed
		}
		
    	IDocument templateDocument= createDocument(origTemplateDoc.get(), createPositions(offsets));
    	
    	String leadingText= getLeadingText(fullDocument, antContext.getAntModel(), completionOffset);
    	String newTemplateString= leadingText + templateString;
    	int indent= computeIndent(leadingText, prefs.getTabWidth());
    	setInitialIndent(indent);
    	
    	newTemplateString= format(newTemplateString, prefs);
    	
    	try {
    		templateDocument.replace(completionOffset, templateString.length(), newTemplateString);
		} catch (BadLocationException e) {
			return;
		}
		
    	offsetsToVariables(offsets, variables, completionOffset);
    	templateBuffer.setContent(newTemplateString, variables);
    }
    
    /**
	 * Returns the indent of the given string.
	 * 
	 * @param line the text line
	 * @param tabWidth the width of the '\t' character.
	 */
	public static int computeIndent(String line, int tabWidth) {
		int result= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c == '\t') {
				result++;
				blanks= 0;
			} else if (isIndentChar(c)) {
				blanks++;
				if (blanks == tabWidth) {
					result++;
					blanks= 0;
				}
			} else {
				return result;
			}
		}
		return result;
	}
	
	/**
	 * Indent char is a space char but not a line delimiters.
	 * <code>== Character.isWhitespace(ch) && ch != '\n' && ch != '\r'</code>
	 */
	public static boolean isIndentChar(char ch) {
		return Character.isWhitespace(ch) && !isLineDelimiterChar(ch);
	}
	
	/**
	 * Line delimiter chars are  '\n' and '\r'.
	 */
	public static boolean isLineDelimiterChar(char ch) {
		return ch == '\n' || ch == '\r';
	}	
	
    /**
     * @return
     */
    public String format(String documentText, FormattingPreferences prefs) {

        Assert.isNotNull(documentText);
        Assert.isNotNull(prefs);

        Reader reader = new StringReader(documentText);
        formattedXml = new StringBuffer();

        if (depth == -1) {
        	depth = 0;
        }
        lastNodeWasText = false;
        try {
            while (true) {
                reader.mark(1);
                int intChar = reader.read();
                reader.reset();

                if (intChar != -1) {
                    copyNode(reader, formattedXml, prefs);
                } else {
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
           AntUIPlugin.log(e);
        }
        return formattedXml.toString();
    }

    private boolean hasNewlineAlready(StringBuffer out) {
        return out.lastIndexOf("\n") == formattedXml.length() - 1 //$NON-NLS-1$
               || out.lastIndexOf("\r") == formattedXml.length() - 1; //$NON-NLS-1$
    }

    private String indent(String canonicalIndent) {
        StringBuffer indent = new StringBuffer(30);
        for (int i = 0; i < depth; i++) {
            indent.append(canonicalIndent);
        }
        return indent.toString();
    }
    
    public void setInitialIndent(int indent) {
        depth= indent;
    }
    
    private Position[] createPositions(int[] positions) {
    	Position[] p= null;
		
		if (positions != null) {
			p= new Position[positions.length];
			for (int i= 0; i < positions.length; i++) {
				p[i]= new Position(positions[i], 0);
			}
		}
		return p;
    }
    private Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
		Document doc= new Document(string);
		try {
			if (positions != null) {
				final String POS_CATEGORY= "tempAntFormatterCategory"; //$NON-NLS-1$
				
				doc.addPositionCategory(POS_CATEGORY);
				doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
					protected boolean notDeleted() {
						if (fOffset < fPosition.offset && (fPosition.offset + fPosition.length < fOffset + fLength)) {
							fPosition.offset= fOffset + fLength; // deleted positions: set to end of remove
							return false;
						}
						return true;
					}
				});
				for (int i= 0; i < positions.length; i++) {
					try {
						doc.addPosition(POS_CATEGORY, positions[i]);
					} catch (BadLocationException e) {
						throw new IllegalArgumentException("Position outside of string. offset: " + positions[i].offset + ", length: " + positions[i].length + ", string size: " + string.length());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
				}
			}
		} catch (BadPositionCategoryException cannotHappen) {
			// can not happen: category is correctly set up
		}
		return doc;
	}
    
    private int[] variablesToOffsets(TemplateVariable[] variables, int start) {
		List list= new ArrayList();
		for (int i= 0; i != variables.length; i++) {
		    int[] offsets= variables[i].getOffsets();
		    for (int j= 0; j != offsets.length; j++) {
				list.add(new Integer(offsets[j]));
		    }
		}
		
		int[] offsets= new int[list.size()];
		for (int i= 0; i != offsets.length; i++) {
			offsets[i]= ((Integer) list.get(i)).intValue() + start;
		}

		Arrays.sort(offsets);

		return offsets;	    
	}
	
	private void offsetsToVariables(int[] allOffsets, TemplateVariable[] variables, int start) {
		int[] currentIndices= new int[variables.length];
		for (int i= 0; i != currentIndices.length; i++) {
			currentIndices[i]= 0;
		}

		int[][] offsets= new int[variables.length][];
		for (int i= 0; i != variables.length; i++) {
			offsets[i]= variables[i].getOffsets();
		}
		
		for (int i= 0; i != allOffsets.length; i++) {

			int min= Integer.MAX_VALUE;
			int minVariableIndex= -1;
			for (int j= 0; j != variables.length; j++) {
			    int currentIndex= currentIndices[j];
			    
			    // determine minimum
				if (currentIndex == offsets[j].length)
					continue;
					
				int offset= offsets[j][currentIndex];

				if (offset < min) {
				    min= offset;
					minVariableIndex= j;
				}		
			}

			offsets[minVariableIndex][currentIndices[minVariableIndex]]= allOffsets[i] - start + 3;
			currentIndices[minVariableIndex]++;
		}

		for (int i= 0; i != variables.length; i++) {
			variables[i].setOffsets(offsets[i]);	
		}
	}
	
	/**
	 * Returns the indentation level at the position of code completion.
	 */
	private String getLeadingText(IDocument document, AntModel model, int completionOffset) {
		AntProjectNode project= model.getProjectNode(false);
		if (project == null) {
			return ""; //$NON-NLS-1$
		}
		AntElementNode node= project.getNode(completionOffset);// - fAccumulatedChange);
		if (node == null) {
			return ""; //$NON-NLS-1$
		}
		
		StringBuffer buf= new StringBuffer();
		buf.append(getLeadingWhitespace(node.getOffset(), document));
		buf.append(createIndent());
		return buf.toString();
	}
	
	/**
	 * Returns the indentation of the line at <code>offset</code> as a
	 * <code>StringBuffer</code>. If the offset is not valid, the empty string
	 * is returned.
	 * 
	 * @param offset the offset in the document
	 * @return the indentation (leading whitespace) of the line in which
	 * 		   <code>offset</code> is located
	 */
	public static StringBuffer getLeadingWhitespace(int offset, IDocument document) {
		StringBuffer indent= new StringBuffer();
		try {
			IRegion line= document.getLineInformationOfOffset(offset);
			int lineOffset= line.getOffset();
			int nonWS= findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent.append(document.get(lineOffset, nonWS - lineOffset));
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}
	
	/**
	 * Returns the first offset greater than <code>offset</code> and smaller than 
	 * <code>end</code> whose character is not a space or tab character. If no such
	 * offset is found, <code>end</code> is returned.
	 *
	 * @param document the document to search in
	 * @param offset the offset at which searching start
	 * @param end the offset at which searching stops
	 * @return the offset in the specifed range whose character is not a space or tab
	 * @exception BadLocationException if position is an invalid range in the given document
	 */
	private static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c= document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}
	
	/**
	 * Creates a string that represents one indent (can be
	 * spaces or tabs..)
	 * 
	 * @return one indentation
	 */
	public static StringBuffer createIndent() {
		StringBuffer oneIndent= new StringBuffer();
		IPreferenceStore pluginPrefs= AntUIPlugin.getDefault().getPreferenceStore();
		pluginPrefs.getBoolean(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR);
		
		if (!pluginPrefs.getBoolean(AntEditorPreferenceConstants.FORMATTER_TAB_CHAR)) {
			int tabLen= pluginPrefs.getInt(AntEditorPreferenceConstants.FORMATTER_TAB_SIZE);
			for (int i= 0; i < tabLen; i++) {
				oneIndent.append(' ');
			}
		} else {
			oneIndent.append('\t'); // default
		}
		
		return oneIndent;
	}
}