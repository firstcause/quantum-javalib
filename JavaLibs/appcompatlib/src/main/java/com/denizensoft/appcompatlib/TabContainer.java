package com.denizensoft.appcompatlib;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class TabContainer extends AppFragment
{
	private ViewPager mViewPager = null;

	private TabScrollView mTabScrollView = null;

	private ColorStateList tabStripColorStateList = null;

	abstract public PagerAdapter initPagerAdapter();

	abstract public void initTabScrollView(TabScrollView tabScrollView);

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view,savedInstanceState);

		mViewPager = (ViewPager)view.findViewById(R.id.view_pager);

		mTabScrollView = (TabScrollView)view.findViewById(R.id.tab_scroll_view);

		PagerAdapter pagerAdapter = initPagerAdapter();

		if(pagerAdapter != null)
		{
			setTabPagerAdapter(pagerAdapter);

			initTabScrollView(mTabScrollView);

			mTabScrollView.setViewPager(mViewPager);
		}
	}

	@Override
	protected int getFragmentLayout()
	{
		int nLayout = super.getFragmentLayout();

		if(nLayout == 0)
		{
			nLayout = R.layout.fragment_tab_container;
		}
		return nLayout;
	}

	public TabScrollView tabScrollView()
	{
		return mTabScrollView;
	}

	public void setTabPagerAdapter(PagerAdapter adapter)
	{
		mViewPager.setAdapter(adapter);

		mTabScrollView.setViewPager(mViewPager);
	}

	public TabContainer()
	{
	}
}
