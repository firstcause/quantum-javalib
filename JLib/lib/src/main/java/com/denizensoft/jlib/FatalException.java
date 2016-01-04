package com.denizensoft.jlib;

/**
 * Created by sjm on 12/28/15.
 */
public class FatalException extends LibException
{
	public FatalException()
	{
	}

	public FatalException(String message)
	{
		super(message);
	}

	public FatalException(Throwable cause)
	{
		super(cause);
	}

	public FatalException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
