##fb-contrib Eclipse quick fix plugin (fb-contrib quickfixes, for short) [![Build Status](https://travis-ci.org/kjlubick/fb-contrib-eclipse-quick-fixes.svg?branch=master)](https://travis-ci.org/kjlubick/fb-contrib-eclipse-quick-fixes) ##
This repository extends the quick-fixes offered by the [FindBugs](http://findbugs.sourceforge.net/) [Eclipse Plugin](http://findbugs.sourceforge.net/downloads.html) to cover the bugs detected by [fb-contrib](http://fb-contrib.sourceforge.net/), [Find Security Bugs](http://h3xstream.github.io/find-sec-bugs/) and FindBugs itself.

![quickfix-demo](https://cloud.githubusercontent.com/assets/6819944/4264324/b5e2f5ba-3c21-11e4-966b-3264f7e22dae.gif)


Currently, the FindBugs Eclipse Plugin only has support for extension in the nightly builds, but the public feature will be released soon.  Thus, I'm getting a head start on preparing custom quick-fixes.


##Installing the plugin##
To install this project, open Eclipse, go to Help>Install New Software, and copy in the following link:

`http://dl.bintray.com/kjlubick/fb-contrib-eclipse-quickfixes/`

Uncheck "Group items by category" to see the plugin.

![image](https://cloud.githubusercontent.com/assets/6819944/6541841/bd15e5b6-c4b3-11e4-9599-c84c4bc8a033.png)


Because the aforementioned extension point is [only in dev](https://code.google.com/p/findbugs/source/detail?r=491d7f9cae6cef8919f0d76104dd567d8489db06), I have included an unofficial release of FindBugs called 3.0.1.
This is the minimum version required for this plugin.

Fb-contrib quickfixes has been tested on Eclipe Kepler and Luna.


##Supported quickfixes##
See the [FindBugs](http://findbugs.sourceforge.net/bugDescriptions.html) and [fb-contrib](http://fb-contrib.sourceforge.net/bugdescriptions.html) bug description pages for more info

**fb-contrib**  
- ACEM_ABSTRACT_CLASS_EMPTY_METHODS
- CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET
- CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME
- COM_COPIED_OVERRIDDEN_METHOD
- HCP_HTTP_REQUEST_RESOURCES_NOT_FREED_LOCAL
- LO_SUSPECT_LOG_CLASS
- LSC_LITERAL_STRING_COMPARISON
- MDM_RANDOM_SEED
- NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION
- NAB_NEEDLESS_BOXING_PARSE
- OCP_OVERLY_CONCRETE_PARAMETER
- SPP_CONVERSION_OF_STRING_LITERAL
- SPP_EQUALS_ON_ENUM
- SPP_USE_BIGDECIMAL_STRING_CTOR
- SPP_USE_ISEMPTY
- SPP_USE_ISNAN
- UCPM_USE_CHARACTER_PARAMETERIZED_METHOD
- UEC_USE_ENUM_COLLECTIONS
- USBR_UNNECESSARY_STORE_BEFORE_RETURN
- UVA_USE_VAR_ARGS

**FindBugs**
- CO_COMPARETO_INCORRECT_FLOATING
- DLS_DEAD_LOCAL_STORE_SHADOWS_FIELD
- DLS_DEAD_LOCAL_STORE
- DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE
- DMI_INVOKING_TOSTRING_ON_ARRAY
- FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER
- ITA_INEFFICIENT_TO_ARRAY
- RV_RETURN_VALUE_IGNORED
- RV_RETURN_VALUE_IGNORED_BAD_PRACTICE
- SE_BAD_FIELD
- SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH
- SF_SWITCH_NO_DEFAULT
- SQL_BAD_PREPARED_STATEMENT_ACCESS
- SQL_BAD_RESULTSET_ACCESS
- VA_FORMAT_STRING_BAD_CONVERSION
- VA_FORMAT_STRING_BAD_CONVERSION_FROM_ARRAY
- VA_FORMAT_STRING_BAD_CONVERSION_TO_BOOLEAN
- VA_FORMAT_STRING_USES_NEWLINE
- WMI_WRONG_MAP_ITERATOR

**Find Security Bugs**
- PREDICTABLE_RANDOM

##Simultaneous Quickfixes##
Do you have many bugs detected with a quickfix? The base FindBugs plugin supports multiple fixes simultaneously:
![multiple-quickfixes](https://cloud.githubusercontent.com/assets/6819944/4324949/7e882a7c-3f5f-11e4-9170-bac5b24c2dbc.gif)

##Setting up the project for development##
1. Download/install [Eclipse](https://www.eclipse.org/home/index.php) Luna (4.4) or newer.  The Eclipse for RCP and RAP Developers will work fine.
2. **Install** findbugs-eclipse-plugin-3.0.1 from bintray, following the above instructions.
3. Clone findbugs : `git clone https://code.google.com/p/findbugs/`  [GitHub for Windows](https://windows.github.com/) or [GitHub for Mac](https://mac.github.com/) are good clients if you don't already have one.  There are a lot of folders and projects associated with the root project, but you only need the `findbugs`.  You may optionally include the `findBugsEclipsePlugin` project.
4. Open Eclipse.  File>Import and then choose "Existing projects into workspace", and find the `findbugs` project (not the root project) you just cloned.  You may have to run the build.xml in `findbugs` and `findBugsEclipsePlugin` to get the lib jars copied over, if you imported that project as well.
5. Clone and import [fb-contrib](https://github.com/mebigfatguy/fb-contrib) into Eclipse.  Follow the readme to get it setup to build.
6. Build fb-contrib by running the ant script `build.xml`.
7. Copy `fb-contrib-6.1.0.jar` (or later) to `[Eclipse folder]\plugins\edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20140817-dd3f35c\plugin` where [Eclipse folder] is wherever you installed/extracted Eclipse to.
8. [Fork/Clone](https://help.github.com/articles/fork-a-repo) this git repo. Import this project into Eclipse.  You should have now imported 3 or 4 projects, `fb-contrib`, `fb-contrib-eclipse-quick-fixes`, `findbugs` and [optionally] `findBugsEclipsePlugin`. There should be no compile errors.

###Deploying fb-contrib quickfixes from Eclipse###
1. There are two default Run Configurations for Eclipse.  If you don't see the included Launch Configurations under the green Run button, you may need to refresh the projects, or browse Run Configurations.  Launch 3.x was tested on Indigo and Launch 4.x was tested on Kepler and Luna.  Don't run either yet.

2. Make sure the desired Run Configuration is setup such that both findbugs plugins are selected under **Target Platform**.  If you installed the compiled fb-contrib-eclipse-plugin, make sure *only* the version under Workspace is selected, else you may wonder why your features don't show up. ![image](https://cloud.githubusercontent.com/assets/6819944/4005374/9bd7dfa8-2990-11e4-81d2-a6ce8ed75452.png)

3. You will be able to run either Fb-Contrib-Eclipse configuration.

###Building the project as an update site to deploy###
1. Make a copy of [local.properties.example](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/blob/master/local.properties.example) and name it `local.properties`.  In that file,  point 'build.eclipse.dir' to a clean Eclipse (Luna) installation that you want to use to build the plugin.  You may also update `test.eclipse.dir` to point to a clean Eclipse installation that you want to use to test, which can be an older version if you want. 
I build with Luna and test on Kepler on my local machine.  Travis CI uses Luna for both stages.

2. Run [build.xml](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/blob/master/build.xml) as an Ant Build, which should build and create a p2 update site under ./output-site.

###Running unit tests from Ant###
Execute the `test-headless` target in build.xml.  Assuming your local.properties are setup (as above), it should fire up an instance of Eclipse and run the tests.


###Troubleshooting dev environment setup###
`Execute failed: java.io.IOException: Cannot run program "git"`: Assuming you have installed git, you may have to [add git to your External Tools Configurations](http://stackoverflow.com/a/3196633/1447621)

###Development notes###
This project comes with a preconfigured Eclipse formatter setting.  These are the same settings used in the FindBugs Eclipse project.  Try not to mix feature additions with formatting corrections - make a standalone "Formatting" commit.

Additionally, the launch configuration will use the workspace version of fb-contrib, but the installed version of the FindBugs Eclipse Plugin. You can easily make a launch configuration that uses the version from your workspace if you want.  

Unit tests are encouraged.  Write some buggy code (preferably sticking to one family of bugs), place it in [classesToFix](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/tree/master/classesToFix) and a fixed version in [fixedClasses](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/tree/master/fixedClasses).  Then, add a test case to [TestContributedQuickFix.java](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/blob/7cbb5cb7a5a77436b626e76212b742c0a763b302/test/tests/TestContributedQuickFixes.java) - look at the other test cases for examples of how to use the high-level assertion TestHarness.

To run the tests, you'll need to find rt.jar from your JAVA_HOME/lib and copy it to [testResources](https://github.com/kjlubick/fb-contrib-eclipse-quick-fixes/tree/master/testresources).  For Java 1.7, name it rt17.jar, for Java 1.8, rt18.jar, and so on.  This allows the test code to compile.

Not sure where to start?  How about [this tutorial on making a quickfix](http://kjlubick.github.io/blog/post/3?building-your-first-eclipse-quick-fix) and [its follow up tutorial](http://kjlubick.github.io/blog/post/4?a-slightly-more-advanced-quickfix)?

##License##
[FindBugs](http://findbugs.sourceforge.net/downloads.html), the [FindBugs Eclipse plugin](http://findbugs.sourceforge.net/downloads.html), [fb-contrib](https://github.com/mebigfatguy/fb-contrib), [Find Security Bugs](https://github.com/h3xstream/find-sec-bugs), are all released under the [LGPL license](https://tldrlegal.com/license/gnu-lesser-general-public-license-v2.1-(lgpl-2.1)#fulltext).  This project relies on the compiled, released versions of each of those libraries.  These libraries, and their source code, can be found by following the links provided.

Some small pieces of the testing framework are redistributed (having been modified) under the [Eclipse Public License](https://tldrlegal.com/license/eclipse-public-license-1.0-(epl-1.0)).  Those source files have links to the originals.

**This project**, where not otherwise specified, is released under the [MIT license](https://tldrlegal.com/license/mit-license#fulltext).  It's a very loose license, allowing liberal reuse/modification.