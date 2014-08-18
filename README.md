##fb-contrib Eclipse quick fix plugin##
This repository extends the quick-fixes offered by the [FindBugs](http://findbugs.sourceforge.net/) [Eclipse Plugin](http://findbugs.cs.umd.edu/eclipse/) to cover the bugs detected by [fb-contrib](http://fb-contrib.sourceforge.net/).

Currently, the FindBugs Eclipse Plugin only has support for extension in the nightly builds, but the public feature will be released soon.  Thus, I'm getting a head start on preparing custom quick-fixes.

Because the aforementioned extension point is [only in dev](https://code.google.com/p/findbugs/source/detail?r=491d7f9cae6cef8919f0d76104dd567d8489db06), I've included an unofficial release of the plugin (through revision dd3f35c74eb8) under [releases](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases).  I've dubbed this release 3.0.1, and it is the current minimum version required for this project.  This prereq will be updated when the official release happens.


##Installing the plugin##
This project isn't quite ready for release yet.  But, if you really want, check out one of the [pre-releases](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases).  This works for Eclipse 3.6+.

1. Extract both the fb-contrib-quick-fixes-eclipse-0.X.Y.zip and the findbugs-eclipse-plugin-3.0.1.zip to their own folders. 
2. Open Eclipse.  Navigate to Help>Install New Software.  Add a repository, select Local and then navigate to the site folders that were extracted.  Install the FindBugs plugin and then the fb-contrib plugin, restarting in between.


##Setting up the project for development##
1. Download/install [Eclipse](https://www.eclipse.org/home/index.php), ideally 4.3 (Kepler) or newer.  The standard release (for Java) will work fine.
2. [Fork/Clone](https://help.github.com/articles/fork-a-repo) this git repo. [GitHub for Windows](https://windows.github.com/) or [GitHub for Mac](https://mac.github.com/) are good clients if you don't already have one.
3. Open Eclipse.  File>Import and then choose "Existing projects into workspace", and find the fb-contrib folder you just created.
4. (optional) Run [build.xml](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/blob/master/build.xml) as an Ant Build, which should build and create an update site under /bin_build/site.
5. (optional) Run MANIFEST.MF as an Eclipse Application.  Under the default configuration, the plugin should work just fine.

##License##
[FindBugs](http://findbugs.sourceforge.net/downloads.html), the [FindBugs Eclipse plugin](http://findbugs.sourceforge.net/downloads.html) and fb-contrib are all released under the [LGPL license](https://tldrlegal.com/license/gnu-lesser-general-public-license-v2.1-(lgpl-2.1)#fulltext).  This project relies on the compiled, released versions of each of those libraries.  These libraries, and their source code, can be found by following the links provided.

**This project** is released under the [MIT license](https://tldrlegal.com/license/mit-license#fulltext).  It's a very loose license, allowing liberal reuse/modification.