
package org.fit.cssbox.svgpdf.layout;

import org.fit.cssbox.layout.Rectangle;

/**
 * A single border corner representation
 *
 * @author Martin Safar
 * @author burgetr
 * @author Tomas Chocholaty
 */
public class CornerRadius implements Cloneable {
    //
    public float x;
    public float y;

    // jednotlive body
    public DPoint a;
    public DPoint b;
    public DPoint c;
    public DPoint d;
    public DPoint e;
    public DPoint o;
    public DPoint g;
    public DPoint h;

    public DPoint q;

    // smernice a vysek na ose y pro hranicni primku
    public double z;
    public double k;

    // obdelnik vymezeny body AOCE, ve kterem budeme hledat pruseciky
    public Rectangle bounds;

    public int s;

    public boolean isDrawn;

    public boolean draw;


    public CornerRadius() {
        this.o = new DPoint();
        this.e = new DPoint();
        this.d = new DPoint();
        this.g = new DPoint();
        this.h = new DPoint();
        this.a = new DPoint();
        this.b = new DPoint();
        this.c = new DPoint();
    }

    public CornerRadius(float x, float y, DPoint a, DPoint b, DPoint c, DPoint d, DPoint e, DPoint o, DPoint g, DPoint h, DPoint q, int s) {
        this.x = x;
        this.y = y;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.o = o;
        this.g = g;
        this.h = h;
        this.q = q;
        this.s = s;
    }

    /**
     * v konstruktoru jsou pouze inicializovany hodnoty
     *
     * @param radx
     * @param rady
     * @param ss
     */
    public CornerRadius(float radx, float rady, int ss) {
        this.o = new DPoint();
        this.e = new DPoint();
        this.d = new DPoint();
        this.g = new DPoint();
        this.h = new DPoint();
        this.a = new DPoint();
        this.b = new DPoint();
        this.c = new DPoint();
        q = null;
        x = radx;
        y = rady;
        s = ss;

        isDrawn = true;
        draw = false;
    }


    protected Object clone() {
        CornerRadius cr = null;
        try {
            cr = (CornerRadius) super.clone();
            cr.x = this.x;
            cr.y = this.y;
            cr.o = (DPoint) o.clone();
            cr.e = (DPoint) e.clone();
            cr.d = (DPoint) d.clone();
            cr.g = (DPoint) g.clone();
            cr.h = (DPoint) h.clone();
            cr.a = (DPoint) a.clone();
            cr.b = (DPoint) b.clone();
            cr.c = (DPoint) c.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            cloneNotSupportedException.printStackTrace();
        }
        return cr;
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
    }

    /**
     * vygenerovani SVG kodu do atributu pro tag path. tato cast vykresluje
     * jednu pulku rohu (mezi body C, D, G a H)
     *
     * @param widthVer - vertical width
     * @param widthHor - horizontal width
     * @return String representing path
     */
    public String getPathRadiusC(float widthVer, float widthHor) {
        String path1 = "";
        path1 += "M " + d.x + " " + d.y + " ";
        if (widthVer > y || widthHor > x) {
            path1 += " L " + Math.round(g.x) + " " + Math.round(g.y) + " ";
        } else {
            path1 += " A " + (x - widthHor) + " " + (y - widthVer) + " 0 0 0 " + Math.round(g.x) + " "
                    + Math.round(g.y);
        }
        path1 += " L " + Math.round(h.x) + " " + Math.round(h.y) + " ";
        path1 += " A " + x + " " + y + " 0 0 1 " + c.x + " " + c.y;

        if (widthVer > y || widthHor > x) {
            if (s == 1 || s == 4) {
                path1 += " L " + o.x + " " + d.y + " ";
            } else {
                path1 += " L " + d.x + " " + o.y + " ";
            }
        }

        return path1;

    }

    /**
     * vygenerovani SVG kodu do atributu pro tag path. tato cast vykresluje
     * jednu pulku rohu (mezi body A, B, G a H)
     *
     * @param widthVer - vertical wisth
     * @param widthHor - horizontal width
     * @return String representing path
     */
    public String getPathRadiusA(float widthVer, float widthHor) {
        String path2 = "";

        path2 += " M " + b.x + " " + b.y + " ";
        if (widthVer > y || widthHor > x) {
            path2 += " L " + Math.round(g.x) + " " + Math.round(g.y) + " ";
        } else {
            path2 += " A " + (x - widthHor) + " " + (y - widthVer) + " 0 0 1 " + Math.round(g.x) + " "
                    + Math.round(g.y);
        }
        path2 += " L " + Math.round(h.x) + " " + Math.round(h.y) + " ";

        path2 += " A " + x + " " + y + " 0 0 0 " + a.x + " " + a.y;


        if (widthVer > y || widthHor > x) {
            if (s == 1 || s == 4) {
                path2 += " L " + b.x + " " + o.y + " ";
            } else {
                path2 += " L " + o.x + " " + b.y + " ";

            }
        }
        return path2;
    }

}
