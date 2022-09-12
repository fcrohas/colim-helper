package org.lca.colim;

import de.wonderplanets.firecapture.plugin.CamInfo;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Processor {
    private ProcessorContext context = null;
    // Matrix
    private Mat source = null;
    private Mat color = null;
    private Mat stack = new Mat();
    private Mat stackr = new Mat();
    private Mat stackrc = new Mat();
    private Mat targetr = new Mat();
    private int counter = 0;
    private Mat xAxis = null;
    private Mat yAxis = null;
    private Mat warpMat = null;
    // Other
    private int meanLevel = 0;
    private int stackCount = 0;
    //
    private short s[] = null;
    private int meanDirX = 0;
    private int meanDirY = 0;
    private Notifier notifier;
    private int bayerFmt = Imgproc.COLOR_BayerRG2BGR;
    private Size boxSize = null;
    private ProcessorContext oldContext = null;
    private int pixelType;
    private List<Mat> planes = new ArrayList<>();
    // Static opencv initialization
    // Should support all OS(es)
    // Linux, Windows, RPI, Mac
    static {
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (final UnsatisfiedLinkError exception) {
            // No throw exception not let some one else load this library
        }
    }

    public Processor(ProcessorContext context) {
        // local context
        this.context = context;
        // Prepare
        xAxis = new Mat();
        yAxis = new Mat();
        // Translation matrix
        warpMat = new Mat( 2, 3, CvType.CV_64FC1 );
    }

    private void initialize() {
        // Bayer matrix
        switch(context.getSelectedPatternIndex()) {
            case 0: bayerFmt = Imgproc.COLOR_BayerRG2BGR; break;
            case 1: bayerFmt = Imgproc.COLOR_BayerBG2BGR; break;
            case 2: bayerFmt = Imgproc.COLOR_BayerGR2BGR; break;
            case 3: bayerFmt = Imgproc.COLOR_BayerGB2BGR; break;
        }
        // Cropping size
        if (!context.isDoCrop()) {
            boxSize = source.size();
        } else {
            boxSize = new Size(context.getCropSize(), context.getCropSize());
        }
        pixelType = context.getInfo().is16Bit ? CvType.CV_16UC1 : CvType.CV_8UC1;
        context.setTarget(new BufferedImage((int)(boxSize.width), (int)(boxSize.height), BufferedImage.TYPE_3BYTE_BGR));
        source = new Mat(context.getImageSize().height, context.getImageSize().width, pixelType);
        color = new Mat(context.getImageSize().height, context.getImageSize().width, context.getInfo().is16Bit ? CvType.CV_16UC3 : CvType.CV_8UC3);
        stack = new Mat((int)(boxSize.height), (int)(boxSize.width), pixelType);
        stackr = new Mat((int)(boxSize.height), (int)(boxSize.width), context.getInfo().is16Bit ? CvType.CV_16UC1 : CvType.CV_8UC1);
        stackrc = new Mat((int)(boxSize.height), (int)(boxSize.width), context.getInfo().is16Bit ? CvType.CV_16UC3 : CvType.CV_8UC3);
        if ((oldContext == null) || (oldContext.getInfo().is16Bit != context.getInfo().is16Bit)) {
            notifier.parametersChange();
        }
        meanLevel = 0;
        meanDirX = 0;
        meanDirY = 0;
        counter = 0;
        stackCount = 0;
        oldContext = (ProcessorContext) context.clone();
    }

    public void process(byte[] pixels, Rectangle imageSize, CamInfo info) {
        // Crop if needed ?
        if (needReinit()) {
            if (context.getImageSize() == null) {
                context.setImageSize(imageSize);
            } else {
                context.getImageSize().setSize(imageSize.width, imageSize.height);
            }
            context.setInfo(info);
            initialize();
            context.setMessage("Initialization...");
            notifier.refresh();
        }
        //
        try {
            // Fill matrix
            if (info.is16Bit) {
                // Only reallocate if needed
                if (s == null || s.length != pixels.length/2) {
                    s = new short[pixels.length / 2];
                }
                ByteBuffer.wrap(pixels).order(ByteOrder.nativeOrder()).asShortBuffer().get(s);
                source.put(0, 0, s);
            } else {
                source.put(0, 0, pixels);
            }
            // Process separate color
            if (info.isColor) {
                // Do debayer
                Imgproc.cvtColor(source, color, bayerFmt);
                // Split color channels
                Core.split(color, planes);
                // Select the right one
                source = planes.get(context.getSelectedColorIndex());
            }
            // Get projection
            Core.reduce(source, xAxis, 0, Core.REDUCE_SUM, info.is16Bit ? CvType.CV_64F : CvType.CV_32S);
            Core.reduce(source, yAxis, 1, Core.REDUCE_SUM, info.is16Bit ? CvType.CV_64F : CvType.CV_32S);
            // locate maximum position on histogram
            Core.MinMaxLocResult mmrX = Core.minMaxLoc(xAxis);
            Core.MinMaxLocResult mmrY = Core.minMaxLoc(yAxis);
            Point center = new Point((int)mmrX.maxLoc.x, (int)mmrY.maxLoc.y);
            int maxPeak = ((int)mmrX.maxVal + (int)mmrY.maxVal) / 2;
            meanLevel = (meanLevel + maxPeak) / 2;
            // Compute direction move to center
            int translateX = (int)(source.cols() / 2 - center.x);
            int translateY = (int)(source.rows() / 2 - center.y);
            warpMat.put(0 , 0, 1, 0, translateX, 0, 1, translateY);
            Imgproc.warpAffine(source, source, warpMat, source.size());
            // Allow to crop image
            if (context.isDoCrop() && boxSize.width < source.width() && boxSize.height<source.height()) {
                int top = source.height() / 2 - (int) boxSize.height / 2;
                int left = source.width() / 2 - (int) boxSize.width / 2;
                targetr = source.submat(new Rect(left, top, (int) boxSize.width, (int) boxSize.height));
            } else {
                targetr = source.clone();
            }
            // Stack n count
            if (counter < context.getFramesToStack()) {
                if  (maxPeak > meanLevel) {
                    Core.add(stack, targetr.clone(), stack);
                    Core.normalize(stack, stack, context.getNormalizeMin(), context.getNormalizeMax(), Core.NORM_MINMAX);
                    stackCount++;
                }
                counter++;
            } else {
                // update label
                context.setMessage("Stack " + stackCount + " frames on top of " + context.getFramesToStack());
                // reinit
                counter = 0;
                meanLevel = 0;
                stackCount = 0;
                // convert back
                int zoomX = context.getTarget().getWidth() / stackr.width();
                int zoomY = context.getTarget().getHeight() / stackr.height();
                Imgproc.resize(stack, stackr,new Size(context.getTarget().getWidth(), context.getTarget().getHeight()), zoomX, zoomY, Imgproc.INTER_LINEAR);
                Imgproc.cvtColor(stackr, stackrc, Imgproc.COLOR_GRAY2BGR);
                if (context.isShowCorrection()) {
                    Moments moments = Imgproc.moments(stackr, false);
                    // Direction Overlay
                    org.opencv.core.Point targetP = new org.opencv.core.Point(moments.m10 / (moments.m00 + 1e-5), moments.m01 / (moments.m00 + 1e-5));
                    int centerX = stackr.width() / 2;
                    int centerY = stackr.height() / 2;
                    // scale up line
                    Imgproc.line(stackrc, new org.opencv.core.Point(centerX, centerY),
                            new org.opencv.core.Point(centerX + (targetP.x - centerX) * 10, centerY + (targetP.y - centerY) * 10), new Scalar(info.is16Bit ? 65535 : 255, 0, 0));
                    // Compute vector mean
                    if (meanDirX == 0 && meanDirY == 0) {
                        meanDirX = centerX - (int)targetP.x;
                        meanDirY = centerY - (int)targetP.y;
                    } else {
                        meanDirX = (meanDirX + ((int)targetP.x - centerX)) / 2;
                        meanDirY = (meanDirY + ((int)targetP.y - centerY)) / 2;
                        Imgproc.line(stackrc, new org.opencv.core.Point(centerX, centerY),
                                new org.opencv.core.Point(centerX + meanDirX * 10, centerY + meanDirY * 10), new Scalar(0, info.is16Bit ? 65535 : 255, 0));

                    }
                }
                if (info.is16Bit) {
                    stackrc.convertTo(stackrc, CvType.CV_8UC3, 1.0/255.0);
                }
                final byte[] data = ((DataBufferByte) context.getTarget().getRaster().getDataBuffer()).getData();
                stackrc.get(0, 0, data);
                notifier.refresh();
                // better way to clear stack
                stack = targetr.clone();

            }
        } catch(Exception e) {
            context.setMessage("Error : " + e.getMessage());
            initialize();
            notifier.refresh();
        }
    }

    private boolean needReinit() {
        return !context.equals(oldContext);
    }

    void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public void dispose() {
        xAxis.release();
        yAxis.release();
        source.release();
        color.release();
        stack.release();
        stackr.release();
        stackrc.release();
        warpMat.release();
        source = null;
        stack = null;
    }
}
