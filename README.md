
------------------
| Project        |
------------------

[CSC library - Android](git@git.tyfone.blr:u4ia/androidcsclibrary.git)

-------------------------
| Description           |
-------------------------

This is an android library project that provides api's to interact with Tyfone's Connected SmartCard(CSC) present in various form factors having different communication channels for eg. SDIO, Bluetooth and BluetoothLE.


----------------------------
| Requirements             |
----------------------------

1. Android SDK
 - Download the [Android SDK ](http://developer.android.com/sdk/index.html)
 - Set the **ANDROID_HOME** environment variable pointing to the Android SDK installation
 - Include it in build path using command- ``` export PATH=$PATH:$ANDROID_SDK_HOME; ```
 
2. Android NDK
 - Download the [Android NDK ](http://developer.android.com/ndk/index.html)
 - Set the **ANDROID_NDK_HOME** environment variable pointing to the Android SDK installation
 - Include it in build path using command- ``` export PATH=$PATH:$ANDROID_NDK_HOME; ``` 

3. Gradle 
 - Install gradle 2.8 or above.
 - Set the **GRADLE_HOME** environment variable pointing to the Gradle directory
  
4. Sonar-Runner
 - Download the [Sonar-Runner](http://www.google.com/url?q=http%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fcodehaus%2Fsonar%2Frunner%2Fsonar-runner-dist%2F2.4%2Fsonar-runner-dist-2.4.zip&sa=D&sntz=1&usg=AFQjCNFeF7VxzT_NTGzjNf3un1FCfb57rQ) code analyzer
 - Set the **SONAR_RUNNER_HOME** environment variable pointing to the Sonar-Runner installation
 - Include it in build path using command- ``` export PATH=$PATH:$SONAR_RUNNER_HOME/bin ```
 - Configure sonar-runner.properties file
 - Make sure SonarQube server and MySql database are installed to store the code analysis report.

---------------------------------------
| Building the project                |
---------------------------------------
## Structure:

- `cscLib/` - Source and resource files of this project.
- `cscLib/androidTest/` - Unit tests for this project.

## Packaging
- For creating aar output run
  ```./gradlew cscLib:assemble``` or ```./gradlew build``` or ```./gradlew cscLib:build```.
- Packaged file `aar` will be generated in `cscLib/build/outputs/aar`.  
- To updated the package version name and code use **versionCode** and **versionName** as arguments.  
  For example to build csc library by passing version code and version name,  use command `./gradlew  assemble -PversionCode=1 -PversionName="1.0.1"`   


## Distribution
   For uploading binaries to nexus run `gradle uploadArchives -PbuildVersion=<Version>`


-------------------------------------------------------
| Running test cases & generate code coverage report  |
-------------------------------------------------------

## Running tests
   
 For running unit test cases run
  ```./gradlew connectedAndroidTest```


## Code coverage  
Test coverage reports for  can be found in `cscLib/build/reports/coverage`.

---------------------------------
| Java Documentation            |
--------------------------------- 
To generate java documentation for java source code use ```./gradlew  javadoc``` command. Java documentation will be generated in `cscLib/build/docs/javadoc` folder.


------------------------------------
| Running code analysis            |
------------------------------------

1. Make sure SonarQube server and database are installed and running.
2. Edit your sonar-runner configuration file to point to the SonarQube server and database.
3. Navigate to- *csc-library/*
4. Run the code analysis by running command- ``` sonar-runner ```
5. Check the report on SonarQube server.


