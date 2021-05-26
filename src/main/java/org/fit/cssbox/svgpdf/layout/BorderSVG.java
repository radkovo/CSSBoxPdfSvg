package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.csskit.Color;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.LengthSet;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.svgpdf.render.SVGRenderer;
import org.w3c.dom.Element;


/**
 * A class for draw borders and corners for SVG
 *
 * @author Tomas Chocholaty
 */

public class BorderSVG extends Border {
    SVGRenderer svgRenderer;

    public BorderSVG(LengthSet lengths, Rectangle bb, ElementBox eb, SVGRenderer svgRenderer) {
        super(lengths, bb, eb);
        this.svgRenderer = svgRenderer;
    }


    @Override
    public boolean writeBorder(ElementBox elem, Border border) {
        boolean ret = false;
        final LengthSet borders = border.border;

        // generate borders
        ret |= writeBorderSVG(elem, border.topLeftH, border.topRightH, "top", borders.top);
        ret |= writeBorderSVG(elem, border.topRightV, border.bottomRightV, "right", borders.right);
        ret |= writeBorderSVG(elem, border.bottomLeftH, border.bottomRightH, "bottom", borders.bottom);
        ret |= writeBorderSVG(elem, border.topLeftV, border.bottomLeftV, "left", borders.left);

        // generate corners
        ret |= writeBorderCorner(border, 1, elem, "top", "right");
        ret |= writeBorderCorner(border, 2, elem, "left", "top");
        ret |= writeBorderCorner(border, 3, elem, "right", "bottom");
        ret |= writeBorderCorner(border, 4, elem, "bottom", "left");

        return ret;
    }

