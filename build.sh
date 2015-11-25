pushd droidlib
gradle --daemon --refresh-dependencies build artifactoryPublish
popd

pushd dbclient
gradle --daemon --refresh-dependencies build artifactoryPublish
popd

pushd IabLib-v17
gradle --daemon --refresh-dependencies build artifactoryPublish
popd

pushd appcompatlib
gradle --daemon --refresh-dependencies build artifactoryPublish
popd


