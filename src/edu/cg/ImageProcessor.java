package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	// MARK: fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	// MARK: constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
			int outHeight) {
		super(); // initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
	}

	// Changes the picture's hue - example
	public BufferedImage changeHue() {
		logger.log("Preparing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed() / max;
			int green = g * c.getGreen() / max;
			int blue = b * c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}

	// Sets the ForEach parameters with the input dimensions
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	// Sets the ForEach parameters with the output dimensions
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	// A helper method that creates an empty image with the specified input dimensions.
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}

	// A helper method that creates an empty image with the specified output dimensions.
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	// A helper method that creates an empty image with the specified dimensions.
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	// A helper method that deep copies the current working image.
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();

		forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));

		return output;
	}
	
	public BufferedImage greyscale() {
		logger.log("Preparing for grayscale changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed();
			int green = g * c.getGreen();
			int blue = b * c.getBlue();
			int greyHue = (red + green + blue) / (r + g + b);
			Color color = new Color(greyHue, greyHue, greyHue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing greyscale done!");

		return ans;
	}

	public BufferedImage nearestNeighbor() {
		logger.log("Preparing for nearest neighbor changing...");

		BufferedImage ans = newEmptyOutputSizedImage();

		this.setForEachParameters(this.outWidth,this.outHeight);
		forEach((y, x) -> {

			int originalY = Math.round((y * this.inHeight)/ this.outHeight);
			int originalX = Math.round((x * this.inWidth)/ this.outWidth);

			int color = this.workingImage.getRGB(originalX,originalY);
			ans.setRGB(x, y, color);
		});

		logger.log("Rescaling with nearest neighbor done!");

		return ans;
	}

}
