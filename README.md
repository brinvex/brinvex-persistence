# Brinvex Persistence

_Brinvex Persistence_ is a Java micro-library built on top of _JPA/Hibernate_,  
designed to simplify the implementation of data access layer components while  
remaining straightforward and focused on _PostgreSQL_ and _Microsoft SQL Server_.

````
<properties>
     <brinvex-persistence.version>1.0.0</brinvex-persistence.version>
</properties>

<repository>
    <id>repository.brinvex</id>
    <name>Brinvex Repository</name>
    <url>https://github.com/brinvex/brinvex-repo/raw/main/</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>

<dependency>
    <groupId>com.brinvex</groupId>
    <artifactId>brinvex-persistence-api</artifactId>
    <version>${brinvex-persistence.version}</version>
</dependency>
````

The library supports _JPMS_ and exports the module named ````com.brinvex.persistence````.

## Requirements

Java 17 or above, Hibernate ORM 6.6 or above.

### License

The _Brinvex Persistence_ is released under version 2.0 of the Apache License.
