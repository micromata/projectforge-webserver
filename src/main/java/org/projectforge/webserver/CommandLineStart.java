/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.webapp.WebAppContext;
import org.projectforge.common.DatabaseDialect;

/**
 * Use this starter for starting ProjectForge from command line.<br/>
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CommandLineStart extends AbstractStartHelper
{
  private static Options options = new Options();

  private String warFile;

  static {
    options.addOption(createOption('w', "war", "war", false, "war file, default is ProjectForge.war"));
    options
        .addOption(createOption('v', "development-mode", "boolean", false, "If true, ProjectForge will be started in development mode."));
    options.addOption(createOption('h', "help", false, "Print this help."));
    options.addOption(createOption('l', "location", "string", false,
        "The home directory of ProjectForge, default is the current directory."));
    options.addOption(createOption('p', "port", "port", false, "The http port, default is 8080."));
    options.addOption(createOption('u', "schema-update", "boolean", false,
        "If true then the schema update of Hibernate is enabled (for development mode only), default is false."));
    options.addOption(createOption('b', "launch-browser", "boolean", false,
        "If true then the browser is launched automatically after start-up, default is true."));
    options.addOption(createOption("ja", "jdbc-url", "string", false, "If not given then '"
        + StartSettings.getJdbcDefaultUrl("<location>")
        + "'is used."));
    options.addOption(createOption("jc", "jdbc-driver", "string", false, "If not given then '"
        + StartSettings.getJdbcDefaultDriverClass()
        + "."));
    options.addOption(createOption("ju", "jdbc-user", "string", false, "If not given then '"
        + StartSettings.getJdbcDefaultUser()
        + "'is used."));
    options.addOption(createOption("jp", "jdbc-password", "string", false, "If not given then no password is assumed."));
  }

  @SuppressWarnings("static-access")
  private static Option createOption(String flag, String longOpt, String arg, boolean required, String description)
  {
    Option option = OptionBuilder.withArgName(arg).isRequired(required).hasArg().withDescription(description).withLongOpt(longOpt)
        .create(flag);
    return option;
  }

  @SuppressWarnings("static-access")
  private static Option createOption(char flag, String longOpt, String arg, boolean required, String description)
  {
    Option option = OptionBuilder.withArgName(arg).isRequired(required).hasArg().withDescription(description).withLongOpt(longOpt)
        .create(flag);
    return option;
  }

  @SuppressWarnings("static-access")
  private static Option createOption(char flag, String longOpt, boolean required, String description)
  {
    Option option = OptionBuilder.isRequired(required).withDescription(description).withLongOpt(longOpt).create(flag);
    return option;
  }

  public static void main(final String[] args) throws Exception
  {
    CommandLineParser parser = new GnuParser();
    CommandLine cmdLine = null;
    try {
      // parse the command line arguments
      cmdLine = parser.parse(options, args);
    } catch (ParseException exp) {
      // oops, something went wrong
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
      printHelp();
      return;
    }
    String userDir = System.getProperty("user.dir");
    String baseDir = getString(cmdLine, 'l', userDir);
    if (StringUtils.isEmpty(baseDir) == true) {
      baseDir = ".";
    }
    final StartSettings settings = new StartSettings(DatabaseDialect.HSQL, baseDir);
    settings.setJdbcDriverClass(getString(cmdLine, "jc", StartSettings.getJdbcDefaultDriverClass()));
    settings.setJdbcUrl(getString(cmdLine, "jdbc-url", StartSettings.getJdbcDefaultUrl(baseDir)));
    settings.setJdbcUser(getString(cmdLine, "ju", StartSettings.getJdbcDefaultUser()));
    settings.setJdbcPassword(getString(cmdLine, "jp", null));
    settings.setSchemaUpdate(getBoolean(cmdLine, 'u', false));
    settings.setLaunchBrowserAfterStartup(getBoolean(cmdLine, 'b', true));
    settings.setDevelopment(getBoolean(cmdLine, 'v', false));
    settings.setPort(getInt(cmdLine, 'p', settings.getPort()));
    // Set the url of ProjectForge's storage web server:
    // System.setProperty(StorageConstants.SYSTEM_PROPERTY_URL, "http://localhost:8081/");

    if (new File(baseDir).isDirectory() == false) {
      System.err.println("'" + baseDir + "' isn't a directory. Please specify other location (-l).");
      printHelp();
      return;
    }
    System.out.println("Using location '" + new File(baseDir).getAbsolutePath() + "'.");
    String warFile = getString(cmdLine, 'w', null);
    if (warFile == null) {
      warFile = new File(new File(baseDir, "webapps"), "ProjectForge.war").getAbsolutePath();
    }
    if (new File(warFile).exists() == false) {
      System.err.println("War file '" + warFile + "' doesn't exist. Please specify other location (-l) or war file (-w).");
      printHelp();
      return;
    }
    System.out.println("Using war file '" + new File(warFile).getAbsolutePath() + "'.");

    final CommandLineStart startHelper = new CommandLineStart(settings, warFile);
    startHelper.start();
  }

  private static void printHelp()
  {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("projectforge-webserver", options, true);
  }

  private static boolean getBoolean(CommandLine cmdLine, char option, boolean defaultValue)
  {
    String val = cmdLine.getOptionValue(option);
    if (StringUtils.isEmpty(val) == true) {
      return defaultValue;
    }
    return "true".equals(val.toLowerCase());
  }

  private static int getInt(CommandLine cmdLine, char option, int defaultValue)
  {
    String val = cmdLine.getOptionValue(option);
    if (StringUtils.isEmpty(val) == true) {
      return defaultValue;
    }
    return Integer.parseInt(val);
  }

  private static String getString(CommandLine cmdLine, char option, String defaultValue)
  {
    String val = cmdLine.getOptionValue(option);
    if (StringUtils.isEmpty(val) == true) {
      return defaultValue;
    }
    return val;
  }

  private static String getString(CommandLine cmdLine, String option, String defaultValue)
  {
    String val = cmdLine.getOptionValue(option);
    if (StringUtils.isEmpty(val) == true) {
      return defaultValue;
    }
    return val;
  }

  /**
   * @param startSettings
   */
  public CommandLineStart(final StartSettings startSettings, String warFile)
  {
    super(startSettings);
    this.warFile = warFile;
  }

  /**
   * @see org.projectforge.webserver.AbstractStartHelper#getWebAppContext()
   */
  @Override
  protected WebAppContext getWebAppContext()
  {
    final WebAppContext webAppContext = new WebAppContext();
    webAppContext.setConfigurationClasses(CONFIGURATION_CLASSES);
    webAppContext.setContextPath("/ProjectForge");
    webAppContext.setWar(warFile);
    // webAppContext.setDescriptor("src/main/webapp/WEB-INF/web.xml");
    // webAppContext.setExtraClasspath("target/classes");
    webAppContext.setInitParameter("development", String.valueOf(startSettings.isDevelopment()));
    webAppContext.setInitParameter("stripWicketTags", String.valueOf(startSettings.isStripWicketTags()));
    return webAppContext;
  }
}
