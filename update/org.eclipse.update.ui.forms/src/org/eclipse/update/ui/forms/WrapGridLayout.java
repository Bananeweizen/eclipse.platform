package org.eclipse.update.ui.forms;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public final class WrapGridLayout extends Layout {
	/**
	 * marginWidth specifies the number of pixels of horizontal margin
	 * that will be placed along the left and right edges of the layout.
	 *
	 * The default value is 5.
	 */
 	public int marginWidth = 5;
	/**
	 * marginHeight specifies the number of pixels of vertical margin
	 * that will be placed along the top and bottom edges of the layout.
	 *
	 * The default value is 5.
	 */
 	public int marginHeight = 5;
 	/**
 	 * numColumns specifies the number of cell columns in the layout.
 	 *
 	 * The default value is 1.
 	 */
	public int numColumns = 1;
	/**
	 * makeColumnsEqualWidth specifies whether all columns in the layout
	 * will be forced to have the same width.
	 *
	 * The default value is false.
	 */
	public boolean makeColumnsEqualWidth = false;
	/**
	 * horizontalSpacing specifies the number of pixels between the right
	 * edge of one cell and the left edge of its neighbouring cell to
	 * the right.
	 *
	 * The default value is 5.
	 */
 	public int horizontalSpacing = 5;
	/**
	 * verticalSpacing specifies the number of pixels between the bottom
	 * edge of one cell and the top edge of its neighbouring cell underneath.
	 *
	 * The default value is 5.
	 */
 	public int verticalSpacing = 5;

  	// Private variables.  Cached values used to cut down on grid calculations.
	boolean initialLayout = true;
	Vector grid = new Vector();
	int [] pixelColumnWidths;
	int [] pixelRowHeights;
	int [] expandableColumns;
	int [] expandableRows;
	
public WrapGridLayout() {
	super();
}
void adjustGridDimensions(Composite composite, boolean flushCache) {
	// Ensure that widgets that span more than one row or column have enough space.
	for (int row = 0; row < grid.size(); row++) {
		for (int column = 0; column < numColumns; column++) {
			WrapGridData spec = ((WrapGridData[]) grid.elementAt(row))[column];
			if (spec.isItemData()) {
				// Widgets spanning columns.
				if (spec.horizontalSpan > 1) {
					Control child = composite.getChildren()[spec.childIndex];
					Point extent = child.computeSize(spec.widthHint, spec.heightHint, flushCache);

					// Calculate the size of the widget's spanned columns.
					int lastSpanIndex = column + spec.horizontalSpan;
					int spannedSize = 0;
					for (int c = column; c < lastSpanIndex; c++) {
						spannedSize = spannedSize + pixelColumnWidths[c] + horizontalSpacing;
					}
					spannedSize = spannedSize - horizontalSpacing;

					// If the spanned columns are not large enough to display the widget, adjust the column
					// sizes to account for the extra space that is needed.
					if (extent.x + spec.horizontalIndent > spannedSize) {
						int extraSpaceNeeded = extent.x + spec.horizontalIndent - spannedSize;
						int lastColumn = column + spec.horizontalSpan - 1;
						int colWidth;
						if (makeColumnsEqualWidth) {
							// Evenly distribute the extra space amongst all of the columns.
							int columnExtra = extraSpaceNeeded / numColumns;
							int columnRemainder = extraSpaceNeeded % numColumns;
							for (int i = 0; i < pixelColumnWidths.length; i++) {
								colWidth = pixelColumnWidths[i] + columnExtra;
								pixelColumnWidths[i] = colWidth;
							}
							colWidth = pixelColumnWidths[lastColumn] + columnRemainder;
							pixelColumnWidths[lastColumn] = colWidth;
						} else {
							Vector localExpandableColumns = new Vector();
							for (int i = column; i < lastColumn; i++) {
								for (int j = 0; j < expandableColumns.length; j++) {
									if (expandableColumns[j] == i) {
										localExpandableColumns.addElement(new Integer(i));
									}
								}
							}
							if (localExpandableColumns.size() > 0) {
								// If any of the widget's columns grab excess space, allocate the space amongst those columns.
								int columnExtra = extraSpaceNeeded / localExpandableColumns.size();
								int columnRemainder = extraSpaceNeeded % localExpandableColumns.size();
								for (int i = 0; i < localExpandableColumns.size(); i++) {
									int expandableCol = ((Integer) localExpandableColumns.elementAt(i)).intValue();
									colWidth = pixelColumnWidths[expandableCol] + columnExtra;
									pixelColumnWidths[expandableCol] = colWidth;
								}
								colWidth = pixelColumnWidths[lastColumn] + columnRemainder;
								pixelColumnWidths[lastColumn] = colWidth;
							} else {
								// Add the extra space to the widget's last column if none of its columns grab excess space.
								colWidth = pixelColumnWidths[lastColumn] + extraSpaceNeeded;
								pixelColumnWidths[lastColumn] = colWidth;
							}
						}
					}
				}

				// Widgets spanning rows.
				if (spec.verticalSpan > 1) {
					Control child = composite.getChildren()[spec.childIndex];
					Point extent = child.computeSize(spec.widthHint, spec.heightHint, flushCache);

					// Calculate the size of the widget's spanned rows.
					int lastSpanIndex = row + spec.verticalSpan;
					int spannedSize = 0;
					for (int r = row; r < lastSpanIndex; r++) {
						spannedSize = spannedSize + pixelRowHeights[r] + verticalSpacing;
					}
					spannedSize = spannedSize - verticalSpacing;
					// If the spanned rows are not large enough to display the widget, adjust the row
					// sizes to account for the extra space that is needed.
					if (extent.y > spannedSize) {
						int extraSpaceNeeded = extent.y - spannedSize;
						int lastRow = row + spec.verticalSpan - 1;
						int rowHeight;
						Vector localExpandableRows = new Vector();
						for (int i = row; i < lastRow; i++) {
							for (int j = 0; j < expandableRows.length; j++) {
								if (expandableRows[j] == i) {
									localExpandableRows.addElement(new Integer(i));
								}
							}
						}
						if (localExpandableRows.size() > 0) {
							// If any of the widget's rows grab excess space, allocate the space amongst those rows.
							int rowExtra = extraSpaceNeeded / localExpandableRows.size();
							int rowRemainder = extraSpaceNeeded % localExpandableRows.size();
							for (int i = 0; i < localExpandableRows.size(); i++) {
								int expandableRow = ((Integer) localExpandableRows.elementAt(i)).intValue();
								rowHeight = pixelRowHeights[expandableRow] + rowExtra;
								pixelRowHeights[expandableRow] = rowHeight;
							}
							rowHeight = pixelRowHeights[lastRow] + rowRemainder;
							pixelRowHeights[lastRow] = rowHeight;
						} else {
							// Add the extra space to the widget's last row if no rows grab excess space.
							rowHeight = pixelRowHeights[lastRow] + extraSpaceNeeded;
							pixelRowHeights[lastRow] = rowHeight;
						}
					}
				}
			}
		}
	}
}
void calculateGridDimensions(Composite composite, boolean flushCache) {
	int maxWidth, childWidth, maxHeight, childHeight;
	
	//
	Control[] children = composite.getChildren();
	Point[] childSizes = new Point[children.length];
	pixelColumnWidths = new int[numColumns];
	pixelRowHeights = new int[grid.size()];
	
	// Loop through the grid by column to get the width that each column needs to be.
	// Each column will be as wide as its widest widget.
	for (int column = 0; column < numColumns; column++) {
		maxWidth = 0;
		for (int row = 0; row < grid.size(); row++) {
			WrapGridData spec = ((WrapGridData[]) grid.elementAt(row))[column];
			if (spec.isItemData()) {
				Control child = children[spec.childIndex];
				childSizes[spec.childIndex] = child.computeSize(spec.widthHint, spec.heightHint, flushCache);
				childWidth = childSizes[spec.childIndex].x + spec.horizontalIndent;
				if (spec.horizontalSpan == 1) {
					maxWidth = Math.max(maxWidth, childWidth);
				}
			}
		}
		// Cache the values for later use.
		pixelColumnWidths[column] = maxWidth;
	}

	// 
	if (makeColumnsEqualWidth) {
		maxWidth = 0;
		// Find the largest column size that is necessary and make each column that size.
		for (int i = 0; i < numColumns; i++) {
			maxWidth = Math.max(maxWidth, pixelColumnWidths[i]);
		}
		for (int i = 0; i < numColumns; i++) {
			pixelColumnWidths[i] = maxWidth;
		}
	}

	// Loop through the grid by row to get the height that each row needs to be.
	// Each row will be as high as its tallest widget.
	for (int row = 0; row < grid.size(); row++) {
		maxHeight = 0;
		for (int column = 0; column < numColumns; column++) {
			WrapGridData spec = ((WrapGridData[]) grid.elementAt(row))[column];
			if (spec.isItemData()) {
				Control child = children[spec.childIndex];
				childHeight = childSizes[spec.childIndex].y;
				if (spec.verticalSpan == 1) {
					maxHeight = Math.max(maxHeight, childHeight);
				}
			}
		}
		// Cache the values for later use.
		pixelRowHeights[row] = maxHeight;
	}
}
void computeExpandableCells() {
	// If a widget grabs excess horizontal space, the last column that the widget spans
	// will be expandable.  Similarly, if a widget grabs excess vertical space, the 
	// last row that the widget spans will be expandable.
	Hashtable growColumns = new Hashtable();
	Hashtable growRows = new Hashtable();
	for (int col = 0; col < numColumns; col++) {
		for (int row = 0; row < grid.size(); row++) {
			WrapGridData spec = ((WrapGridData[]) grid.elementAt(row))[col];
			if (spec.grabExcessHorizontalSpace) {
				growColumns.put(new Integer(col + spec.horizontalSpan - 1), new Object());
			}
			if (spec.grabExcessVerticalSpace) {
				growRows.put(new Integer(row + spec.verticalSpan - 1), new Object());
			}
		}
	}

	// Cache the values.  These values are used later during children layout.
	int i = 0;
	Enumeration enum = growColumns.keys();
	expandableColumns = new int[growColumns.size()];
	while (enum.hasMoreElements()) {
		expandableColumns[i] = ((Integer)enum.nextElement()).intValue();
		i = i + 1;
	}
	i = 0;
	enum = growRows.keys();
	expandableRows = new int[growRows.size()];
	while (enum.hasMoreElements()) {
		expandableRows[i] = ((Integer)enum.nextElement()).intValue();
		i = i + 1;
	}
}
Point computeLayoutSize(Composite composite, int wHint, int hHint, boolean flushCache) {
	int totalMarginHeight, totalMarginWidth;
	int totalWidth, totalHeight;
	int cols, rows;

	// Initialize the grid and other cached information that help with the grid layout.
	if (grid.size() == 0) {
		createGrid(composite);
		calculateGridDimensions(composite, flushCache);
		computeExpandableCells();
		adjustGridDimensions(composite, flushCache);
	}

	//
	cols = numColumns;
	rows = grid.size();
	totalMarginHeight = marginHeight;
	totalMarginWidth = marginWidth;

	// The total width is the margin plus border width plus space between each column, 
	// plus the width of each column.
	totalWidth = (totalMarginWidth * 2) + ((cols - 1) * horizontalSpacing);

	//Add up the width of each column. 
	for (int i = 0; i < pixelColumnWidths.length; i++) {
		totalWidth = totalWidth + pixelColumnWidths[i];
	}

	// The total height is the margin plus border height, plus space between each row, 
	// plus the height of the tallest child in each row.
	totalHeight = (totalMarginHeight * 2) + ((rows - 1) * verticalSpacing);

	//Add up the height of each row. 
	for (int i = 0; i < pixelRowHeights.length; i++) {
		totalHeight = totalHeight + pixelRowHeights[i];
	}

	if (wHint != SWT.DEFAULT) {
		totalWidth = wHint;};
	if (hHint != SWT.DEFAULT) {
		totalHeight = hHint;};
	// The preferred extent is the width and height that will accomodate the grid's widgets.
	return new Point(totalWidth, totalHeight);
}
protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
	Control[] children = composite.getChildren();
	int numChildren = children.length;

	if (numChildren == 0) return new Point(0,0);

	// Make sure that all of the composite children have a layout spec.  Do this by
	// using the initialLayout flag.
	if (flushCache)
		initialLayout = true;
	if (initialLayout) {
		initializeLayoutData(composite);
		initialLayout = false;
	}

	if (flushCache) {
		// Cause the grid and its related information to be calculated
		// again.
		grid.removeAllElements();
	}
	return computeLayoutSize(composite, wHint, hHint, flushCache);
}
void createGrid(Composite composite) {
	int row, column, rowFill, columnFill;
	Vector rows;
	Control[] children;
	WrapGridData spacerSpec;

	// 
	children = composite.getChildren();

	// 
	grid.addElement(emptyRow());
	row = 0;
	column = 0;

	// Loop through the children and place their associated layout specs in the
	// grid.  Placement occurs left to right, top to bottom (i.e., by row).
	for (int i = 0; i < children.length; i++) {
		// Find the first available spot in the grid.
		Control child = children[i];
		WrapGridData spec = (WrapGridData) child.getLayoutData();
		while (((WrapGridData[]) grid.elementAt(row))[column] != null) {
			column = column + 1;
			if (column >= numColumns) {
				row = row + 1;
				column = 0;
				if (row >= grid.size()) {
					grid.addElement(emptyRow());
				}
			}
		}
		// See if the place will support the widget's horizontal span.  If not, go to the
		// next row.
		if (column + spec.horizontalSpan - 1 >= numColumns) {
			grid.addElement(emptyRow());
			row = row + 1;
			column = 0;
		}

		// The vertical span for the item will be at least 1.  If it is > 1,
		// add other rows to the grid.
		for (int j = 2; j <= spec.verticalSpan; j++) {
			if (row + j > grid.size()) {
				grid.addElement(emptyRow());
			}
		}

		// Store the layout spec.  Also cache the childIndex.  NOTE: That we assume the children of a
		// composite are maintained in the order in which they are created and added to the composite.
		((WrapGridData[]) grid.elementAt(row))[column] = spec;
		spec.childIndex = i;

		// Put spacers in the grid to account for the item's vertical and horizontal
		// span.
		rowFill = spec.verticalSpan - 1;
		columnFill = spec.horizontalSpan - 1;
		for (int r = 1; r <= rowFill; r++) {
			for (int c = 0; c < spec.horizontalSpan; c++) {
				spacerSpec = new WrapGridData();
				spacerSpec.isItemData = false;
				((WrapGridData[]) grid.elementAt(row + r))[column + c] = spacerSpec;
			}
		}
		for (int c = 1; c <= columnFill; c++) {
			for (int r = 0; r < spec.verticalSpan; r++) {
				spacerSpec = new WrapGridData();
				spacerSpec.isItemData = false;
				((WrapGridData[]) grid.elementAt(row + r))[column + c] = spacerSpec;
			}
		}
		column = column + spec.horizontalSpan - 1;
	}

	// Fill out empty grid cells with spacers.
	for (int k = column + 1; k < numColumns; k++) {
		spacerSpec = new WrapGridData();
		spacerSpec.isItemData = false;
		((WrapGridData[]) grid.elementAt(row))[k] = spacerSpec;
	}
	for (int k = row + 1; k < grid.size(); k++) {
		spacerSpec = new WrapGridData();
		spacerSpec.isItemData = false;
		((WrapGridData[]) grid.elementAt(k))[column] = spacerSpec;
	}
}
WrapGridData[] emptyRow() {
	WrapGridData[] row = new WrapGridData[numColumns];
	for (int i = 0; i < numColumns; i++) {
		row[i] = null;}
	return row;
}
void initializeLayoutData(Composite composite) {
	Control[] children = composite.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child.getLayoutData() == null) {
				child.setLayoutData(new WrapGridData());
			}
		}
}
protected void layout(Composite composite, boolean flushCache) {
	int[] columnWidths;
	int[] rowHeights;
	int columnSize, rowSize, rowY, columnX;
	int compositeWidth, compositeHeight;
	int excessHorizontal, excessVertical;
	Control[] children;
	if (flushCache) {
		// Cause the grid and its related information to be calculated
		// again.
		grid.removeAllElements();
	}
	children = composite.getChildren();
	if (children.length == 0)
		return;

	//
	Point extent = computeSize(composite, SWT.DEFAULT, SWT.DEFAULT, flushCache);
	columnWidths = new int[numColumns];
	for (int i = 0; i < pixelColumnWidths.length; i++) {
		columnWidths[i] = pixelColumnWidths[i];
	}
	rowHeights = new int[grid.size()];
	for (int i = 0; i < pixelRowHeights.length; i++) {
		rowHeights[i] = pixelRowHeights[i];
	}
	int columnWidth = 0;
	columnSize = Math.max(1, numColumns);
	rowSize = Math.max(1, grid.size());

	// 
	compositeWidth = extent.x;
	compositeHeight = extent.y;

	// Calculate whether or not there is any extra space or not enough space due to a resize 
	// operation.  Then allocate/deallocate the space to columns and rows that are expandable.  
	// If a widget grabs excess space, its last column or row will be expandable.
	excessHorizontal = composite.getClientArea().width - compositeWidth;
	excessVertical = composite.getClientArea().height - compositeHeight;

	// Allocate/deallocate horizontal space.
	if (expandableColumns.length != 0) {
		int excess, remainder, last;
		int colWidth;
		excess = excessHorizontal / expandableColumns.length;
		remainder = excessHorizontal % expandableColumns.length;
		last = 0;
		for (int i = 0; i < expandableColumns.length; i++) {
			int expandableCol = expandableColumns[i];
			colWidth = columnWidths[expandableCol];
			colWidth = colWidth + excess;
			columnWidths[expandableCol] = colWidth;
			last = Math.max(last, expandableCol);
		}
		colWidth = columnWidths[last];
		colWidth = colWidth + remainder;
		columnWidths[last] = colWidth;
	}

	// Go through all specs in each expandable column and get the maximum specified
	// widthHint.  Use this as the minimumWidth for the column.
	for (int i = 0; i < expandableColumns.length; i++) {
		int expandableCol = expandableColumns[i];
		int colWidth = columnWidths[expandableCol];
		int minWidth = 0;
		for (int j = 0; j < grid.size(); j++) {
			WrapGridData[] row = (WrapGridData[]) grid.elementAt(j);
			WrapGridData spec = row[expandableCol];
			minWidth = Math.max(minWidth, spec.widthHint);
		}
		columnWidths[expandableCol] = Math.max(colWidth, minWidth);
	}
	// Allocate/deallocate vertical space.
	if (expandableRows.length != 0) {
		int excess, remainder, last;
		int rowHeight;
		excess = excessVertical / expandableRows.length;
		remainder = excessVertical % expandableRows.length;
		last = 0;
		for (int i = 0; i < expandableRows.length; i++) {
			int expandableRow = expandableRows[i];
			rowHeight = rowHeights[expandableRow];
			rowHeight = rowHeight + excess;
			rowHeights[expandableRow] = rowHeight;
			last = Math.max(last, expandableRow);
		}
		rowHeight = rowHeights[last];
		rowHeight = rowHeight + remainder;
		rowHeights[last] = rowHeight;
	}
	// Go through all specs in each expandable row and get the maximum specified
	// heightHint.  Use this as the minimumHeight for the row.
	for (int i = 0; i < expandableRows.length; i++) {
		int expandableRow = expandableRows[i];
		int rowHeight = rowHeights[expandableRow];
		int minHeight = 0;
		WrapGridData[] row = (WrapGridData[]) grid.elementAt(expandableRow);
		for (int j = 0; j < numColumns; j++) {
			WrapGridData spec = row[j];
			minHeight = Math.max(minHeight, spec.heightHint);
		}
		rowHeights[expandableRow] = Math.max(rowHeight, minHeight);
	}

	// Get the starting x and y.
	columnX = marginWidth + composite.getClientArea().x;
	rowY = marginHeight + composite.getClientArea().y;

	// Layout the widget left to right, top to bottom.
	for (int r = 0; r < rowSize; r++) {
		int rowHeight = rowHeights[r];
		WrapGridData[] row = (WrapGridData[]) grid.elementAt(r);

		// 
		for (int c = 0; c < row.length; c++) {
			int spannedWidth = 0, spannedHeight = 0;
			int hAlign = 0, vAlign = 0;
			int widgetX = 0, widgetY = 0;
			int widgetW = 0, widgetH = 0;

			//
			WrapGridData spec = (WrapGridData) row[c];
			if (makeColumnsEqualWidth) {
				columnWidth = composite.getClientArea().width - 2 * (marginWidth)  - ((numColumns - 1) * horizontalSpacing);
				columnWidth = columnWidth / numColumns;
				for (int i = 0; i < columnWidths.length; i++) {
					columnWidths[i] = columnWidth;
				}
			} else {
				columnWidth = columnWidths[c];
			}

			//
			spannedWidth = columnWidth;
			for (int k = 1; k < spec.horizontalSpan; k++) {
				if ((c + k) <= numColumns) {
					if (!makeColumnsEqualWidth) {
						columnWidth = columnWidths[c + k];
					}
					spannedWidth = spannedWidth + columnWidth + horizontalSpacing;
				}
			}

			//
			spannedHeight = rowHeight;
			for (int k = 1; k < spec.verticalSpan; k++) {
				if ((r + k) <= grid.size()) {
					spannedHeight = spannedHeight + rowHeights[r + k] + verticalSpacing;
				}
			}

			//
			if (spec.isItemData()) {
				Control child = children[spec.childIndex];
				Point childExtent = child.computeSize(spec.widthHint, spec.heightHint, flushCache);
				hAlign = spec.horizontalAlignment;
				widgetX = columnX;

				// Calculate the x and width values for the widget.
				if (hAlign == spec.CENTER) {
					widgetX = widgetX + (spannedWidth / 2) - (childExtent.x / 2);
				} else
					if (hAlign == spec.END) {
						widgetX = widgetX + spannedWidth - childExtent.x - spec.horizontalIndent;
					} else {
						widgetX = widgetX + spec.horizontalIndent;
					}
				if (hAlign == spec.FILL) {
					widgetW = spannedWidth - spec.horizontalIndent;
					widgetX = columnX + spec.horizontalIndent;
				} else {
					widgetW = childExtent.x;
				}

				// Calculate the y and height values for the widget.
				vAlign = spec.verticalAlignment;
				widgetY = rowY;
				if (vAlign == spec.CENTER) {
					widgetY = widgetY + (spannedHeight / 2) - (childExtent.y / 2);
				} else
					if (vAlign == spec.END) {
						widgetY = widgetY + spannedHeight - childExtent.y;
					} else {
						widgetY = widgetY;
					}
				if (vAlign == spec.FILL) {
					widgetH = spannedHeight;
					widgetY = rowY;
				} else {
					widgetH = childExtent.y;
				}
				// Place the widget.
				child.setBounds(widgetX, widgetY, widgetW, widgetH);
			}
			// Update the starting x value.
			columnX = columnX + columnWidths[c] + horizontalSpacing;
		}
		// Update the starting y value and since we're starting a new row, reset the starting x value.
		rowY = rowY + rowHeights[r] + verticalSpacing;
		columnX = marginWidth + composite.getClientArea().x;
	}
}
}


