package org.fit.cssbox.svgpdf.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.csskit.Color;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.fit.cssbox.awt.BackgroundBitmap;
import org.fit.cssbox.awt.BitmapImage;
import org.fit.cssbox.css.BackgroundDecoder;
import org.fit.cssbox.layout.*;
import org.fit.cssbox.misc.Base64Coder;
import org.fit.cssbox.render.*;
import org.fit.cssbox.render.RadialGradient;
import org.fit.cssbox.svgpdf.layout.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A box rendered that produces an SVG DOM model as the output.
 *
 * @author Martin Safar
 * @author burgetr
 * @author Tomas Chocholaty
 */
public class SVGRenderer extends StructuredRenderer {
    private static final float MIN = 0.0001f; //minimal coordinate difference to take into account

    private PrintWriter out;
    private int idcounter;

    private Document doc;
    private final String svgNS = "http://www.w3.org/2000/svg";
    private final String xlinkNS = "http://www.w3.org/1999/xlink";

    /**
     * Generated SVG root
     */
    private Element svgRoot;
    private Stack<Element> elemStack;
    private boolean streamResult;
    private Element backgroundStore;
    private Element bgWrap;
    private Element gBorder;
    private boolean bgUsed;
    private boolean bordersUsed;

    /**
     * @param rootWidth  - width of website
     * @param rootHeight - height of website
     * @param out        - output
     */
    public SVGRenderer(float rootWidth, float rootHeight, Writer out) {
        super(rootWidth, rootHeight, "SVG");
        elemStack = new Stack<Element>();
        doc = createDocument();
        idcounter = 1;
        streamResult = true;
        this.out = new PrintWriter(out);
        writeHeader();
    }

    //====================================================================================================

    public Element getCurrentElem() {
        return elemStack.peek();
    }

    public Document getDocument() {
        return doc;
    }

    //====================================================================================================

    /**
     * Method for create SVG output document
     */
    private Document createDocument() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.getDOMImplementation().createDocument(svgNS, "svg", null);
            return doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method for init SVG output document
     */
    private void writeHeader() {
        svgRoot = doc.getDocumentElement();
        elemStack.push(svgRoot);
        svgRoot.setAttribute("width", Float.toString(getRootWidth()) + "px");
        svgRoot.setAttribute("height", Float.toString(getRootHeight()) + "px");
        svgRoot.setAttribute("viewBox", "0 0 " + getRootWidth() + " " + getRootHeight());
        svgRoot.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xlink", xlinkNS);
        svgRoot.appendChild(doc.createComment(" Rendered by CSSBox http://cssbox.sourceforge.net "));
    }


    @Override
    public void startElementContents(ElementBox elem) {
        final Element g = createElement("g");
        boolean useGroup = false;

        //transformace napriklad pro replacement content
        TransformSVG t = new TransformSVG();
        String tm = t.insertTransformSVG(elem);
        if (!tm.equals("")) {
            g.setAttribute("transform", tm);
            useGroup = true;
        }

        String opacity = elem.getStylePropertyValue("opacity");
        if (!opacity.equals("")) {
            g.setAttribute("opacity", opacity);
            useGroup = true;
        }

        if (useGroup)
            elemStack.push(g);
    }

    @Override
    public void finishElementContents(ElementBox elem) {
        if (elemStack.peek() != svgRoot) {
            Element buf = elemStack.pop();
            getCurrentElem().appendChild(buf);
        }
    }

    @Override
    public void renderElementBackground(ElementBox elem) {
        bgUsed = false;
        backgroundStore = createElement("g");
        backgroundStore.setAttribute("id", "bgstore" + (idcounter++));
        this.bgWrap = createElement("g"); //background wrapper
        super.renderElementBackground(elem);

        if (bgUsed)
            backgroundStore.appendChild(bgWrap);

        if (bordersUsed)
            backgroundStore.appendChild(gBorder);

        // append the whole backgound group when something was used
        if (bgUsed) {
            // if transform was used, transform the backgound as well
            final TransformSVG t = new TransformSVG();
            final String tm = t.insertTransformSVG(elem);
            if (!tm.isEmpty()) {
                backgroundStore.setAttribute("transform", tm);
            }

            // if opacity is applied to the element, make the background opaque as well
            final String opacity = elem.getStylePropertyValue("opacity");
            if (!opacity.isEmpty()) {
                backgroundStore.setAttribute("opacity", opacity);
            }

            getCurrentElem().appendChild(backgroundStore);
        }
    }


