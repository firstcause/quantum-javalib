package com.denizensoft.dbclient;

/**
 * Created by sjm on 12/20/2014.
 */
public class DbStateException extends DbException
{
	public DbStateException()
	{
	}

	public DbStateException(String message)
	{
		super(message);
	}

	public DbStateException(Throwable cause)
	{
		super(cause);
	}

	public DbStateException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
