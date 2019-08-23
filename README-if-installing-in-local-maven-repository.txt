If you're installing this module in a local maven repository, 
run mvn package install from the main project directory not the 
territorium.core directory (even though you probably only need core) 
as territorium.core will not get the correct path in the local maven
repository otherwise.