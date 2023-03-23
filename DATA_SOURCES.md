Learn Jakarta EE Data Sources
=============================

This document contains the approaches one may take to configure data sources
in a Jakarta EE application.

Using @DataSourceDefinition
===========================
This is currently the way data sources are configured in this project. The reason
is that it provides the minimum amount of files and works across all the app
servers tested in this project. Here is a sample file:

```
package io.github.learnjakartaee.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Startup;

@DataSourceDefinition(
		name = "java:app/env/jdbc/appDataSource",
		className = "org.apache.derby.jdbc.EmbeddedDataSource",
		databaseName = "memory:appdb",
		user = "APP",
		password = "",
		properties = {
				"createDatabase=create"
		})
@Singleton
public class DataSourceConfiguration {

	@Resource(lookup="java:app/env/jdbc/appDataSource")
	DataSource dataSource;
	
	@Produces
	public DataSource getDatasource() {
		return dataSource;
	}	
}
```

The configuration defines a resource that can be used elsewhere in the application
and it also sets a JNDI Entry under a name that can be used in `persistence.xml`.

```
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
	version="3.0">
 
	<persistence-unit name="jdbcappDataSource" transaction-type="JTA">
		<jta-data-source>java:app/env/jdbc/appDataSource</jta-data-source>
	</persistence-unit>
	
</persistence>
```

TomEE, GlassFish and WildFly all are able to use database drivers that are included with the application.
However, Open Liberty does not use that classpath. To workaround this issue, one must set the drivers
in the global classpath by having this block in `server.xml` (where the variables refer to JAR files
in the file system.)

```
<library id="global">
	<file name="${derbypath}" />
	<file name="${derbyclientpath}" />
	<file name="${derbytoolspath}" />
	<file name="${derbysharedpath}" />
</library>
```

The pros of this approach include the simplicity. However, the main con is that the connection
information to the database is included in the code and is not externalized. Some app servers
support means of using environment variables, but the naming convention is inconsistent across
app servers. See this link for more information:

```
https://dplatz.de/blog/2018/self-contained-jee-app.html
```

Having to re-build an application to adjust connection information or pool settings is not
a position you want to be in,

Using persistence.xml
=====================
The schema for `persistence.xml` now includes a way to specify a database connection like so:

```
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
	version="3.0">
 
	<persistence-unit name="jdbcAppDataSource" transaction-type="JTA">
		<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
		<properties>
			<property name="jakarta.persistence.jdbc.driver"
				value="org.apache.derby.jdbc.EmbeddedDriver" />
			<property name="jakarta.persistence.jdbc.url"
				value="jdbc:derby:memory:appdb;create=true" />

			<property name="eclipselink.logging.level" value="INFO" />
			<property name="eclipselink.target-database" value="DERBY" />
			<property name="eclipselink.ddl-generation"
				value="drop-and-create-tables" />
		</properties>
	</persistence-unit>
	
</persistence>
```

This shares similar pros and cons as the `@DataSourceCOnfiguration` but only defines a connection
for use with an `EntityManager`.

Using web.xml
=============
The schema for `web.xml` also includes a way to specify a database connection like so:

```
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
	version="5.0">

	<data-source>
		<name>jdbc/appDataSourc</name>
		<class-name>org.apache.derby.jdbc.EmbeddedDataSource</class-name>
		<database-name>memory:appdb</database-name>
		<user>APP</user>
		<password></password>
		<property>
			<name>connectionAttributes</name>
			<value>create=true</value>
		</property>
	</data-source>

</web-app>
```

This defines a JNDI name at `java:comp/env/jdbc/appDataSource` that can be used to look up the connection.
I believe this name should be used in `persistence.xml` as well, however GlassFish does not interpret the
specification that way.

Likewise, this approach hard codes connection information that may or may not be easily changed without
rebuilding the application.


Using App Server Specific Configuration
=======================================
This approach is the original one. To begin, an application defines a outlet in `web.xml` like so:

```
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
	version="5.0">

	<resource-ref>
		<res-ref-name>jdbc/appDataSource</res-ref-name>
		<res-type>javax.sql.DataSource</res-type>
		<res-auth>Container</res-auth>
	</resource-ref>

</web-app>
```

That outlet now needs to be plugged by each app server and their approaches are different. But most
importantly, that configuration can be external to the WAR file, meaning code does not need to be
changed. This makes this approach best. For convenience, configuration files can be included with
the code. This approach is described below:

Open Liberty
------------
To map the name used in the application to the name used in Open Liberty, include a file named
`ibm.web.bnd.xml` in the `WEB-INF` directory as a sibling to `web.xml` that looks as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<web-bnd xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://websphere.ibm.com/xml/ns/javaee"
	xsi:schemaLocation="http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd"
	version="1.0">

	<resource-ref name="jdbc/appDataSource"
		binding-name="jdbc/appDataSource" />

