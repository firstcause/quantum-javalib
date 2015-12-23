package com.denizensoft.droidlib;

import android.util.Base64;

import java.security.SecureRandom;

/**
 * Created by sjm on 11/22/15.
 */
abstract public class TargetNode
{
	private final Object mOwner;

	private String mNodeTag = generateNodeTag();

	protected String generateNodeTag()
	{
		byte[] buffer = new byte[16];

		SecureRandom secureRandom = new SecureRandom();

		secureRandom.setSeed(secureRandom.generateSeed(64));
		secureRandom.nextBytes(buffer);

		return Base64.encodeToString(buffer,Base64.URL_SAFE|Base64.NO_PADDING|Base64.NO_WRAP);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public Object nodeOwner()
	{
		return mOwner;
	}

	public String nodeTag()
	{
		return mNodeTag;
	}

	public void setTag(String stNodeTag)
	{
		mNodeTag = stNodeTag;
	}

	public TargetNode()
	{
		mOwner = null;
	}

	public TargetNode(Object owner)
	{
		mOwner = owner;
	}
}

