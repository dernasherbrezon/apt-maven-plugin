# maven-apt-plugin

This plugin deploys .deb artifacts into maven distribution repository. 

## 

## Usage

To publish .deb artifacts into apt-repository a distribution section must be configured:

```
<distributionManagement>
    <repository>
      <id>apt-repository</id>
      <name>Maven artifact repository to act as apt-repository</name>
      <url>https://example.com/release</url>
    </repository>
</distributionManagement>
```

Then configure maven repository to search for `maven-apt-plugin`:

```
<pluginRepositories>
  <pluginRepository>
    <id>maven-apt-plugin repository</id>
    <url>https://raw.githubusercontent.com/dernasherbrezon/maven-apt-plugin/mvn-repo/</url>
  </pluginRepository>
</pluginRepositories>
```

Finally configure `maven-apt-plugin`:

```
<plugins>
...
  <plugin>
    <groupId>com.st.maven</groupId>
    <artifactId>maven-apt-plugin</artifactId>
    <version>1.0</version>
    <executions>
      <execution>
        <id>deploy</id>
        <goals>
          <goal>deploy</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <component>main</component> <!-- Required. Example: main restricted universe. -->
      <codename>repo</codename> <!-- Required. Example: mycompany, repo -->
      <skip>false</skip> <!-- Not required. By default: false -->
    </configuration>
  </plugin>
...
</plugins>
```

* component - type of repository. Ubuntu proposes the following naming for them:
  * main- Officially supported software. Recommended.
  * restricted - Supported software that is not available under a completely free license.
  * universe - Community maintained software, i.e. not officially supported software.
  * multiverse - Software that is not free.
* codename - codename of release. 


## Hints

Works perfectly with [aws-maven](https://github.com/spring-projects/aws-maven "aws-maven"). Just configure distribution section to use `s3://bucketname`.

If you want to publish only .deb artifacts, then turn standard deploy plugin off:

```
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-deploy-plugin</artifactId>
    <version>2.7</version>
    <configuration>
      <skip>true</true>
    </configuration>
  </plugin>
```


