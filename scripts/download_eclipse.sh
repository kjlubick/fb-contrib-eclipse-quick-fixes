if [ ! -d "../eclipse-relang" ]; then
  # Control will enter here if $DIRECTORY doesn't exist.
  wget http://git.eclipse.org/c/platform/eclipse.platform.releng.basebuilder.git/snapshot/R38M6PlusRC3G.tar.gz -O - | tar -xz --directory ../
  mv ../R38M6PlusRC3G ../eclipse-relang
  echo $(pwd)
fi