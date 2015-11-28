if [[ $# -eq 0 ]]
then
	arg1="clean"
	arg2="build"
	arg3="artifactoryPublish"
elif [[ "$1" -eq "clearcache" ]]
then
	find . -name "libraries" -type d -exec rm -frv "{}/*.xml" \;
	find . -name "build" -type d -exec rm -frv "{}" \;
	find ~/.gradle/ -name "com.denizensoft*" -type d -exec rm -frv "{}" \;
	exit 0
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


