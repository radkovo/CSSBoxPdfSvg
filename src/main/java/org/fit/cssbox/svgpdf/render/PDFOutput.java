/*
 * PDFOutput.java
 * Copyright (c) 2020 Radek Burget
 *
 * CSSBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CSSBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created on 18. 4. 2020, 18:49:12 by burgetr
 * Extended by Tomáš Chocholatý
 */

package org.fit.cssbox.svgpdf.render;

import cz.vutbr.web.csskit.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.util.Matrix;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.TextBox;
import org.fit.cssbox.svgpdf.layout.Border;
import org.fit.cssbox.svgpdf.layout.BorderPDF;
import org.fit.cssbox.svgpdf.layout.CornerRadius;
import org.fit.cssbox.svgpdf.layout.DPoint;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A representation of the output PDF document.
 * Extrernally, it works with CSSBox coordinates. Internally, it recomputes the coordinates to the PDF box scale.
 *
 * @author burgetr
 * @author Tomas Chocholaty
 */
public class PDFOutput implements Closeable {
    private PDDocument doc;
    private PDPage page;
    private PDPageContentStream content;
    private PDRectangle pageFormat;
    private int lastpage;

    /**
     * The number of pages necessary for rendering the output
     */
    private int pageCount;

    /**
     * Rendering area height in CSSBox units
     */
    private float rootHeight;

    /**
     * The resolution ratio between the CSSBox units and the PDFBox units
     */
    private float resCoef;

    /**
     * The resolution ratio constant for text
     */
    private final float resCoefTextConstant = (float) 1.343;

    //========================================================================================

    /**
     * Creates a PDF output to a given document.
     *
     * @param rootWidth  the rendered area width
     * @param rootHeight the rendered area height
     * @param doc        the output document to use for output
     */
    public PDFOutput(float rootWidth, float rootHeight, PDDocument doc) {
        this.doc = doc;
        this.page = doc.getPage(0);
        this.pageFormat = page.getMediaBox();
        // calculate resize coefficient
        resCoef = this.pageFormat.getWidth() / rootWidth;
        setRootHeight(rootHeight);
    }

    public float getResCoef() {
        return resCoef;
    }

    public int getPageCount() {
        return pageCount;
    }

