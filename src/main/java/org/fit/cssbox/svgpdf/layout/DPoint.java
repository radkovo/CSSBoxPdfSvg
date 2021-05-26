
package org.fit.cssbox.svgpdf.layout;

/**
 * class representing a point in floating point coordinates
 *
 * @author Martin Safar
 * @author Tomas Chocholaty
 */
public class DPoint implements Cloneable {

    public float x;
    public float y;

    public DPoint() {
    }

    public DPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Object clone() {
        DPoint point = null;
        try {
            point = (DPoint) super.clone();
            point.x = this.x;
            point.y = this.y;
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            cloneNotSupportedException.printStackTrace();
        }
        return point;
    }

}
