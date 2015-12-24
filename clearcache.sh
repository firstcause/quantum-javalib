echo Clearing the cache...
find . -name "libraries" -type d -exec rm -frv "{}/*.xml" \;
find . -name "build" -type d -exec rm -frv "{}" \;
find ~/.gradle/ -name "com.denizensoft*" -type d -exec rm -frv "{}" \;
exit 0