    public PDRectangle getPageFormat() {
        return pageFormat;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public float getRootHeight() {
        return rootHeight;
    }

    public void setRootHeight(float rootHeight) {
        this.rootHeight = rootHeight;
        // update the required number of pages
        pageCount = (int) Math.ceil(rootHeight * resCoef / this.pageFormat.getHeight());
    }

    public float getResCoefTextConstant() {
        return resCoefTextConstant;
    }

    /**
     * Creates an empty set of pages and starts the output.
     *
     * @throws IOException
     */
    public void openStream() throws IOException {
        content = new PDPageContentStream(doc, page);
        insertPages(pageCount);
    }

    /**
     * Closes the output document.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        content.close();
    }

    /**
     * Inserts N pages to PDF document
     */
    private void insertPages(int pageCount) {
        for (int i = 1; i < pageCount; i++) {
            PDPage page = new PDPage(pageFormat);
            doc.addPage(page);
        }
    }

    /**
     * Computes the height of a single page in CSSBox scale.
     *
     * @return the page height
     */
    public float getPageHeight() {
        return pageFormat.getHeight() / resCoef;
    }

    /**
     * Changes the current page.
     *
     * @param pageIndex the index of the page to use
     * @throws IOException
     */
    public void setCurrentPage(int pageIndex) throws IOException {
        if (lastpage != pageIndex) {
            page = (PDPage) doc.getDocumentCatalog().getPages().get(pageIndex);
            content.close();
            content = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
            lastpage = pageIndex;
        }
    }

    /**
     * Changes the current page.
     *
     * @param pageIndex the index of the page to use
     * @throws IOException
     */
    public void setCurrentPageHard(int pageIndex) throws IOException {
        page = (PDPage) doc.getDocumentCatalog().getPages().get(pageIndex);
        content.close();
        content = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
        lastpage = pageIndex;
    }

    //========================================================================================

    /**
     * Draw rectangle to output page
     *
     * @param lineWidth - width of line
     * @param bgColor   - color for background
     * @param x         - x coordinate
     * @param y         - y coordinate
     * @param width     - width of rectangle
     * @param height    - height of rectangle
     */
    public void drawRectangle(float lineWidth, Color bgColor, float x, float y, float width, float height)
            throws IOException {
        if (bgColor != null) {
            content.setLineWidth(lineWidth);
            setNonStrokingColor(bgColor);
            content.addRect(x * resCoef, y * resCoef, width * resCoef, height * resCoef);
            content.fill();
        }
    }

    /**
     * Draw rectangle to output page
     *
     * @param lineWidth - width of line
     * @param color     - color for background or color for line
     * @param cx        - x coordinate of center
     * @param cy        - y coordinate of center
     * @param r         - radius
     * @param fill      - true if circle has colored background
     */
    public void drawCircle(float lineWidth, Color color, float cx, float cy, float r, boolean fill)
            throws IOException {
        cx = cx * resCoef;
        cy = cy * resCoef;
        r = r * resCoef;

        final float k = 0.552284749831f;
        if (fill) {
            setNonStrokingColor(color);
        } else {
            content.setLineWidth(lineWidth);
            setStrokingColor(color);
        }
        content.moveTo(cx - r, cy);
        content.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
        content.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
        content.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
        content.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
        if (fill)
            content.fill();
        else
            content.stroke();
    }

    /**
     * Writes String to current PDF page using PDFBox.
     *
     * @param x             - x coordinate
     * @param y             - t coordinate
     * @param textToInsert  - text to insert
     * @param font          - font of text
     * @param fontSize      - size of text
     * @param isUnderlined  - true if text is underlined
     * @param isBold        - true if text is bold
     * @param letterSpacing - space between letter
     * @param leading       - the distance between two text line
     * @throws IOException
     */
    public void addText(float x, float y, String textToInsert, PDFont font, float fontSize,
                        boolean isUnderlined, boolean isBold, float letterSpacing, float leading, Color color) throws IOException {
        x = x * resCoef;
        y = y * resCoef;
        y = pageFormat.getHeight() - y - leading * resCoef * resCoefTextConstant;

        setNonStrokingColor(color);

        content.beginText();
        content.setFont(font, fontSize * (resCoef * resCoefTextConstant));
        content.setCharacterSpacing(letterSpacing);
        content.newLineAtOffset(x, y);
        try {
            content.showText(textToInsert);
        } catch (IllegalArgumentException e) {
            // NOTE: seems to happen for embedded icon fonts like glyphicons
            // and fa, add space so there is some text otherwise PDFBox
            // throws IllegalStateException: subset is empty; these work
            // with SVGRenderer
            content.showText(" ");
            System.err.println("Error: " + e.getMessage());
        }
        content.endText();

        // underlines text if text is set underlined
        if (isUnderlined) {
            content.setLineWidth(1);
            float strokeWidth = font.getStringWidth(textToInsert) / 1000 * fontSize;
            float lineHeightCalibration = 1f;
            float yOffset = fontSize / 6.4f;
            if (isBold) {
                lineHeightCalibration = 1.5f;
                yOffset = fontSize / 5.7f;
            }

            content.addRect(x, y - yOffset, strokeWidth * resCoef * resCoefTextConstant, resCoef * lineHeightCalibration);
            content.fill();
        }
    }

    /**
     * Inserts image to recent PDF page using PDFBox
     *
     * @param img    - image to render
     * @param x      - x coordinate
     * @param y      - y coordinate
     * @param width  - width of image
     * @param height - height of image
     * @throws IOException
     */
    public void insertImage(BufferedImage img, float x, float y, float width, float height) throws IOException {
        x = x * resCoef;
        y = y * resCoef;
        width = width * resCoef;
        height = height * resCoef;
        y = pageFormat.getHeight() - height - y;
        PDImageXObject ximage = LosslessFactory.createFromImage(doc, img);
        content.drawImage(ximage, x, y, width, height);
    }

    /**
     * Inserts background to whole recent PDF page using PDFBox
     *
     * @throws IOException
     */
    private void fillPage(Color bgColor) throws IOException {
        setNonStrokingColor(bgColor);
        content.addRect(0, 0, pageFormat.getWidth(), pageFormat.getHeight());
        content.fill();
    }

    //========================================================================================

    public void saveGraphicsState() throws IOException {
        content.saveGraphicsState();
    }

    public void restoreGraphicsState() throws IOException {
        content.restoreGraphicsState();
    }

    /**
     * Method for add transformation for element
     *
     * @param aff - transformation
     * @param ox  - x coordinate for transformation
     * @param oy  - y coordinate for transformation
     * @throws IOException
     */
    public void addTransform(AffineTransform aff, float ox, float oy) throws IOException {
        Matrix matrix = new Matrix(aff);
        content.transform(Matrix.getTranslateInstance(ox, oy));
        content.transform(matrix);
        content.transform(Matrix.getTranslateInstance(-ox, -oy));

    }

    //========================================================================================

    /**
     * Sets the stroking color for the content stream including the alpha channel.
     *
     * @param color a CSS color to set
     * @throws IOException
     */
    private void setStrokingColor(Color color) throws IOException {
        content.setStrokingColor(toPDColor(color));
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setStrokingAlphaConstant(color.getAlpha() / 255.0f);
        content.setGraphicsStateParameters(graphicsState);
    }

    /**
     * Sets the non-stroking color for the content stream including the alpha channel.
     *
     * @param color a CSS color to set
     * @throws IOException
     */
    private void setNonStrokingColor(Color color) throws IOException {
        content.setNonStrokingColor(toPDColor(color));
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(color.getAlpha() / 255.0f);
        content.setGraphicsStateParameters(graphicsState);
    }

    /**
     * Converts a CSSBox color to a PDFBox color.
     *
     * @param color
     * @return
     */
    private PDColor toPDColor(Color color) {
        if (color == null)
            return null;
        float[] components = new float[]{
                color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
        return new PDColor(components, PDDeviceRGB.INSTANCE);
    }

    //========================================================================================

    /**
     * Draw gradient for background
     *
     * @param lineWidth - width of line
     * @param shading   - gradient shading
     * @param x         - x coordinate
     * @param y         - y coordinate
     * @param width     - width of gradient
     * @param height    - height of gradient
     * @param matrix    - matrix for transformation
     */
    void drawBgGrad(float lineWidth, PDShadingType3 shading, float x, float y, float width, float height,
                    Matrix matrix) throws IOException {
        if (shading == null)
            return;
        content.saveGraphicsState();
        content.setLineWidth(lineWidth);
        content.addRect(x, y, width, height);
        content.clip();
        content.transform(matrix);
        content.shadingFill(shading);
        content.fill();
        content.restoreGraphicsState();
    }


    /**
     * Method for convert radius corner coordinates to user space coordinate system
     *
     * @param cr          - class representing radius corner
     * @param node        - node corresponding with actual element
     * @param currentPage - page for actual corner
     */
    public void recalculateYCoordinatesForPDFCorner(CornerRadius cr, Node node, int currentPage) {
        cr.a.x *= getResCoef();
        cr.b.x *= getResCoef();
        cr.c.x *= getResCoef();
        cr.d.x *= getResCoef();
        cr.e.x *= getResCoef();
        cr.g.x *= getResCoef();
        cr.h.x *= getResCoef();
        cr.o.x *= getResCoef();
        cr.a.y = convertToPFDCoordinate(cr.a.y, currentPage, node) * getResCoef();
        cr.b.y = convertToPFDCoordinate(cr.b.y, currentPage, node) * getResCoef();
        cr.c.y = convertToPFDCoordinate(cr.c.y, currentPage, node) * getResCoef();
        cr.d.y = convertToPFDCoordinate(cr.d.y, currentPage, node) * getResCoef();
        cr.e.y = convertToPFDCoordinate(cr.e.y, currentPage, node) * getResCoef();
        cr.g.y = convertToPFDCoordinate(cr.g.y, currentPage, node) * getResCoef();
        cr.h.y = convertToPFDCoordinate(cr.h.y, currentPage, node) * getResCoef();
        cr.o.y = convertToPFDCoordinate(cr.o.y, currentPage, node) * getResCoef();
    }


    /**
     * Transform device-space to user-space for point of CornerRadius class
     *
     * @param border      - class representing all border
     * @param node        - node represent elems values for PDF
     * @param currentPage - current page number
     */
    public void recalculateYCoordinatesForPDFCorners(Border border, Node node, int currentPage) {

        for (int i = 0; i < 4; i++) {
            CornerRadius cr;
            cr = border.getRadius(i + 1);
            recalculateYCoordinatesForPDFCorner(cr, node, currentPage);
        }
    }


    /**
     * Method for convert y coordinate from device-space to user-space for PDF with pagination
     *
     * @param value - value for convert
     * @param i     - current page number
     * @param node  - node represent elems values for PDF
     */
    private float convertToPFDCoordinate(float value, int i, Node node) {
        return (getPageHeight() - (value + node.getPlusOffset() - i * getPageHeight()) - node.getPlusHeight());//node.getElemHeight() -
    }


    /**
     * Method for generate first half of rounded corner
     *
     * @param borderPDF - class represent all border for PDF
     * @param cr        - rounded corner
     * @param elem      - the element
     * @param side      - the side of the border that is drawn
     * @param widthVer  - vertical width of border
     * @param widthHor  - horizontal width of border
     */
    public void drawBorderRadiusACA(BorderPDF borderPDF, CornerRadius cr, ElementBox elem, String side, float widthVer, float widthHor) throws IOException {
        // special case when one edge is zero width - this half of corner is skip
        if (!(cr.a.x == cr.h.x && cr.g.x == cr.b.x && cr.a.y == cr.h.y && cr.g.y == cr.b.y)) {
            content.setLineWidth(0.1f);
            setStrokingColor(borderPDF.getBorderColor(elem, side));
            setNonStrokingColor(borderPDF.getBorderColor(elem, side));

            content.moveTo(cr.a.x, cr.a.y);
            DPoint controlPoint = getOuterControlPointAHA(cr);
            content.curveTo1(controlPoint.x, controlPoint.y, cr.h.x, cr.h.y);

            content.curveTo1((cr.h.x + cr.g.x) / 2, (cr.h.y + cr.g.y) / 2, cr.g.x, cr.g.y);

            // special case when width border is same as radius
            if (widthHor != cr.x && widthVer != cr.y) {
                controlPoint = getInnerControlPointHB(cr);
                content.curveTo1(controlPoint.x, controlPoint.y, cr.b.x, cr.b.y);
            }

            if (widthVer > cr.y || widthHor > cr.x) {
                if ((cr.a.x < cr.h.x && cr.a.y < cr.h.y) || (cr.a.x > cr.h.x && cr.a.y > cr.h.y)) {
                    //2. and 3. corner has the coordinates reversed
                    content.curveTo1((cr.b.x + cr.a.x) / 2, (cr.b.y + cr.b.y) / 2, cr.a.x, cr.b.y);
                    content.curveTo1((cr.a.x + cr.a.x) / 2, (cr.b.y + cr.a.y) / 2, cr.a.x, cr.a.y);
                } else {
                    content.curveTo1((cr.b.x + cr.b.x) / 2, (cr.b.y + cr.a.y) / 2, cr.b.x, cr.a.y);
                    content.curveTo1((cr.b.x + cr.a.x) / 2, (cr.a.y + cr.a.y) / 2, cr.a.x, cr.a.y);
                }
            } else {
                content.curveTo1((cr.b.x + cr.a.x) / 2, (cr.b.y + cr.b.y) / 2, cr.a.x, cr.a.y);
            }
            content.fill();
        }
    }


    /**
     * Method for generate second half of rounded corner
     *
     * @param borderPDF - class represent all border for PDF
     * @param cr        - rounded corner
     * @param elem      - the element
     * @param side      - the side of the border that is drawn
     * @param widthVer  - vertical width of border
     * @param widthHor  - horizontal width of border
     */
    public void drawBorderRadiusACB(BorderPDF borderPDF, CornerRadius cr, ElementBox elem, String side, float widthVer, float widthHor) throws IOException {
        // special case when one edge is zero width - this half of corner is skip
        if (!(cr.c.x == cr.h.x && cr.g.x == cr.d.x && cr.c.y == cr.h.y && cr.g.y == cr.d.y)) {
            content.setLineWidth(0.1f);
            setStrokingColor(borderPDF.getBorderColor(elem, side));
            setNonStrokingColor(borderPDF.getBorderColor(elem, side));

            content.moveTo(cr.c.x, cr.c.y);
            DPoint controlPoint = getOuterControlPointCHC(cr);
            content.curveTo1(controlPoint.x, controlPoint.y, cr.h.x, cr.h.y);

            content.curveTo1((cr.h.x + cr.g.x) / 2, (cr.h.y + cr.g.y) / 2, cr.g.x, cr.g.y);

            // special case when width border is same as radius
            if (widthHor != cr.x && widthVer != cr.y) {
                controlPoint = getInnerControlPointCD(cr);
                content.curveTo1(controlPoint.x, controlPoint.y, cr.d.x, cr.d.y);
            }

            if (widthVer > cr.y || widthHor > cr.x) {
                if ((cr.a.x < cr.h.x && cr.a.y < cr.h.y) || (cr.a.x > cr.h.x && cr.a.y > cr.h.y)) {
                    //2. and 3. corner has the coordinates reversed
                    content.curveTo1((cr.d.x + cr.d.x) / 2, (cr.d.y + cr.c.y) / 2, cr.d.x, cr.c.y);
                    content.curveTo1((cr.c.x + cr.d.x) / 2, (cr.c.y + cr.c.y) / 2, cr.c.x, cr.c.y);
                } else {
                    content.curveTo1((cr.d.x + cr.c.x) / 2, (cr.d.y + cr.d.y) / 2, cr.c.x, cr.d.y);
                    content.curveTo1((cr.c.x + cr.c.x) / 2, (cr.d.y + cr.c.y) / 2, cr.c.x, cr.c.y);
                }
            } else {
                content.curveTo1((cr.d.x + cr.c.x) / 2, (cr.d.y + cr.c.y) / 2, cr.c.x, cr.c.y);
            }

            content.fill();
        }
    }


    /**
     * Method for calculation control point for bezier curve for outer first half of rounded corner
     *
     * @param cr - class representing rounded corner
     */
    private DPoint getOuterControlPointAHA(CornerRadius cr) {
        float b1x = cr.e.x;
        float b1y = cr.e.y;
        float b2x = 0;
        float b2y = 0;

        if (cr.a.x < cr.h.x) {
            b2x = cr.a.x + (Math.abs(cr.h.x - cr.a.x)) / 2;
        } else {
            b2x = cr.a.x - (Math.abs(cr.h.x - cr.a.x)) / 2;
        }

        if (cr.a.y < cr.h.y) {
            b2y = cr.h.y - (Math.abs(cr.a.y - cr.h.y)) / 2;
        } else {
            b2y = cr.h.y + (Math.abs(cr.a.y - cr.h.y)) / 2;
        }

        float vx = b1x - b2x;
        float vy = b1y - b2y;
        float temp = vx;
        vx = -vy;
        vy = temp;

        float c = 0 - vx * cr.e.x - vy * cr.e.y;
        float point;
        DPoint dPoint = new DPoint();
        if ((cr.a.x < cr.h.x && cr.a.y < cr.h.y) || (cr.a.x > cr.h.x && cr.a.y > cr.h.y)) {
            point = (0 - vx * cr.a.x - c) / vy;
            dPoint.x = cr.a.x;
            dPoint.y = point;
        } else {
            if (vx == 0) {
                dPoint.x = cr.a.x;
            } else {
                point = (0 - vy * cr.a.y - c) / vx;
                dPoint.x = point;
            }
            dPoint.y = cr.a.y;
        }
        return dPoint;
    }


    /**
     * Method for calculation control point for bezier curve for outer second half of rounded corner
     *
     * @param cr - class representing rounded corner
     */
    private DPoint getOuterControlPointCHC(CornerRadius cr) {
        float b1x = cr.e.x;
        float b1y = cr.e.y;
        float b2x = 0;
        float b2y = 0;

        if (cr.c.x > cr.h.x) {
            b2x = cr.h.x + (Math.abs(cr.c.x - cr.h.x) / 2);
        } else {
            b2x = cr.h.x - (Math.abs(cr.c.x - cr.h.x) / 2);
        }

        if (cr.c.y < cr.h.y) {
            b2y = cr.c.y + (Math.abs(cr.h.y - cr.c.y) / 2);
        } else {
            b2y = cr.c.y - (Math.abs(cr.h.y - cr.c.y) / 2);
        }

        float vx = b1x - b2x;
        float vy = b1y - b2y;
        float temp = vx;
        vx = -vy;
        vy = temp;


        float c = 0 - vx * cr.e.x - vy * cr.e.y;
        float point = 0;
        DPoint dPoint = new DPoint();
        if ((cr.c.x < cr.h.x && cr.c.y < cr.h.y) || (cr.c.x > cr.h.x && cr.c.y > cr.h.y)) {
            point = (0 - vy * cr.c.y - c) / vx;
            dPoint.x = point;
            dPoint.y = cr.c.y;
        } else {
            point = (0 - vx * cr.c.x - c) / vy;
            dPoint.x = cr.c.x;
            dPoint.y = point;
        }
        return dPoint;
    }

    /**
     * Method for calculation control point for bezier curve for inner first half of rounded corner
     *
     * @param cr - class representing rounded corner
     */
    private DPoint getInnerControlPointHB(CornerRadius cr) {
        float b2x = cr.b.x + (cr.g.x - cr.b.x) / 2;
        float b2y = cr.g.y + (cr.b.y - cr.g.y) / 2;

        float b1x = cr.e.x;
        float b1y = cr.e.y;
        float vx = b1x - b2x;
        float vy = b1y - b2y;
        float temp = vx;
        vx = -vy;
        vy = temp;

        float point;
        DPoint dPoint = new DPoint();
        float c = 0 - vx * cr.e.x - vy * cr.e.y;
        if ((cr.a.x < cr.h.x && cr.a.y < cr.h.y) || (cr.a.x > cr.h.x && cr.a.y > cr.h.y)) {
            point = (0 - vx * cr.b.x - c) / vy;
            dPoint.x = cr.b.x;
            dPoint.y = point;
        } else {
            point = (0 - vy * cr.b.y - c) / vx;
            dPoint.x = point;
            dPoint.y = cr.b.y;
        }
        return dPoint;
    }


    /**
     * Method for calculation control point for bezier curve for inner second half of rounded corner
     *
     * @param cr - class representing rounded corner
     */
    private DPoint getInnerControlPointCD(CornerRadius cr) {
        float b2x = cr.g.x + (cr.d.x - cr.g.x) / 2;
        float b2y = cr.d.y + (cr.g.y - cr.d.y) / 2;

        float b1x = cr.e.x;
        float b1y = cr.e.y;
        float vx = b1x - b2x;
        float vy = b1y - b2y;
        float temp = vx;
        vx = -vy;
        vy = temp;

        DPoint dPoint = new DPoint();
        float point;
        float c = 0 - vx * cr.e.x - vy * cr.e.y;
        if ((cr.c.x < cr.h.x && cr.c.y < cr.h.y) || (cr.c.x > cr.h.x && cr.c.y > cr.h.y)) {
            point = (0 - vy * cr.d.y - c) / vx;
            dPoint.x = point;
            dPoint.y = cr.d.y;
        } else {
            point = (0 - vx * cr.d.x - c) / vy;
            dPoint.x = cr.d.x;
            dPoint.y = point;
        }
        return dPoint;
    }


    /**
     * Method for generate rounded corner for creating a images clip
     *
     * @param border - class representing all border
     * @param i      - number of corner radius
     * @throws IOException
     */
    private void drawClippedPartOfPathForRaius(Border border, int i) throws IOException {
        CornerRadius cr = border.getRadius(i);
        content.lineTo(cr.a.x, cr.a.y);
        if (!(cr.a.x == cr.h.x && cr.g.x == cr.b.x && cr.a.y == cr.h.y && cr.g.y == cr.b.y)) {
            DPoint pointAH = getOuterControlPointAHA(cr);
            content.curveTo1(pointAH.x, pointAH.y, cr.h.x, cr.h.y);
        }
        if (!(cr.c.x == cr.h.x && cr.g.x == cr.d.x && cr.c.y == cr.h.y && cr.g.y == cr.d.y)) {
            DPoint pointCH = getOuterControlPointCHC(cr);
            content.curveTo1(pointCH.x, pointCH.y, cr.c.x, cr.c.y);
        }
    }

    /**
     * Method for create clipped path for image or color background
     *
     * @param border - class representing all border
     * @throws IOException
     */
    public void createClippedPath(Border border) throws IOException {
        content.setLineWidth(0);

        CornerRadius cr = border.getRadius(1);
        content.moveTo(cr.a.x, cr.a.y);
        drawClippedPartOfPathForRaius(border, 1);
        drawClippedPartOfPathForRaius(border, 3);
        drawClippedPartOfPathForRaius(border, 4);
        drawClippedPartOfPathForRaius(border, 2);

        content.closePath();
    }


    /**
     * Method for generate image, when clip is needed
     *
     * @param border - class representing all border
     * @param img    - image
     * @param x      - x coordinate
     * @param y      - y coordinte
     * @param width  - width for image
     * @param height - height for image
     * @throws IOException
     */
    public void insertClippedImage(Border border, BufferedImage img, float x, float y, float width, float height) throws IOException {
        createClippedPath(border);
        saveGraphicsState();
        content.appendRawCommands("W ");
        content.stroke();

        // transform X,Y coordinates to PDFBox format
        x = x * resCoef;
        y = y * resCoef;
        width = width * resCoef;
        height = height * resCoef;
        y = pageFormat.getHeight() - height - y;

        PDImageXObject ximage = LosslessFactory.createFromImage(doc, img);
        content.drawImage(ximage, x, y, width, height);
        restoreGraphicsState();
    }


    /**
     * Method for generate background color, when border is used
     *
     * @param border - class representing all border
     * @param color  - color of background
     * @throws IOException
     */
    public void insertClippedBackground(Border border, Color color) throws IOException {
        createClippedPath(border);
        setNonStrokingColor(color);
        content.fill();
    }

    /**
     * Method for generate half of normal corner
     *
     * @param point1      - first point for normal corner
     * @param point2      - second point for normal corner
     * @param node        - node corresponding with actual element
     * @param currentPage - current page
     * @param color       - color of border
     */
    public void drawNormalCorner(DPoint point1, DPoint point2, Node node, int currentPage, Color color) throws IOException {
        setNonStrokingColor(color);
        float x1 = point1.x * getResCoef();
        float y1 = convertToPFDCoordinate(point1.y, currentPage, node) * getResCoef();
        float x2 = point2.x * getResCoef();
        float y2 = convertToPFDCoordinate(point2.y, currentPage, node) * getResCoef();
        content.moveTo(x1, y2);
        content.lineTo(x2, y1);
        content.lineTo(x1, y1);
        content.closePath();
        content.fill();
    }

    /**
     * Method for render hypertext link
     *
     * @param text    - text for render
     * @param href    - url
     * @param x       - x coordinate
     * @param y       - y coordinate
     * @param leading - the distance between two text line
     */
    public void renderLink(TextBox text, String href, float x, float y, float leading) throws IOException {
        x = x * resCoef;
        y = y * resCoef;
        y = pageFormat.getHeight() - y - leading * resCoef;

        URL base = text.getViewport().getFactory().getBaseURL();
        URL url = null;
        try {
            url = new URL(base, href);
        } catch (MalformedURLException e) {
        }

        PDAnnotationLink link = new PDAnnotationLink();
        PDActionURI actionURI = new PDActionURI();
        actionURI.setURI(url != null ? url.toString() : href);
        link.setAction(actionURI);

        PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
        borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
        borderULine.setWidth(0);
        link.setBorderStyle(borderULine);

        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(x + text.getContentWidth() * resCoef);
        pdRectangle.setLowerLeftY(y - text.getContentHeight() * resCoef);
        pdRectangle.setUpperRightX(x);
        pdRectangle.setUpperRightY(y);
        link.setRectangle(pdRectangle);
        page.getAnnotations().add(link);
    }
}
