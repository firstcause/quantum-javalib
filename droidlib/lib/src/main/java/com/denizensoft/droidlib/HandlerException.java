package com.denizensoft.droidlib;

import com.denizensoft.jlib.FatalException;

public class HandlerException extends FatalException
{
	public HandlerException()
	{
	}

	public HandlerException(String message)
	{
		super(message);
	}

	public HandlerException(Throwable cause)
	{
		super(cause);
	}

	public HandlerException(String message, Throwable cause)
	{
		super(message, cause);
	}
}

