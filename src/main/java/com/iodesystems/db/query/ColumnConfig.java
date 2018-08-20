package com.iodesystems.db.query;

import com.iodesystems.db.search.ConditionFactory;

public class ColumnConfig {

  private final String name;
  private ConditionFactory search;
  private boolean orderable;

  public ColumnConfig(String name) {
    this.name = name;
  }

  public ColumnConfig orderable(boolean orderable) {
    this.orderable = orderable;
    return this;
  }

  public ColumnConfig orderable() {
    return orderable(true);
  }

  public ColumnConfig search(ConditionFactory conditionFactory) {
    this.search = conditionFactory;
    return this;
  }

  public Column create() {
    return new Column(orderable, search, name);
  }
}
