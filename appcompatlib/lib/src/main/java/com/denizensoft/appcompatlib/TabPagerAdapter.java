package com.denizensoft.appcompatlib;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class TabPagerAdapter extends FragmentStatePagerAdapter
{
	List<String> stTitles = new ArrayList<String>();

	List<Fragment> mFragments = new ArrayList<Fragment>();

	public void addFragment(String stTitle,Fragment fragment)
	{
		stTitles.add(stTitle);
		mFragments.add(fragment);
	}

	@Override
	public Fragment getItem(int position)
	{
		Fragment fragment = mFragments.get(position);

		return fragment;
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		return stTitles.get(position);
	}

	@Override
	public int getCount()
	{
		return mFragments.size();
	}

	public TabPagerAdapter(FragmentManager fm)
	{
		super(fm);
	}
}