/*====================================================================
Copyright (c) 2002, 2003 Object Factory Inc.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    Object Factory Inc. - Initial implementation
====================================================================*/
package org.eclipse.ui.externaltools.internal.ant.dtd.schema;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.ui.externaltools.internal.ant.dtd.IAtom;
import org.eclipse.ui.externaltools.internal.ant.dtd.IModel;

/**
 * IModel implementation.
 * @author Bob Foster
 */
public class Model implements IModel {
	
	protected int fKind;
	protected int fMin = 1;
	protected int fMax = 1;
	protected int fNum = 0;
	protected IModel[] fContents;
	protected List fContentsList;
	protected IAtom fLeaf;
	protected boolean fMixed;
	
	protected static IModel[] fEmptyContents = new IModel[0];
	
	public Model(int kind) {
		fKind = kind;
	}
	
	public Model() {
		fKind = UNKNOWN;
	}
	
	public void setKind(int kind) {
		fKind = kind;
	}
	
	public void setMinOccurs(int min) {
		fMin = min;
	}
	
	public void setMaxOccurs(int max) {
		fMax = max;
	}
	
	public void setContents(IModel[] contents) {
		fContents = contents;
	}
	
	public void addModel(IModel model) {
		if (fContents != null)
			throw new IllegalStateException("model may not be changed once its contents have been requested");
			
		if (fContentsList == null)
			fContentsList = new LinkedList();
			
		fContentsList.add(model);
	}
	
	public void setLeaf(IAtom leaf) {
		fLeaf = leaf;
	}

	protected static final String[] fOps = {"?",",","|","&","!!!"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private Nfm qualifyNfm(Nfm nfm) {
		if (nfm == null)
			return null;
		if (fMin == 1 && fMax == 1)
			return nfm;
		if (fMin == 0 && fMax == 1) {
			return Nfm.getQuestion(nfm);
		}
		if (fMin == 0 && fMax == UNBOUNDED) {
			return Nfm.getStar(nfm);
		}
		if (fMin == 1 && fMax == UNBOUNDED) {
			return Nfm.getPlus(nfm);
		}
		//the following cases cannot be reached by DTD models
		if (fMax == 0)
			return Nfm.getNfm(null);
		if (fMax == UNBOUNDED) {
			return Nfm.getUnbounded(nfm, fMin);
		}
		else {
			return Nfm.getMinMax(nfm, fMin, fMax);
		}
	}

	public Model shallowCopy() {
		Model copy = new Model(getKind());
		copy.fMixed = fMixed;
		copy.fLeaf = fLeaf;
		if (fContents != null) {
			copy.fContentsList = new LinkedList();
			for (int i = 0; i < fContents.length; i++) {
				copy.fContentsList.add(fContents[i]);
			}
		}
		else if (fContentsList != null) {
			copy.fContentsList = new LinkedList();
			Iterator it = fContentsList.iterator();
			while (it.hasNext()) {
				copy.fContentsList.add(it.next());
			}
		}
		return copy;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getKind()
	 */
	public int getKind() {
		return 0;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getMinOccurs()
	 */
	public int getMinOccurs() {
		return fMin;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getMaxOccurs()
	 */
	public int getMaxOccurs() {
		return fMax;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getContents()
	 */
	public IModel[] getContents() {
		// A model contents may be referred to many times
		// it would be inefficient to convert to array each time
		if (fContents == null) {
			if (fContentsList != null) {
				fContents = (IModel[]) fContentsList.toArray(new IModel[fContentsList.size()]);
				fContentsList = null;
			}
			else
				fContents = fEmptyContents;
		}
		return fContents;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getLeaf()
	 */
	public IAtom getLeaf() {
		return fLeaf;
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getOperator()
	 */
	public String getOperator() {
		return fOps[fKind];
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.schema.IModel#stringRep()
	 */
	public String stringRep() {
		StringBuffer buf = new StringBuffer();
		stringRep(buf);
		return buf.toString();
	}
	
	private void stringRep(StringBuffer buf) {
		switch (getKind()) {
			case IModel.CHOICE:
			case IModel.SEQUENCE:
				buf.append('(');
				Iterator it = fContentsList.iterator();
				while (it.hasNext()) {
					Model model = (Model) it.next();
					model.stringRep(buf);
					if (it.hasNext())
						buf.append(getOperator());
				}
				buf.append(')');
				buf.append(getQualifier());
				break;
			case IModel.LEAF:
				IAtom atom = getLeaf();
				buf.append(atom.getName());
				break;
			default:
				buf.append("***UNKNOWN***");
				break;
		}
	}

	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#getQualifier()
	 */
	public String getQualifier() {
		return fMin == 1 ? (fMax == UNBOUNDED ? "+" : "") : (fMax == UNBOUNDED ? "*" : "?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	/**
	 * @see org.eclipse.ui.externaltools.internal.ant.dtd.IModel#toNfm()
	 */
	public Nfm toNfm() {
		Nfm nfm = null;
		switch (fKind) {
			case CHOICE:
			case SEQUENCE:
			{
				IModel[] contents = getContents();
				if (contents == null || contents.length == 0)
					nfm = null;
				else {
					nfm = contents[0].toNfm();
					for (int i = 1; i < contents.length; i++) {
						Nfm tmp = contents[i].toNfm();
						if (fKind == SEQUENCE) {
							nfm = Nfm.getComma(nfm, tmp);
						} else {
							nfm = Nfm.getOr(nfm, tmp);
						}
					}
				}
				break;
			}
			case LEAF:
			{
				nfm = Nfm.getNfm(fLeaf);
				break;
			}
		}
		return qualifyNfm(nfm);
	}
	
}
