/**
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.deem.zkui;

import com.deem.zkui.backup.dao.impl.MysqlBackupDaoImpl;
import com.deem.zkui.dao.Dao;
import com.deem.zkui.backup.dao.BackupDao;
import com.deem.zkui.backup.dao.impl.OracleBackupDaoImpl;
import com.deem.zkui.backup.service.BackupService;
import com.deem.zkui.backup.service.impl.BackupServiceImpl;
import com.deem.zkui.utils.BeanFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

@Slf4j
public class Main {


  public static void main(String[] args) throws Exception {
    log.debug("Starting ZKUI!");
    Properties globalProps = new Properties();
    File f = new File("config.cfg");
    if (f.exists()) {
      globalProps.load(new FileInputStream("config.cfg"));
    } else {
      System.out.println("Please create config.cfg properties file and then execute the program!");
      System.exit(1);
    }

    globalProps.setProperty("uptime", new Date().toString());
    new Dao(globalProps).checkNCreate();

    BackupDao backupDao = null;
    String driverClass = globalProps.getProperty("spring.datasource.dbcp2.driver-class-name");
    if (driverClass != null) {
      if (driverClass.contains("oracle")) {
        backupDao = new OracleBackupDaoImpl(globalProps);
      } else if (driverClass.contains("mysql")) {
        backupDao = new MysqlBackupDaoImpl(globalProps);
      }
    }
    if (backupDao != null) {
      BeanFactory.registry(BackupDao.class, backupDao);
      BackupServiceImpl backupService = new BackupServiceImpl(globalProps, backupDao);
      BeanFactory.registry(BackupService.class, backupService);

      backupDao.createBackTable();
      backupService.afterPropertiesSet();
      backupService.taskBackup();
    }

    String webFolder = "webapp";
    Server server = new Server();

    WebAppContext servletContextHandler = new WebAppContext();
    servletContextHandler.setContextPath("/");
    servletContextHandler.setResourceBase("src/main/resources/" + webFolder);
    ClassList clist = ClassList.setServerDefault(server);
    clist.addBefore(JettyWebXmlConfiguration.class.getName(),
        AnnotationConfiguration.class.getName());
    servletContextHandler
        .setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
            ".*(/target/classes/|.*.jar)");
    servletContextHandler.setParentLoaderPriority(true);
    servletContextHandler.setInitParameter("useFileMappedBuffer", "false");
    servletContextHandler.setAttribute("globalProps", globalProps);

    ResourceHandler staticResourceHandler = new ResourceHandler();
    staticResourceHandler.setDirectoriesListed(false);
    Resource staticResources = Resource.newClassPathResource(webFolder);
    staticResourceHandler.setBaseResource(staticResources);
    staticResourceHandler.setWelcomeFiles(new String[]{"html/index.html"});

    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[]{staticResourceHandler, servletContextHandler});

    server.setHandler(handlers);
    HttpConfiguration http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
    http_config.setSecurePort(Integer.parseInt(globalProps.getProperty("serverPort")));

    if (globalProps.getProperty("https").equals("true")) {
      File kFile = getKeystoreFile(globalProps);
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(kFile.getAbsolutePath());
      sslContextFactory.setKeyStorePassword(globalProps.getProperty("keystorePwd"));
      sslContextFactory.setKeyManagerPassword(globalProps.getProperty("keystoreManagerPwd"));
      HttpConfiguration https_config = new HttpConfiguration(http_config);
      https_config.addCustomizer(new SecureRequestCustomizer());

      ServerConnector https =
          new ServerConnector(server,
              new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1
                  .asString()), new HttpConnectionFactory(https_config));
      https.setPort(Integer.parseInt(globalProps.getProperty("serverPort")));
      server.setConnectors(new Connector[]{https});
    } else {
      if (globalProps.getProperty("X-Forwarded-For").equals("true")) {
        http_config.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
      }
      ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
      http.setPort(Integer.parseInt(globalProps.getProperty("serverPort")));
      server.setConnectors(new Connector[]{http});
    }

    server.start();
    server.join();
  }

  private static File getKeystoreFile(Properties globalProps) {
    String keystoreFile = globalProps.getProperty("keystoreFile");
    String workDir = System.getProperty("user.dir");
    if (workDir.endsWith("/")) {
      workDir = workDir.substring(0, workDir.length() - 1);
    }
    if (!workDir.endsWith("zkui")) {
      int eIndex = workDir.lastIndexOf('/');
      workDir = workDir.substring(0, eIndex);
    }
    File file = new File(workDir + "/" + keystoreFile);
    log.info("证书目录: {}", file.getAbsolutePath());
    return file;
  }

}
