package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.TermColor;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.LengthSet;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.svgpdf.render.*;
import org.w3c.dom.Element;

import java.io.IOException;

/**
 * A class for draw borders and corners for PDF
 *
 * @author Tomas Chocholaty
 */

public class BorderPDF extends Border {

    PDFRenderer pdfRenderer;
    BreakAvoidTables breakAvoidTables;
    PDFOutput pdf;

    public BorderPDF(LengthSet lengths, Rectangle bb, ElementBox eb, PDFRenderer pdfRenderer, BreakAvoidTables breakAvoidTables, PDFOutput pdf) {
        super(lengths, bb, eb);
        this.pdfRenderer = pdfRenderer;
        this.breakAvoidTables = breakAvoidTables;
        this.pdf = pdf;
    }


    public BorderPDF(LengthSet lengths, Rectangle bb, ElementBox elem, PDFRenderer pdfRenderer) {
        super(lengths, bb, elem);
        this.pdfRenderer = pdfRenderer;
    }


    @Override
    public boolean writeBorder(ElementBox elem, Border border) throws IOException {
        boolean ret = false;
        final LengthSet borders = border.border;

        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());
        if (node != null) {
            for (int i = 0; i < pdf.getPageCount(); i++) {

                float pageStart = i * pdf.getPageHeight();
                float pageEnd = (i + 1) * pdf.getPageHeight();
                //control if element is for current page

                float startOfElement = elem.getAbsoluteBorderBounds().y + node.getPlusOffset();
                float endOfElement = elem.getAbsoluteBorderBounds().y + node.getPlusOffset() + elem.getBorder().bottom + node.getPlusHeight() + elem.getAbsoluteBorderBounds().getHeight();

                if ((startOfElement > pageStart && startOfElement < pageEnd) || (endOfElement > pageStart && endOfElement < pageEnd)) {
                    pdf.setCurrentPage(i);
                    //Recalculate only corner, which are on current page
                    for (int j = 0; j < 4; j++) {

                        CornerRadius cr = border.getRadius(j + 1);

                        float pointA, pointC;
                        if ((j == 0) || (j == 1)) {
                            pointA = cr.a.y + node.getPlusOffset();
                            pointC = cr.c.y + node.getPlusOffset();
                        } else {
                            pointA = cr.a.y + node.getPlusOffset() + node.getPlusHeight();
                            pointC = cr.c.y + node.getPlusOffset() + node.getPlusHeight();
                        }

                        if ((pointA >= pageStart && pointA <= pageEnd) || (pointC >= pageStart && pointC <= pageEnd)) {
                            //generate corners
                            switch (j) {
                                case 0:
                                    ret |= writeBorderCorner(border, 1, elem, "top", "right", node.getPlusOffset(), node.getPlusHeight(), i, node);
                                    break;
                                case 1:
                                    ret |= writeBorderCorner(border, 2, elem, "left", "top", node.getPlusOffset(), node.getPlusHeight(), i, node);
                                    break;
                                case 2:
                                    ret |= writeBorderCorner(border, 3, elem, "right", "bottom", node.getPlusOffset(), node.getPlusHeight(), i, node);
                                    break;
                                case 3:
                                    ret |= writeBorderCorner(border, 4, elem, "bottom", "left", node.getPlusOffset(), node.getPlusHeight(), i, node);
                                    break;
                            }
                        }
                    }

                    //generate straight lines
                    ret |= writeBorderPDF(elem, border.topLeftH, border.topRightH, "top", borders.top, node.getPlusOffset(), node.getPlusHeight(), i);
                    ret |= writeBorderPDF(elem, border.bottomLeftH, border.bottomRightH, "bottom", borders.bottom, node.getPlusOffset(), node.getPlusHeight(), i);
                    ret |= writeBorderPDF(elem, border.bottomLeftV, border.topLeftV, "left", borders.left, node.getPlusOffset(), node.getPlusHeight(), i);
                    ret |= writeBorderPDF(elem, border.bottomRightV, border.topRightV, "right", borders.right, node.getPlusOffset(), node.getPlusHeight(), i);
                }
            }
        }
        return ret;
    }


    /**
     * render straight line for border
     *
     * @param elem       - instance of ElementBox
     * @param a          - start of actual border side
     * @param b          - end of actual border side
     * @param side       - actual side
     * @param widthLine  - width of border
     * @param plusOffset - plus offset for this element
     * @param plusHeight -plus height for this element
     * @param i          - actual page
     */
    private boolean writeBorderPDF(ElementBox elem, DPoint a, DPoint b, String side, float widthLine, float plusOffset, float plusHeight, int i) throws IOException {
        if (borderIsVisible(elem, side)) {

            float y = 0, x = 0, widthRect = 0, heightRect = 0;
            switch (side) {
                case "top":
                    x = a.x;
                    y = pdf.getPageHeight() - (a.y + plusOffset - i * pdf.getPageHeight()) - widthLine - plusHeight;
                    widthRect = b.x - a.x;
                    heightRect = widthLine;
                    break;
                case "bottom":
                    x = a.x;
                    y = pdf.getPageHeight() - (a.y + plusOffset - i * pdf.getPageHeight()) - plusHeight;
                    widthRect = b.x - a.x;
                    heightRect = widthLine;
                    break;
                case "left":
                    x = a.x;
                    y = pdf.getPageHeight() - (a.y + plusOffset - i * pdf.getPageHeight()) - plusHeight;
                    widthRect = widthLine;
                    heightRect = a.y - b.y;
                    break;
                case "right":
                    x = a.x - widthLine;
                    y = pdf.getPageHeight() - (a.y + plusOffset - i * pdf.getPageHeight()) - plusHeight;
                    widthRect = widthLine;
                    heightRect = a.y - b.y;
                    break;
            }
            if (heightRect != 0) {
                pdfRenderer.getPdf().drawRectangle(widthLine, getBorderColor(elem, side), x, y, widthRect, heightRect);
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * Render corner for border
     *
     * @param border      - class represent points for border
     * @param s           - actual corner
     * @param elem        - instance of element
     * @param side1       - first side
     * @param side2       - second side
     * @param plusOffset  - plus Offset for actual element
     * @param plusHeight  - plus height for actual element
     * @param currentPage - actual page
     * @param node        - node corresponding with actual element
     */
    private boolean writeBorderCorner(Border border, int s, ElementBox elem, String side1, String side2, float plusOffset, float plusHeight, int currentPage, Node node) throws IOException {
        CornerRadius crOld = border.getRadius(s);
        CornerRadius cr = (CornerRadius) crOld.clone();
        pdf.recalculateYCoordinatesForPDFCorner(cr, node, currentPage);
        if (!cr.draw) {
            cr.setDraw(true);
            final float radx = cr.x;
            final float rady = cr.y;
            float widthHor, widthVer;
            DPoint point1, point2;
            // depending on which corner is drawn, we get the frame widths and colors in the appropriate directions
            if (s == 1) { // top-right
                widthHor = border.border.right;
                widthVer = border.border.top;
                point1 = border.topRightH;
                point2 = border.topRightV;
            } else if (s == 2) { // top-left
                widthHor = border.border.left;
                widthVer = border.border.top;
                point1 = border.topLeftV;
                point2 = border.topLeftH;
            } else if (s == 3) { // bottom-right
                widthHor = border.border.right;
                widthVer = border.border.bottom;
                point1 = border.bottomRightV;
                point2 = border.bottomRightH;
            } else { // bottom-left
                widthHor = border.border.left;
                widthVer = border.border.bottom;
                point1 = border.bottomLeftH;
                point2 = border.bottomLeftV;
            }
            //render rounded corner
            if (radx > MIN || rady > MIN) {
                if (borderIsVisible(elem, side1))
                    pdf.drawBorderRadiusACA(this, cr, elem, side1, widthVer, widthHor);
                if (borderIsVisible(elem, side2))
                    pdf.drawBorderRadiusACB(this, cr, elem, side2, widthVer, widthHor);

                if (widthVer > rady || widthHor > radx) {
                    cr.isDrawn = false;
                }
                //render normal corner
            } else if (widthHor > 0 || widthVer > 0) { // draw only if element has border
                if (borderIsVisible(elem, side1))
                    pdf.drawNormalCorner(point1, point2, node, currentPage, getBorderColor(elem, side1));
                if (borderIsVisible(elem, side2))
                    pdf.drawNormalCorner(point2, point1, node, currentPage, getBorderColor(elem, side2));
            } else {
                return false;
            }
        }
        return true;
    }

    public Element getClipPathElementForBorder(Border border) {
        return null;
    }
}