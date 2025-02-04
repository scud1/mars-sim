/**
 * Mars Simulation Project
 * SVGIcon.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */

/*
 * SVGIcon.java
 *
 * A Swing Icon that draws an SVG image.
 *
 * Cameron McCormack <cam (at) mcc.id.au>
 *
 * Permission is hereby granted to use, copy, modify and distribte this
 * code for any purpose, without fee.
 *
 * Initial version: April 21, 2005
 */
package org.mars_sim.msp.ui.swing.tool.svg;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
//import org.jdom2.Document;
//import org.w3c.dom.Document;
//import org.w3c.dom.Document;

/**
 * A Swing Icon that draws an SVG image.
 *
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 */
public class SVGIcon extends UserAgentAdapter implements Icon {

    /**
     * The BufferedImage generated from the SVG document.
     */
    protected BufferedImage bufferedImage;

    /**
     * The width of the rendered image.
     */
    protected int width;
    
    /**
     * The height of the rendered image.
     */
    protected int height;

    /**
     * Creates a new SVGIcon object.
     * 
     * @param uri The URI to read the SVG document from.
     */
    public SVGIcon(String uri) throws TranscoderException {
        this(uri, 0, 0);
    }

    /**
     * Creates a new SVGIcon object.
     * 
     * @param uri The URI to read the SVG document from.
     * @param w The width of the icon.
     * @param h The height of the icon.
     */
    public SVGIcon(String uri, int w, int h) throws TranscoderException {
       //InputStream source = getClass().getClassLoader().getResourceAsStream(uri);

        generateBufferedImage(new TranscoderInput(uri), w, h);
    }

    /**
     * Generates the BufferedImage.
     * 
     * @param in
     * @param w
     * @param h
     * @throws TranscoderException
     */
    protected void generateBufferedImage(TranscoderInput in, int w, int h)
            throws TranscoderException {
        BufferedImageTranscoder t = new BufferedImageTranscoder();
        if (w != 0 && h != 0) {
            t.setDimensions(w, h);
        }
        t.transcode(in, null);
        bufferedImage = t.getBufferedImage();
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }

    /**
     * A transcoder that generates a BufferedImage.
     */
    protected class BufferedImageTranscoder extends ImageTranscoder {

        /**
         * The BufferedImage generated from the SVG document.
         */
        protected BufferedImage bufferedImage;

        /**
         * Creates a new ARGB image with the specified dimension.
         * 
         * @param width the image width in pixels
         * @param height the image height in pixels
         */
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        /**
         * Writes the specified image to the specified output.
         * 
         * @param img the image to write
         * @param output the output where to store the image
         * @param TranscoderException if an error occured while storing the image
         */
        public void writeImage(BufferedImage img, TranscoderOutput output)
                throws TranscoderException {
            bufferedImage = img;
        }

        /**
         * Returns the BufferedImage generated from the SVG document.
         */
        public BufferedImage getBufferedImage() {
            return bufferedImage;
        }

        /**
         * Sets the dimensions to be used for the image.
         * 
         * @param w
         * @param h
         */
        public void setDimensions(int w, int h) {
            hints.put(KEY_WIDTH, (float) w);
            hints.put(KEY_HEIGHT, (float) h);
        }
    }

    /**
     * Gets the underlying backing image.
     * 
     * @return
     */
    public Image getImage() {
        return bufferedImage;
    }

    // Icon //////////////////////////////////////////////////////////////////

    /**
     * Returns the icon's width.
     */
    public int getIconWidth() {
        return width;
    }

    /**
     * Returns the icon's height.
     */
    public int getIconHeight() {
        return height;
    }

    /**
     * Draws the icon at the specified location.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(bufferedImage, x, y, null);
    }

    // UserAgent /////////////////////////////////////////////////////////////

    /**
     * Returns the default size of this user agent.
     */
    public Dimension2D getViewportSize() {
        return new Dimension(width, height);
    }
}
