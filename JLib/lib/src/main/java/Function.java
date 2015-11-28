package com.denizensoft.jlib;

abstract public class Function extends Node
{
	@Override
	public void addedToScope(Scope scope)
	{
	}

	public Function(String stTag)
	{
		super(stTag);
	}
}
