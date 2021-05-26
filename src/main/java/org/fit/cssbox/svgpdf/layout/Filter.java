
package org.fit.cssbox.svgpdf.layout;

import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermList;
import org.fit.cssbox.layout.ElementBox;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * This class is used for applicating graphics effects to the pictures.
 *
 * @author Nguyen Hoang Duong
 * @author Tomas Chocholaty
 */
public class Filter {
    // filter variables
    public float invert;
    public float grayscale;
    public float bright;
    public float opacity;
    public String[] filterType;

    public Filter(String[] filterType, float invert, float grayscale, float opacity, float brightness) {
        this.invert = invert;
        this.grayscale = grayscale;
        this.bright = brightness;
        this.opacity = opacity;
        this.filterType = filterType;
    }

    /**
     * This function represents CSS3 filter function brightness() and opacity()
     *
     * @param img - the original image to be filtered
     * @return the filtered image
     */
    private BufferedImage setBrightOpacImg(BufferedImage img) {
        float[] factors;

        factors = new float[]{bright, bright, bright, opacity};

        float[] offsets = new float[]{0, 0, 0, 0};
        RescaleOp op = new RescaleOp(factors, offsets, null);
        BufferedImage bi = new BufferedImage(
                img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img = op.filter(bi, bi);
        return img;
    }

    /**
     * This function represents CSS3 filter function invert()
     *
     * @param img - the original image to be filtered
     * @return the filtered image
     */
    private BufferedImage invertImg(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);
                // get values of each component of the RGBA model
                int a = (rgba >> 24) & 0xff;
                int red = (rgba >> 16) & 0xff;
                int green = (rgba >> 8) & 0xff;
                int blue = rgba & 0xff;

                if (invert < 0.5) { // because of the white picture when invert is 0
                    red = (int) ((255 * invert) - red);
                    green = (int) ((255 * invert) - green);
                    blue = (int) ((255 * invert) - blue);

                } else if (invert == 0.5) { // when is 0.5 then the picture is covered with gray colour
                    red = 128;
                    green = 128;
                    blue = 128;
                } else {
                    red = (int) (255 - (red * invert));
                    green = (int) (255 - (green * invert));
                    blue = (int) (255 - (blue * invert));
                }
                // component must have the value in range 0-255
                if (red < 0) red *= -1;
                if (green < 0) green *= -1;
                if (blue < 0) blue *= -1;
                if (a < 0) a *= -1;

                //col = new Color(red, green, blue);
                rgba = (a << 24) | (red << 16) | (green << 8) | blue;
                img.setRGB(x, y, rgba); // set pixel with new colour to the image

            }
        }
        return img;
    }

    /**
     * This function represents CSS3 filter function grayscale(). However, it is
     * working only with grayscale parameter 100%
     *
     * @param img - the original image to be filtered
     * @return the filtered image
     */
    private BufferedImage grayScaleImg(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        if (grayscale > 1) grayscale = 1;
        if (grayscale <= 0) return img;
        grayscale = 1 - grayscale + 1;
        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width; j++) {

                Color c = new Color(img.getRGB(j, i));
                // calculating gray scale with luminosity method
                int red = (int) ((c.getRed() * 0.299) * grayscale);
                int green = (int) ((c.getGreen() * 0.587) * grayscale);
                int blue = (int) ((c.getBlue() * 0.114) * grayscale);
                int rgb = red + green + blue;
                if (rgb < 0) rgb = 0;
                if (rgb > 255) rgb = 255;
                Color newColor = new Color(rgb, rgb, rgb);

                img.setRGB(j, i, newColor.getRGB());
            }
        }
        return img;
    }

    /**
     * This function invokes other methods from this class depending on the
     * filter name.
     *
     * @param img - the original image to be filtered
     * @return the filtered image
     */
    public BufferedImage filterImg(BufferedImage img) {
        if (filterType != null) {
            for (int n = 0; n < filterType.length; n++) {
                if (filterType[n] == "invert")
                    img = invertImg(img);
                else if ((filterType[n] == "bright") || (filterType[n] == "opacity"))
                    img = setBrightOpacImg(img);
                else if ((filterType[n] == "grayscale"))
                    img = grayScaleImg(img);
            }
        }
        return img;
    }


    /**
     * Creates a filter structure based on the element style.
     *
     * @param elem the element
     * @return a filter structure
     */
    public Filter createFilter(ElementBox elem) {
        TermList values = elem.getStyle().getValue(TermList.class, "filter");
        int n = 0;
        String[] filterType = new String[10];
        float invert = 0;
        float grayscale = 0;
        float opacity = 1;
        float brightness = 1;
        for (Term<?> term : values) {
            if (term instanceof TermFunction.Invert) {
                filterType[n] = "invert";
                invert = ((TermFunction.Invert) term).getAmount();
            } else if (term instanceof TermFunction.Brightness) {
                filterType[n] = "bright";
                brightness = ((TermFunction.Brightness) term).getAmount();
            } else if (term instanceof TermFunction.Opacity) {
                filterType[n] = "opacity";
                opacity = ((TermFunction.Opacity) term).getAmount();
            } else if (term instanceof TermFunction.Grayscale) {
                filterType[n] = "grayscale";
                grayscale = 1;
            } else
                filterType[n] = "";

            n++;
        }
        return new Filter(filterType, invert, grayscale, opacity, brightness);
    }

}
