package org.fit.cssbox.svgpdf.render;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.TermList;
import cz.vutbr.web.csskit.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.util.Matrix;
import org.fit.cssbox.awt.BackgroundBitmap;
import org.fit.cssbox.awt.BitmapImage;
import org.fit.cssbox.css.BackgroundDecoder;
import org.fit.cssbox.css.CSSUnits;
import org.fit.cssbox.layout.*;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.render.BackgroundImageGradient;
import org.fit.cssbox.render.RadialGradient;
import org.fit.cssbox.render.StructuredRenderer;
import org.fit.cssbox.svgpdf.layout.*;
import org.w3c.dom.Element;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;


/**
 * A class for processing all elements of DOM tree from CSSBox
 *
 * @author Tomas Chocholaty
 */

public class PDFRenderer extends StructuredRenderer {
    private PDDocument doc;
    final float avoidsSize = 0.8f;
    private PDFOutput pdf;
    private boolean bordersUsed;
    private boolean bgUsed;


    private BreakAvoidTables breakAvoidTables;
    private float outputTopPadding;
    private float outputBottomPadding;

    public PDFRenderer(float rootWidth, float rootHeight, PDDocument doc) throws IOException {
        super(rootWidth, rootHeight, "PDF");
        this.doc = doc;
        this.pdf = new PDFOutput(rootWidth, rootHeight, doc);
        // sets the default top and bottom paddings for the output page
        outputTopPadding = 50;
        outputBottomPadding = 50;
        writeHeader();
    }

    public PDFOutput getPdf() {
        return pdf;
    }

    private void writeHeader() throws IOException {
        breakAvoidTables = new BreakAvoidTables(this);
    }

    @Override
    public void startElementContents(ElementBox elem) {
    }

    @Override
    public void finishElementContents(ElementBox elem) {
    }

    /**
     * Create BreakTable and AvoidTable for paging
     */
    private void createAndProcessBreakAndAvoidTables() {
        breakAvoidTables.createBreakAvoidTables();
        breakAvoidTables.deleteAvoidsBiggerThan(avoidsSize, pdf);
        breakAvoidTables.mergeAvoids(avoidsSize, pdf);
    }

