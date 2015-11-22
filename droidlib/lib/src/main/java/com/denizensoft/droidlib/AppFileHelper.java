package com.denizensoft.droidlib;

import android.content.Context;
import android.util.Log;

import com.denizensoft.jlib.LibException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by sjm on 5/14/2015.
 */
public class AppFileHelper
{
	protected Context mAppContext = null;

	protected File mAppFilesFolder = null;

	protected byte[]
				mBuffer1 = new byte[4096],
				mBuffer2 = new byte[4096];

	public int deleteFiles(File fsBaseFolder, String[] stFileSpecs)
	{
		int nDeleted = 0;

		for(int i=( stFileSpecs.length - 1 ); i >= 0; --i)
		{
			File file = new File(String.format("%s/%s",fsBaseFolder.getPath(),stFileSpecs[i]));

			if(file.exists())
			{
				if(file.delete())
				{
					Log.d("deleteFiles", "Deleted: " + file.getAbsolutePath());
					++nDeleted;
				}
				else
				{
					Log.d("deleteFiles","Couldn't delete: "+file.getAbsolutePath());
				}
			}
		}
		return nDeleted;
	}

	private String[] enumAssets(String stAssetSpec, String stEnumOffset) throws IOException
	{
		ArrayList<String> specArray = new ArrayList<String>();

		String s1;

		if(stEnumOffset != null)
			s1 = String.format("%s/%s",stAssetSpec,stEnumOffset);
		else
			s1 = stAssetSpec;

		String stAssetList[] = mAppContext.getAssets().list(s1);

		if(stAssetList.length == 0)
			return null;

		for(String s2: stAssetList)
		{
			String s3;

			if(stEnumOffset != null)
				s3 = String.format("%s/%s",stEnumOffset,s2);
			else
				s3 = s2;

			specArray.add(s3);

			try
			{
				InputStream inputStream = mAppContext.getAssets().open(String.format("%s/%s",stAssetSpec,s3));
				inputStream.close();
				continue;
			}
			catch(FileNotFoundException e)
			{
				// Do nothing, its a folder
				//
			}

			// enumerate the sub folders
			//
			String[] specs = enumAssets(stAssetSpec,s3);

			for(String s4: specs)
				specArray.add(s4);
		}
		return specArray.toArray(new String[specArray.size()]);
	}

	public boolean compareAssetToFile(String stAssetSpec,String stTargetSpec) throws IOException
	{
		int nRC1, nRC2;

		long nCount = 0;

		File fTarget = new File(stTargetSpec);

		Log.d("AppFile", String.format("Compare file: %s", fTarget.getPath()));

		InputStream inputStream1 = mAppContext.getAssets().open(stAssetSpec);

		InputStream inputStream2 = new FileInputStream(fTarget);

		while((nRC1 = inputStream1.read(mBuffer1)) != -1)
		{
			nCount += nRC1;

			nRC2 = inputStream2.read(mBuffer2);

			if(nRC2 !=  nRC1 || !Arrays.equals(mBuffer1, mBuffer2))
				return false;
		}

		inputStream1.close();
		inputStream2.close();

		if(fTarget.length() != nCount)
			return false;

		return true;
	}

	public void copyAssetFile(String stAssetSpec,String stTargetSpec, boolean bUpdateOnly) throws IOException
	{
		int nRC;

		File fTarget = new File(stTargetSpec);

		if(bUpdateOnly)
		{
			if(fTarget.isDirectory())
			{
				Log.d("AppFile", "Skipping update, target is a folder!");

				return;
			}

			if(fTarget.exists())
			{
				Log.d("AppFile", String.format("Comparing asset to file: %s", stTargetSpec));

				if(compareAssetToFile(stAssetSpec, stTargetSpec))
				{
					Log.d("AppFile", String.format("Identical, skipping: %s", stTargetSpec));
					return;
				}

				Log.d("AppFile", String.format("      Updating file: %s", stTargetSpec));
			}
			else
			{
				Log.d("AppFile", String.format("           New file: %s", stTargetSpec));
			}
		}

		InputStream inputStream = mAppContext.getAssets().open(stAssetSpec);

		OutputStream outputStream = new FileOutputStream(fTarget);

		Log.d("AppFile", String.format("            Copying: %s", stTargetSpec));

		while((nRC = inputStream.read(mBuffer1)) != -1)
		{
			outputStream.write(mBuffer1, 0, nRC);
		}

		inputStream.close();
		outputStream.flush();
		outputStream.close();
	}

