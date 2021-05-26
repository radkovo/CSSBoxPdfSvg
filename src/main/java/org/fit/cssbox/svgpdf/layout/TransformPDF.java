package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.*;
import org.fit.cssbox.layout.CSSDecoder;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.svgpdf.render.Node;
import org.fit.cssbox.svgpdf.render.PDFOutput;

import java.awt.geom.AffineTransform;
import java.io.IOException;

/**
 * Class for generating transform PDF
 *
 * @author Hoang Duong Nguyen
 * @author Tomas Chocholaty
 */
public class TransformPDF {

    private float resCoef;

    public TransformPDF(float resCoef) {
        super();
        this.resCoef = resCoef;
    }

    AffineTransform affineTransform;

    private CSSDecoder dec;

    private Rectangle bounds;

    private boolean transformUsed = false;


    /**
     * Transform the CSS coordinates to PDF coordinates.
     *
     * @param elem       - element, which has the CSS coordinates
     * @param x          - x-axis of the element
     * @param y          - y-axis of the element
     * @param plusOffset - offset of the element content
     * @param plusHeight - height outside the element content
     * @param i          - number of the current page
     * @return an array, which contains new x-axis([0]) and y-axis([1])
     */
    public float[] transXYtoPDF(ElementBox elem, float x, float y, float plusOffset, float plusHeight, int i, float pageHeight) {
        float[] xy = new float[2];

        float paddingBottom = elem.getPadding().bottom;
        float paddingLeft = elem.getPadding().left;

        xy[0] = elem.getAbsoluteContentX() * -paddingLeft + x;
        xy[1] = (pageHeight - (elem.getAbsoluteContentY() + plusOffset)
                + i * pageHeight - elem.getContentHeight() - plusHeight - paddingBottom) + y;
        return xy;
    }


    /**
     * Set the CSS3 transform properties values.
     *
     * @param elem       - instance of ElementBox
     * @param node       - node corresponding with actual element
     * @param actualPage - current page
     * @param pageHeight - height of page
     * @return transform
     */
    public AffineTransform insertTransformPDF(ElementBox elem, Node node, int actualPage, float pageHeight) {
        if (elem.isBlock() || elem.isReplaced()) {

            dec = new CSSDecoder(elem.getVisualContext());
            bounds = elem.getAbsoluteBorderBounds();
            float oxoy[] = getOxOy(elem);
            float ox = oxoy[0];
            float oy = oxoy[1];

            float pageStart = actualPage * pageHeight;
            float pageEnd = (actualPage + 1) * pageHeight;

            float startOfTheElement = elem.getAbsoluteBorderBounds().y + node.getPlusOffset();
            float endOfTheElement = startOfTheElement + elem.getAbsoluteBorderBounds().height + node.getPlusHeight() + elem.getBorder().bottom;

            // checks if the element if completely out of page
            if ((startOfTheElement < pageEnd && startOfTheElement >= pageStart) || (endOfTheElement <= pageEnd && endOfTheElement > pageStart) || (startOfTheElement <= pageStart && endOfTheElement >= pageEnd)) {

                // compute the transformation matrix
                CSSProperty.Transform trans = elem.getStyle().getProperty("transform");

                if (trans == CSSProperty.Transform.list_values) {
                    boolean transformed = false;
                    AffineTransform ret = new AffineTransform();
                    TermList values = elem.getStyle().getValue(TermList.class, "transform");
                    for (Term<?> term : values) {
                        if (term instanceof TermFunction.Rotate) {
                            final double theta = dec.getAngle(((TermFunction.Rotate) term).getAngle());
                            ret.rotate(-theta);
                            transformed = true;
                        } else if (term instanceof TermFunction.Scale) {
                            float sx = ((TermFunction.Scale) term).getScaleX();
                            float sy = ((TermFunction.Scale) term).getScaleY();
                            ret.scale(sx, sy);
                            transformed = true;
                        } else if (term instanceof TermFunction.ScaleX) {
                            float sx = ((TermFunction.ScaleX) term).getScale();
                            ret.scale(sx, 1.0f);
                            transformed = true;
                        } else if (term instanceof TermFunction.ScaleY) {
                            float sy = ((TermFunction.ScaleY) term).getScale();
                            ret.scale(1.0f, sy);
                            transformed = true;
                        } else if (term instanceof TermFunction.Skew) {
                            double anx = dec.getAngle(((TermFunction.Skew) term).getSkewX());
                            double any = dec.getAngle(((TermFunction.Skew) term).getSkewY());
                            ret.shear(Math.tan(-anx), Math.tan(-any));
                            transformed = true;
                        } else if (term instanceof TermFunction.SkewX) {
                            double anx = dec.getAngle(((TermFunction.SkewX) term).getSkew());
                            ret.shear(Math.tan(-anx), 0.0);
                            transformed = true;
                        } else if (term instanceof TermFunction.SkewY) {
                            double any = dec.getAngle(((TermFunction.SkewY) term).getSkew());
                            ret.shear(0.0, -any);
                            transformed = true;
                        } else if (term instanceof TermFunction.Matrix) {
                            float[] vals = new float[6];
                            vals = ((TermFunction.Matrix) term).getValues();
                            vals[1] = -vals[1]; // must be inverted because of
                            // coordinate system in PDF
                            vals[2] = -vals[2];
                            vals[5] = -vals[5];
                            ret.concatenate(new AffineTransform(vals));
                            transformed = true;
                        } else if (term instanceof TermFunction.Translate) {
                            float tx = dec.getLength(((TermFunction.Translate) term).getTranslateX(), false, 0, 0,
                                    bounds.width);
                            float ty = resCoef * dec.getLength(((TermFunction.Translate) term).getTranslateY(), false, 0, 0,
                                    bounds.height);
                            ret.translate(tx * resCoef, -ty * resCoef); // - because of the different coordinate system in PDF
                            transformed = true;
                        } else if (term instanceof TermFunction.TranslateX) {
                            float tx = dec.getLength(((TermFunction.TranslateX) term).getTranslate(), false, 0, 0,
                                    bounds.width);
                            ret.translate(tx * resCoef, 0.0);
                            transformed = true;
                        } else if (term instanceof TermFunction.TranslateY) {
                            float ty = dec.getLength(((TermFunction.TranslateY) term).getTranslate(), false, 0, 0,
                                    bounds.height);
                            ret.translate(0.0, -ty * resCoef);
                            transformed = true;
                        }
                    }

                    if (transformed) {
                        return ret;
                    } else
                        return null; // no transformation applied
                } else
                    return null; // no transformation declared
            } else
                return null; // not applicable for this element type
        }
        return null;
    }


