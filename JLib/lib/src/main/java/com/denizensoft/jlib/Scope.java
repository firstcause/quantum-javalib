package com.denizensoft.jlib;

import java.util.HashMap;
import java.util.Map;

public class Scope
{
	protected Scope mParentScope = null;

	protected Map<String,Node> mNodes = new HashMap<String,Node>();

	public void addNode(Node node)
	{
		mNodes.put(node.tag(), node);
		node.addedToScope(this);
	}

	public Function findFunction(String stTag) throws LibException
	{
		Node node = findNode(stTag);

		if(!( node instanceof Function))
			throw new TypeErrorException(String.format("'%s' is not a function!",stTag));

		return (Function)node;
	}

	public Node findNode(String stTag) throws LibException
	{
		Node n1 = mNodes.get(stTag);

		if(n1 == null)
		{
			if(mParentScope != null)
				n1 = mParentScope.findNode(stTag);
			else
				throw new NotFoundException(String.format("Node not found: %s", stTag));
		}
		return n1;
	}

	public Variable findVariable(String stTag) throws LibException
	{
		Node node = findNode(stTag);

		if(!( node instanceof Variable))
			throw new TypeErrorException(String.format("'%s' is not a variable!",stTag));

		return (Variable)node;
	}

	public void setVariable(String stTag,String stValue) throws LibException
	{
		try
		{
			mNodes.remove(findVariable(stTag));
		}
		catch(NotFoundException e)
		{
			// Do nothing...
		}

		addNode(new Variable(stTag, stValue));
	}

	public Scope(Scope parentScope)
	{
		this.mParentScope = parentScope;
	}
}
