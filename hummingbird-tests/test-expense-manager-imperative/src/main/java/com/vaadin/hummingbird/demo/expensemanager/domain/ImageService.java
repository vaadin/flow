package com.vaadin.hummingbird.demo.expensemanager.domain;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.servlet.http.Part;

public class ImageService {
    
    public static final ImageService INSTANCE = new ImageService();
    
    public String partToData(Part filePart) throws IOException {
        BufferedImage imBuff = ImageIO.read(filePart.getInputStream());
        imBuff = scaleImage(imBuff, 400, 400, true, true);
        return imageToData(imBuff);
    }

    private BufferedImage scaleImage(BufferedImage img, int targetWidth, int targetHeight,
            boolean trimImage, boolean progressiveBilinear) {

        if (trimImage) {
            double rel = (double)img.getWidth() / (double)img.getHeight();
            if (img.getHeight() > img.getWidth()) {
                targetWidth = Math.min(targetWidth, img.getWidth());
                targetHeight = (int)(targetWidth / rel);
            } else {
                targetHeight = Math.min(targetHeight, img.getHeight());
                targetWidth = (int)(targetHeight * rel);
            }
        }
    
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        int w, h;
        int prevW = ret.getWidth();
        int prevH = ret.getHeight();
        if (progressiveBilinear) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }
            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }
            if (scratchImage == null) {
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;
            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);
    
        if (g2 != null) {
            g2.dispose();
        }
    
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }
        return ret;
    }

    private String imageToData(BufferedImage bi) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "PNG", out);
        String base64bytes = Base64.getEncoder().encodeToString(out.toByteArray());
        return "data:image/png;base64," + base64bytes;
    }
}
