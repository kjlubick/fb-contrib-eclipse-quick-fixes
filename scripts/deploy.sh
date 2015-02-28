build_version=`cat version.txt`
echo "build version = "$build_version
versionJson=`curl -X GET -u kjlubick:$API_KEY https://api.bintray.com/packages/kjlubick/fb-contrib-eclipse-quickfixes/fb-contrib-eclipse-quickfixes/versions/_latest`
#echo $versionJson
current_version=`echo $versionJson | grep -oP '"name":"(.*?)",' | grep -oP '[0-9\\.]+'`
echo "deployed version = "$current_version
if [ "$build_version" != "$current_version" ]; then
   #create new version
   response=`curl -X POST -u kjlubick:$API_KEY  -H "Content-Type: application/json" -d '{"name":"'"$build_version"'","desc":"For more release information, see https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases"}'  https://api.bintray.com/packages/kjlubick/fb-contrib-eclipse-quickfixes/fb-contrib-eclipse-quickfixes/versions`
   echo $response
   if [[ $response != *"created"* ]]; then
      echo "Could not make new version "$build_version
      exit 1
   fi
   
   #updates metadata files
   curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/artifacts.jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/artifacts.jar
   curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/content.jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/content.jar
   
   findbugs_plugin_jar=`ls -1 output-site/features/edu.umd.cs.findbugs.plugin.eclipse* | grep -o edu.*`
   eclipse_plugin_jar=`ls -1 output-site/features/fb-contrib-eclipse-quick-fixes* | grep -o fb-contrib.*`
   echo $findbugs_plugin_jar
   echo $eclipse_plugin_jar
   
   #pushes findbugs plugin - probably already exists, but that's okay
   curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/features/$findbugs_plugin_jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/features/$findbugs_plugin_jar -H "X-Bintray-Package: fb-contrib-eclipse-quickfixes" -H "X-Bintray-Version: $current_version"
   curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/plugins/$findbugs_plugin_jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/plugins/$findbugs_plugin_jar -H "X-Bintray-Package: fb-contrib-eclipse-quickfixes" -H "X-Bintray-Version: $current_version"
   
   #pushes fb-contrib-eclipse-quickfixes plugin - fail if it doesn't exist
   response=`curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/features/$eclipse_plugin_jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/features/$eclipse_plugin_jar -H "X-Bintray-Package: fb-contrib-eclipse-quickfixes" -H "X-Bintray-Version: $current_version"`
   echo $response
   if [[ $response != *"success"* ]]; then
      echo "Failed to upload output-site/features/"$eclipse_plugin_jar
      exit 2
   fi
   response=`curl -X PUT -u kjlubick:$API_KEY --data-binary @output-site/plugins/$eclipse_plugin_jar https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/plugins/$eclipse_plugin_jar -H "X-Bintray-Package: fb-contrib-eclipse-quickfixes" -H "X-Bintray-Version: $current_version"`
   echo $response
   if [[ $response != *"success"* ]]; then
      echo "Failed to upload output-site/plugins/"$eclipse_plugin_jar
      exit 3
   fi
   
   #curl -X POST -u kjlubick:$API_KEY https://api.bintray.com/content/kjlubick/fb-contrib-eclipse-quickfixes/fb-contrib-eclipse-quickfixes/$current_version/publish
else
   echo "Not pushing new version because we are on the same version as bintray"
fi