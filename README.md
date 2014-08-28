##fb-contrib Eclipse quick fix plugin (fb-contrib quickfixes, for short)##
This repository extends the quick-fixes offered by the [FindBugs](http://findbugs.sourceforge.net/) [Eclipse Plugin](http://findbugs.sourceforge.net/downloads.html) to cover the bugs detected by [fb-contrib](http://fb-contrib.sourceforge.net/).

Currently, the FindBugs Eclipse Plugin only has support for extension in the nightly builds, but the public feature will be released soon.  Thus, I'm getting a head start on preparing custom quick-fixes.

Because the aforementioned extension point is [only in dev](https://code.google.com/p/findbugs/source/detail?r=491d7f9cae6cef8919f0d76104dd567d8489db06), I've included an unofficial release of the plugin (through revision dd3f35c74eb8) under [releases](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases).  I've dubbed this release 3.0.1, and it is the current minimum version required for fb-contrib quickfixes.  This prereq will be updated when the official release happens.

##Supported quickfixes##
See the [FindBugs](http://findbugs.sourceforge.net/bugDescriptions.html) and [fb-contrib](http://fb-contrib.sourceforge.net/bugdescriptions.html) bug description pages for more info

**fb-contrib**
- CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET
- CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME
- SPP_USE_BIGDECIMAL_STRING_CTOR
- SPP_USE_ISNAN
- LSC_LITERAL_STRING_COMPARISON

**FindBugs**
- DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE
- FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER


##Installing the plugin##
This project isn't quite ready for release yet.  But, if you really want, check out one of the [pre-releases](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/releases).  This works for Eclipse 3.6+.

1. Extract both the fb-contrib-quick-fixes-eclipse-0.X.Y.zip and the findbugs-eclipse-plugin-3.0.1.zip to their own folders. 
2. Open Eclipse.  Navigate to Help>Install New Software.  Add a repository, select Local and then navigate to the site folders that were extracted.  Install the FindBugs plugin and then the fb-contrib plugin, restarting in between.


##Setting up the project for development##
1. Download/install [Eclipse](https://www.eclipse.org/home/index.php), ideally 4.3 (Kepler) or newer.  The standard release (for Java) will work fine.
2. Install findbugs-eclipse-plugin-3.0.1.zip, following the above instructions.  Don't install the compiled fb-contrib-quick-fixes unless you want to unlock the optional step 11.
3. Clone findbugs : `git clone https://code.google.com/p/findbugs/`  [GitHub for Windows](https://windows.github.com/) or [GitHub for Mac](https://mac.github.com/) are good clients if you don't already have one.  There are a lot of folders associated with that project, but you only need the `findbugs` folder.
4. Open Eclipse.  File>Import and then choose "Existing projects into workspace", and find the `findbugs` folder you just cloned.
5. Clone and import [fb-contrib](https://github.com/mebigfatguy/fb-contrib) into Eclipse.  Follow the readme to get it setup to build.
6. Build fb-contrib by running the ant script `build.xml`.
7. Copy `fb-contrib-6.1.0.jar` (or later) to `[Eclipse folder]\plugins\edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20140817-dd3f35c\plugin` where [Eclipse folder] is wherever you installed/extracted Eclipse to.
8. [Fork/Clone](https://help.github.com/articles/fork-a-repo) this git repo. Import this project into Eclipse.  There should be no compile errors
9. (optional) Run [build.xml](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/blob/master/build.xml) as an Ant Build, which should build and create an update site under /bin_build/site.
11. (optional) Run MANIFEST.MF as an Eclipse Application.  Under one of the included launch configurations, the plugin should work just fine, but you might need to make sure both findbugs plugins are selected.
![image](https://cloud.githubusercontent.com/assets/6819944/4005374/9bd7dfa8-2990-11e4-81d2-a6ce8ed75452.png)If you don't see the included Launch Configurations under the green Run button, you may need to refresh the projects, or browse Run Configurations.  Launch 3.x was tested on Indigo and Launch 4.x was tested on Kepler.
10. (optional, see step 2) As step 10, just make sure the Run Configuration is setup such that fb-contrib-quick-fixes is not selected under **Target Platform**

###Troubleshooting dev environment setup###
`Execute failed: java.io.IOException: Cannot run program "git"`: Assuming you have installed git, you may have to [add git to your External Tools Configurations](http://stackoverflow.com/a/3196633/1447621)

###Development notes###
This project comes with a preconfigured Eclipse formatter setting.  These are the same settings used in the FindBugs Eclipse project.  Try not to mix feature additions with formatting corrections - make a standalone "Formatting" commit.

##License##
[FindBugs](http://findbugs.sourceforge.net/downloads.html), the [FindBugs Eclipse plugin](http://findbugs.sourceforge.net/downloads.html) and fb-contrib are all released under the [LGPL license](https://tldrlegal.com/license/gnu-lesser-general-public-license-v2.1-(lgpl-2.1)#fulltext).  This project relies on the compiled, released versions of each of those libraries.  These libraries, and their source code, can be found by following the links provided.

**This project** is released under the [MIT license](https://tldrlegal.com/license/mit-license#fulltext).  It's a very loose license, allowing liberal reuse/modification.