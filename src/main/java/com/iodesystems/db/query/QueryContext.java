package com.iodesystems.db.query;

import com.iodesystems.db.search.ConditionFactory;
import com.iodesystems.db.search.SearchParser;
import java.util.HashMap;
import java.util.Map;

public class QueryContext {

  private final Params params;
  private final Map<String, ConditionFactory> targetedConditionFactories;
  private final Map<String, Column> columnConfigurations;
  private String sql;

  public QueryContext(String sql) {
    this(new Params(0), sql, new HashMap<>(), new HashMap<>());
    this.sql = sql;
  }

  public QueryContext(
      Params params,
      String sql,
      Map<String, ConditionFactory> targetedConditionFactories,
      Map<String, Column> columnConfigurations) {
    this.params = params;
    this.sql = sql;
    this.targetedConditionFactories = targetedConditionFactories;
    this.columnConfigurations = columnConfigurations;
  }

  public Params getParams() {
    return params;
  }

  public String getSql() {
    return sql;
  }

  protected void setSql(String sql) {
    this.sql = sql;
  }

  public QueryContext set(String key, Object value) {
    params.set(key, value);
    return this;
  }

  public QueryContext setPrefixed(String key, Object value) {
    params.setPrefixed(key, value);
    return this;
  }

  public Column getColumn(String name) {
    return columnConfigurations.get(name.toLowerCase());
  }

  public ConditionFactory getTargetedSearch(String name) {
    return targetedConditionFactories.get(name.toLowerCase());
  }

  public QueryContext searchColumn(String column, ConditionFactory search) {
    return column(column, search, false);
  }

  public QueryContext column(String column, ConditionFactory search, Boolean sortable) {
    return column(column, columnConfig -> {
      if (search != null) {
        columnConfig = columnConfig.search(search);
      }
      if (sortable) {
        columnConfig = columnConfig.orderable();
      }
      return columnConfig;
    });
  }

  public QueryContext column(String column, QueryColumnConfigurerer queryColumnConfigurerer) {
    Column configuredColumn = queryColumnConfigurerer.configure(new ColumnConfig(column)).create();
    columnConfigurations.put(column.toLowerCase(), configuredColumn);
    return this;
  }

  public QueryContext param(String param, ConditionFactory conditionFactory) {
    targetedConditionFactories.put(param.toLowerCase(), conditionFactory);
    return this;
  }

  public QueryContext param(String param) {
    return param(param, c -> c.param(param, c.term()));
  }

  public Map<String, Column> getColumns() {
    return columnConfigurations;
  }

  public SearchParser getSearchParser() {
    return new SearchParser();
  }
}
