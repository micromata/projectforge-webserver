/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.webserver;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Helper for starting ProjectForge via Jetty web server.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class AbstractStartHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractStartHelper.class);

  public static final int MILLIS_HOUR = 60 * 60 * 1000;

  protected StartSettings startSettings;

  public AbstractStartHelper(StartSettings startSettings)
  {
    this.startSettings = startSettings;
  }

  public void start()
  {
    final int timeout = MILLIS_HOUR;
    setProperty("base.dir", startSettings.getBaseDir());
    if (startSettings.getDialect() != null) {
      setProperty("hibernate.dialect", startSettings.getDialect());
      setProperty("jettyEnv.driverClassName", startSettings.getJdbcDriverClass());
      setProperty("jettyEnv.jdbcUrl", startSettings.getJdbcUrl());
      setProperty("hibernate.schemaUpdate", startSettings.isSchemaUpdate());
      setProperty("jettyEnv.jdbcUser", startSettings.getJdbcUser());
      setProperty("jettyEnv.jdbcPassword", startSettings.getJdbcPassword(), false);
      setProperty("jettyEnv.jdbcMaxActive", startSettings.getJdbcMaxActive() != null ? startSettings.getJdbcMaxActive() : 200);
    }
    setProperty("jetty.home", startSettings.getBaseDir());

    final Server server = new Server();
    final SocketConnector connector = new SocketConnector();

    // Set some timeout options to make debugging easier.
    connector.setMaxIdleTime(timeout);
    connector.setSoLingerTime(-1);
    connector.setPort(startSettings.getPort());
    server.addConnector(connector);

    // check if a keystore for a SSL certificate is available, and
    // if so, start a SSL connector on port 8443. By default, the
    // quickstart comes with a Apache Wicket Quickstart Certificate
    // that expires about half way september 2021. Do not use this
    // certificate anywhere important as the passwords are available
    // in the source.

    // Resource keystore = Resource.newClassPathResource("/keystore");
    // if (keystore != null && keystore.exists()) {
    // connector.setConfidentialPort(8443);
    //
    // SslContextFactory factory = new SslContextFactory();
    // factory.setKeyStoreResource(keystore);
    // factory.setKeyStorePassword("wicket");
    // factory.setTrustStore(keystore);
    // factory.setKeyManagerPassword("wicket");
    // SslSocketConnector sslConnector = new SslSocketConnector(factory);
    // sslConnector.setMaxIdleTime(timeout);
    // sslConnector.setPort(8443);
    // sslConnector.setAcceptors(4);
    // server.addConnector(sslConnector);
    //
    // System.out.println("SSL access to the quickstart has been enabled on port 8443");
    // System.out.println("You can access the application using SSL on https://localhost:8443");
    // System.out.println();
    // }

    final WebAppContext webAppContext = getWebAppContext();
    if (webAppContext.getClassLoader() == null) {
      webAppContext.setClassLoader(webAppContext.getClassLoader());
    }
    webAppContext.setServer(server);
    if (startSettings.isUsingCookies() == false) {
      log.info("Using cookies is disabled.");
      final HashSessionManager manager = (HashSessionManager) webAppContext.getSessionHandler().getSessionManager();
      manager.setUsingCookies(false);
    }
    server.setHandler(webAppContext);
    // START JMX SERVER
    // MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    // MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
    // server.getContainer().addEventListener(mBeanContainer);
    // mBeanContainer.start();

    try {
      final ClassLoader classLoader = AbstractStartHelper.class.getClassLoader();
      final InputStream is = classLoader.getResourceAsStream("jetty.xml");
      final XmlConfiguration configuration = new XmlConfiguration(is);
      configuration.configure(server);
      System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
      server.start();
      if (startSettings.isLaunchBrowserAfterStartup() == true) {
        launchBrowser(connector, webAppContext);
      }
      System.in.read();
      System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
      server.stop();
      server.join();
    } catch (final Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  protected abstract WebAppContext getWebAppContext();

  protected String getLaunchUrlPath()
  {
    return "";
  }

  private void launchBrowser(final SocketConnector connector, WebAppContext webAppContext)
  {
    Desktop desktop = null;
    if (Desktop.isDesktopSupported()) {
      desktop = Desktop.getDesktop();
    }
    if (desktop != null) {
      try {
        desktop.browse(new URI("http://localhost:" + connector.getPort() + webAppContext.getContextPath() + getLaunchUrlPath()));
      } catch (final IOException e) {
        log.error("Can't launch browser: " + e.getMessage(), e);
      } catch (final URISyntaxException e) {
        log.error("Can't launch browser: " + e.getMessage(), e);
      }
    }
  }

  /**
   * @param key
   * @param value
   */
  private void setProperty(final String key, final String value)
  {
    setProperty(key, value, true);
  }

  /**
   * @param key
   * @param value
   * @param logValue If true (default) then the property value will be logged, otherwise "****" is logged.
   */
  private void setProperty(final String key, final String value, final boolean logValue)
  {
    if (value == null) {
      return;
    }
    if (logValue == true) {
      log.info(key + "=" + value);
    } else {
      log.info(key + "=*****");
    }
    System.setProperty(key, value);
  }

  private void setProperty(final String key, final Object value)
  {
    if (value == null) {
      setProperty(key, (String) null);
    } else {
      setProperty(key, String.valueOf(value));
    }
  }

  public static final String[] CONFIGURATION_CLASSES = { //
  org.eclipse.jetty.webapp.WebInfConfiguration.class.getName(), //
      org.eclipse.jetty.webapp.WebXmlConfiguration.class.getName(), //
      org.eclipse.jetty.webapp.MetaInfConfiguration.class.getName(), //
      org.eclipse.jetty.webapp.FragmentConfiguration.class.getName(), //
      org.eclipse.jetty.plus.webapp.EnvConfiguration.class.getName(), //
      org.eclipse.jetty.plus.webapp.PlusConfiguration.class.getName(), //
      org.eclipse.jetty.annotations.AnnotationConfiguration.class.getName(), //
      org.eclipse.jetty.webapp.JettyWebXmlConfiguration.class.getName(), //
      org.eclipse.jetty.webapp.TagLibConfiguration.class.getName()};
}
