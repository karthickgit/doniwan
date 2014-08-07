
package com.easemob.chatuidemo.image;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.res.Resources;

public class ResourceImageDecoder extends ImageDecoder {
	private Resources mResources;
	private int mId;
	
	public ResourceImageDecoder(Resources res, int id){
		mResources = res;
		mId = id;
	}
	
	@Override
	public InputStream getStream() throws FileNotFoundException {
		return mResources.openRawResource(mId);
	}
}
