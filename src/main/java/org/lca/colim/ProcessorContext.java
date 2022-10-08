package org.lca.colim;

import de.wonderplanets.firecapture.plugin.CamInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

public class ProcessorContext {
    private BufferedImage target;
    private boolean doCrop = true;
    private boolean showCorrection = false;
    private int framesToStack = 10;
    private int cropSize = 75;
    private int normalizeMin = 0;
    private int normalizeMax = 200;
    private String[] bayerPattern = { "RG", "BG", "GR", "GB" };
    private String[] colors = { "RED", "GREEN", "BLUE" }; // Due to opencv bug, switch color RED / BLUE there as we are in BGR
    private int selectedPatternIndex = 0;
    private int selectedColorIndex = 0;
    private String message;
    private CamInfo info = new CamInfo();
    private Rectangle imageSize = null;

    public ProcessorContext() {

    }

    public ProcessorContext(ProcessorContext context) {
        this.target = new BufferedImage(context.getTarget().getWidth(), context.getTarget().getHeight(), context.getTarget().getType());
        this.doCrop = context.isDoCrop();
        this.showCorrection = context.isShowCorrection();
        this.framesToStack = context.getFramesToStack();
        this.cropSize = context.getCropSize();
        this.normalizeMin = context.getNormalizeMin();
        this.normalizeMax = context.getNormalizeMax();
        this.selectedColorIndex = context.getSelectedColorIndex();
        this.selectedPatternIndex = context.getSelectedPatternIndex();
        this.message = context.getMessage();
        this.info.is16Bit = context.getInfo().is16Bit;
        this.info.roiOffset = context.getInfo().roiOffset;
        this.info.cameraName = context.getInfo().cameraName;
        this.info.isColor = context.getInfo().isColor;
        this.info.isBin2 = context.getInfo().isBin2;
        this.info.pixelSize = context.getInfo().pixelSize;
        this.info.sensorTempInCelsius = context.getInfo().sensorTempInCelsius;
        this.info.maxImageSize = context.getInfo().maxImageSize;

        this.imageSize = context.getImageSize();
    }

    public BufferedImage getTarget() {
        return target;
    }

    public ProcessorContext setTarget(BufferedImage target) {
        this.target = target;
        return this;
    }

    public boolean isDoCrop() {
        return doCrop;
    }

    public ProcessorContext setDoCrop(boolean doCrop) {
        this.doCrop = doCrop;
        return this;
    }

    public int getFramesToStack() {
        return framesToStack;
    }

    public ProcessorContext setFramesToStack(int framesToStack) {
        this.framesToStack = framesToStack;
        return this;
    }

    public int getCropSize() {
        return cropSize;
    }

    public ProcessorContext setCropSize(int cropSize) {
        this.cropSize = cropSize;
        return this;
    }

    public int getNormalizeMin() {
        return normalizeMin;
    }

    public ProcessorContext setNormalizeMin(int normalizeMin) {
        this.normalizeMin = normalizeMin;
        return this;
    }

    public int getNormalizeMax() {
        return normalizeMax;
    }

    public ProcessorContext setNormalizeMax(int normalizeMax) {
        this.normalizeMax = normalizeMax;
        return this;
    }

    public String[] getBayerPattern() {
        return bayerPattern;
    }

    public ProcessorContext setBayerPattern(String[] bayerPattern) {
        this.bayerPattern = bayerPattern;
        return this;
    }

    public String[] getColors() {
        return colors;
    }

    public ProcessorContext setColors(String[] colors) {
        this.colors = colors;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ProcessorContext setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getSelectedPatternIndex() {
        return selectedPatternIndex;
    }

    public ProcessorContext setSelectedPatternIndex(int selectedPatternIndex) {
        this.selectedPatternIndex = selectedPatternIndex;
        return this;
    }

    public int getSelectedColorIndex() {
        return selectedColorIndex;
    }

    public ProcessorContext setSelectedColorIndex(int selectedColorIndex) {
        this.selectedColorIndex = selectedColorIndex;
        return this;
    }

    public boolean isShowCorrection() {
        return showCorrection;
    }

    public ProcessorContext setShowCorrection(boolean showCorrection) {
        this.showCorrection = showCorrection;
        return this;
    }

    public CamInfo getInfo() {
        return info;
    }

    public ProcessorContext setInfo(CamInfo info) {
        this.info = info;
        return this;
    }

    public Rectangle getImageSize() {
        return imageSize;
    }

    public ProcessorContext setImageSize(Rectangle imageSize) {
        this.imageSize = imageSize;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessorContext that = (ProcessorContext) o;
        return doCrop == that.doCrop &&
                showCorrection == that.showCorrection &&
                framesToStack == that.framesToStack &&
                cropSize == that.cropSize &&
                normalizeMin == that.normalizeMin &&
                normalizeMax == that.normalizeMax &&
                selectedPatternIndex == that.selectedPatternIndex &&
                selectedColorIndex == that.selectedColorIndex &&
                Arrays.equals(bayerPattern, that.bayerPattern) &&
                Arrays.equals(colors, that.colors) &&
                info.isColor == that.info.isColor &&
                info.is16Bit == that.info.is16Bit &&
                info.maxImageSize.width == that.info.maxImageSize.width &&
                info.maxImageSize.height == that.info.maxImageSize.height &&
                imageSize.width == that.imageSize.width &&
                imageSize.height == that.imageSize.height;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(doCrop, showCorrection, framesToStack, cropSize, normalizeMin, normalizeMax, selectedPatternIndex, selectedColorIndex, message, info, imageSize);
        result = 31 * result + Arrays.hashCode(bayerPattern);
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }

    @Override
    protected Object clone() {
        return new ProcessorContext(this);
    }
}
