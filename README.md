# Setup
## Upload secure connect bundle under CASS-STAC folder
## Updated cassandra connection details in config.properties

## Updated build section in pom.xml as below

<build>
    <plugins>
      <plugin>
        <inherited>true</inherited>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.10.1</version>
        <configuration>
          <source>17</source>
          <target>17</target>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <version>3.1.1</version>
        <configuration>
            <descriptorRefs>
                <descriptorRef>jar-with-dependencies</descriptorRef>
            </descriptorRefs>
        </configuration>
        <executions>
          <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
</build>

## execute below command to run application
java -cp target/cass-stac-0.1-SNAPSHOT-jar-with-dependencies.jar com.datastax.oss.cass_stac.App