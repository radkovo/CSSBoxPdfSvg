package org.fit.cssbox.svgpdf.layout;


import org.apache.pdfbox.pdmodel.font.PDFont;
import org.fit.cssbox.layout.TextBox;


/**
 * Class for representing text for PDF version.
 *
 * @author Tomas Chocholaty
 */
public class TextClassPDF extends TextClass {
    public PDFont font;

    public TextClassPDF(TextBox text, PDFont font) {
        super(text);
        this.font = font;
    }
}
