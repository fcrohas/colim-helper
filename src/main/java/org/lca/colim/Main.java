package org.lca.colim;

import de.wonderplanets.firecapture.plugin.CamInfo;
import de.wonderplanets.firecapture.plugin.IFilterListener;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class Main implements de.wonderplanets.firecapture.plugin.IFilter{

    public static final String PLUGIN_VERSION = "1.1";
    public static final String PLUGIN_NAME = "Colimation Helper";
    public static final String PLUGIN_DESCIPTION = "Help user to get the airy figure well";

    private Window window = null;

    public String getName() {
        return PLUGIN_NAME;
    }

    public String getDescription() {
        return PLUGIN_DESCIPTION;
    }

    public String getMaxValueLabel() {
        return null;
    }

    public String getCurrentValueLabel() {
        return null;
    }

    public String getStringUsage(int percent) {
        return null;
    }

    public boolean useValueFields() {
        return false;
    }

    public boolean useSlider() {
        return false;
    }

    public String getMaxValue() {
        return null;
    }

    public String getCurrentValue() {
        return null;
    }

    public void sliderValueChanged(int value) {

    }

    public int getInitialSliderValue() {
        return 0;
    }

    public void imageSizeChanged() {

    }

    public void filterChanged(String prevFilter, String filter) {

    }

    public void activated() {
        if (window == null) {
            window = new Window();
        }

    }

    public void release() {
        if (window != null)
            window.dispose();
            window = null;
    }

    public boolean capture() {
        return false;
    }

    public void computeMono(byte[] bytePixels, Rectangle imageSize, CamInfo info) {
        window.drawMono(bytePixels, imageSize, info);
    }

    public void computeColor(int[] rgbPixels, Rectangle imageSize, CamInfo info) {
        window.drawColor(rgbPixels, imageSize);
    }

    public void captureStoped() {

    }

    public void captureStarted() {

    }

    public boolean isNullFilter() {
        return false;
    }

    public boolean processEarly() {
        return false;
    }

    public boolean supportsColor() {
        return true;
    }

    public boolean supportsMono() {
        return true;
    }

    public void registerFilterListener(IFilterListener listener) {

    }

    public JButton getButton() {
        return null;
    }

    public String getInterfaceVersion() {
        return PLUGIN_VERSION;
    }

    public String getFilenameAppendix() {
        return null;
    }

    public void appendToLogfile(Properties properties) {

    }
}
