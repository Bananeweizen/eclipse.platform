package org.eclipse.ui.externaltools.internal.ant.launchConfigurations;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others.
All rights reserved. � This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
�
Contributors:
**********************************************************************/
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.externaltools.internal.model.AntImageDescriptor;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsImages;
import org.eclipse.ui.externaltools.internal.ui.IExternalToolsUIConstants;

/**
 * Ant target label provider
 */
public class TargetTableLabelProvider extends LabelProvider implements ITableLabelProvider {

	public TargetTableLabelProvider() {
		super();
	}
	
	/* (non-Javadoc)
	 * Method declared on ILabelProvider.
	 */
	public String getText(Object model) {
		TargetInfo target = (TargetInfo) model;
		StringBuffer result = new StringBuffer(target.getName());
		if (target.isDefault()) {
			result.append(" ("); //$NON-NLS-1$;
			result.append(AntLaunchConfigurationMessages.getString("AntTargetLabelProvider.default_target_1")); //$NON-NLS-1$
			result.append(")"); //$NON-NLS-1$;
		}
		return result.toString();
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		TargetInfo target = (TargetInfo)element;
		ImageDescriptor base = null;
		int flags = 0;
		if (target.isDefault()) {
			flags = flags | AntImageDescriptor.DEFAULT_TARGET;
		}
		if (target.getDescription() == null) {
			base = ExternalToolsImages.getImageDescriptor(IExternalToolsUIConstants.IMG_ANT_TARGET_PRIVATE);
		} else {
			base = ExternalToolsImages.getImageDescriptor(IExternalToolsUIConstants.IMG_ANT_TARGET);
		}
		return ExternalToolsImages.getImage(new AntImageDescriptor(base, flags));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getImage(element);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0){
			return getText(element);
		}
		String desc = ((TargetInfo)element).getDescription();
		if (desc == null) {
			return ""; //$NON-NLS-1$
		}
		return desc;
	}

}