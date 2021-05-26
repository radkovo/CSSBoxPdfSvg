package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.*;
import org.fit.cssbox.layout.CSSDecoder;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.Rectangle;

/**
 * Class for generating transform SVG
 *
 * @author Martin Safar
 * @author Tomas Chocholaty
 */
public class TransformSVG {

    public TransformSVG() {
        super();
    }

    private CSSDecoder dec;

    private Rectangle bounds;

    /**
     * Set the CSS3 transform properties values.
     *
     * @param elem - instance of ElementBox
     * @return transform
     */
    public String insertTransformSVG(ElementBox elem) {

        if (elem.isBlock() || elem.isReplaced()) {
            dec = new CSSDecoder(elem.getVisualContext());
            bounds = elem.getAbsoluteBorderBounds();
            //decode the origin
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
            ox += bounds.x;
            oy += bounds.y;
            CSSProperty.Transform trans = elem.getStyle().getProperty("transform");
            if (trans == CSSProperty.Transform.list_values) {
                String transformStr = "translate(" + ox + " " + oy + ") ";
                TermList values = elem.getStyle().getValue(TermList.class, "transform");
                boolean testRotate = false;
                for (Term<?> term : values) {
                    if (term instanceof TermFunction) {
                        final TermFunction func = (TermFunction) term;
                        final String fname = func.getFunctionName().toLowerCase();
                        if (fname.equals("rotate")) {
                            transformStr += getRotate(func);
                            testRotate = true;
                        } else if (fname.equals("translate")) {
                            transformStr += getTranslate(func);
                        } else if (fname.equals("translatex")) {
                            transformStr += getTranslateX(func);
                        } else if (fname.equals("translatey")) {
                            transformStr += getTranslateY(func);
                        } else if (fname.equals("scale")) {
                            transformStr += getScale(func);
                        } else if (fname.equals("scalex")) {
                            transformStr += getScaleX(func);
                        } else if (fname.equals("scaley")) {
                            transformStr += getScaleY(func);
                        } else if (fname.equals("skew")) {
                            transformStr += getSkew(func);
                        } else if (fname.equals("skewx")) {
                            transformStr += getSkewX(func);
                        } else if (fname.equals("skewy")) {
                            transformStr += getSkewY(func);
                        } else if (fname.equals("matrix")) {
                            transformStr += getMatrix(func);
                        }
                    }

                    transformStr += " ";
                }
                transformStr += " translate( -" + ox + " -" + oy + ")";
                return transformStr;
            }
        }
        return "";
    }

    //=================================================================================================

    /**
     * Convert term to matrix
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getMatrix(TermFunction func) {
        for (int i = 0; i < func.size(); i++) {
            if (!isNumber(func.get(i))) {
                func.remove(func.get(i));
            }
        }

        if (func.size() == 6) {
            double[] vals = new double[6];
            boolean typesOk = true;
            for (int i = 0; i < 6; i++) {
                if (isNumber(func.get(i))) {
                    vals[i] = getNumber(func.get(i));
                } else {
                    typesOk = false;
                }
            }
            if (typesOk) {
            }
            return "matrix( " + vals[0] + " " + vals[1] + " " + vals[2] + " " + vals[3] + " " + vals[4] + " " + vals[5]
                    + " ) ";
        }
        return "";
    }

    /**
     * Convert skewX transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getSkewX(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermAngle) {
            TermAngle t = (TermAngle) func.get(0);
            return "skewX( " + t.getValue() + " ) ";
        }
        return "";
    }

    /**
     * Convert skewY transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getSkewY(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermAngle) {
            TermAngle t = (TermAngle) func.get(0);
            return "skewY( " + t.getValue() + " ) ";
        }
        return "";
    }

    /**
     * Convert skew transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getSkew(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermAngle) {
            TermAngle t = (TermAngle) func.get(0);
            return "skewX( " + t.getValue() + " ) ";

        } else if (func.size() == 3 && func.get(0) instanceof TermAngle && func.get(2) instanceof TermAngle) {
            TermAngle tx = (TermAngle) func.get(0);
            TermAngle ty = (TermAngle) func.get(2);
            return "matrix( " + 1 + " " + Math.tan(Math.toRadians(tx.getValue())) + " "
                    + Math.tan(Math.toRadians(ty.getValue())) + " " + 1 + " " + 0 + " " + 0 + " ) ";
        }
        return "";
    }

    /**
     * Convert scaleX transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getScaleX(TermFunction func) {
        if (func.size() == 1 && isNumber(func.get(0))) {
            double sx = getNumber(func.get(0));
            return "scale( " + sx + ", 1 ) ";
        }
        return "";
    }

    /**
     * Convert scaleY transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getScaleY(TermFunction func) {
        if (func.size() == 1 && isNumber(func.get(0))) {
            double sy = getNumber(func.get(0));
            return "scale( 1 " + sy + " ) ";
        }
        return "";
    }

    /**
     * Convert scale transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getScale(TermFunction func) {
        if (func.size() == 1 && isNumber(func.get(0))) {
            double sx = getNumber(func.get(0));
            return "scale( " + sx + " ) ";
        } else if (func.size() == 3 && isNumber(func.get(0)) && isNumber(func.get(2))) {
            double sx = getNumber(func.get(0));
            double sy = getNumber(func.get(2));
            return "scale( " + sx + " " + sy + " ) ";
        }
        return "";
    }

    /**
     * Convert translateX transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getTranslateX(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent) {
            final float tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            return "translate( " + tx + " ) ";
        }
        return "";
    }

    /**
     * Convert translateY transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getTranslateY(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent) {
            final float ty = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.height);
            return "translate( 0 " + ty + " ) ";
        }
        return "";
    }

    /**
     * Convert translate transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getTranslate(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermLengthOrPercent) {
            final float tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            final float ty = 0;
            return "translate( " + tx + " " + ty + " ) ";
        } else if (func.size() == 3 && func.get(0) instanceof TermLengthOrPercent
                && func.get(2) instanceof TermLengthOrPercent) {
            final float tx = dec.getLength((TermLengthOrPercent) func.get(0), false, 0, 0, bounds.width);
            final float ty = dec.getLength((TermLengthOrPercent) func.get(2), false, 0, 0, bounds.height);
            return "translate( " + tx + " " + ty + " ) ";
        }
        return "";
    }

    /**
     * Convert rotate transform to String
     *
     * @param func - transform function
     * @return transform as String
     */
    private String getRotate(TermFunction func) {
        if (func.size() == 1 && func.get(0) instanceof TermAngle) {
            TermAngle t = (TermAngle) func.get(0);
            return "rotate( " + t.getValue() + " " + 0 + " " + 0 + " ) ";
        }
        return "";
    }

    private static boolean isNumber(Term<?> term) {
        return term instanceof TermNumber || term instanceof TermInteger;
    }

    private static float getNumber(Term<?> term) {
        if (term instanceof TermNumber) {
            return ((TermNumber) term).getValue();
        } else {
            return ((TermInteger) term).getValue();
        }
    }
}