    /**
     * @param elem  - instance of ElementBox
     * @param a     - start of actual border side
     * @param b     - end of actual border side
     * @param side  - actual side
     * @param width - width of actual side
     * @return {@code true} when something has been written
     */
    private boolean writeBorderSVG(ElementBox elem, DPoint a, DPoint b, String side, float width) {
        TermColor tclr = elem.getStyle().getValue(TermColor.class, "border-" + side + "-color");
        CSSProperty.BorderStyle bst = elem.getStyle().getProperty("border-" + side + "-style");
        if (borderIsVisible(elem, side)) {
            Color clr = getBorderColor(elem, side);
            String coords = "";
            switch (side) {
                case "left":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x + width) + "," + b.y
                            + " L " + (a.x + width) + "," + a.y;
                    break;
                case "top":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x) + "," + (b.y + width)
                            + " L " + a.x + "," + (a.y + width);
                    break;
                case "right":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x - width) + "," + b.y
                            + " L " + (a.x - width) + "," + a.y;
                    break;
                case "bottom":
                    coords = "M " + a.x + "," + a.y + " L " + b.x + "," + b.y + " L " + (b.x) + "," + (b.y - width)
                            + " L " + a.x + "," + (a.y - width);
                    break;
            }

            Element path = svgRenderer.createPath(coords, svgRenderer.colorString(clr), svgRenderer.colorString(clr), 0);
            svgRenderer.getCurrentElem().appendChild(path);
            return true;
        } else
            return false;
    }


    /**
     * Method for generate corner of border
     *
     * @param border - class representing all points for border
     * @param s      - actual corner
     */
    private boolean writeBorderCorner(Border border, int s, ElementBox elem, String side1, String side2) {
        final CornerRadius cr = border.getRadius(s);
        final float radx = cr.x;
        final float rady = cr.y;
        String cString1 = "#000000";
        String cString2 = "#000000";

        TermColor startColor;
        TermColor stopColor;
        float widthHor, widthVer;
        DPoint point1, point2;

        if (s == 1) { // top-right
            widthHor = border.border.right;
            widthVer = border.border.top;
            startColor = border.colorRight;
            stopColor = border.colorTop;
            point1 = border.topRightH;
            point2 = border.topRightV;
        } else if (s == 2) { // topleft
            widthHor = border.border.left;
            widthVer = border.border.top;
            startColor = border.colorTop;
            stopColor = border.colorLeft;
            point1 = border.topLeftV;
            point2 = border.topLeftH;
        } else if (s == 3) { // bottomright
            widthHor = border.border.right;
            widthVer = border.border.bottom;
            startColor = border.colorBottom;
            stopColor = border.colorRight;
            point1 = border.bottomRightV;
            point2 = border.bottomRightH;
        } else { // bottomleft
            widthHor = border.border.left;
            widthVer = border.border.bottom;
            startColor = border.colorLeft;
            stopColor = border.colorBottom;
            point1 = border.bottomLeftH;
            point2 = border.bottomLeftV;
        }

        if (startColor != null) {
            cString1 = svgRenderer.colorString(startColor.getValue());
        }

        if (stopColor != null) {
            cString2 = svgRenderer.colorString(stopColor.getValue());
        }

        if (radx > MIN || rady > MIN) {
            String path1 = cr.getPathRadiusC(widthVer, widthHor);
            String path2 = cr.getPathRadiusA(widthVer, widthHor);

            if (widthVer > rady || widthHor > radx) {
                cr.isDrawn = false;
            }
            if (borderIsVisible(elem, side1)) {
                Element q = svgRenderer.createPath(path1, cString1, "none", 1);
                svgRenderer.getCurrentElem().appendChild(q);
            }
            if (borderIsVisible(elem, side2)) {
                Element q = svgRenderer.createPath(path2, cString2, "none", 1);
                svgRenderer.getCurrentElem().appendChild(q);
            }
        } else if (widthHor > 0 || widthVer > 0) {
            if (borderIsVisible(elem, side1)) {
                Element corner1 = drawNormalCorner(point2, point1, cString1);
                svgRenderer.getCurrentElem().appendChild(corner1);
            }
            if (borderIsVisible(elem, side2)) {
                Element corner2 = drawNormalCorner(point1, point2, cString2);
                svgRenderer.getCurrentElem().appendChild(corner2);
            }
        } else {
            return false;
        }
        return true;
    }


    /**
     * Method for render normal corner
     *
     * @param point1 - first point for draw normal corner
     * @param point2 - secont point for draw normal corne
     * @param color  - color for normal corner
     */
    private Element drawNormalCorner(DPoint point1, DPoint point2, String color) {
        String path = "M " + (point1.x) + " " + (point2.y) + " ";

        path += "L " + (point2.x) + " " + (point1.y) + " ";
        path += "L " + (point1.x) + " " + (point1.y) + " ";

        return svgRenderer.createPath(path, color, "none", 0);

    }

    /**
     * Method for get clip path, if rounded corner if used
     *
     * @param border - class representing all points for border
     * @return
     */
    public Element getClipPathElementForBorder(Border border) {
        Element q;
        CornerRadius crTopLeft = border.getRadius(2);
        CornerRadius crTopRight = border.getRadius(1);
        CornerRadius crBottomLeft = border.getRadius(4);
        CornerRadius crBottomRight = border.getRadius(3);

        String path = "M " + (crTopLeft.c.x) + " " + (crTopLeft.c.y) + " ";

        path += " L " + crTopRight.a.x + " " + (crTopRight.a.y) + " ";

        if (crTopRight.isDrawn) {
            path += " A " + crTopRight.x + " " + crTopRight.y + " 0 0 1 "
                    + crTopRight.c.x + " " + crTopRight.c.y;
        } else {
            path += " L " + crTopRight.h.x + " " + crTopRight.h.y;
            path += " L " + crTopRight.c.x + " " + crTopRight.c.y;
        }

        path += " L " + crBottomRight.a.x + " " + (crBottomRight.a.y) + " ";

        if (crBottomRight.isDrawn) {
            path += " A " + crBottomRight.x + " " + crBottomRight.y
                    + " 0 0 1 " + crBottomRight.c.x + " " + crBottomRight.c.y;
        } else {
            path += " L " + crBottomRight.h.x + " " + crBottomRight.h.y;
            path += " L " + crBottomRight.c.x + " " + crBottomRight.c.y;
        }

        path += " L " + crBottomLeft.a.x + " " + (crBottomLeft.a.y) + " ";

        if (crBottomLeft.isDrawn) {
            path += " A " + crBottomLeft.x + " " + crBottomLeft.y
                    + " 0 0 1 " + crBottomLeft.c.x + " " + crBottomLeft.c.y;
        } else {
            path += " L " + crBottomLeft.h.x + " " + crBottomLeft.h.y;
            path += " L " + crBottomLeft.c.x + " " + crBottomLeft.c.y;
        }

        path += " L " + crTopLeft.a.x + " " + (crTopLeft.a.y) + " ";

        if (crTopLeft.isDrawn) {
            path += " A " + crTopLeft.x + " " + crTopLeft.y + " 0 0 1 "
                    + crTopLeft.c.x + " " + crTopLeft.c.y;
        } else {
            path += " L " + crTopLeft.h.x + " " + crTopLeft.h.y;
            path += " L " + crTopLeft.c.x + " " + crTopLeft.c.y;
        }
        q = svgRenderer.createPath(path, "none", "none", 0);
        return q;
    }
}