</web-bnd>
```

The names can be different, but for sanity, everyone uses the same name.

Next, the database connection configuration is in `src/main/liberty/config/server.xml`

```
<?xml version="1.0" encoding="UTF-8"?>
<server description="jpa">

	<featureManager>
		<feature>jakartaee-9.1</feature>
		<feature>jndi-1.0</feature>
	</featureManager>

	<variable name="default.http.port" defaultValue="9080" />
	<variable name="default.https.port" defaultValue="9443" />

	<httpEndpoint id="defaultHttpEndpoint"
		httpPort="${default.http.port}" httpsPort="${default.https.port}" />
	
	<library id="derby">
		<file name="${derbypath}" />
		<file name="${derbyclientpath}" />
		<file name="${derbytoolspath}" />
		<file name="${derbysharedpath}" />
	</library>

	<!-- See persistence.xml and web.xml -->
	<dataSource id="appDataSource"
		jndiName="jdbc/appDataSource">
		<jdbcDriver libraryRef="derby" />
		<connectionManager minPoolSize="1"/>
		<properties.derby.embedded
			databaseName="memory:appdb"
			createDatabase="create"
			user="APP"
			password="" />
	</dataSource>

</server>
```

The JNDI name `java:comp/env/jdbc/appDataSource` is used to look up the data source and
can also be used in `persistence.xml`.

WildFly
-------
WildFly data sources are defined in files ending in `-ds.xml` such as `wildfly-ds.xml` or `wildfly-app-ds.xml`
should be located in the `WEB-INF` directory as a sibling of `web.xml`.

Here is an example:

```
<?xml version="1.0" encoding="UTF-8"?>
<datasources xmlns="http://www.jboss.org/ironjacamar/schema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.jboss.org/ironjacamar/schema http://docs.jboss.org/ironjacamar/schema/datasources_1_0.xsd">

	<!-- Wildfly DataSources -->

	<datasource jndi-name="jdbc/appDataSource" pool-name="DerbyDS">
		<connection-url>jdbc:derby:memory:appdb;create=true</connection-url>
		<driver>appdriver</driver>
		<pool></pool>
		<security>
			<user-name>APP</user-name>
			<password></password>
		</security>
	</datasource>

	<drivers>
		<driver name="appdriver">
			<xa-datasource-class>org.apache.derby.jdbc.EmbeddedDataSource</xa-datasource-class>
		</driver>
	</drivers>

</datasources>
```

The JNDI name `java:comp/env/jdbc/appDataSource` is used to look up the data source and
can also be used in `persistence.xml`.

TomEE
-----
TomEE data sources are defined in a file called `resources.xml` and
should be located in the `WEB-INF` directory as a sibling of `web.xml`.

Here is an example:

```
<?xml version="1.0" encoding="UTF-8"?>
<resources>

	<!-- TomEE App Server Resources -->

	<Resource id="jdbc/appDataSource" type="DataSource">
	    #Embedded Derby for TomEE server
	
	    JdbcDriver org.apache.derby.jdbc.EmbeddedDriver
	    JdbcUrl jdbc:derby:memory:appdb;create=true
	    UserName APP
	    Password 
	</Resource>
</resources>
```

The JNDI name `java:comp/env/jdbc/appDataSource` is used to look up the data source and
can also be used in `persistence.xml`.

GlassFish
---------
GlassFish data sources are defined in a file called `glassfish-resources.xml` and
should be located in the `WEB-INF` directory as a sibling of `web.xml`.

Here is an example:

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE resources PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Resource Definitions//EN"
        "http://glassfish.org/dtds/glassfish-resources_1_5.dtd">
<resources>

	<!-- GlassFish App Server Resources -->
	
	<jdbc-resource
		jndi-name="jdbc/appDataSource"
		pool-name="jdbcAppDataSource"
		object-type="user">
	</jdbc-resource>

	<jdbc-connection-pool
		name="jdbcAppDataSource"
		res-type="javax.sql.DataSource" 
		datasource-classname="org.apache.derby.jdbc.EmbeddedDataSource">

		<!-- https://db.apache.org/derby/docs/10.1/publishedapi/org/apache/derby/jdbc/EmbeddedDataSource.html -->
		<property name="databaseName" value="memory:appdb"/>
		<property name="createDatabase" value="create"/> 

		<property name="user" value="APP" />
		<property name="password" value="" />
	</jdbc-connection-pool>

</resources>
```

The GlassFish Conundrum
-----------------------

The full JNDI names registered in GlassFish are different than other app servers.
Furthermore, their Entity Manager Factory system is not implemented to have visibility
to the `resource-ref` names in `web.xml`. This make GlassFish a thorn in the side of
anyone trying to create an app that can work in any app server.

Is it for this reason, that this project went with the `@DataSourceConfiguration` approach
so that people wanting to run the code need not change any code.

Summary
=======

If you got lost in how everything connects together, perhaps this helps.

An OpenLiberty data Source is configured from this file: `src/main/liberty/config/server.xml`

It uses the Jakarta EE Web Profile with JNDI support. The database connection information
is in this file. You will find that the `jdbc/appDataSource` connects to:

```
src/main/webapp/WEB-INF/ibm-bnd-web.xml
```

Alternatively, one could use:

```
src/main/webapp/WEB-INF/resources.xml (TomEE)
```

or

```
src/main/webapp/WEB-INF/wildfly-ds.xml (WildFly)
```

or

```
src/main/webapp/WEB-INF/glassfish-resources.xml (GlassFish)
```

The JNDI names in these previous files connect to

```
src/main/webapp/WEB-INF/web.xml
```

which connects to

```
src/main/resources/META-INF/persistence.xml
```

which connects to

```
Appervice.java

@PersistenceContext(unitName = "jdbcAppDataSource")
EntityManager entityManager;
```

The `server.xml` has a `derbypath` variable that is set in `pom.xml`
