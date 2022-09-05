package org.lca.colim;

import de.wonderplanets.firecapture.plugin.CamInfo;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.accumulate;

public class Window {
    public static final String PIXEL_SIZE = "Pixel size : ";
    private JFrame frame;
    private JPanel toolbar = new JPanel();
    private Label label;
    private Label error;
    private Checkbox doCropCheck = null;
    private Checkbox doCorrCheck = null;
    private ComboBoxEditor doCropChoice = null;
    private JComboBox bayerList = null;
    private JComboBox colorList = null;
    private JPanel panel = null;
    private BufferedImage target = null;
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
    private JSlider framesPerStack = null;
    private JSlider framesZoom = null;
    private JSlider framesNormalizeMin = null;
    private JSlider framesNormalizeMax = null;
    private JSlider cropSize = null;
    private int meanLevel = 0;
    private int stackCount = 0;
    private int scale = 1;
    private boolean doCrop = true;
    private String[] bayerMatrix = { "RG", "BG", "GB", "GR" };
    private String[] colors = { "RED", "GREEN", "BLUE" };

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (final UnsatisfiedLinkError exception) {
            // No throw exception not let some one else load this library
        }
    }

    public Window() {
        // Prepare
        xAxis = new Mat();
        yAxis = new Mat();
        warpMat = new Mat( 2, 3, CvType.CV_64FC1 );
        // init frame
        frame = new JFrame("Colimation Helper");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (target != null) {
                    g.drawImage(target.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
                    //this.repaint();
                }
            }
        };
        framesPerStack = new JSlider(JSlider.HORIZONTAL, 2, 400, 10);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        doCropCheck = new Checkbox("Crop image", doCrop);
        doCorrCheck = new Checkbox("Show correction", false);
        Label zoomLabel= new Label("Scale");
        Label normalizeLabel= new Label("Normalize");
        Label stackCountLabel= new Label("To stack");
        Label cropSizeLabel= new Label("Crop size");
        Label bayerPatternLabel= new Label("Bayer pattern");
        Label colorChannelLabel= new Label("Color channel");
        colorChannelLabel.setIgnoreRepaint(true);
        framesZoom = new JSlider(JSlider.HORIZONTAL, 1, 20, 2);
        framesNormalizeMin = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
        framesNormalizeMax = new JSlider(JSlider.HORIZONTAL, 0, 255, 200);
        cropSize = new JSlider(JSlider.HORIZONTAL, 25, 150, 75);
        bayerList = new JComboBox(bayerMatrix);
        colorList = new JComboBox(colors);
        colorList.setLightWeightPopupEnabled(false);
        bayerList.setLightWeightPopupEnabled(false);
        label = new Label(PIXEL_SIZE);
        error = new Label("");
        toolbar.add(label);
        toolbar.add(bayerPatternLabel, BorderLayout.NORTH);
        toolbar.add(bayerList, BorderLayout.NORTH);
        toolbar.add(colorChannelLabel, BorderLayout.NORTH);
        toolbar.add(colorList, BorderLayout.NORTH);
        toolbar.add(normalizeLabel);
        toolbar.add(framesNormalizeMin, BorderLayout.NORTH);
        toolbar.add(framesNormalizeMax, BorderLayout.NORTH);
        toolbar.add(stackCountLabel);
        toolbar.add(framesPerStack, BorderLayout.NORTH);
        toolbar.add(doCropCheck, BorderLayout.NORTH);
        toolbar.add(cropSizeLabel);
        toolbar.add(cropSize, BorderLayout.NORTH);
        toolbar.add(zoomLabel);
        toolbar.add(framesZoom, BorderLayout.NORTH);
        toolbar.add(doCorrCheck, BorderLayout.NORTH);
        frame.getContentPane().add(toolbar, BorderLayout.EAST);
        panel.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.getContentPane().add(error, BorderLayout.SOUTH);
        frame.setAlwaysOnTop(true);
        frame.setMinimumSize(new Dimension(600, 400));
        frame.pack();
        frame.validate();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    public void drawMono(byte[] pixels, Rectangle imageSize, CamInfo info) {
        int pixelType = info.is16Bit ? CvType.CV_16UC1 : CvType.CV_8UC1;
        // Bayer matrix
        int bayerFmt = Imgproc.COLOR_BayerRG2RGB;
        switch(bayerList.getSelectedIndex()) {
            case 0: bayerFmt = Imgproc.COLOR_BayerRG2RGB; break;
            case 1: bayerFmt = Imgproc.COLOR_BayerBG2RGB; break;
            case 2: bayerFmt = Imgproc.COLOR_BayerGR2RGB; break;
            case 3: bayerFmt = Imgproc.COLOR_BayerGB2RGB; break;
        }
        // init
        Size boxSize = new Size(cropSize.getValue(), cropSize.getValue());
        if (source == null || scale != framesZoom.getValue()) {
            scale = framesZoom.getValue();
            target = new BufferedImage((int)(boxSize.width * scale), (int)(boxSize.height * scale), BufferedImage.TYPE_3BYTE_BGR);
            source = new Mat(imageSize.height, imageSize.width, pixelType);
            color = new Mat(imageSize.height, imageSize.width, info.is16Bit ? CvType.CV_16UC3 : CvType.CV_8UC3);
            stack = new Mat((int)(boxSize.height), (int)(boxSize.width), pixelType);
            stackr = new Mat((int)(boxSize.height * scale), (int)(boxSize.width * scale), CvType.CV_8UC1);
            stackrc = new Mat((int)(boxSize.height * scale), (int)(boxSize.width * scale), CvType.CV_8UC3);
        }
        // Crop if needed ?
        if (!doCropCheck.getState()) {
            boxSize = source.size();
        }
        //
        try {
            error.setText("No error.");
            // Fill matrix
            source.put(0, 0, pixels);
            // Do debayer
            Imgproc.cvtColor(source, color, bayerFmt);
            // Split color channels
            List<Mat> planes = new ArrayList<>();
            Core.split(color, planes);
            // Select the right one
            source = planes.get(colorList.getSelectedIndex());
            // Get projection
            Core.reduce(source, xAxis, 0, Core.REDUCE_SUM, CvType.CV_32S);
            Core.reduce(source, yAxis, 1, Core.REDUCE_SUM, CvType.CV_32S);
            // locate maximum position on histogram
            Core.MinMaxLocResult mmrX = Core.minMaxLoc(xAxis);
            Core.MinMaxLocResult mmrY = Core.minMaxLoc(yAxis);
            org.opencv.core.Point center = new org.opencv.core.Point((int)mmrX.maxLoc.x, (int)mmrY.maxLoc.y);
            int maxPeak = ((int)mmrX.maxVal + (int)mmrY.maxVal) / 2;
            meanLevel = (meanLevel + maxPeak) / 2;
            // Compute direction move to center
            int translateX = (int)(source.cols() / 2 - center.x);
            int translateY = (int)(source.rows() / 2 - center.y);
            warpMat.put(0 , 0, 1, 0, translateX, 0, 1, translateY);
            Imgproc.warpAffine(source, source, warpMat, source.size());
            // Allow to crop image
            if (doCropCheck.getState() && boxSize.width < source.width() && boxSize.height<source.height()) {
                int top = source.height() / 2 - (int) boxSize.height / 2;
                int left = source.width() / 2 - (int) boxSize.width / 2;
                targetr = source.submat(new Rect(left, top, (int) boxSize.width, (int) boxSize.height));
            } else {
                targetr = source.clone();
            }
            // Stack n count
            if (counter < framesPerStack.getValue()) {
                if  (maxPeak > meanLevel) {
                    Core.add(stack, targetr.clone(), stack);
                    Core.normalize(stack, stack, framesNormalizeMin.getValue(), framesNormalizeMax.getValue(), Core.NORM_MINMAX);
                    stackCount++;
                }
                counter++;
            } else {
                // update label
                label.setText("Stack " + stackCount + " frames on top of " + framesPerStack.getValue());
                // reinit
                counter = 0;
                meanLevel = 0;
                stackCount = 0;
                // convert back
                Imgproc.resize(stack, stackr, stackr.size(), scale, scale, Imgproc.INTER_NEAREST);
                Imgproc.cvtColor(stackr, stackrc, Imgproc.COLOR_GRAY2BGR);
                if (doCorrCheck.getState()) {
                    Moments moments = Imgproc.moments(stackr, false);
                    // Direction Overlay
                    org.opencv.core.Point targetP = new org.opencv.core.Point(moments.m10 / (moments.m00 + 1e-5), moments.m01 / (moments.m00 + 1e-5));
                    int centerX = stackr.width() / 2;
                    int centerY = stackr.height() / 2;
                    // scale up line
                    Imgproc.line(stackrc, new org.opencv.core.Point(centerX, centerY),
                            new org.opencv.core.Point(centerX + (targetP.x - centerX) * 20, centerY + (targetP.y - centerY) * 20), new Scalar(255, 0, 0));
                }
                final byte[] data = ((DataBufferByte) target.getRaster().getDataBuffer()).getData();
                stackrc.get(0,0, data);
                // better way to clear stack
                stack = targetr.clone();
            }
        } catch(Exception e) {
            error.setText("Error : " + e.getMessage());
        }
        panel.repaint();
    }

    public void drawColor(int[] rgbPixels, Rectangle imageSize) {

    }

    public void dispose() {
        frame.dispose();
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
