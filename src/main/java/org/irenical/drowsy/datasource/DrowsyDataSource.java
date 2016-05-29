package org.irenical.drowsy.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.irenical.jindy.Config;
import org.irenical.jindy.ConfigFactory;
import org.irenical.lifecycle.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

/**
 * DrowsyDataSource is a DataSource with dynamic configuration capabilities. It
 * includes FlyWay and uses HikariCP as the actual DataSource implementation.
 * Configuration is done by the current Jindy binding.
 * <h3>DataSource configuration:</h3> As described in
 * https://github.com/brettwooldridge/HikariCP<br>
 * Prefixed with jdbc (ex: jdbc.username, jdbc.password, jdbc.jdbcUrl, etc...)
 * <br>
 * <h3>FlyWay configuration:</h3> flyway.bypass - whether to bypass flyway
 * [optional,default=false]<br>
 * flyway.baselineVersion - the baseline version to consider [optional,
 * default=null]
 */
public class DrowsyDataSource implements LifeCycle, DataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(DrowsyDataSource.class);

  private static String DATASOURCECLASSNAME = "jdbc.dataSourceClassName";
  private static String JDBCURL = "jdbc.jdbcUrl";
  private static String USERNAME = "jdbc.username";
  private static String PASSWORD = "jdbc.password";
  private static String AUTOCOMMIT = "jdbc.autoCommit";
  private static String CONNECTIONTIMEOUT = "jdbc.connectionTimeout";
  private static String IDLETIMEOUT = "jdbc.idleTimeout";
  private static String MAXLIFETIME = "jdbc.maxLifetime";
  private static String CONNECTIONTESTQUERY = "jdbc.connectionTestQuery";
  private static String MINIMUMIDLE = "jdbc.minimumIdle";
  private static String MAXIMUMPOOLSIZE = "jdbc.maximumPoolSize";
  private static String POOLNAME = "jdbc.poolName";
  private static String INITIALIZATIONFAILFAST = "jdbc.initializationFailFast";
  private static String ISOLATEINTERNALQUERIES = "jdbc.isolateInternalQueries";
  private static String ALLOWPOOLSUSPENSION = "jdbc.allowPoolSuspension";
  private static String READONLY = "jdbc.readOnly";
  private static String REGISTERMBEANS = "jdbc.registerMbeans";
  private static String CATALOG = "jdbc.catalog";
  private static String CONNECTIONINITSQL = "jdbc.connectionInitSql";
  private static String DRIVERCLASSNAME = "jdbc.driverClassName";
  private static String TRANSACTIONISOLATION = "jdbc.transactionIsolation";
  private static String VALIDATIONTIMEOUT = "jdbc.validationTimeout";
  private static String LEAKDETECTIONTHRESHOLD = "jdbc.leakDetectionThreshold";

  private static String FLYWAY_BYPASS = "flyway.bypass";
  private static String FLYWAY_BASELINE_VERSION = "flyway.baselineVersion";

  private final Config config;

  private HikariDataSource dataSource;

  /**
   * Creates a new DataSource that reads it's configuration from the properties
   * as described in this class
   */
  public DrowsyDataSource() {
    this(null);
  }

  /**
   * Creates a new DataSource
   * 
   * @param configPrefix
   *          - the configuration keys prefix, all the DataSource configuration
   *          properties described in this class will be prefixed by given value
   */
  public DrowsyDataSource(String configPrefix) {
    this.config = configPrefix == null || configPrefix.trim().isEmpty() ? ConfigFactory.getConfig()
        : ConfigFactory.getConfig().filterPrefix(configPrefix);
  }

  @Override
  public void start() {
    initDataSource();
    setupConfigListeners();
  }

  @Override
  public void stop() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Override
  public boolean isRunning() {
    try {
      dataSource.getConnection();
      return true;
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return dataSource.getConnection(username, password);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return dataSource.getLoginTimeout();
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return dataSource.getLogWriter();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return dataSource.getParentLogger();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return dataSource.isWrapperFor(iface);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    dataSource.setLoginTimeout(seconds);
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    dataSource.setLogWriter(out);
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return dataSource.unwrap(iface);
  }

  private HikariDataSource createDataSource() {
    HikariConfig hikariConfig = jindyToHikari();
    return new HikariDataSource(hikariConfig);
  }

  private HikariConfig jindyToHikari() {
    HikariConfig result = new HikariConfig();
    result.setDataSourceClassName(config.getString(DATASOURCECLASSNAME));
    result.setJdbcUrl(config.getString(JDBCURL));
    result.setUsername(config.getString(USERNAME));
    result.setPassword(config.getString(PASSWORD));
    result.setAutoCommit(config.getBoolean(AUTOCOMMIT, false));
    result.setConnectionTimeout(config.getInt(CONNECTIONTIMEOUT, 30000));
    result.setIdleTimeout(config.getInt(IDLETIMEOUT, 600000));
    result.setMaxLifetime(config.getInt(MAXLIFETIME, 1800000));
    result.setConnectionTestQuery(config.getString(CONNECTIONTESTQUERY));
    result.setMinimumIdle(config.getInt(MINIMUMIDLE, 1));
    result.setMaximumPoolSize(config.getInt(MAXIMUMPOOLSIZE, 10));
    result.setPoolName(config.getString(POOLNAME));
    result.setInitializationFailFast(config.getBoolean(INITIALIZATIONFAILFAST, true));
    result.setIsolateInternalQueries(config.getBoolean(ISOLATEINTERNALQUERIES, false));
    result.setAllowPoolSuspension(config.getBoolean(ALLOWPOOLSUSPENSION, false));
    result.setReadOnly(config.getBoolean(READONLY, false));
    result.setRegisterMbeans(config.getBoolean(REGISTERMBEANS, false));
    result.setCatalog(config.getString(CATALOG));
    result.setConnectionInitSql(config.getString(CONNECTIONINITSQL));
    String driverClassName = config.getString(DRIVERCLASSNAME);
    if (driverClassName != null) {
      result.setDriverClassName(driverClassName);
    }
    result.setTransactionIsolation(config.getString(TRANSACTIONISOLATION));
    result.setValidationTimeout(config.getInt(VALIDATIONTIMEOUT, 5000));
    result.setLeakDetectionThreshold(config.getInt(LEAKDETECTIONTHRESHOLD, 0));
    return result;
  }

  private void initDataSource() {
    this.dataSource = createDataSource();
    migrate();
  }

  private void migrate() {
    if (config.getBoolean(FLYWAY_BYPASS, false)) {
      return;
    }
    Flyway flyway = new Flyway();
    flyway.setDataSource(dataSource);
    String baseline = config.getString(FLYWAY_BASELINE_VERSION);
    if (baseline != null) {
      flyway.setBaselineVersionAsString(baseline);
      flyway.setBaselineOnMigrate(true);
    }
    flyway.migrate();
  }

  private void setupConfigListeners() {
    // require reboot
    Arrays.asList(DATASOURCECLASSNAME, JDBCURL, USERNAME, PASSWORD, AUTOCOMMIT, CONNECTIONTIMEOUT,
        POOLNAME, CONNECTIONTESTQUERY, INITIALIZATIONFAILFAST, ISOLATEINTERNALQUERIES,
        ALLOWPOOLSUSPENSION, READONLY, REGISTERMBEANS, CATALOG, CONNECTIONINITSQL, DRIVERCLASSNAME,
        TRANSACTIONISOLATION, LEAKDETECTIONTHRESHOLD, FLYWAY_BYPASS, FLYWAY_BASELINE_VERSION)
        .stream().forEach(this::addPropertyListener);

    // hot swappable
    config.listen(MAXIMUMPOOLSIZE,
        () -> dataSource.setMaximumPoolSize(config.getInt(MAXIMUMPOOLSIZE, 10)));
    config.listen(MINIMUMIDLE, () -> dataSource.setMinimumIdle(config.getInt(MINIMUMIDLE, 1)));
    config.listen(IDLETIMEOUT, () -> dataSource.setIdleTimeout(config.getInt(IDLETIMEOUT, 600000)));
    config.listen(MAXLIFETIME,
        () -> dataSource.setMaxLifetime(config.getInt(MAXLIFETIME, 1800000)));
    config.listen(VALIDATIONTIMEOUT,
        () -> dataSource.setValidationTimeout(config.getInt(VALIDATIONTIMEOUT, 5000)));
  }

  private void addPropertyListener(String property) {
    config.listen(property, this::onConnectionPropertyChanged);
  }

  private void onConnectionPropertyChanged() {
    LOGGER.info("DataSource Configuration changed. Creating new datasource...");
    try {
      HikariDataSource oldDataSource = dataSource;
      initDataSource();
      oldDataSource.close();
    } catch (HikariPool.PoolInitializationException e) {
      LOGGER.error("Error initializing backend", e);
      if (dataSource != null && !dataSource.isClosed()) {
        dataSource.close();
      }
    }
  }

}
