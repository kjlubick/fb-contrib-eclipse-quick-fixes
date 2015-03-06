
if [ ! -f "$HOME/eclipse-tar/eclipse.tar.gz" ]; then
  wget 'https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/luna/SR2/eclipse-java-luna-SR2-linux-gtk-x86_64.tar.gz&r=1' -O $HOME/eclipse-tar/eclipse.tar.gz
  ls -lh $HOME/eclipse-tar
fi

tar -C $HOME -zxf $HOME/eclipse-tar/eclipse.tar.gz
ls -lh $HOME
ls -lh $HOME/eclipse

echo "Installing org.eclipse.osgi.compatibility.plugins.feature.feature.group"
$HOME/eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director     -repository http://download.eclipse.org/eclipse/updates/4.4/ -installIU org.eclipse.osgi.compatibility.plugins.feature.feature.group

echo "Installing org.eclipse.pde.junit.runtime"
$HOME/eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director  -repository http://download.eclipse.org/eclipse/updates/4.4/ -installIU org.eclipse.pde.junit.runtime


mv ./scripts/travisci.properties ./local.properties
ls -rlh $JAVA_HOME
cp $JAVA_HOME/lib/rt.jar ./testresources/rt17.jar