    @Override
    public void renderElementBackground(ElementBox elem) {
        if (elem instanceof Viewport) {
            ElementBox test = elem.getViewport().getRootBox();
            //STEP A create tree of all elements as nodes
            breakAvoidTables.createNodesTree(elem);
            //STEP B mozna dodelat pridavani uzlu co se do stromu z nejakeho duvodu nemohli pridat
            breakAvoidTables.tryToInsertNotInsertedNodes();
            //STEP C Avoid/Breaks table
            createAndProcessBreakAndAvoidTables();
            //STEP D padding
            breakAvoidTables.makePaging(pdf);
            try {
                pdf.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        TransformPDF t = new TransformPDF(pdf.getResCoef());
        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());

        try {
            t.transformIn(elem, pdf, node);
            super.renderElementBackground(elem);
            //t.transformOut(pdf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renderTextContent(TextBox text) {
        TextClassPDF textClass = new TextClassPDF(text, ((PDFVisualContext) text.getVisualContext()).getFont());

        ElementBox parent = text.getParent().getParent();
        ElementBox currentNode = text.getParent();
        float parentRightEndOfElement = (parent.getAbsoluteContentX() + parent.getWidth());
        float recentRightEndOfElement = (currentNode.getAbsoluteContentX() + currentNode.getWidth());
        float widthRecentElem = currentNode.getWidth();

        Node node = breakAvoidTables.getNodeByText(text, breakAvoidTables.getRootNodeOfTree());

        if (parentRightEndOfElement - recentRightEndOfElement > -widthRecentElem * 0.6) {
            if (!(text.isEmpty() || !text.isVisible() || !text.isDeclaredVisible() || !text.isDisplayed())) {
                TransformPDF t = new TransformPDF(pdf.getResCoef());
                try {
                    t.transformIn(text.getParent(), pdf, node);
                    int actualPage = 0;
                    for (int i = 0; i < pdf.getPageCount(); i++) {
                        insertText(text, i, node.getPlusOffset(), node.getPlusHeight(), textClass);
                        actualPage = i;
                    }
                    t.transformOut(pdf);
                    pdf.setCurrentPageHard(actualPage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void close() throws IOException {
        pdf.close();
    }


    @Override
    protected void renderBorder(ElementBox elem, Rectangle bb) {
        final Border border = new BorderPDF(elem.getBorder(), bb, elem, this, breakAvoidTables, pdf);
        try {
            bordersUsed = border.writeBorder(elem, border);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void renderColorBg(ElementBox elem, Rectangle bb, BackgroundDecoder bg) {
        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());
        if (node != null) {
            for (int i = 0; i < pdf.getPageCount(); i++) {
                try {
                    drawBgToElem(elem, i, node.getPlusOffset(), node.getPlusHeight(), false, false, bb, bg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        bgUsed = true;
    }


    @Override
    protected void renderImageBg(ElementBox elem, Rectangle bb, BackgroundBitmap bitmap) {
        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());
        for (int i = 0; i < pdf.getPageCount(); i++) {
            try {
                insertBgImg(elem, i, node, bitmap.getBufferedImage(), bb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bgUsed = true;
    }


    /**
     * Create background with image and add clip when rounded corner is used.
     *
     * @param elem - the element
     * @param i    - actual page
     * @param node - node corresponding with actual element
     * @param img  - image
     * @param bb   - the background bounding box
     */
    private void insertBgImg(ElementBox elem, int i, Node node, BufferedImage img, Rectangle bb) throws IOException {
        float pageStart = i * pdf.getPageHeight();
        float pageEnd = (i + 1) * pdf.getPageHeight();

        float startOfElement = elem.getAbsoluteBorderBounds().y + node.getPlusOffset();
        float endOfElement = elem.getAbsoluteBorderBounds().y + node.getPlusOffset() + node.getPlusHeight() + elem.getBorder().bottom + elem.getAbsoluteBorderBounds().getHeight() + node.getPlusHeight();

        if ((img != null) && (startOfElement > pageStart && startOfElement < pageEnd || endOfElement > pageStart && endOfElement < pageEnd)) {
            pdf.setCurrentPage(i);
            float startX = bb.x;
            float startY = bb.y + node.getPlusOffset() + node.getPlusHeight() - i * pdf.getPageHeight();
            float width = bb.width;
            float height = bb.height + node.getPlusHeight();

            if (height > 5 * node.getPlusHeight()) height += node.getPlusHeight();

            if (bordersUsed(elem) || clippingUsed(elem)) { // Tady se provede orezani pokud byl pouzit borderraius
                final Border border = new BorderPDF(elem.getBorder(), bb, elem, this, breakAvoidTables, pdf);
                pdf.recalculateYCoordinatesForPDFCorners(border, node, i);
                pdf.insertClippedImage(border, img, startX, startY, width, height);
            } else {
                //insert image
                pdf.insertImage(img, startX, startY, width, height);
            }
        }
    }


    /**
     * Test if actual element has border
     *
     * @param elem - the element
     */
    private boolean bordersUsed(ElementBox elem) {
        String sides[] = {"top", "left", "bottom", "right"};
        for (String side : sides) {
            if (elem.getStyle().getProperty("border-" + side + "-style") != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if actual element has border radius
     *
     * @param elem - the element
     */
    public boolean bordersRadiusUsed(ElementBox elem) {
        TermList value1 = elem.getStyle().getValue(TermList.class, "border-top-right-radius");
        TermList value2 = elem.getStyle().getValue(TermList.class, "border-top-left-radius");
        TermList value3 = elem.getStyle().getValue(TermList.class, "border-bottom-right-radius");
        TermList value4 = elem.getStyle().getValue(TermList.class, "border-bottom-left-radius");
        if (value1 != null || value2 != null || value3 != null || value4 != null) {
            return true;
        }
        return false;
    }


    /**
     * Draws colored background to OUTPUT
     *
     * @param elem       - the element
     * @param i          - actual page
     * @param plusOffset - offset due to paging
     * @param plusHeight - plusHeight due to paging
     * @param radialGrad - true for render radial gradient
     * @param linearGrad - true for render linear gradient
     * @param bb         - the background bounding box
     * @param bg         - source of background
     * @throws IOException
     */
    private void drawBgToElem(ElementBox elem, int i, float plusOffset, float plusHeight, boolean radialGrad,
                              boolean linearGrad, Rectangle bb, BackgroundDecoder bg) throws IOException {
        // checks if any color available
        if ((elem.getBgcolor() == null) && (!radialGrad) && (!linearGrad))
            return;

        // calculates the start and the end of current page
        float pageStart = i * pdf.getPageHeight();
        float pageEnd = (i + 1) * pdf.getPageHeight();

        float startOfTheElement = elem.getAbsoluteBorderBounds().y + plusOffset;
        float endOfTheElement = startOfTheElement + elem.getAbsoluteBorderBounds().height + plusHeight + elem.getBorder().bottom;

        // checks if the element if completely out of page
        if ((startOfTheElement < pageEnd && startOfTheElement >= pageStart) || (endOfTheElement <= pageEnd && endOfTheElement > pageStart) || (startOfTheElement <= pageStart && endOfTheElement >= pageEnd)) {
            pdf.setCurrentPage(i);

            float converted_y = (pdf.getPageHeight() - (elem.getAbsoluteContentY())
                    + i * pdf.getPageHeight() - elem.getContentHeight() - plusHeight - plusOffset - elem.getPadding().bottom - elem.getBorder().bottom);

            if (bordersRadiusUsed(elem)) {
                final Border border = new BorderPDF(elem.getBorder(), bb, elem, this, breakAvoidTables, pdf);
                Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());
                pdf.recalculateYCoordinatesForPDFCorners(border, node, i);
                pdf.insertClippedBackground(border, bg.getBgcolor());
            } else {
                pdf.drawRectangle(0, bg.getBgcolor(), bb.x, converted_y,
                        bb.width,
                        bb.height + plusHeight);
            }
        }
    }


    /**
     * Draws text to OUTPUT
     *
     * @throws IOException
     */
    private void insertText(TextBox text, int i, float plusOffset, float plusHeight, TextClassPDF textClass) throws IOException {
        // counts the distance between top of the document and the start/end of
        // the page
        float pageStart = i * pdf.getPageHeight();
        float pageEnd = (i + 1) * pdf.getPageHeight();

        // checks if the whole text is out of the page
        float endOfElement = (text.getAbsoluteContentY() + text.getLineHeight() + plusOffset);
        float startOfElement = (text.getAbsoluteContentY() + plusOffset);

        if ((startOfElement > pageStart && startOfElement < pageEnd) || (endOfElement > pageStart && endOfElement < pageEnd)) {
            pdf.setCurrentPage(i);

            float startX = text.getAbsoluteContentX();
            float startY = (text.getAbsoluteContentY() + plusOffset + plusHeight) - i * pdf.getPageHeight();

            // write to PDF
            if (text.getWordSpacing() == null && text.getExtraWidth() == 0) {
                pdf.addText(startX, startY, text.getText(), textClass.font, textClass.fontSize, textClass.isUnderline.equals("underline"), textClass.boldStyle.equals("bold"), textClass.letterSpacing * pdf.getResCoef(), textClass.fontSize, textClass.color);
            } else {
                addTextByWords(startX, startY, text, textClass);
            }

            // render links
            String href = getLinkURL(text);
            if (href != null) {
                pdf.renderLink(text, href, startX, startY, 2f * textClass.fontSize * pdf.getResCoef());
            }
        }
    }


    /**
     * Examines the given element and all its parent elements in order to find the "a" element.
     *
     * @param e the child element to start with
     * @return the "a" element found or null if it is not present
     */
    private org.w3c.dom.Element findAnchorElement(org.w3c.dom.Element e) {
        final String href = e.getAttribute("href");
        if ("a".equalsIgnoreCase(e.getTagName().trim()) && href != null && !href.isEmpty())
            return e;
        else if (e.getParentNode() != null && e.getParentNode().getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
            return findAnchorElement((org.w3c.dom.Element) e.getParentNode());
        else
            return null;
    }

    /**
     * Test if actual element has border radius
     *
     * @param elem - the element
     */
    private String getLinkURL(Element elem) {
        Element el = findAnchorElement(elem);
        if (el != null)
            return el.getAttribute("href").trim();
        else
            return null;
    }


    /**
     * Get URL for hypertext link
     *
     * @param text - element representing text
     */
    private String getLinkURL(TextBox text) {
        org.w3c.dom.Node parent = text.getNode().getParentNode();
        if (parent != null && parent instanceof Element)
            return getLinkURL((Element) parent);
        else
            return null;
    }


    /**
     * Method for render text, when words space are not default
     *
     * @param x         - x coordinate
     * @param y         - y coordinate
     * @param text      - element representing text
     * @param textClass - class which contain information about rendering text (font, color etc.)
     */
    private void addTextByWords(float x, float y, TextBox text, TextClassPDF textClass) throws IOException {
        final String[] words = text.getText().split(" ");
        if (words.length > 0) {
            final float[][] offsets = text.getWordOffsets(words);
            for (int i = 0; i < words.length; i++)
                pdf.addText(x + offsets[i][0], y, words[i], textClass.font, textClass.fontSize, textClass.isUnderline.equals("underline"), textClass.boldStyle.equals("bold"), textClass.letterSpacing * pdf.getResCoef(), textClass.fontSize, textClass.color);
        } else
            pdf.addText(x, y, text.getText(), textClass.font, textClass.fontSize, textClass.isUnderline.equals("underline"), textClass.boldStyle.equals("bold"), textClass.letterSpacing * pdf.getResCoef(), textClass.fontSize, textClass.color);
    }

    /**
     * Draw image for replaced content
     *
     * @param box - element representing replaced box
     * @patam img - image to render
     */
    protected void insertReplacedImage(ReplacedBox box, ContentImage img) {
        Rectangle cb = ((Box) box).getAbsoluteContentBounds();

        for (int i = 0; i < pdf.getPageCount(); i++) {
            Node node = breakAvoidTables.getNodeByElement(((ElementBox) box), breakAvoidTables.getRootNodeOfTree());
            float pageStart = i * pdf.getPageHeight();
            float pageEnd = (i + 1) * pdf.getPageHeight();
            float startOfElement = ((ElementBox) box).getAbsoluteContentY() + img.getHeight() + node.getPlusOffset() + node.getPlusHeight();
            float endOfElement = ((ElementBox) box).getAbsoluteContentY() + node.getPlusOffset();

            if ((img != null) && (startOfElement > pageStart && startOfElement < pageEnd || endOfElement > pageStart && endOfElement < pageEnd)) {
                TransformPDF t = new TransformPDF(pdf.getResCoef());
                try {
                    t.transformIn(((ElementBox) box), pdf, node);
                    pdf.setCurrentPage(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Filter pdfFilter = new Filter(null, 0, 0, 1.0f, 1.0f);

                CSSProperty.Filter filter = ((ElementBox) box).getStyle().getProperty("filter");
                BufferedImage img2 = ((BitmapImage) img).getBufferedImage();

                if (filter == CSSProperty.Filter.list_values) {
                    pdfFilter = pdfFilter.createFilter((ElementBox) box);
                    img2 = pdfFilter.filterImg(img2);
                }

                float y = cb.y + node.getPlusOffset() + node.getPlusHeight() - i * pdf.getPageHeight();
                try {
                    pdf.insertImage(img2, cb.x, y, cb.width, cb.height);
                    t.transformOut(pdf);
                    pdf.setCurrentPageHard(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Check if the element is on the current page
     *
     * @param lb - the list-item box
     * @param i  - current page
     */
    private boolean controlPageRange(ListItemBox lb, int i) {
        float pageStart = i * pdf.getPageHeight();
        float pageEnd = (i + 1) * pdf.getPageHeight();
        Node node = breakAvoidTables.getNodeByElement(lb, breakAvoidTables.getRootNodeOfTree());
        if (!(lb.getAbsoluteContentY() + node.getPlusOffset() > pageEnd
                || (lb.getAbsoluteContentY() + lb.getHeight()) + node.getPlusOffset() < pageStart)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Convert y coordinate to user space coordinate system and set current page for PDFBox
     *
     * @param lb - the list-item box
     * @param i  - current page
     */
    private float getYCoordinateAndSetActualPage(ListItemBox lb, int i) throws IOException {

        Node node = breakAvoidTables.getNodeByElement(lb, breakAvoidTables.getRootNodeOfTree());
        float y = 0;
        if (controlPageRange(lb, i)) {
            pdf.setCurrentPage(i);
            y = (lb.getAbsoluteContentY() + node.getPlusOffset()) % pdf.getPageHeight();
            y = (pdf.getPageHeight() - y) - (lb.getParent().getVisualContext().getFontSize());
        }
        return y;
    }

    /**
     * Convert coordinates to user space coordinate system
     *
     * @param elem       - the element
     * @param x          - x coordinate
     * @param y          - y coordinate
     * @param plusOffset - offset due to paging
     * @param plusHeight - plusHeight due to paging
     * @param i          - current page
     */
    private float[] transXYtoPDF(ElementBox elem, float x, float y, float plusOffset, float plusHeight, int i) {
        float[] xy = new float[2];

        float paddingBottom = elem.getPadding().bottom * pdf.getResCoef();
        float paddingLeft = elem.getPadding().left * pdf.getResCoef();

        xy[0] = elem.getAbsoluteContentX() * pdf.getResCoef() - paddingLeft + x;
        xy[1] = (pdf.getPageFormat().getHeight() - (elem.getAbsoluteContentY() * pdf.getResCoef() + plusOffset)
                + i * pdf.getPageFormat().getHeight() - elem.getContentHeight() * pdf.getResCoef() - plusHeight - paddingBottom) + y;
        return xy;
    }

    @Override
    protected void writeCircleBullet(ListItemBox lb, float x, float y, float r, Color color) {
        for (int i = 0; i < pdf.getPageCount(); i++) {
            try {
                y = getYCoordinateAndSetActualPage(lb, i);
                pdf.drawCircle(1.0f, color, x + r / 2, y - r / 2, r / 2, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void writeSquareBullet(ListItemBox lb, float x, float y, float r, Color color) {
        for (int i = 0; i < pdf.getPageCount(); i++) {
            try {
                y = getYCoordinateAndSetActualPage(lb, i);
                pdf.drawRectangle(1, color, x, y - r, r, r);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void writeDiscBullet(ListItemBox lb, float x, float y, float r, Color color) {
        for (int i = 0; i < pdf.getPageCount(); i++) {
            try {
                y = getYCoordinateAndSetActualPage(lb, i);
                pdf.drawCircle(1.0f, color, x + r / 2, y - r / 2, r / 2, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void writeOtherBullet(ListItemBox lb, float x, float y) {
        PDFVisualContext ctx = (PDFVisualContext) lb.getVisualContext();
        Node node = breakAvoidTables.getNodeByElement(lb, breakAvoidTables.getRootNodeOfTree());
        float fontSize = ctx.getFontSize();
        boolean isBold = ctx.getFontInfo().isBold();
        float letterSpacing = CSSUnits.pixels(ctx.getLetterSpacing());
        PDFont font = ctx.getFont();
        for (int i = 0; i < pdf.getPageCount(); i++) {
            if (controlPageRange(lb, i)) {
                float baseline = lb.getFirstInlineBoxBaseline();
                if (baseline == -1) baseline = ctx.getBaselineOffset(); //use the font baselin
                y = (lb.getAbsoluteContentY() + baseline - 0.85f * fontSize * pdf.getResCoefTextConstant() + node.getPlusOffset()) - i * pdf.getPageHeight();
                try {
                    pdf.setCurrentPage(i);
                    pdf.addText(x, y, lb.getMarkerText(), font, fontSize, false, isBold, letterSpacing * pdf.getResCoef(), fontSize, ctx.getColor());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void createImageBullet(ListItemBox lb, float ix, float iy, float iw, float ih, ContentImage img) {
        for (int i = 0; i < pdf.getPageCount(); i++) {
            if (controlPageRange(lb, i)) {
                Node node = breakAvoidTables.getNodeByElement(lb, breakAvoidTables.getRootNodeOfTree());
                iy = (lb.getAbsoluteContentY() + node.getPlusOffset()) % pdf.getPageHeight();
                try {
                    pdf.setCurrentPage(i);
                    pdf.insertImage(((BitmapImage) img).getBufferedImage(), ix, iy, iw, ih);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void addLinearGradient(BackgroundImageGradient bgimage, float absx, float absy, ElementBox elem) {
        LinearGradientPDF gradPDF = new LinearGradientPDF();
        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());

        Matrix radMatrix = new Matrix();

        for (int i = 0; i < pdf.getPageCount(); i++) {

            float pageStart = i * pdf.getPageFormat().getHeight();
            float pageEnd = (i + 1) * pdf.getPageFormat().getHeight();

            if (!(elem.getAbsoluteContentY() * pdf.getResCoef() + node.getPlusOffset() > pageEnd
                    || (elem.getAbsoluteContentY() + elem.getContentHeight()) * pdf.getResCoef() + node.getPlusOffset()
                    + node.getPlusHeight() < pageStart)) {


                float border_x = elem.getAbsoluteContentX() * pdf.getResCoef() - elem.getPadding().left * pdf.getResCoef();
                float border_y = pdf.getPageFormat().getHeight() - (elem.getAbsoluteContentY() * pdf.getResCoef() + node.getPlusOffset())
                        + i * pdf.getPageFormat().getHeight() - elem.getContentHeight() * pdf.getResCoef() - node.getPlusHeight() - elem.getPadding().bottom * pdf.getResCoef();

                float width = elem.getContentWidth() * pdf.getResCoef();
                float height = elem.getContentHeight() * pdf.getResCoef() + elem.getPadding().top * pdf.getResCoef() + elem.getPadding().bottom * pdf.getResCoef() + node.getPlusHeight();


                PDShadingType3 shading = null;
                try {
                    shading = gradPDF.createLinearGrad(bgimage, border_x, border_y, width, height, pdf.getResCoef());
                    pdf.setCurrentPage(i);
                    pdf.drawBgGrad(0, shading, border_x, border_y,
                            (elem.getContentWidth()) * pdf.getResCoef() + elem.getPadding().left * pdf.getResCoef() + elem.getPadding().right * pdf.getResCoef(),
                            elem.getContentHeight() * pdf.getResCoef() + elem.getPadding().top * pdf.getResCoef() + elem.getPadding().bottom * pdf.getResCoef() + node.getPlusHeight(), radMatrix);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void addRadialGradient(BackgroundImageGradient bgimage, float absx, float absy, ElementBox elem) {
        RadialGradient grad = (RadialGradient) bgimage.getGradient();

        RadialGradientPDF gradPDF = new RadialGradientPDF();
        Node node = breakAvoidTables.getNodeByElement(elem, breakAvoidTables.getRootNodeOfTree());

        Matrix radMatrix = new Matrix();

        for (int i = 0; i < pdf.getPageCount(); i++) {

            float pageStart = i * pdf.getPageFormat().getHeight();
            float pageEnd = (i + 1) * pdf.getPageFormat().getHeight();

            if (!(elem.getAbsoluteContentY() * pdf.getResCoef() + node.getPlusOffset() > pageEnd
                    || (elem.getAbsoluteContentY() + elem.getContentHeight()) * pdf.getResCoef() + node.getPlusOffset()
                    + node.getPlusHeight() < pageStart)) {

                //rotate gradient
                AffineTransform ret = new AffineTransform();
                ret.scale(1, -1);
                TransformPDF t = new TransformPDF(pdf.getResCoef());
                float[] oxoy = t.getOxOy(elem);
                oxoy = t.transXYtoPDF(elem, oxoy[0], oxoy[1], node.getPlusOffset(),
                        node.getPlusHeight(), i, (int) pdf.getPageHeight());
                try {
                    pdf.addTransform(ret, oxoy[0] * pdf.getResCoef(), oxoy[1] * pdf.getResCoef());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                float border_x = elem.getAbsoluteContentX() * pdf.getResCoef() - elem.getPadding().left * pdf.getResCoef();
                float border_y = pdf.getPageFormat().getHeight() - (elem.getAbsoluteContentY() * pdf.getResCoef() + node.getPlusOffset())
                        + i * pdf.getPageFormat().getHeight() - elem.getContentHeight() * pdf.getResCoef() - node.getPlusHeight() - elem.getPadding().bottom * pdf.getResCoef();


                float width = elem.getContentWidth() * pdf.getResCoef();
                float height = elem.getContentHeight() * pdf.getResCoef() + elem.getPadding().top * pdf.getResCoef() + elem.getPadding().bottom * pdf.getResCoef() + node.getPlusHeight();

                PDShadingType3 shading = gradPDF.createRadialGrad(bgimage, border_x, border_y, width, height, pdf.getResCoef());

                AffineTransform moveToCenter;
                if (!grad.isCircle()) {
                    float scaleX = 1.0f, scaleY = 1.0f;

                    float[] newXY = new float[2];
                    newXY = transXYtoPDF(elem, grad.getCx() * pdf.getResCoef(), grad.getCy() * pdf.getResCoef(),
                            node.getPlusOffset(),
                            node.getPlusHeight(), i);

                    if (grad.getRx() > grad.getRy()) {
                        scaleY = grad.getRy() / grad.getRx();
                        moveToCenter = AffineTransform.getTranslateInstance(0, newXY[1] - (newXY[1] * scaleY));
                    } else {
                        scaleX = grad.getRx() / grad.getRy();
                        moveToCenter = AffineTransform.getTranslateInstance(absx - (absx * scaleX), 0);
                    }

                    AffineTransform at = AffineTransform.getScaleInstance(scaleX, scaleY);
                    moveToCenter.concatenate(at);

                    radMatrix = new Matrix(moveToCenter);
                }

                try {
                    pdf.setCurrentPage(i);
                    pdf.drawBgGrad(0, shading, border_x, border_y,
                            (elem.getContentWidth()) * pdf.getResCoef() + elem.getPadding().left * pdf.getResCoef() + elem.getPadding().right * pdf.getResCoef(),
                            elem.getContentHeight() * pdf.getResCoef() + elem.getPadding().top * pdf.getResCoef() + elem.getPadding().bottom * pdf.getResCoef() + node.getPlusHeight(), radMatrix);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}