    @Override
    public void renderTextContent(TextBox text) {
        TextClassSVG textClass = new TextClassSVG(text, text.getVisualContext().getFontInfo().getFamily());
        Rectangle b = text.getAbsoluteBounds();
        String textStyle = textClass.createText(text);
        if (text.getWordSpacing() == null && text.getExtraWidth() == 0) {
            addText(getCurrentElem(), b.x, b.y + text.getBaselineOffset(), b.width, b.height, textStyle, text.getText());
        } else {
            addTextByWords(getCurrentElem(), b.x, b.y + text.getBaselineOffset(), b.width, b.height, textStyle, text);
        }
    }

    /**
     * Method for render text
     *
     * @param parent - parent element of text
     * @param x      - x coordinate
     * @param y      - y coordinate
     * @param width  - width of text element
     * @param height - height of text element
     * @param style  - style of text
     * @param text   - text string
     */
    private void addText(Element parent, float x, float y, float width, float height, String style, String text) {
        Element txt = doc.createElementNS(svgNS, "text");
        txt.setAttributeNS(XMLConstants.XML_NS_URI, "space", "preserve");
        txt.setAttribute("x", Float.toString(x));
        txt.setAttribute("y", Float.toString(y));
        txt.setAttribute("width", Float.toString(width));
        txt.setAttribute("height", Float.toString(height));
        txt.setAttribute("style", style);
        txt.setTextContent(text);
        parent.appendChild(txt);
    }


    /**
     * Method for render text, when words space are not default
     *
     * @param parent - parent element of text
     * @param x      - x coordinate
     * @param y      - y coordinate
     * @param width  - width of text element
     * @param height - height of text element
     * @param style  - style of text
     * @param text   - text string
     */
    private void addTextByWords(Element parent, float x, float y, float width, float height, String style, TextBox text) {
        final String[] words = text.getText().split(" ");
        if (words.length > 0) {
            Element g = doc.createElementNS(svgNS, "g");
            final float[][] offsets = text.getWordOffsets(words);
            for (int i = 0; i < words.length; i++)
                addText(g, x + offsets[i][0], y, offsets[i][1], height, style, words[i]);
            parent.appendChild(g);
        } else
            addText(parent, x, y, width, height, style, text.getText());
    }


    @Override
    public void close() {
        writeFooter();
    }

