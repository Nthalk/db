package com.iodesystems.db.query;

import com.iodesystems.db.search.ConditionFactory;

public class Column {

  private final boolean orderable;
  private final ConditionFactory search;
  private final String name;

  public Column(boolean orderable, ConditionFactory search, String name) {
    this.orderable = orderable;
    this.search = search;
    this.name = name;
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
