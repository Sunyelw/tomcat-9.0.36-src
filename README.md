### 1.apache tomcat 下载地址 

[apache-8.5.56](https://tomcat.apache.org/download-80.cgi)

[apache-9.0.36](https://tomcat.apache.org/download-90.cgi)


### 2.apache ant 下载

[apache-ant-1.10.8](https://ant.apache.org/bindownload.cgi)


### 3.pom.xml 文件

```xml
<?xml version="1.0" encoding="utf-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">  
    <modelVersion>4.0.0</modelVersion>  
    <groupId>org.apache.tomcat</groupId>  
    <artifactId>Apache-Tomcat-9.0.36</artifactId>  
    <name>Tomcat-9.0.36</name>  
    <version>9.0.36</version>  
    <build> 
        <finalName>Tomcat7.0</finalName>  
        <sourceDirectory>java</sourceDirectory>  
        <testSourceDirectory>test</testSourceDirectory>  
        <resources> 
            <resource> 
                <directory>java</directory> 
            </resource> 
        </resources>  
        <testResources> 
            <testResource> 
                <directory>test</directory> 
            </testResource> 
        </testResources>  
        <plugins> 
            <plugin> 
                <groupId>org.apache.maven.plugins</groupId>  
                <artifactId>maven-compiler-plugin</artifactId>  
                <version>2.3</version>  
                <configuration> 
                    <encoding>UTF-8</encoding>  
                    <source>1.8</source>  
                    <target>1.8</target> 
                </configuration> 
            </plugin> 
        </plugins> 
    </build>  
    <dependencies> 
        <dependency> 
            <groupId>junit</groupId>  
            <artifactId>junit</artifactId>  
            <version>4.12</version>  
            <scope>test</scope> 
        </dependency>  
        <dependency> 
            <groupId>ant</groupId>  
            <artifactId>ant</artifactId>  
            <version>1.7.0</version> 
        </dependency>  
        <dependency> 
            <groupId>wsdl4j</groupId>  
            <artifactId>wsdl4j</artifactId>  
            <version>1.6.2</version> 
        </dependency>  
        <dependency> 
            <groupId>javax.xml</groupId>  
            <artifactId>jaxrpc</artifactId>  
            <version>1.1</version> 
        </dependency>  
        <dependency> 
            <groupId>org.eclipse.jdt.core.compiler</groupId>  
            <artifactId>ecj</artifactId>  
            <version>4.2.2</version> 
        </dependency>  
        <dependency> 
            <groupId>org.easymock</groupId>  
            <artifactId>easymock</artifactId>  
            <version>3.3</version> 
        </dependency>
        <dependency> 
            <groupId>javax.xml.soap</groupId>  
            <artifactId>javax.xml.soap-api</artifactId>  
            <version>1.4.0</version> 
        </dependency> 
    </dependencies> 
</project>
```


### 4.VM配置


```properties
-Dcatalina.home=C:\idea\hy\apache-tomcat-9.0.36-src\resource
-Dcatalina.base=C:\idea\hy\apache-tomcat-9.0.36-src\resource
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djava.util.logging.config.file=C:\idea\hy\apache-tomcat-9.0.36-src\resource\conf\logging.properties
```


### 5.jasper引擎初始化

`ContextConfig.java` 添加一行代码 初始化 jasper 代码

```java
// 初始化 jasper 引擎
context.addServletContainerInitializer(new JasperInitializer(), null);
```


### 6.创建resource文件夹

创建 resource 文件夹

将 conf / webapps 文件夹拷贝到 resource 文件夹下



亲测可用

[tomcat9 源码编译流程](https://blog.csdn.net/linxdcn/article/details/72811928?utm_source=blogxgwz9)



乱码问题

[VersionLoggerListener tomcat输出乱码](https://www.cnblogs.com/davidwang456/p/11224923.html)

```properties
-Duser.language=en
-Duser.region=US
```

