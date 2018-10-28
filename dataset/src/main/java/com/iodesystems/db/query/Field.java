package com.iodesystems.db.query;

import com.iodesystems.db.search.SearchConditionProvider;
import com.iodesystems.db.search.SearchFieldConditionProvider;

public class Field<F> {

  private final String name;
  private final org.jooq.Field<F> field;
  private final SearchConditionProvider searchConditionProvider;
  private final boolean orderable;

  public Field(
      String name,
      org.jooq.Field<F> field,
      SearchFieldConditionProvider<F, org.jooq.Field<F>> searchFieldConditionProvider,
      boolean orderable) {
    this.name = name;
    this.field = field;
    this.orderable = orderable;
    if (searchFieldConditionProvider == null) {
      this.searchConditionProvider = null;
    } else {
      this.searchConditionProvider = search -> searchFieldConditionProvider.search(search, field);
    }
  }

  public String getName() {
    return name;
  }

  public org.jooq.Field<F> getField() {
    return field;
  }

  public SearchConditionProvider getSearchConditionProvider() {
    return searchConditionProvider;
  }

  public boolean isOrderable() {
    return orderable;
  }
}
