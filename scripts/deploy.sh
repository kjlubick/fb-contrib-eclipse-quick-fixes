build_version=`cat version.txt`
echo $build_version
versionJson=`curl -X GET -u kjlubick:$API_KEY https://api.bintray.com/packages/kjlubick/fb-contrib-eclipse-quickfixes/fb-contrib-eclipse-quickfixes/versions/_latest`
#echo $versionJson
current_version=`echo $versionJson | grep -oP '"name":"(.*?)",' | grep -oP '[0-9\\.]+'`
echo $current_version
if [ "$build_version" != "$current_version" ]; then
   #create new version
   curl -X POST -u kjlubick:$API_KEY  -H "Content-Type: application/json" -d '{"name":"'"$build_version"'","desc":"For more release information, see https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases"}'  https://api.bintray.com/packages/kjlubick/fb-contrib-eclipse-quickfixes/fb-contrib-eclipse-quickfixes/versions

fi	