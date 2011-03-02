*****************************
* droid4me framework library *
*****************************

Hereby is available the source code, binary version, documentation of the droid4me framework library, a library which accelerates application development on the Android platform.

Please, read the license "LICENSE.txt" attached to the hereby code source before any use! Except when explicitly expressed, the attached license is LGPL v3.

1. Build with Ant

  This Ant buildfile requires that you have included the following Ant library extensions:
  * Ant-Contrib: http://ant-contrib.sourceforge.net/
  * xmltask: http://www.oopsconsultancy.com/software/xmltask/

  Run "ant build".

  Produced jar is in "dist" directory: dist/com.smartnsoft.droid4me.jar

  TODO: update Ant script to use libraries from Android install instead of copy in libs/

2. Build with Maven

  Define an environment variable "ANDROID_HOME" pointing to your Android SDK install.
    Linux/Mac OS X, run: "export ANDROID_HOME=/path/to/your/AndroidSDK/"
    Windows, run: "set ANDROID_HOME=c:\path\to\your\AndroidSDK\"

  Package droid4me:
    mvn clean package

  Install droid4me in your local Maven repository (for later use from another project):
    mvn clean install

  Deploy droid4me into Nuxeo repository (requires deployment rights):
    mvn clean deploy

  Produced jar is in "target" directory: target/droid4me-2.0-SNAPSHOT.jar

  Download droid4me from online repository:
    https://maven.nuxeo.org/nexus/index.html#nexus-search;quick~droid4me

