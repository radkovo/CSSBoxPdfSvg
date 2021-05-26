package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.csskit.Color;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.render.BackgroundImageGradient;
import org.fit.cssbox.render.LinearGradient;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;

/**
 * A linear gradient for PDF version
 *
 * @author Nguyen Hoang Duong
 * @author Tomas Chocholaty
 */
public class LinearGradientPDF extends GradientPDF {


    public LinearGradientPDF() {

    }

    /**
     * Method for create linear gradient
     *
     * @param bgimage - bacground image gradient
     * @param absx    - x coordinate
     * @param absy    - y coordinate
     * @param width   - width of gradient
     * @param height  - height of gradient
     * @param resCoef - coefficient for size
     * @return gradient as PDShadingType3
     */
    public PDShadingType3 createLinearGrad(BackgroundImageGradient bgimage, float absx, float absy, float width, float height, float resCoef) {
        LinearGradient grad = (LinearGradient) bgimage.getGradient();

        Color[] trueColors = new Color[grad.getStops().size()];
        float[] trueColorLen = new float[grad.getStops().size()];

        for (int i = 0; i < grad.getStops().size(); i++) {
            Color cc = grad.getStops().get(i).getColor();
            trueColors[i] = new Color(cc.getRed(), cc.getGreen(), cc.getBlue());
            trueColorLen[i] = grad.getStops().get(i).getPercentage() / 100;
        }


        float[] components = new float[]{trueColors[0].getRed() / 255f, trueColors[0].getGreen() / 255f,
                trueColors[0].getBlue() / 255f};

        PDColor fcolor = new PDColor(components, PDDeviceRGB.INSTANCE);

        Rectangle bgsize = new Rectangle(absx, absy, width, height);

        PDShadingType3 shading = new PDShadingType3(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(fcolor.getColorSpace());
        COSArray coords = new COSArray();

        coords.add(new COSFloat((float) grad.getX1() * resCoef + bgsize.x));
        coords.add(new COSFloat((float) grad.getEfficientY2() * resCoef + bgsize.y));
        coords.add(new COSFloat((float) grad.getEfficientX2() * resCoef + bgsize.x));
        coords.add(new COSFloat((float) grad.getY1() * resCoef + bgsize.y));

        shading.setCoords(coords);

        PDFunctionType3 type3 = buildType3Function(trueColors, trueColorLen);

        COSArray extend = new COSArray();
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        shading.setFunction(type3);
        shading.setExtend(extend);
        return shading;
    }
}
