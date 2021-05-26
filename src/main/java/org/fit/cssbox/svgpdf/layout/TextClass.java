package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.csskit.Color;
import org.fit.cssbox.layout.TextBox;
import org.fit.cssbox.layout.VisualContext;

/**
 * Class for representing text.
 *
 * @author Tomas Chocholaty
 */
public abstract class TextClass {
    public float fontSize;
    public String boldStyle;
    public String italicStyle;
    public String isUnderline;
    public Color color;
    public float letterSpacing;


    public TextClass(TextBox text) {
        VisualContext ctx = text.getVisualContext();
        fontSize = ctx.getFontSize();
        boldStyle = ctx.getFontInfo().isBold() ? "bold" : "normal";
        italicStyle = ctx.getFontInfo().isItalic() ? "italic" : "normal";
        isUnderline = text.getEfficientTextDecoration().contains(CSSProperty.TextDecoration.UNDERLINE) ? "underline" : "normal";
        color = ctx.getColor();
        letterSpacing = ctx.getLetterSpacing();
    }
}
