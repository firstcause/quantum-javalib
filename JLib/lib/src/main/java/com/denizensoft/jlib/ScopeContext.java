package com.denizensoft.jlib;

import java.util.Stack;

/**
 * Created by sjm on 1/29/2015.
 */
public class ScopeContext
{
	private Scope mGlobalScope;

	private Stack<Scope> mScopeStack;

	public Scope globalScope()
	{
		return mGlobalScope;
	}

	public void popScope()
	{
		mScopeStack.pop();
	}

	public void pushScope()
	{
		mScopeStack.push(new Scope(mScopeStack.peek()));
	}

	public Scope scope()
	{
		return mScopeStack.peek();
	}

	public Stack<Scope> scopeStack()
	{
		return mScopeStack;
	}

	public void setVariable(String stTag,String stValue) throws LibException
	{
		mScopeStack.peek().setVariable(stTag,stValue);
	}

	public String valueOf(String stTag) throws LibException
	{
		String stResult = mScopeStack.peek().findVariable(stTag).invoke(mScopeStack.peek(),null);

		return stResult;
	}

	public ScopeContext()
	{
		mScopeStack = new Stack<Scope>();

		mGlobalScope = new Scope(null);

		mScopeStack.push(mGlobalScope);
	}
}