    /**
     * Set transformation for current element
     *
     * @param elem - instance of ElementBox
     * @param pdf  - pdf output
     * @param node - node corresponding with actual element
     * @throws IOException
     */
    public void transformIn(ElementBox elem, PDFOutput pdf, Node node) throws IOException {
        AffineTransform ret = null;
        for (int i = 0; i < pdf.getPageCount(); i++) {
            ret = insertTransformPDF(elem, node, i, pdf.getPageHeight());
            if (ret != null) {
                pdf.setCurrentPage(i);
                float[] oxoy = getOxOy(elem);
                oxoy = transXYtoPDF(elem, oxoy[0], oxoy[1], node.getPlusOffset(),
                        node.getPlusHeight(), i, (int) pdf.getPageHeight());
                pdf.addTransform(ret, oxoy[0] * pdf.getResCoef(), oxoy[1] * pdf.getResCoef());
                transformUsed = true;
            }
        }
    }

    /**
     * ends the transformation for the current element
     *
     * @param pdf - pdf output
     * @throws IOException
     */
    public void transformOut(PDFOutput pdf) throws IOException {
        if (transformUsed) {
            pdf.restoreGraphicsState();
            transformUsed = false;
        }
    }

    /**
     * Get center point for transformation
     *
     * @param elem - instance of ElementBox
     * @return field with point coordinates
     */
    public float[] getOxOy(ElementBox elem) {
        CSSDecoder dec = new CSSDecoder(elem.getVisualContext());
        Rectangle bounds = elem.getAbsoluteBorderBounds();
        // decode the origin
        float ox, oy;
        CSSProperty.TransformOrigin origin = elem.getStyle().getProperty("transform-origin");
        if (origin == CSSProperty.TransformOrigin.list_values) {
            TermList values = elem.getStyle().getValue(TermList.class, "transform-origin");
            ox = dec.getLength((TermLengthOrPercent) values.get(0), false, bounds.width / 2, 0, bounds.width);
            oy = dec.getLength((TermLengthOrPercent) values.get(1), false, bounds.height / 2, 0, bounds.height);
        } else {
            ox = bounds.width / 2;
            oy = bounds.height / 2;
        }
        return new float[]{ox, oy};
    }
}
