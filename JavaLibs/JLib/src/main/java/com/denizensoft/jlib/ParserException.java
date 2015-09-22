package com.denizensoft.jlib;

import com.denizensoft.jlib.LibException;

public class ParserException extends LibException
{
	public ParserException()
	{
	}

	public ParserException(String message)
	{
		super(message);
	}

	public ParserException(Throwable cause)
	{
		super(cause);
	}

	public ParserException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