    /**
     * Method for render output SVG file
     */
    private void writeFooter() {
        if (streamResult) {
            try {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer;
                transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
                StreamResult result = new StreamResult(out);

                transformer.transform(source, result);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(SVGRenderer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(SVGRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }


    @Override
    protected void renderBorder(ElementBox elem, Rectangle bb) {
        //Render Border
        gBorder = createElement("g");
        gBorder.setAttribute("id", "borders-" + (idcounter++));
        elemStack.push(gBorder);
        final Border border = new BorderSVG(elem.getBorder(), bb, elem, this);
        try {
            bordersUsed = border.writeBorder(elem, border);
        } catch (IOException e) {
            e.printStackTrace();
        }
        elemStack.pop();
        if (bordersUsed || clippingUsed(elem)) {
            final String clipId = "cssbox-clip-" + (idcounter++);
            final Element clipPath = createElement("clipPath");
            clipPath.setAttribute("id", clipId);
            final Element q = border.getClipPathElementForBorder(border);
            clipPath.appendChild(q);
            bgWrap.setAttribute("clip-path", "url(#" + clipId + ")");
            bgUsed = true;
            backgroundStore.appendChild(clipPath);
        }
    }


    @Override
    protected void renderColorBg(ElementBox elem, Rectangle bb, BackgroundDecoder bg) {
        final String style = "stroke:none;fill-opacity:1;fill:" + colorString(bg.getBgcolor());
        bgWrap.appendChild(createRect(bb.x, bb.y, bb.width, bb.height, style));
        bgUsed = true;
    }


    @Override
    protected void renderImageBg(ElementBox elem, Rectangle bb, BackgroundBitmap bitmap) {
        bgWrap.appendChild(createImage(bb.x, bb.y, bb.width, bb.height, bitmap.getBufferedImage(), null));
        bgUsed = true;
    }

    /**
     * color conversion method.
     *
     * @param color - color as instance of class Color
     * @return color as String
     */
    public String colorString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }


    /**
     * Draw path to output
     *
     * @param dPath       - path for render
     * @param fill        - color of path
     * @param stroke      - border
     * @param strokeWidth - border thickness
     * @return element of path
     */
    public Element createPath(String dPath, String fill, String stroke, float strokeWidth) {
        Element e = createElement("path");
        e.setAttribute("d", dPath);
        e.setAttribute("stroke", stroke);
        e.setAttribute("stroke-width", Float.toString(strokeWidth));
        e.setAttribute("fill", fill);
        return e;
    }


    /**
     * Draw rectangle
     *
     * @param x      - x coordinate
     * @param y      - y coordinate
     * @param width  - with of rectangle
     * @param height - height of rectangle
     * @param style  - style of rectangle
     * @return element of rectangle
     */
    public Element createRect(float x, float y, float width, float height, String style) {
        Element e = createElement("rect");
        e.setAttribute("x", Float.toString(x));
        e.setAttribute("y", Float.toString(y));
        e.setAttribute("width", Float.toString(width));
        e.setAttribute("height", Float.toString(height));
        e.setAttribute("style", style);
        return e;
    }


    /**
     * Load and draw image
     *
     * @param x      - x coordinate
     * @param y      - y coordinate
     * @param width  - width of image
     * @param height - height of image
     * @param img    - image
     * @return element of image
     */
    public Element createImage(float x, float y, float width, float height, BufferedImage img, String style) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        char[] data = Base64Coder.encode(os.toByteArray());
        String imgdata = "data:image/png;base64," + new String(data);

        if (style == null) {
            return createImage(x, y, width, height, imgdata);

        } else {
            return createImage(x, y, width, height, imgdata, style);
        }
    }

    /**
     * Draw loaded image
     *
     * @param x       - x coordinate
     * @param y       - y coordinate
     * @param width   - width of image
     * @param height  - height of image
     * @param imgData - data of image
     * @return element of image
     */
    public Element createImage(float x, float y, float width, float height, String imgData) {
        Element image = createElement("image");
        image.setAttribute("x", Float.toString(x));
        image.setAttribute("y", Float.toString(y));
        image.setAttribute("width", Float.toString(width));
        image.setAttribute("height", Float.toString(height));
        image.setAttributeNS(xlinkNS, "xlink:href", imgData);
        return image;
    }

    /**
     * Draw loaded image
     *
     * @param x       - x coordinate
     * @param y       - y coordinate
     * @param width   - width of image
     * @param height  - height of image
     * @param imgData - data of image
     * @param style   - style of image
     * @return element of image
     */
    public Element createImage(float x, float y, float width, float height, String imgData, String style) {
        Element image = createElement("image");
        image.setAttribute("x", Float.toString(x));
        image.setAttribute("y", Float.toString(y));
        image.setAttribute("width", Float.toString(width));
        image.setAttribute("height", Float.toString(height));
        image.setAttribute("style", style);
        image.setAttributeNS(xlinkNS, "xlink:href", imgData);
        return image;
    }

    /**
     * Create new element for SVG DOM tree
     *
     * @param elementName - name of created element
     */
    public Element createElement(String elementName) {
        return doc.createElementNS(svgNS, elementName);
    }

    @Override
    protected void insertReplacedImage(ReplacedBox box, ContentImage img) {
        Rectangle cb = ((Box) box).getAbsoluteContentBounds();

        Filter svgFilter = new Filter(null, 0, 0, 1.0f, 1.0f);
        String style = null;
        CSSProperty.Filter filter = ((ElementBox) box).getStyle().getProperty("filter");
        BufferedImage img2 = ((BitmapImage) img).getBufferedImage();
        if (filter == CSSProperty.Filter.list_values) {
            svgFilter = svgFilter.createFilter((ElementBox) box);
            img2 = svgFilter.filterImg(img2);
        }

        Element image = createImage(cb.x, cb.y, cb.width, cb.height, img2, style);
        getCurrentElem().appendChild(image);
    }


    @Override
    protected void insertReplacedText(ReplacedBox box) {
        final Rectangle cb = ((Box) box).getClippedBounds();
        final String clip = "cssbox-clip-" + idcounter;

        final Element clipPath = doc.createElementNS(svgNS, "clipPath");
        clipPath.setAttribute("id", clip);
        clipPath.appendChild(createRect(cb.x, cb.y, cb.width, cb.height, ""));
        getCurrentElem().appendChild(clipPath);

        final Element g = doc.createElementNS(svgNS, "g");
        g.setAttribute("id", "cssbox-obj-" + (idcounter++));
        g.setAttribute("clip-path", "url(#" + clip + ")");
        getCurrentElem().appendChild(g);
    }


    @Override
    protected void writeCircleBullet(ListItemBox lb, float x, float y, float r, Color color) {
        String style = "fill:none;fill-opacity:1;stroke:" + colorString(color)
                + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
        //out.println("<circle style=\"" + style + "\" cx=\"" + (x + r / 2) + "\" cy=\"" + (y + r / 2) + "\" r=\"" + (r / 2) + "\" />");
        Element circle = createElement("circle");
        circle.setAttribute("cx", Float.toString(x + r / 2));
        circle.setAttribute("cy", Float.toString(y + r / 2));
        circle.setAttribute("r", Float.toString(r / 2));
        circle.setAttribute("style", style);
        getCurrentElem().appendChild(circle);
    }

    @Override
    protected void writeSquareBullet(ListItemBox lb, float x, float y, float r, Color color) {
        String tclr = colorString(color);
        String style = "fill:" + tclr + ";fill-opacity:1;stroke:" + tclr
                + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
        //out.println("<rect style=\"" + style + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + r + "\" height=\"" + r + "\" />");
        Element rect = createElement("rect");
        rect.setAttribute("x", Float.toString(x));
        rect.setAttribute("y", Float.toString(y));
        rect.setAttribute("width", Float.toString(r));
        rect.setAttribute("height", Float.toString(r));
        rect.setAttribute("style", style);
        getCurrentElem().appendChild(rect);
    }

    @Override
    protected void writeDiscBullet(ListItemBox lb, float x, float y, float r, Color color) {
        String tclr = colorString(color);
        String style = "fill:" + tclr + ";fill-opacity:1;stroke:" + tclr
                + ";stroke-width:1;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1";
        //out.println("<circle style=\"" + style + "\" cx=\"" + (x + r / 2) + "\" cy=\"" + (y + r / 2) + "\" r=\"" + (r / 2) + "\" />");
        Element disc = createElement("circle");
        disc.setAttribute("cx", Float.toString(x + r / 2));
        disc.setAttribute("cy", Float.toString(y + r / 2));
        disc.setAttribute("r", Float.toString(r / 2));
        disc.setAttribute("style", style);
        getCurrentElem().appendChild(disc);
    }


    @Override
    public void writeOtherBullet(ListItemBox lb, float x, float y) {
        float baseline = lb.getFirstInlineBoxBaseline();
        VisualContext ctx = lb.getVisualContext();
        if (baseline == -1) baseline = ctx.getBaselineOffset(); //use the font baseline
        String style = textStyle(ctx) + ";text-align:end;text-anchor:end";
        addText(getCurrentElem(), lb.getAbsoluteContentX() - 0.5f * ctx.getEm(), lb.getAbsoluteContentY() + baseline, lb.getWidth(), lb.getHeight(), style, lb.getMarkerText());
    }

    /**
     * Method for set text style for PDF
     *
     * @param ctx - data of style of text
     * @return String which represent style of text for PDFBox
     */
    private String textStyle(VisualContext ctx) {
        String style = "font-size:" + ctx.getFontSize() + "pt;" + "font-weight:"
                + (ctx.getFontInfo().isBold() ? "bold" : "normal") + ";" + "font-style:"
                + (ctx.getFontInfo().isItalic() ? "italic" : "normal") + ";" + "font-family:" + ctx.getFontInfo().getFamily()
                + ";" + "fill:" + colorString(ctx.getColor()) + ";" + "stroke:none";
        if (ctx.getLetterSpacing() > 0.0001)
            style += ";letter-spacing:" + ctx.getLetterSpacing() + "px";
        return style;
    }


    @Override
    protected void createImageBullet(ListItemBox lb, float ix, float iy, float iw, float ih, ContentImage img) {
        Element image = createImage(ix, iy, iw, ih, ((BitmapImage) img).getBufferedImage(), null);
        getCurrentElem().appendChild(image);
    }


    @Override
    protected void addRadialGradient(BackgroundImageGradient bgimage, float absx, float absy, ElementBox box) {
        RadialGradient grad = (RadialGradient) bgimage.getGradient();
        Rectangle bgsize = bgimage.getComputedPosition();
        bgsize.x = absx;
        bgsize.y = absy;

        final String url = "cssbox-gradient-" + (idcounter++);
        final Element defs = createElement("defs");
        final Element image = createElement("radialGradient");
        image.setAttribute("gradientUnits", "userSpaceOnUse");
        if (grad.isRepeating())
            image.setAttribute("spreadMethod", "repeat");
        float scaleY = 1.0f;
        if (!grad.isCircle()) {
            // scale Y to achieve desired radius ratio
            if (grad.getRx() > 0)
                scaleY = grad.getRy() / grad.getRx();
            image.setAttribute("gradientTransform", "scale(1," + scaleY + ") ");
        }
        image.setAttribute("r", String.valueOf(grad.getEfficientRx()));
        image.setAttribute("cx", String.valueOf(grad.getCx() + bgsize.x));
        image.setAttribute("cy", String.valueOf((grad.getCy() + bgsize.y) / scaleY));
        image.setAttribute("id", url);
        for (int i = 0; i < grad.getStops().size(); i++) {
            Element stop = createElement("stop");
            Color cc = grad.getStops().get(i).getColor();
            stop.setAttribute("offset", "" + grad.getStops().get(i).getPercentage() + "%");
            stop.setAttribute("style",
                    "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() +
                            ");stop-opacity:" + (cc.getAlpha() / 255.0f));
            image.appendChild(stop);
        }

        defs.appendChild(image);
        bgWrap.appendChild(defs);

        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";

        // generate the background element
        bgWrap.appendChild(createRect(bgsize.x, bgsize.y, bgsize.width, bgsize.height, style));
        bgUsed = true;
    }

    @Override
    protected void addLinearGradient(BackgroundImageGradient bgimage, float absx, float absy, ElementBox elem) {
        LinearGradient grad = (LinearGradient) bgimage.getGradient();
        Rectangle bgsize = bgimage.getComputedPosition();
        bgsize.x = absx;
        bgsize.y = absy;
        // generate code
        String url = "cssbox-gradient-" + idcounter;
        idcounter++;
        // generate svg gradient incl. stops
        Element defs = createElement("defs");
        Element image;
        image = createElement("linearGradient");
        image.setAttribute("gradientUnits", "userSpaceOnUse");
        if (grad.isRepeating())
            image.setAttribute("spreadMethod", "repeat");
        image.setAttribute("x1", String.valueOf(grad.getX1() + bgsize.x));
        image.setAttribute("y1", String.valueOf(grad.getY1() + bgsize.y));
        image.setAttribute("x2", String.valueOf(grad.getEfficientX2() + bgsize.x));
        image.setAttribute("y2", String.valueOf(grad.getEfficientY2() + bgsize.y));
        image.setAttribute("id", url);
        for (int i = 0; i < grad.getStops().size(); i++) {
            Element stop = createElement("stop");
            Color cc = grad.getStops().get(i).getColor();
            stop.setAttribute("offset", "" + grad.getStops().get(i).getPercentage() + "%");
            stop.setAttribute("style",
                    "stop-color:rgb(" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue() +
                            ");stop-opacity:" + (cc.getAlpha() / 255.0f));
            image.appendChild(stop);
        }

        defs.appendChild(image);
        bgWrap.appendChild(defs);

        String style = "stroke:none;fill-opacity:1;fill:url(#" + url + ");";

        // generate the element with the gradient background
        bgWrap.appendChild(createRect(bgsize.x, bgsize.y, bgsize.width, bgsize.height, style));
        bgUsed = true;
    }
}
