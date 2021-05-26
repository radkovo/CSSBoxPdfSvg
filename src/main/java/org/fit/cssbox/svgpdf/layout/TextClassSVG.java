package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.csskit.Color;
import org.fit.cssbox.layout.TextBox;

import java.util.Collection;

/**
 * Class for representing textfor SVG version.
 *
 * @author Tomas Chocholaty
 */
public class TextClassSVG extends TextClass {
    public String fontFamily;

    public TextClassSVG(TextBox text, String fontFamily) {
        super(text);
        this.fontFamily = fontFamily;
    }


    /**
     * Create String representing decoration of text for SVG
     *
     * @param textDecoration - decoration of text
     * @return style of test
     */
    private String textDecorationStyle(Collection<CSSProperty.TextDecoration> textDecoration) {
        if (textDecoration.isEmpty())
            return "text-decoration:none";
        else {
            boolean first = true;
            StringBuilder ret = new StringBuilder("text-decoration:");
            for (CSSProperty.TextDecoration dec : textDecoration) {
                if (!first)
                    ret.append(' ');
                ret.append(dec.toString());
                first = false;
            }
            return ret.toString();
        }
    }

    /**
     * Create String representing style of text for SVG
     *
     * @param textBox - text
     * @return String representing style of text
     */
    public String createText(TextBox textBox) {
        String style = "font-size:" + fontSize + "pt;" + "font-weight:" + boldStyle + ";" + "font-style:" + italicStyle + ";" +
                "text-decoration:" + isUnderline + ";" + "font-family:" + fontFamily + ";" + "fill:" + colorString(color) + ";" + "stroke:none";
        if (letterSpacing > 0.0001) {
            style += ";letter-spacing" + letterSpacing + "px";
        }
        style += ";" + textDecorationStyle(textBox.getEfficientTextDecoration());
        return style;
    }

    /**
     * Convert color to String
     *
     * @param color - color
     * @return color as String
     */
    public String colorString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

}
