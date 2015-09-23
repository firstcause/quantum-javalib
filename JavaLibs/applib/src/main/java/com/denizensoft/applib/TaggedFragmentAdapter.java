package com.denizensoft.applib;

import android.app.Activity;
import android.app.Fragment;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by sjm on 11/14/2014.
 */
public class TaggedFragmentAdapter extends FragmentPagerAdapter
{
	private class FragmentTag
	{
		public int nTitleStringId = 0;

		public Fragment item;

		public FragmentTag(int nStringId,Fragment f1)
		{
			nTitleStringId = nStringId;
			item = f1;
		}
	}

	private List<FragmentTag> tagged_fragments = new ArrayList<FragmentTag>();

	protected Activity mActivity = null;

	public void addTaggedFragment(int nStringId, Fragment f1)
	{
		tagged_fragments.add(new FragmentTag(nStringId,f1));
	}

	@Override
	public Fragment getItem(int nPosition)
	{
		Fragment f1 = tagged_fragments.get(nPosition).item;

		if(f1 != null )
			return f1;

		return null;
	}

	@Override
	public int getCount()
	{
		return tagged_fragments.size();
	}

	@Override
	public CharSequence getPageTitle(int nPosition)
	{
		Locale l = Locale.getDefault();

		int nTitleStringId = tagged_fragments.get(nPosition).nTitleStringId;

		return mActivity.getString(nTitleStringId).toUpperCase(l);
	}

	public TaggedFragmentAdapter(Activity activity)
	{
		super(activity.getFragmentManager());

		mActivity = activity;
	}
}
