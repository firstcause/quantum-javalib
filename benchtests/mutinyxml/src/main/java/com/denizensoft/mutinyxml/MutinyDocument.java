package com.denizensoft.mutinyxml;

/**
 * Created by sjm on 10/5/15.
 */
public class MutinyDocument extends MutinyElement
{
	MutinyElement.Requester mRequester = null;

	String mFileSpec;

	public int exec(String args[])
	{
		MutinyElement element = getElement("/main");

		String stCommand = element.attribute("action");

		element.invokeCommand(mRequester,stCommand);

		return 0;
	}

	public MutinyDocument(MutinyElement.Requester requester, String stFileSpec)
	{
		super();

		mRequester = requester;

		mFileSpec = stFileSpec;
	}
}
