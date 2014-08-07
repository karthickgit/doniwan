package com.easemob.chatuidemo.image;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public abstract class ImageDecoder {
	public abstract InputStream getStream() throws FileNotFoundException;
	
	public Bitmap decode(ImageSize targetSize, ImageScaleType scaleType)
			throws IOException {
		return decode(targetSize, scaleType, ViewScaleType.FIT_INSIDE);
	}

	public Bitmap decode(ImageSize targetSize, ImageScaleType scaleType,
			ViewScaleType viewScaleType) throws IOException {
		Options decodeOptions = getBitmapOptionsForImageDecoding(targetSize,
				scaleType, viewScaleType);
		InputStream imageStream = getStream();

		Bitmap subsampledBitmap = null;
		try {
			subsampledBitmap = BitmapFactory.decodeStream(imageStream, null,
					decodeOptions);
		} catch (OutOfMemoryError e) {
			System.gc();
		} finally {
			imageStream.close();
		}
		if (subsampledBitmap == null) {
			return null;
		}

		if (scaleType == ImageScaleType.EXACTLY
				|| scaleType == ImageScaleType.EXACTLY_STRETCHED) {
			subsampledBitmap = scaleImageExactly(subsampledBitmap, targetSize,
					scaleType, viewScaleType);
		}

		return subsampledBitmap;
	}

	private Options getBitmapOptionsForImageDecoding(ImageSize targetSize,
			ImageScaleType scaleType, ViewScaleType viewScaleType)
			throws IOException {
		Options options = new Options();
		// 由于这里使用加载小图，使用RBG_565小图 内存分配减少一倍
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.inSampleSize = computeImageScale(targetSize, scaleType,
				viewScaleType);
		return options;
	}

	private int computeImageScale(ImageSize targetSize,
			ImageScaleType scaleType, ViewScaleType viewScaleType)
			throws IOException {
		int targetWidth = targetSize.getWidth();
		int targetHeight = targetSize.getHeight();

		// decode image size
		Options options = new Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		InputStream imageStream = getStream();
		try {
			BitmapFactory.decodeStream(imageStream, null, options);
		} finally {
			imageStream.close();
		}

		int scale = 1;
		int imageWidth = options.outWidth;
		int imageHeight = options.outHeight;
		int widthScale = imageWidth / targetWidth;
		int heightScale = imageHeight / targetHeight;

		if (viewScaleType == ViewScaleType.FIT_INSIDE) {
			if (scaleType == ImageScaleType.IN_SAMPLE_POWER_OF_2
					|| scaleType == ImageScaleType.POWER_OF_2) {
				while (imageWidth / 2 >= targetWidth
						|| imageHeight / 2 >= targetHeight) { // ||
					imageWidth /= 2;
					imageHeight /= 2;
					scale *= 2;
				}
			} else {
				scale = Math.max(widthScale, heightScale); // max
			}
		} else { // ViewScaleType.CROP
			if (scaleType == ImageScaleType.IN_SAMPLE_POWER_OF_2
					|| scaleType == ImageScaleType.POWER_OF_2) {
				while (imageWidth / 2 >= targetWidth
						&& imageHeight / 2 >= targetHeight) { // &&
					imageWidth /= 2;
					imageHeight /= 2;
					scale *= 2;
				}
			} else {
				scale = Math.min(widthScale, heightScale); // min
			}
		}

		if (scale < 1) {
			scale = 1;
		}

		return scale;
	}

	private Bitmap scaleImageExactly(Bitmap subsampledBitmap,
			ImageSize targetSize, ImageScaleType scaleType,
			ViewScaleType viewScaleType) {
		float srcWidth = subsampledBitmap.getWidth();
		float srcHeight = subsampledBitmap.getHeight();

		float widthScale = srcWidth / targetSize.getWidth();
		float heightScale = srcHeight / targetSize.getHeight();

		int destWidth;
		int destHeight;
		if ((viewScaleType == ViewScaleType.FIT_INSIDE && widthScale >= heightScale)
				|| (viewScaleType == ViewScaleType.CROP && widthScale < heightScale)) {
			destWidth = targetSize.getWidth();
			destHeight = (int) (srcHeight / widthScale);
		} else {
			destWidth = (int) (srcWidth / heightScale);
			destHeight = targetSize.getHeight();
		}

		Bitmap scaledBitmap;
		if ((scaleType == ImageScaleType.EXACTLY && destWidth < srcWidth && destHeight < srcHeight)
				|| (scaleType == ImageScaleType.EXACTLY_STRETCHED
						&& destWidth != srcWidth && destHeight != srcHeight)) {
			scaledBitmap = Bitmap.createScaledBitmap(subsampledBitmap,
					destWidth, destHeight, true);
			subsampledBitmap.recycle();
			subsampledBitmap = null;
		} else {
			scaledBitmap = subsampledBitmap;
		}

		return scaledBitmap;
	}

	/**
	 * Type of image scaling during decoding.
	 * 
	 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
	 */
	public enum ImageScaleType {
		/**
		 * The same as {@link #IN_SAMPLE_POWER_OF_2}
		 * 
		 * @deprecated Will be deleted in the future. Use
		 *             {@link #IN_SAMPLE_POWER_OF_2}.
		 */
		@Deprecated
		POWER_OF_2,
		/**
		 * Image will be reduces 2-fold until next reduce step make image
		 * smaller target size.<br />
		 * It's <b>fast</b> type and it's preferable for usage in
		 * lists/grids/galleries (and other {@linkplain AdapterView
		 * adapter-views}) .<br />
		 * Relates to {@link BitmapFactory.Options#inSampleSize}<br />
		 * Note: If original image size is smaller than target size then
		 * original image <b>won't</b> be scaled.
		 */
		IN_SAMPLE_POWER_OF_2,
		/**
		 * The same as {@link #IN_SAMPLE_INT}
		 * 
		 * @deprecated Will be deleted in the future. Use {@link #IN_SAMPLE_INT}
		 *             .
		 */
		@Deprecated
		EXACT,
		/**
		 * Image will be subsampled in an integer number of times. Use it if
		 * memory economy is quite important.<br />
		 * Relates to {@link BitmapFactory.Options#inSampleSize}<br />
		 * Note: If original image size is smaller than target size then
		 * original image <b>won't</b> be scaled.
		 */
		IN_SAMPLE_INT,
		/**
		 * Image will scaled-down exactly to target size (scaled width or height
		 * or both will be equal to target size; depends on
		 * {@linkplain ScaleType ImageView's scale type}). Use it if memory
		 * economy is critically important.<br />
		 * Note: If original image size is smaller than target size then
		 * original image <b>won't</b> be scaled.<br />
		 * <br />
		 * <b>Important note:</b> For creating result Bitmap (of exact size)
		 * additional Bitmap will be created with
		 * {@link Bitmap#createScaledBitmap(Bitmap, int, int, boolean)
		 * Bitmap.createScaledBitmap(...)}. So this scale type requires more
		 * memory for creation of result Bitmap, but then save memory by keeping
		 * in memory smaller Bitmap (comparing with IN_SAMPLE... scale types).
		 */
		EXACTLY,

		/**
		 * Image will scaled exactly to target size (scaled width or height or
		 * both will be equal to target size; depends on {@linkplain ScaleType
		 * ImageView's scale type}). Use it if memory economy is critically
		 * important.<br />
		 * Note: If original image size is smaller than target size then
		 * original image <b>will be stretched</b> to target size.<br />
		 * <br />
		 * <b>Important note:</b> For creating result Bitmap (of exact size)
		 * additional Bitmap will be created with
		 * {@link Bitmap#createScaledBitmap(Bitmap, int, int, boolean)
		 * Bitmap.createScaledBitmap(...)}. So this scale type requires more
		 * memory for creation of result Bitmap, but then save memory by keeping
		 * in memory smaller Bitmap (comparing with IN_SAMPLE... scale types).
		 */
		EXACTLY_STRETCHED
	}

	/**
	 * Simplify {@linkplain ScaleType ImageView's scale type} to 2 types:
	 * {@link #FIT_INSIDE} and {@link #CROP}
	 * 
	 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
	 */
	public enum ViewScaleType {
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so that
		 * both dimensions (width and height) of the image will be equal to or
		 * less the corresponding dimension of the view.
		 */
		FIT_INSIDE,
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so that
		 * both dimensions (width and height) of the image will be equal to or
		 * larger than the corresponding dimension of the view.
		 */
		CROP,

		CENTER_CROP;

		public static ViewScaleType fromImageView(ImageView imageView) {
			switch (imageView.getScaleType()) {
			case FIT_CENTER:
			case FIT_XY:
			case FIT_START:
			case FIT_END:
			case CENTER_INSIDE:
				return FIT_INSIDE;
			case MATRIX:
			case CENTER:
			case CENTER_CROP:
			default:
				return CROP;
			}
		}
	}

}
