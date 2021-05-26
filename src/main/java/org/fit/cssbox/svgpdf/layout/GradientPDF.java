package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.csskit.Color;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

/**
 * A base for all the gradients for PDF version.
 *
 * @author Nguyen Hoang Duong
 * @author Tomas Chocholaty
 */
public abstract class GradientPDF {


    /**
     * This method is used for setting colour lengths to linear gradient.
     *
     * @param colors    - colours of linear gradient.
     * @param fractions - length of each colour in gradient line.
     * @return the function, which is an important parameter for setting linear gradient.
     */
    protected PDFunctionType3 buildType3Function(Color[] colors, float[] fractions) {
        COSDictionary function = new COSDictionary();
        function.setInt(COSName.FUNCTION_TYPE, 3);

        COSArray domain = new COSArray();
        domain.add(new COSFloat(0));
        domain.add(new COSFloat(1));

        COSArray encode = new COSArray();

        COSArray range = new COSArray();
        range.add(new COSFloat(0));
        range.add(new COSFloat(1));
        COSArray bounds = new COSArray();
        for (int i = 1; i < colors.length - 1; i++)
            bounds.add(new COSFloat(fractions[i]));

        COSArray functions = buildType2Functions(colors, domain, encode);

        function.setItem(COSName.FUNCTIONS, functions);
        function.setItem(COSName.BOUNDS, bounds);
        function.setItem(COSName.ENCODE, encode);
        PDFunctionType3 type3 = new PDFunctionType3(function);
        type3.setDomainValues(domain);
        return type3;
    }

    /**
     * This method is used for setting colours to linear gradient.
     *
     * @param colors - colours to use.
     * @param domain - parameter for setting functiontype2
     * @param encode - encoding COSArray
     * @return the COSArray, which is an important parameter for setting linear
     * gradient.
     */
    protected COSArray buildType2Functions(Color[] colors, COSArray domain, COSArray encode) {
        Color prevColor = colors[0];

        COSArray functions = new COSArray();
        for (int i = 1; i < colors.length; i++) {

            Color color = colors[i];
            float alpha = prevColor.getAlpha() / 255f;
            // calculating transparency if set
            float r = prevColor.getRed() * alpha + (1 - alpha) * 255;
            float g = prevColor.getGreen() * alpha + (1 - alpha) * 255;
            float b = prevColor.getBlue() * alpha + (1 - alpha) * 255;
            float[] component = new float[]{r / 255f, g / 255f, b / 255f};
            PDColor prevPdColor = new PDColor(component, PDDeviceRGB.INSTANCE);
            alpha = color.getAlpha() / 255f;
            r = color.getRed() * alpha + (1 - alpha) * 255;
            g = color.getGreen() * alpha + (1 - alpha) * 255;
            b = color.getBlue() * alpha + (1 - alpha) * 255;
            float[] component1 = new float[]{r / 255f, g / 255f, b / 255f};

            PDColor pdColor = new PDColor(component1, PDDeviceRGB.INSTANCE);
            COSArray c0 = new COSArray();
            COSArray c1 = new COSArray();
            for (float component2 : prevPdColor.getComponents())
                c0.add(new COSFloat(component2));
            for (float component3 : pdColor.getComponents())
                c1.add(new COSFloat(component3));

            COSDictionary type2Function = new COSDictionary();
            type2Function.setInt(COSName.FUNCTION_TYPE, 2);
            type2Function.setItem(COSName.C0, c0);
            type2Function.setItem(COSName.C1, c1);
            type2Function.setInt(COSName.N, 1);
            type2Function.setItem(COSName.DOMAIN, domain);
            functions.add(type2Function);

            encode.add(new COSFloat(0));
            encode.add(new COSFloat(1));
            prevColor = color;
        }
        return functions;
    }
}
