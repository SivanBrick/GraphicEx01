package edu.cg;

import edu.cg.menu.MenuWindow;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;

public class SeamsCarver extends ImageProcessor {

    // MARK: An inner interface for functional programming.
    @FunctionalInterface
    interface ResizeOperation {
        BufferedImage resize();
    }

    // MARK: Fields
    private int numOfSeams;
    private ResizeOperation resizeOp;
    boolean[][] imageMask;
    int[][] trueIndexes;
    private boolean[][] isSeam;
    private boolean[][] seamCarvingMask;

    public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
                       boolean[][] imageMask) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

        numOfSeams = Math.abs(outWidth - inWidth);
        this.imageMask = imageMask;
        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

        // Setting resizeOp by with the appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        this.isSeam = new boolean[workingImage.getHeight()][workingImage.getWidth()];
        this.trueIndexes = initializeTrueIndexes();
        this.seamCarvingMask = imageMask;

        this.logger.log("preliminary calculations were ended.");
    }

    private int[][] initializeTrueIndexes() {
        int[][] ans = new int[inHeight][inWidth];
        for (int i = 0; i < inHeight; i++) {
            for (int j = 0; j < inWidth; j++) {
                ans[i][j] = j;
            }
        }
        return ans;
    }

    public BufferedImage resize() {
        return resizeOp.resize();
    }

    private BufferedImage reduceImageWidth() {

        BufferedImage curWorkingImage = this.duplicateWorkingImage();
        BufferedImage greyWorkingImg = new ImageProcessor(logger, curWorkingImage, rgbWeights, inWidth, curWorkingImage.getHeight()).greyscale();

        for (int i = 0; i < numOfSeams; i++) {

            long[][] energy = calculatePixelsEnergy(inHeight, inWidth - i, greyWorkingImg);
            long[][] cost = calculateCostMatrix(inHeight, inWidth - i, greyWorkingImg, energy);

            Stack<Integer> seamToRemove = backTracking(cost, energy, inHeight, inWidth - i, greyWorkingImg);
            int newWidth = inWidth - i - 1;

            //remove seam + create new image mask
            BufferedImage plainImage = newEmptyImage(newWidth, inHeight);
            boolean[][] newImageMask = new boolean[inHeight][newWidth];
            int[][] newTrueIndexes = new int[inHeight][newWidth];

            for (int rows = 0; rows < inHeight; rows++) {
                int relatedPixel = seamToRemove.pop();
                //initialize the seams matrix
                int trueValPixel = this.trueIndexes[rows][relatedPixel];
                this.isSeam[rows][trueValPixel] = true;

                int col = 0;
                while (col < relatedPixel) {
                    newImageMask[rows][col] = this.seamCarvingMask[rows][col];
                    plainImage.setRGB(col, rows, curWorkingImage.getRGB(col, rows));
                    newTrueIndexes[rows][col] = this.trueIndexes[rows][col];
                    col++;
                }
                col++;
                while (col < inWidth - i) {
                    newImageMask[rows][col - 1] = this.seamCarvingMask[rows][col];
                    newTrueIndexes[rows][col - 1] = trueIndexes[rows][col];
                    plainImage.setRGB(col - 1, rows, curWorkingImage.getRGB(col, rows));
                    col++;
                }
            }
            this.seamCarvingMask = newImageMask;
            this.trueIndexes = newTrueIndexes;
            curWorkingImage = plainImage;
            greyWorkingImg = new ImageProcessor(logger, curWorkingImage, rgbWeights, newWidth, curWorkingImage.getHeight()).greyscale();
        }

        return curWorkingImage;
    }

    private Stack backTracking(long[][] cost, long[][] energy, int height, int width, BufferedImage greyImg) {

        Stack<Integer> ans = new Stack();

        long minVal = Integer.MAX_VALUE;
        int minValPosition = 0;

        //Last row minimal value
        for (int j = 0; j < width; j++) {
            if (minVal > cost[height - 1][j]) {
                minVal = cost[height - 1][j];
                minValPosition = j;
            }
        }

        ans.push(minValPosition);

        for (int i = height - 1; i > 0; i--) {

            int j = ans.peek();
            if (j == 0) {
                long upR = cost[i - 1][j + 1];
                long up = cost[i - 1][j];

                if (up <= upR) {
                    ans.push(j);
                } else {
                    ans.push(j + 1);
                }
            } else if (j == width - 1) {
                long upL = cost[i - 1][j - 1];
                long up = cost[i - 1][j];

                if (up <= upL) {
                    ans.push(j);
                } else {
                    ans.push(j - 1);
                }
            } else {
                long upR = cost[i - 1][j + 1];
                long upL = cost[i - 1][j - 1];
                long up = cost[i - 1][j];

                if (up <= upR && up <= upL) {
                    ans.push(j);
                } else if (upR <= up && upR <= upL) {
                    ans.push(j + 1);
                } else {
                    ans.push(j - 1);
                }
            }


            /*if (cost[i][j] == energy[i][j] + cost[i - 1][j] + calcCU(i, j, greyImg)) {
                ans.push(j);
            } else if (j > 0 && cost[i][j] == energy[i][j] + cost[i - 1][j - 1] + calcCL(i, j, greyImg)) {
                ans.push(j - 1);
            } else {
                ans.push(j + 1);
            }*/
        }

        return ans;
    }

    private BufferedImage increaseImageWidth() {

        reduceImageWidth();
        BufferedImage ans = newEmptyImage(inWidth + numOfSeams, inHeight);
        boolean[][] newImageMask = new boolean[inHeight][inWidth + numOfSeams];

        for (int row = 0; row < inHeight; row++) {
            int duplicatesSeams = 0;
            for (int col = 0; col < inWidth; col++) {
                ans.setRGB(col + duplicatesSeams, row, workingImage.getRGB(col, row));
                newImageMask[row][col + duplicatesSeams] = this.imageMask[row][col];
                if (isSeam[row][col]) {
                    duplicatesSeams++;
                    ans.setRGB(col + duplicatesSeams, row, workingImage.getRGB(col, row));
                    newImageMask[row][col + duplicatesSeams] = this.imageMask[row][col];
                }
            }
        }

        this.seamCarvingMask = newImageMask;

        return ans;
    }


    private long[][] calculatePixelsEnergy(int height, int width, BufferedImage greyscaleImgInProcess) {

        long[][] ans = new long[height][width];
        long e1, e2, e3;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int curVal = new Color(greyscaleImgInProcess.getRGB(j, i)).getRed();
                e1 = (j < width - 1) ?
                        (Math.abs(curVal - new Color(greyscaleImgInProcess.getRGB(j + 1, i)).getRed())) :
                        (Math.abs(curVal - new Color(greyscaleImgInProcess.getRGB(j - 1, i)).getRed()));
                e2 = (i < height - 1) ?
                        (Math.abs(curVal - new Color(greyscaleImgInProcess.getRGB(j, i + 1)).getRed())) :
                        Math.abs(curVal - new Color(greyscaleImgInProcess.getRGB(j, i - 1)).getRed());
                ans[i][j] = e1 + e2;
            }
        }

        return ans;
    }

    private long[][] calculateCostMatrix(int height, int width, BufferedImage greyscaleImgInProcess, long[][] energyCosts) {
        long[][] ans = new long[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (seamCarvingMask[i][j]) {
                    ans[i][j] = Integer.MIN_VALUE;
                } else {
                    long e = energyCosts[i][j];
                    //first row
                    if (i == 0) {
                        ans[i][j] = e;
                    } else {
                        long top = ans[i - 1][j] + calcCU(i, j, greyscaleImgInProcess);
                        //first col
                        if (j == 0) {
                            ans[i][j] = e + Math.min(top, (ans[i - 1][j + 1] + calcCR(i, j, greyscaleImgInProcess)));
                        }// last col
                        else if (j == greyscaleImgInProcess.getWidth() - 1) {
                            ans[i][j] = e + Math.min(top, (ans[i - 1][j - 1] + calcCL(i, j, greyscaleImgInProcess)));
                        } else {
                            ans[i][j] = e + Math.min(top, Math.min((ans[i - 1][j + 1] + calcCR(i, j, greyscaleImgInProcess)), (ans[i - 1][j - 1] + calcCL(i, j, greyscaleImgInProcess))));
                        }
                    }
                }
            }
        }
        return ans;
    }

    private long calcCU(int i, int j, BufferedImage greyscaleImage) {
        if (j == 0 || j == greyscaleImage.getWidth() - 1) {
            return 255;
        } else {
            return Math.abs(new Color(greyscaleImage.getRGB(j + 1, i)).getRed() - new Color(greyscaleImage.getRGB(j - 1, i)).getRed());
        }
    }

    private long calcCR(int i, int j, BufferedImage greyscaleImage) {
        long a = (j > 0) ? Math.abs(new Color(greyscaleImage.getRGB(j + 1, i)).getRed() - new Color(greyscaleImage.getRGB(j - 1, i)).getRed()) : 255;
        long b = Math.abs(new Color(greyscaleImage.getRGB(j + 1, i)).getRed() - new Color(greyscaleImage.getRGB(j, i - 1)).getRed());
        return a + b;
    }

    private long calcCL(int i, int j, BufferedImage greyscaleImage) {
        long a = (j < greyscaleImage.getWidth() - 1) ? Math.abs(new Color(greyscaleImage.getRGB(j + 1, i)).getRed() - new Color(greyscaleImage.getRGB(j - 1, i)).getRed()) : 255;
        long b = Math.abs(new Color(greyscaleImage.getRGB(j, i - 1)).getRed() - new Color(greyscaleImage.getRGB(j - 1, i)).getRed());
        return a + b;
    }

    public BufferedImage showSeams(int seamColorRGB) {
        reduceImageWidth();
        BufferedImage ans = this.duplicateWorkingImage();

        if (numOfSeams > 0) {
            for (int i = 0; i < workingImage.getHeight(); i++) {
                for (int j = 0; j < workingImage.getWidth(); j++) {
                    if (isSeam[i][j] == true) {
                        ans.setRGB(j, i, seamColorRGB);
                    }
                }
            }
        }

        logger.log("Changing greyscale done!");

        return ans;
    }

    public boolean[][] getMaskAfterSeamCarving() {
        return (seamCarvingMask != null) ? seamCarvingMask : this.imageMask;
    }
}