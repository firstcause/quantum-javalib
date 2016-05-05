#!/usr/bin/env bash
echo Invoked with args: $1 $2 $3 $4

if [[ $# = 0 ]];
then
	arg1="clean"
	arg2="build"
	arg3="artifactoryPublish"
else
	arg1=$1
	arg2=$2
	arg3=$3
	arg4=$4
fi

pushd droidlib
gradle --daemon --refresh-dependencies $arg1 $arg2 $arg3 $arg4

if [ $? -eq 0 ]
then
	echo "DROIDLIB: Build succeeded..."
else
	echo "DROIDLIB: Build failed..."
	exit 1
fi

popd

pushd dbclient
gradle --daemon --refresh-dependencies $arg1 $arg2 $arg3 $arg4

if [ $? -eq 0 ]
then
	echo "DBCLIENT: Build succeeded..."
else
	echo "DBCLIENT: Build failed..."
	exit 1
fi

popd

pushd IabLib-v17
gradle --daemon --refresh-dependencies $arg1 $arg2 $arg3 $arg4

if [ $? -eq 0 ]
then
	echo "IABLIB: Build succeeded..."
else
	echo "IABLIB: Build failed..."
	exit 1
fi

popd

pushd appcompatlib
gradle --daemon --refresh-dependencies $arg1 $arg2 $arg3 $arg4

if [ $? -eq 0 ]
then
	echo "APPCOMPATLIB: Build succeeded..."
else
	echo "APPCOMPATLIB: Build failed..."
	exit 1
fi

popd


