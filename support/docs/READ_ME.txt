Need to:
- copy TLD's into working directory

- Register Filters:
<filter>
    <filter-name>ErrorLogFilter</filter-name>
    <filter-class>org.j2free.servlet.filter.ErrorLogFilter</filter-class>
</filter>
<filter>
    <filter-name>PersistenceFilter</filter-name>
    <filter-class>org.j2free.servlet.filter.PersistenceFilter</filter-class>
    <init-param>
        <param-name>persistenceBeanRef</param-name>
        <param-value>com.poliquiz.helpers.PersistenceBean</param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>ErrorLogFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
<filter-mapping>
    <filter-name>PersistenceFilter</filter-name>
    <servlet-name>__Mapping__</servlet-name>
</filter-mapping>

- Register Initialization and Admin Servlets:
<servlet>
    <servlet-name>Init</servlet-name>
    <servlet-class>org.j2free.servlet.InitializationServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet>
    <servlet-name>AdminGenerator</servlet-name>
    <servlet-class>org.j2free.servlet.AdminGenerator</servlet-class>
    <init-param>
        <param-name>debug</param-name>
        <param-value>false</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>AdminGenerator</servlet-name>
    <url-pattern>/j2freeadmin</url-pattern>
</servlet-mapping>
<servlet-mapping>
    <servlet-name>AdminGenerator</servlet-name>
    <url-pattern>/j2freeadmin/*</url-pattern>
</servlet-mapping>

- Put following web components into /WEB-INF/j2free
	From inside /src/web/:
		- img
		- scripts
		- jsp
		- style


J2Free Dependencies

Need to be on classpath at buildtime:
jsp-api.jar
servlet-api.jar
mail.jar
jta.jar

Need to be on classpath at runtime:
commons-collections-3.1.jar
commons-digester-1.8.jar
commons-logging-1.1.jar
commons-logging-adapters-1.1.jar

