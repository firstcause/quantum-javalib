package com.denizensoft.jlib;

abstract public class Node
{
	private String stTag;

	abstract public void addedToScope(Scope scope);

	abstract public String invoke(Scope scope,String[] stArgs);

	public String tag()
	{
		return stTag;
	}

	public Node(String stTag)
	{
		this.stTag = stTag;
	}
}
