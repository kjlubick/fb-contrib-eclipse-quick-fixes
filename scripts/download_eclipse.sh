
if [ ! -f "$HOME/eclipse-tar/eclipse.tar.gz" ]; then
  wget https://github.com/kjlubick/eclipse-wercker-box/releases/download/0.0.1/eclipse-luna.tar.gz -O $HOME/eclipse-tar/eclipse.tar.gz
  ls -lh $HOME/eclipse-tar
fi

tar -C $HOME -zxf $HOME/eclipse-tar/eclipse.tar.gz
ls -lh $HOME
ls -lh $HOME/eclipse
ls -lh $HOME/eclipse/eclipse

$HOME/eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director     -repository http://download.eclipse.org/eclipse/updates/4.4/ -installIU org.eclipse.osgi.compatibility.plugins.feature.feature.group

$HOME/eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director  -repository http://download.eclipse.org/eclipse/updates/4.4/ -installIU org.eclipse.pde.junit.runtime


mv ./scripts/travisci.properties ./local.properties