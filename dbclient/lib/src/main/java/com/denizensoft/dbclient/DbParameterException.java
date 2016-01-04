package com.denizensoft.dbclient;

/**
 * Created by sjm on 12/20/2014.
 */
public class DbParameterException extends DbException
{
	public DbParameterException()
	{
	}

	public DbParameterException(String message)
	{
		super(message);
	}

	public DbParameterException(Throwable cause)
	{
		super(cause);
	}

	public DbParameterException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
