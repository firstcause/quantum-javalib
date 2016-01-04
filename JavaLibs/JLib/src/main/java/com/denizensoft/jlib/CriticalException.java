package com.denizensoft.jlib;

/**
 * Created by sjm on 11/28/15.
 */
public class CriticalException extends LibException
{
	public CriticalException()
	{
	}

	public CriticalException(String message)
	{
		super(message);
	}

	public CriticalException(Throwable cause)
	{
		super(cause);
	}

	public CriticalException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
