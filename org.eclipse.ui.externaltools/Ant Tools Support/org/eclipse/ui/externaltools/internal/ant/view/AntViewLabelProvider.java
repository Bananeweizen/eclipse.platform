package org.eclipse.ui.externaltools.internal.ant.view;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.externaltools.internal.ant.view.elements.ProjectNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.TargetNode;
import org.eclipse.ui.externaltools.internal.model.AntImageDescriptor;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsImages;
import org.eclipse.ui.externaltools.internal.ui.IExternalToolsUIConstants;

/**
 * A label provider that provides labels for elements displayed in the
 * <code>AntView</code>
 */
public class AntViewLabelProvider implements ILabelProvider {

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof ProjectNode) {
			ProjectNode project= (ProjectNode) element;
			int flags = 0;
			if (project.isErrorNode()) {
				flags = flags | AntImageDescriptor.HAS_ERRORS;
			}
			CompositeImageDescriptor descriptor = new AntImageDescriptor(ExternalToolsImages.getImageDescriptor(IExternalToolsUIConstants.IMG_ANT_PROJECT), flags);
			return ExternalToolsImages.getImage(descriptor);
		} else if (element instanceof TargetNode) {
			TargetNode target= (TargetNode) element;
			int flags = 0;
			ImageDescriptor base = null;
			if (target.getDescription() == null) {
				base = ExternalToolsImages.getImageDescriptor(IExternalToolsUIConstants.IMG_ANT_TARGET_PRIVATE);
			} else {
				base = ExternalToolsImages.getImageDescriptor(IExternalToolsUIConstants.IMG_ANT_TARGET);
			}			
			if (target.isErrorNode()) {
				flags = flags | AntImageDescriptor.HAS_ERRORS;
			}
			if (target.equals(target.getProject().getDefaultTarget())){
				flags = flags | AntImageDescriptor.DEFAULT_TARGET;
			}
			return ExternalToolsImages.getImage(new AntImageDescriptor(base, flags));
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof TargetNode) {
			TargetNode node= (TargetNode)element;
			StringBuffer name= new StringBuffer(node.getName());
			if (node.getName().equals(node.getProject().getDefaultTargetName())) {
				name.append(AntViewMessages.getString("TargetNode.default")); //$NON-NLS-1$
			} 
			return name.toString();
		} else if (element instanceof ProjectNode) {
			ProjectNode project= (ProjectNode) element;
			StringBuffer buffer= new StringBuffer(project.getName());
			String defaultTarget= project.getDefaultTargetName();
			if (defaultTarget != null) {
				buffer.append(" [").append(defaultTarget).append(']');
			}
			return buffer.toString();
		} else {
			return element.toString();
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

}
