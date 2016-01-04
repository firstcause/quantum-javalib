package com.denizensoft.jlib;

/**
 * Created by sjm on 11/28/15.
 */
public class LibException extends RuntimeException
{
	public LibException()
	{
	}

	public LibException(String message)
	{
		super(message);
	}

	public LibException(Throwable cause)
	{
		super(cause);
	}

	public LibException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
