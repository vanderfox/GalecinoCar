This is a port of the Python based DonkeyCar project to Groovy and Java.

to build the management module:

- unzip particle-framework.zip into a working directory outside of the project
- cd into that directory and run ./gradlew jar publishToMavenLocal
- go back to the GalicenoCar directory
- run ./gradlew :management:build

to run the management module:
 - go into the GalicinoCar project
 - run ./gradlew :management:run
