package com.denizensoft.dbclient;

/**
 * Created by sjm on 12/20/2014.
 */
public class DbNotFoundException extends DbException
{
	public DbNotFoundException()
	{
	}

	public DbNotFoundException(String message)
	{
		super(message);
	}

	public DbNotFoundException(Throwable cause)
	{
		super(cause);
	}

	public DbNotFoundException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
