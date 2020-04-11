package edu.cg;

import java.awt.image.BufferedImage;
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
    // TODO: Add some additional fields

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

        // TODO: You may initialize your additional fields and apply some preliminary
        // calculations.

        this.logger.log("preliminary calculations were ended.");
    }

    public BufferedImage resize() {
        return resizeOp.resize();
    }

    private BufferedImage reduceImageWidth() {
        //TODO: copy the original or destroy???
        BufferedImage greyWorkingImg = new ImageProcessor(logger, workingImage, rgbWeights, inWidth, workingImage.getHeight()).greyscale();
        BufferedImage curWorkingImage = this.workingImage;
        for (int i = 0; i < numOfSeams; i++) {

            long[][] energy = calculatePixelsEnergy(inHeight, inWidth - i, greyWorkingImg);
            long[][] cost = calculateCostMatrix(inHeight, inWidth - i, greyWorkingImg, energy);

            Stack<Integer> seamToRemove = backTracking(cost, energy, inHeight, inWidth - i, greyWorkingImg);
            int newWidth = inWidth - i - 1;


            //remove seam + create new image mask
            BufferedImage plainImage = newEmptyImage(newWidth, inHeight);
			boolean[][] newImageMask = new boolean[inHeight][newWidth];

            for (int rows = 0; rows < inHeight; rows++) {
                int pixelToRemove = seamToRemove.pop();
                int col = 0;
                while (col < pixelToRemove){
                	newImageMask[rows][col] = this.imageMask[rows][col];
                	plainImage.setRGB(col, rows, curWorkingImage.getRGB(col,rows));
                	col++;
				}
                col++;
                while (col < inWidth - i){
					newImageMask[rows][col-1] = this.imageMask[rows][col];
					plainImage.setRGB(col - 1, rows, curWorkingImage.getRGB(col,rows));
					col++;
                }
            }
            this.imageMask = newImageMask;
            curWorkingImage = plainImage;
			greyWorkingImg = new ImageProcessor(logger, curWorkingImage, rgbWeights, newWidth, curWorkingImage.getHeight()).greyscale();
        }

			return curWorkingImage;
    }



	private Stack backTracking(long[][] cost, long[][] energy, int height, int width, BufferedImage greyImg) {

        Stack<Integer> ans = new Stack();

        long minVal = Integer.MAX_VALUE;
        int minValPositin = 0;

        //Last row minimal value
        for (int j = 0; j < width; j++) {
            if (minVal > cost[height - 1][j]) {
                minVal = cost[height - 1][j];
                minValPositin = j;
            }
        }

        ans.push(minValPositin);

        for (int i = height - 1; i > 0; i--) {
            int j = ans.peek();
            if (cost[i][j] == energy[i][j] + cost[i - 1][j] + calcCV(i, j, greyImg)) {
                ans.push(j);
            } else if (j > 0 && cost[i][j] == energy[i][j] + cost[i - 1][j - 1] + calcCL(i, j, greyImg)) {
                ans.push(j - 1);
            } else {
                ans.push(j + 1);
            }
        }


        return ans;
    }

    private BufferedImage increaseImageWidth() {
        // TODO: Implement this method, remove the exception.
        throw new UnimplementedMethodException("increaseImageWidth");
    }

    private long[][] calculatePixelsEnergy(int height, int width, BufferedImage greyscaleImgInProcess) {

        long[][] ans = new long[height][width];
        int e1, e2, e3;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                e1 = (j < width - 1) ?
                        (Math.abs(greyscaleImgInProcess.getRGB(j, i) - greyscaleImgInProcess.getRGB(j + 1, i))) :
                        (Math.abs(greyscaleImgInProcess.getRGB(j, i) - greyscaleImgInProcess.getRGB(j - 1, i)));
                e2 = (i < height - 1) ?
                        (Math.abs(greyscaleImgInProcess.getRGB(j, i) - greyscaleImgInProcess.getRGB(j, i + 1))) :
                        Math.abs(greyscaleImgInProcess.getRGB(j, i) - greyscaleImgInProcess.getRGB(j, i - 1));
                // TODO: min val not positive
                e3 = (imageMask[i][j]) ? Integer.MIN_VALUE : 0;
                ans[i][j] = e1 + e2 + e3;
            }
        }

        return ans;
    }

    private long[][] calculateCostMatrix(int height, int width, BufferedImage greyscaleImgInProcess, long[][] energyCosts) {
        long[][] ans = new long[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                long e = energyCosts[i][j];
                //first row
                if (i == 0) {
                    ans[i][j] = e;
                } else {
                    //first col
                    if (j == 0) {
                        ans[i][j] = e + Math.min((ans[i - 1][j] + calcCV(i, j, greyscaleImgInProcess)), (ans[i - 1][j + 1] + calcCR(i, j, greyscaleImgInProcess)));
                    }// last col
                    else if (j == greyscaleImgInProcess.getWidth() - 1) {
                        ans[i][j] = e + Math.min((ans[i - 1][j] + calcCV(i, j, greyscaleImgInProcess)), (ans[i - 1][j + 1] + calcCR(i, j, greyscaleImgInProcess)));
                    } else {
                        ans[i][j] = e + Math.min((ans[i - 1][j] + calcCV(i, j, greyscaleImgInProcess)), Math.min((ans[i - 1][j + 1] + calcCR(i, j, greyscaleImgInProcess)), (ans[i - 1][j - 1] + calcCL(i, j, greyscaleImgInProcess))));
                    }
                }
            }
        }
        return ans;
    }

    private long calcCV(int i, int j, BufferedImage greyscaleImage) {
        return Math.abs(greyscaleImage.getRGB(j + 1, i) - greyscaleImage.getRGB(j - 1, i));
    }

    private long calcCR(int i, int j, BufferedImage greyscaleImage) {
        return Math.abs(greyscaleImage.getRGB(j + 1, i) - greyscaleImage.getRGB(j - 1, i)) +
                Math.abs(greyscaleImage.getRGB(j + 1, i) - greyscaleImage.getRGB(j, i - 1));

    }

    private long calcCL(int i, int j, BufferedImage greyscaleImage) {
        return Math.abs(greyscaleImage.getRGB(j + 1, i) - greyscaleImage.getRGB(j - 1, i)) +
                Math.abs(greyscaleImage.getRGB(j, i - 1) - greyscaleImage.getRGB(j - 1, i));

    }

    public BufferedImage showSeams(int seamColorRGB) {
        // TODO: Implement this method (bonus), remove the exception.
        throw new UnimplementedMethodException("showSeams");
    }

    public boolean[][] getMaskAfterSeamCarving() {
        // TODO: Implement this method, remove the exception.
        // This method should return the mask of the resize image after seam carving.
        // Meaning, after applying Seam Carving on the input image,
        // getMaskAfterSeamCarving() will return a mask, with the same dimensions as the
        // resized image, where the mask values match the original mask values for the
        // corresponding pixels.
        // HINT: Once you remove (replicate) the chosen seams from the input image, you
        // need to also remove (replicate) the matching entries from the mask as well.
        throw new UnimplementedMethodException("getMaskAfterSeamCarving");
    }
}
