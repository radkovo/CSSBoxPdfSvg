/**
 * ImageRenderer.java Copyright (c) 2005-2014 Radek Burget
 *
 * CSSBox is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * CSSBox is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created on 5.2.2009, 12:00:02 by burgetr
 */

package org.fit.cssbox.svgpdf.demo;

import cz.vutbr.web.css.MediaSpec;
import org.fit.cssbox.awt.GraphicsEngine;
import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserConfig;
import org.fit.cssbox.layout.Dimension;
import org.fit.cssbox.layout.Rectangle;
import org.fit.cssbox.layout.Viewport;
import org.fit.cssbox.svgpdf.render.SVGRenderer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a rendering interface for obtaining the document image
 * form an URL.
 *
 * @author burgetr
 */
public class ImageRendererSvg
{

    public enum Type
    {
        PNG, SVG
    }

    private String mediaType = "screen";
    private Dimension windowSize;
    private boolean cropWindow = false;
    private boolean loadImages = true;
    private boolean loadBackgroundImages = true;

    public ImageRendererSvg()
    {
        windowSize = new Dimension(1920, 1080);
    }

    public void setMediaType(String media)
    {
        mediaType = new String(media);
    }

    public void setWindowSize(Dimension size, boolean crop)
    {
        windowSize = new Dimension(size);
        cropWindow = crop;
    }

    public void setLoadImages(boolean content, boolean background)
    {
        loadImages = content;
        loadBackgroundImages = background;
    }

    /**
     * Renders the URL and prints the result to the specified output stream in
     * the specified format.
     *
     * @param urlstring
     *            the source URL
     * @param out
     *            output stream
     * @return true in case of success, false otherwise
     * @throws SAXException
     */
    public boolean renderURL(String urlstring, OutputStream out) throws IOException, SAXException
    {
        if (!urlstring.startsWith("http:") && !urlstring.startsWith("https:") && !urlstring.startsWith("ftp:")
                && !urlstring.startsWith("file:"))
        {
            urlstring = "http://" + urlstring;
        }

        //Open the network connection 
        DocumentSource docSource = new DefaultDocumentSource(urlstring);

        //Parse the input document
        DOMSource parser = new DefaultDOMSource(docSource);
        Document doc = parser.parse();

        //create the media specification
        MediaSpec media = new MediaSpec(mediaType);
        media.setDimensions(windowSize.width, windowSize.height);
        media.setDeviceDimensions(windowSize.width, windowSize.height);

        //Create the CSS analyzer
        DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
        da.setMediaSpec(media);
        da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
        da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
        da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
        da.addStyleSheet(null, CSSNorm.formsStyleSheet(), DOMAnalyzer.Origin.AGENT); //render form fields using css
        da.getStyleSheets(); //load the author style sheets

        GraphicsEngine engine = new GraphicsEngine(da.getRoot(), da, docSource.getURL());
        engine.setUseFractionalMetrics(true); //fractional metrics are useful for vector output 
        engine.setAutoMediaUpdate(false); //we have a correct media specification, do not update
        engine.getConfig().setClipViewport(cropWindow);
        engine.getConfig().setLoadImages(loadImages);
        engine.getConfig().setLoadBackgroundImages(loadBackgroundImages);
        defineLogicalFonts(engine.getConfig());
        
        //create the layout but do not render
        engine.createLayout(windowSize, new Rectangle(windowSize), false);

        Writer w = new OutputStreamWriter(out, "utf-8");
        writeSVG(engine.getViewport(), w);
        w.close();

        docSource.close();

        return true;
    }

    /**
     * Sets some common fonts as the defaults for generic font families.
     */
    protected void defineLogicalFonts(BrowserConfig config)
    {
        config.setLogicalFont(BrowserConfig.SERIF, Arrays.asList("Times", "Times New Roman"));
        config.setLogicalFont(BrowserConfig.SANS_SERIF, Arrays.asList("Arial", "Helvetica"));
        config.setLogicalFont(BrowserConfig.MONOSPACE, Arrays.asList("Courier New", "Courier"));
    }

    /**
     * Renders the viewport using an SVGRenderer to the given output writer.
     *
     * @param vp
     * @param out
     * @throws IOException
     */
    protected void writeSVG(Viewport vp, Writer out)
    {

        //obtain the viewport bounds depending on whether we are clipping to viewport size or using the whole page
        float w = vp.getClippedContentBounds().width;
        float h = vp.getClippedContentBounds().height;

        SVGRenderer render = new SVGRenderer(w, h, out);
        vp.draw(render);
        render.close();
    }

    //=================================================================================
    
    public static void main(String[] args)
    {
        //System.out.println(args[0]);
        //System.out.println("VectorRender");
        //        long start = System.nanoTime();

        if (args.length != 2)
        {
            System.err.println("Usage: ImageRenderer <url> <output_file>");
            System.err.println();
            System.err.println("Renders a HTML document at the specified URL and stores the document image");
            System.err.println("to the specified SVG output file.");
            System.exit(0);
        }

        FileOutputStream os;
        try
        {
            os = new FileOutputStream(args[1]);

            ImageRendererSvg r = new ImageRendererSvg();
            r.renderURL(args[0], os);
            os.close();
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(ImageRendererSvg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ImageRendererSvg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex)
        {
            Logger.getLogger(ImageRendererSvg.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.err.println("Done.");
        //        long end = System.nanoTime();
        // System.out.println((end - start) / 1000000);

    }

}
