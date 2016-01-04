package com.denizensoft.droidlib;

import com.denizensoft.jlib.LibException;
import org.json.JSONException;

/**
 * Created by sjm on 12/5/15.
 */
public interface ApiMethod
{
	public void func(ApiContext ac) throws JSONException, LibException;
}

