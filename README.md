# Center-To-Center Message Validation Tool
The Center-To-Center Message Validation Tool (C2C-MVT) is an application used to test whether or not messages defined by a C2C Standard conform to the data format specified by the standard. The intent is to use common interfaces and methods to be able to validate messages for many C2C Standards. The first standard being implemented is the Next Generation Traffic Management Data Dictionary (ngTMDD).

## Minimum Hardware Requirements
2 GB of memory
64 MB of disk space
Windows 10 or higher | Ubuntu Linux 20.04 LTS or higher | Red Hat Enterprise Linux 7 or higher

## Developer Instructions
Clone the repository and create a Maven project using your IDE of choice. The embedded webserver runs on port 3116 by default. To change this port edit the server.port properties in the c2c-mvt\src\resources\application.properties file.

## User Instructions
In this release we added a windows executable for users, simply download the execuatble and open it, then open a browser and go to localhost:3116,
the MVT should be opened and ready.

## Build/Test
Use the Maven Command "install" or your IDE's built in Maven capabilities to initiate the build and test process. 
```
<path to repo>\c2c-mvt\mvnw.cmd install -f <path to repo>\c2c-mvt\pom.xml
```

There are unit tests that run as part of the process. The output of the build process will detail the test that are ran and if they are successful.

## Run
Use Java to start the application. Once the application is running open a browser and go to http://localhost:3116/ to access the User Interface.
```
java -jar <path to repo>\c2c-mvt\target\c2c-mvt.jar
```

To start the application with a port 8082 open for debugging use the following command:
```
java -agentlib:jdwp=transport=dt_socket,address=8082,server=y,suspend=n -jar <path to repo>\c2c-mvt\target\c2c-mvt.jar
```

