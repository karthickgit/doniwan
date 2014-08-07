
package com.easemob.chatuidemo.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileImageDecoder extends ImageDecoder {
	File mFile;

	public FileImageDecoder(File imageFile){
		mFile = imageFile;
	}

	@Override
	public InputStream getStream() throws FileNotFoundException {
		return new FileInputStream(mFile);
	}
}