	public void deployAssetsFolder(File fsBaseFolder, String stAssetFolder,boolean bUpdateOnly) throws IOException
	{
		String[] assets = enumAssetsAt(stAssetFolder);

		String stAssetSpec, stTargetSpec;

		for(String s1: assets)
		{
			stAssetSpec = String.format("%s/%s",stAssetFolder,s1);
			stTargetSpec = String.format("%s/%s",fsBaseFolder, s1);

			try
			{
				copyAssetFile(stAssetSpec,stTargetSpec,bUpdateOnly);
			}
			catch(FileNotFoundException e)
			{
				// Do nothing, its a folder
				//
				File fTarget = new File(stTargetSpec);

				if(!fTarget.mkdir())
					throw new IOException(String.format("Couldn't create folder: %s",stTargetSpec));
			}
		}
	}

	public String[] enumAssetsAt(String stAssetSpec) throws IOException
	{
		return enumAssets(stAssetSpec, null);
	}

	public String[] enumFiles(File fsBaseFolder, String[] stFolderSpecs) throws IOException
	{
		ArrayList<String> specArray = null;

		FilenameFilter fileFilter = new FilenameFilter()
		{
			@Override
			public boolean accept(File file, String s)
			{
				File test = new File(String.format("%s/%s",file.getPath(),s));

				if(test.isFile())
				{
					Log.d("app",String.format("Filter accepting file: %s",s));
					return true;
				}
				return false;
			}
		};

		if(fsBaseFolder == null)
			fsBaseFolder = mAppFilesFolder;

		for(String s1: stFolderSpecs)
		{
			File folder = new File(String.format("%s/%s",fsBaseFolder.getPath(),s1));

			String[] stFileList = folder.list(fileFilter);

			if(stFileList.length == 0)
				continue;

			if(specArray == null)
				specArray = new ArrayList<String>();

			for(String s2: stFileList)
				specArray.add(String.format("%s/%s",s1,s2));
		}

		if(specArray != null)
			return specArray.toArray(new String[specArray.size()]);

		return null;
	}

	public String[] enumFolders(File fsBaseFolder, String stFolderSpec) throws IOException
	{
		ArrayList<String> specArray = null;

		FilenameFilter folderFilter = new FilenameFilter()
		{
			@Override
			public boolean accept(File file, String s)
			{
				File test = new File(String.format("%s/%s",file.getPath(),s));

				if(test.isDirectory())
				{
					Log.d("app",String.format("Filter accepting folder: %s",s));
					return true;
				}
				return false;
			}
		};

		if(fsBaseFolder == null)
			fsBaseFolder = mAppFilesFolder;

		File folder = new File(String.format("%s/%s",fsBaseFolder.getPath(),stFolderSpec));

		if(folder.exists())
		{
			String[] stFolderList = folder.list(folderFilter);

			if(stFolderList.length == 0)
				return null;

			specArray = new ArrayList<String>();

			specArray.add(stFolderSpec);

			for(String s1: stFolderList)
			{
				String s2 = String.format("%s/%s",stFolderSpec,s1);

				specArray.add(s2);

				String[] specs = enumFolders(fsBaseFolder,s2);

				if(specs != null)
				{
					for(String s3 : specs)
						specArray.add(s3);
				}
			}
			return specArray.toArray(new String[specArray.size()]);
		}
		return null;
	}

	public File filesFolder()
	{
		return mAppFilesFolder;
	}

	public AppFileHelper(Context appContext) throws LibException
	{
		mAppContext = appContext;
		mAppFilesFolder = mAppContext.getFilesDir();
	}
}
