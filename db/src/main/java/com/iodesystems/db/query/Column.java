package com.iodesystems.db.query;

import com.iodesystems.db.search.ConditionFactory;

public class Column<T> {

  private final boolean orderable;
  private final ConditionFactory search;
  private final String name;
  private final Class<T> columnClass;

  public Column(boolean orderable, ConditionFactory search, String name, Class<T> columnClass) {
    this.orderable = orderable;
    this.search = search;
    this.name = name;
    this.columnClass = columnClass;
  }

  public Class<T> getColumnClass() {
    return columnClass;
  }

  public boolean isOrderable() {
    return orderable;
  }

  public boolean getOrderable() {
    return orderable;
  }

  public ConditionFactory getSearch() {
    return search;
  }

  public String getName() {
    return name;
  }
}
