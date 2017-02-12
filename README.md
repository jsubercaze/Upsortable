# Install the environment

* Install [ant](https://ant.apache.org/bindownload.cgi)
* Clone the repository
* Run 'ant dist' to create the lombok.jar
* Double click the lombok.jar and patch your Eclipse
* Run 'ant eclipse' to prepare the project
* Import the project into Eclipse

# Development process

the process is quite tedious and requires Eclipse restart after each modification ! 
to ease its purpose, use the script that copy the lombok.jar int the Eclipse directory.




#Original Readme

Project Lombok makes java a spicier language by adding 'handlers' that know how to build and compile simple, boilerplate-free, not-quite-java code.
See LICENSE for the Project Lombok license.


To start, run:

ant -projecthelp

HINT: If you'd like to develop lombok in eclipse, run 'ant eclipse' first. It creates the necessary project infrastructure and downloads dependencies. Note that, in order to run "LombokizedEclipse.launch", you need to have "Eclipse SDK" installed.

For a list of all authors, see the AUTHORS file. 

Project Lombok was started by: 

Reinier Zwitserloot
twitter: @surial
home: http://zwitserloot.com/

Roel Spilker
