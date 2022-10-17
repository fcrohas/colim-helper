package org.lca.colim;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class MainWindow {
    private final ProcessorContext context;
    private JFrame frame;
    private JPanel toolbar = new JPanel();
    private Label error;
    private Checkbox doCropCheck = null;
    private Checkbox doCorrCheck = null;
    private ComboBoxEditor doCropChoice = null;
    private JComboBox bayerList = null;
    private JComboBox colorList = null;
    private JPanel panel = null;
    private JSlider framesPerStack = null;
    private JSlider framesNormalizeMin = null;
    private JSlider framesNormalizeMax = null;
    private JSlider cropSize = null;
    private boolean doCrop = true;
    private JPanel focus = new JPanel();
    private JPanel adc = new JPanel();
    private JPanel info = new JPanel();
    private TextArea infoLabel = new TextArea();
    private ChangeListener normMaxChange = null;
    private ChangeListener normMinChange = null;
    private Label normalizeLabel= null;
    private JButton resetFocus = new JButton();
    private JLabel minFoundFwhm = new JLabel("Minimum FWHM : ");
    private JLabel curFoundFwhm = new JLabel("Current FWHM : ");
    public MainWindow(ProcessorContext context) {
        // to local context
        this.context = context;
        // init frame
        initializeComponents(context);
        // Build layout
        buildLayout();
    }

    private void initializeComponents(ProcessorContext context) {
        // Image panel
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (context.getTarget() != null) {
                    g.drawImage(context.getTarget().getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
                }
            }
        };
        // Colimate tab
        // Stack size slider
        framesPerStack = new JSlider(JSlider.HORIZONTAL, 2, 400, 10);
        framesPerStack.addChangeListener(e -> {
            context.setFramesToStack(framesPerStack.getValue());
        });
        // Crop checkbox
        doCropCheck = new Checkbox("Crop image", doCrop);
        doCropCheck.addItemListener(e -> {
            context.setDoCrop(doCropCheck.getState());
        });
        // Display correction checkbox
        doCorrCheck = new Checkbox("Show correction", false);
        doCorrCheck.addItemListener(e -> {
            context.setShowCorrection(doCorrCheck.getState());
        });
        // Crop section
        cropSize = new JSlider(JSlider.HORIZONTAL, 25, 300, context.getCropSize());
        cropSize.addChangeListener(e -> {
            context.setCropSize(cropSize.getValue());
        });
        // Color section
        bayerList = new JComboBox(context.getBayerPattern());
        colorList = new JComboBox(context.getColors());
        colorList.setSelectedIndex(context.getSelectedColorIndex());
        bayerList.setSelectedIndex(context.getSelectedPatternIndex());
        colorList.setLightWeightPopupEnabled(false);
        bayerList.setLightWeightPopupEnabled(false);
        bayerList.addItemListener(e -> {
            context.setSelectedPatternIndex(bayerList.getSelectedIndex());
        });
        colorList.addItemListener(e -> {
            context.setSelectedColorIndex(colorList.getSelectedIndex());
        });
        // Normallize
        framesNormalizeMin = new JSlider(JSlider.HORIZONTAL, 0, 255, context.getNormalizeMin());
        framesNormalizeMax = new JSlider(JSlider.HORIZONTAL, 0, 255, context.getNormalizeMax());
        normMaxChange = e -> context.setNormalizeMax(framesNormalizeMax.getValue());
        normMinChange = e -> context.setNormalizeMin(framesNormalizeMin.getValue());
        framesNormalizeMin.addChangeListener(normMinChange);
        framesNormalizeMax.addChangeListener(normMaxChange);
        error = new Label("");
        // Create layout
        normalizeLabel= new Label("Normalize");
        Label stackCountLabel= new Label("To stack");
        Label cropSizeLabel= new Label("Crop size");
        Label bayerPatternLabel= new Label("Bayer pattern");
        Label colorChannelLabel= new Label("Color channel");
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
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
        toolbar.add(doCorrCheck, BorderLayout.NORTH);
        // Focus
        focus.setLayout(new BoxLayout(focus, BoxLayout.Y_AXIS));
        resetFocus.setText("Reset");
        resetFocus.addActionListener(e -> {
            // initialize FWHM
            context.setMinFWHM(-1);
        });
        focus.add(curFoundFwhm, BorderLayout.NORTH);
        focus.add(minFoundFwhm, BorderLayout.NORTH);
        focus.add(resetFocus, BorderLayout.NORTH);
        // ADC
        adc.setLayout(new BoxLayout(adc, BoxLayout.Y_AXIS));
        // Info
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        //info.add(infoLabel, BorderLayout.NORTH);
    }

    private void buildLayout() {
        frame = new JFrame("Colimation Helper");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Colimate", toolbar);
        tabbedPane.addTab("Focus", focus);
        tabbedPane.addTab("ADC", adc);
        tabbedPane.addTab("Info", info);
        tabbedPane.setSelectedIndex(0);
        tabbedPane.addChangeListener(e -> {
            context.setDisplayMode(tabbedPane.getSelectedIndex());
        });
        frame.getContentPane().add(tabbedPane, BorderLayout.EAST);
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

    public void parameterChange() {
        // 16 bits format change
        framesNormalizeMax.removeChangeListener(normMaxChange);
        framesNormalizeMin.removeChangeListener(normMinChange);
        if (context.getInfo().is16Bit) {
            framesNormalizeMax.setMaximum(65535);
            framesNormalizeMin.setMaximum(65535);
            framesNormalizeMax.setValue(55000);
            context.setNormalizeMax(55000);
        } else {
            framesNormalizeMax.setMaximum(255);
            framesNormalizeMin.setMaximum(255);
            framesNormalizeMax.setValue(200);
            context.setNormalizeMax(200);
        }
        framesNormalizeMin.addChangeListener(normMinChange);
        framesNormalizeMax.addChangeListener(normMaxChange);
        // Color format change
        if (context.getInfo().isColor) {
            bayerList.setEnabled(true);
            colorList.setEnabled(true);
        } else {
            bayerList.setEnabled(false);
            colorList.setEnabled(false);
        }
        // information change
        infoLabel.setText("Camera : " + context.getInfo().cameraName + "\r\n"
                +"Pixel size : " + context.getInfo().pixelSize + "\r\n"
                +"Temperature : " + context.getInfo().sensorTempInCelsius + "°C \r\n"
                +"Max Image : " + context.getInfo().maxImageSize.width + "x" + context.getInfo().maxImageSize.height + "\r\n"
                +"Image : " + context.getImageSize().width + "x" + context.getImageSize().height + "\r\n"
                +"ROI : " + context.getInfo().roiOffset.x +", " + context.getInfo().roiOffset.y + "\r\n"
        );
        //panel.repaint();
    }

    public void reDraw() {
        error.setText(context.getMessage());
        minFoundFwhm.setText("Minimum FWHM : " + context.getMinFWHM() + "\r\n");
        curFoundFwhm.setText("Current FWHM : " + context.getCurFWHM() + "\r\n");
        panel.repaint();

    }

    public void close() {
        frame.dispose();
    }
}
