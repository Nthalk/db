package com.iodesystems.db.jdbc;

import com.iodesystems.db.errors.DbException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class TransactionalDataSource implements DataSource {

  private final DataSource dataSource;
  private final Connection connection;
  private final boolean autoCommit;
  private final Savepoint savepoint;

  public TransactionalDataSource(DataSource dataSource, Connection connection, boolean autoCommit,
      Savepoint savepoint) {
    this.dataSource = dataSource;
    this.connection = new UnclosableConnection(connection);
    this.autoCommit = autoCommit;
    this.savepoint = savepoint;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return connection;
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return dataSource.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    dataSource.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    dataSource.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return dataSource.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return dataSource.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return dataSource.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return dataSource.isWrapperFor(iface);
  }

  public void transactionEnd() {
    try {
      if (!connection.isClosed()) {
        connection.commit();
        connection.setAutoCommit(autoCommit);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public DataSource getParentDataSource() {
    return dataSource;
  }

  public void transactionRollback() {
    try {
      if (!connection.isClosed()) {
        connection.rollback(savepoint);
        connection.setAutoCommit(autoCommit);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }
}
