package com.denizensoft.dbclient;

import com.denizensoft.jlib.LibException;

/**
 * Created by sjm on 12/20/2014.
 */
public class DbException extends LibException
{
	public DbException()
	{
	}

	public DbException(String message)
	{
		super(message);
	}

	public DbException(Throwable cause)
	{
		super(cause);
	}

	public DbException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
