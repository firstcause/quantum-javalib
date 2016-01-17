package com.denizensoft.droidlib;

/**
 * Created by sjm on 10/5/15.
 */
public class MainContainer extends FolderElement
{
	private final Requester mRequester;

	@Override
	public String elementPath()
	{
		return "/";
	}

	@Override
	public Requester requester()
	{
		return mRequester;
	}

	public MainContainer(Requester requester)
	{
		mRequester = requester;
	}
}
