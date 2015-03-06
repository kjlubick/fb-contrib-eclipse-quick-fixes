
if [ ! -f "$HOME/eclipse-tar/eclipse.tar.gz" ]; then
  mkdir $HOME/eclipse-tar
  wget https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases/download/0.4.0/eclipse-luna-linux-with-build-plugins.tar.gz -O $HOME/eclipse-tar/eclipse.tar.gz
  ls -lh $HOME/eclipse-tar
fi

gunzip -c $HOME/eclipse-tar/eclipse.tar.gz > $HOME
ls -lh $HOME
ls -lh $HOME/eclipse