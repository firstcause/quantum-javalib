package com.denizensoft.jlib;

public class Variable extends Node
{
	protected String stValue = "";

	@Override
	public void addedToScope(Scope scope)
	{
	}

	@Override
	public String invoke(Scope scope, String[] stArgs)
	{
		return stValue;
	}

	public Variable(String stTag,String stValue)
	{
		super(stTag);

		this.stValue = stValue;
	}
}
