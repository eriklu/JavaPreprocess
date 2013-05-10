This project code come from the open source project Antenna. u can find the Antenna 
project in http://antenna.sourceforge.net/index.php. 

The license file J2ME_POLISH_DEVICE_DATABASE_LICENSE、LICENSE、LICENSE-EPL、LICENSE-LGPL 
come from antenna project. you can read them to get the licence information.

you user antenna as a toolkit, and rarely use the source code in your project. so you need
not worry about the copyright.

Ant Build.xml example:

<?xml version="1.0" encoding="UTF-8"?>
<project name="test" default="preprocess" basedir=".">
    <property name="version" value="1.0" />
    <target name="preprocess" >
         <property name="ooommm" value="true" />
        <wtkprocess srcdir="./src/test"
            destdir="./bin"
            symbols="var1,var2=lll,var3=false"
            debuglevel="info"
            printsymbols="true"
            verbose="false" 
            filter="true"
               />
    </target>
    <taskdef name="wtkprocess" classname="de.pleumann.antenna.WtkPreprocess" classpath="/Users/aoro/work/git/out/antenna_preprocess.jar"/> 
</project>


in java code, ${abc} will cause ant to replace abc with property abc, if not found abc property, will use abc symbol to replace.
