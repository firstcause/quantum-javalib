package com.denizensoft.jlib;

/**
 * Created by sjm on 12/20/2014.
 */
public class WarningException extends LibException
{
	public WarningException()
	{
	}

	public WarningException(String message)
	{
		super(message);
	}

	public WarningException(Throwable cause)
	{
		super(cause);
	}

	public WarningException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
