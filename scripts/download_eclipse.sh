
if [ ! -f "$HOME/eclipse-tar/eclipse.tar.gz" ]; then
  wget https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases/download/0.4.0/eclipse-luna-linux-with-build-plugins.tar.gz -O $HOME/eclipse-tar/eclipse.tar.gz
  ls -lh $HOME/eclipse-tar
fi

tar -c $HOME -zxf $HOME/eclipse-tar/eclipse.tar.gz
ls -lh $HOME
ls -lh $HOME/eclipse