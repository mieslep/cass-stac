# cass-stac-spring
Created of development of cass-stac using Springboot
The structure of the file structure is
 - pom.xml
 - src

The source folder under src are
 - main
   - java
     - com.datastax.oss.cass-stac
     - com.datastax.oss.cass-stac.controller
     - com.datastax.oss.cass-stac.service
     - com.datastax.oss.cass-stac.dao
     - com.datastax.oss.cass-stac.entity
     - com.datastax.oss.cass-stac.dto
     - com.datastax.oss.cass-stac.util


# Setup required to deploy the application in gitpod
 ## From gitpod open workspace with "https://github.com/Anant/cass-stac.git" repository
 ## Switch to cass-stac-spring branch
 ## Upload secure connect bundle under CASS-STAC folder
 ## Updated cassandra connection details in config.properties file
	datastax.astra.password=password # Should be astra token starting with AstraCS
 ## execute below command to run application
	java -jar target/stacapi-0.0.1-SNAPSHOT.jar
 ## Get rest api detail using below url from either curl or browser
	http://localhost:8080/swagger-ui/index.html