package org.eclipse.team.internal.ccvs.ui.sync;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Shell;
/**
 * A class that adds tool-tips to the buttons of a standard message dialog.
 */
public class ToolTipMessageDialog extends MessageDialog {
	private String[] buttonToolTips;
	/**
	 * Same as the MessageDialog constructor, with the addition of a button tooltip
	 * argument.  The number of button tool tips must match the number of button labels.
	 */
	public ToolTipMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, String[] buttonToolTips, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		this.buttonToolTips = buttonToolTips;
	}
	/**
	 * Method declared on MessageDialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (buttonToolTips != null) {
			for (int i = 0; i < buttonToolTips.length; i++) {
				getButton(i).setToolTipText(buttonToolTips[i]);
			}
		}
	}}
