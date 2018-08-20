package com.iodesystems.db.search;

import com.iodesystems.db.query.Column;
import com.iodesystems.db.query.Ordering;
import com.iodesystems.db.query.Params;
import com.iodesystems.db.query.QueryContext;
import java.util.List;
import java.util.Map;

public class SearchContext {

  private final QueryContext queryContext;
  private final List<String> searches;
  private final List<Ordering> orderings;
  private final int page;
  private final int pageSize;
  private String sql = null;
  private Params params = null;

  public SearchContext(
      QueryContext queryContext,
      List<String> searches,
      List<Ordering> orderings,
      int page,
      int pageSize) {
    this.queryContext = queryContext;
    this.searches = searches;
    this.orderings = orderings;
    this.page = page;
    this.pageSize = pageSize;
    generateSqlAndParams();
  }

  private void generateSqlAndParams() {
    params = new Params(queryContext.getParams());

    StringBuilder searchedSql = new StringBuilder();
    if (!searches.isEmpty()) {
      searchedSql.append(generatedSearchedSql(queryContext, params, searches));
    } else {
      searchedSql.append(queryContext.getSql());
    }

    final StringBuilder orderedSql;
    if (!orderings.isEmpty()) {
      orderedSql = new StringBuilder();
      orderedSql.append(
          generateOrderedSql(searchedSql.toString(), queryContext.getColumns(), orderings));
    } else {
      orderedSql = searchedSql;
    }

    final StringBuilder pagedSql;
    if (pageSize != -1) {
      pagedSql = new StringBuilder();
      pagedSql.append(generatePagedSql(orderedSql.toString(), page, pageSize, params));
    } else {
      pagedSql = orderedSql;
    }

    this.sql = pagedSql.toString();
  }

  private String generatePagedSql(String sql, int page, int pageSize, Params params) {
    return "SELECT * FROM (SELECT PAGED.*, ROWNUM \"__RN\" FROM ("
        + sql
        + ") PAGED WHERE ROWNUM <="
        + params.setPrefixed("end", (page + 1) * pageSize)
        + ") WHERE \"__RN\" >"
        + params.setPrefixed("start", (page) * pageSize);
  }

  private String generateOrderedSql(
      String query, Map<String, Column> columns, List<Ordering> orderings) {
    StringBuilder ordered = new StringBuilder();
    boolean didWriteOrderBy = false;
    for (Ordering ordering : orderings) {
      Column column = columns.get(ordering.getColumn().toLowerCase());
      if (column != null) {
        if (column.getOrderable()) {
          if (!didWriteOrderBy) {
            didWriteOrderBy = true;
            ordered.append("SELECT * FROM (");
            ordered.append(query);
            ordered.append(") ORDER BY ");
            ordered.append(column.getName());
            ordered.append(" ");
            ordered.append(ordering.getSort().name());
          } else {
            ordered.append(", ");
            ordered.append(column.getName());
            ordered.append(" ");
            ordered.append(ordering.getSort().name());
          }
        } else {
          throw new IllegalArgumentException(
              "Column " + ordering.getColumn() + " is not orderable");
        }
      } else {
        throw new IllegalArgumentException(
            "Column " + ordering.getColumn() + " is not present on query");
      }
    }
    return ordered.toString();
  }

  private String generatedSearchedSql(
      QueryContext queryContext, Params params, List<String> searches) {
    ConditionContext conditionContext = new ConditionContext(params);
    ConditionBuilder conditionBuilder = new ConditionBuilder(conditionContext);

    StringBuilder searchesCondition = new StringBuilder();
    for (String search : searches) {
      StringBuilder groupsCondition = new StringBuilder();
      for (SearchTermGroup searchTermGroup : queryContext.getSearchParser().parse(search)) {
        StringBuilder groupCondition = new StringBuilder();
        for (SearchTerm term : searchTermGroup.getTerms()) {
          String target = term.getTarget();
          String value = term.getValue();
          conditionContext.setArgument(value);
          if (target != null) {
            ConditionFactory conditionFactory = queryContext.getTargetedSearch(target);

            if (conditionFactory == null) {
              Column column = queryContext.getColumn(target);
              if (column != null) {
                conditionFactory = column.getSearch();
                conditionContext.setTarget(column.getName());
              }
            } else {
              conditionContext.setTarget(target);
            }

            if (conditionFactory != null) {
              String condition = conditionFactory.build(conditionBuilder);
              if (condition != null && !condition.isEmpty()) {
                if (groupCondition.length() == 0) {
                  groupCondition.append("   (       ");
                } else {
                  groupCondition.append("\n                    AND ( ");
                }
                groupCondition.append(condition);
                groupCondition.append(" )");
              }
            }
            continue;
          }
          // Apply the query to all searches
          conditionContext.setArgument(term.getRawTerm());
          StringBuilder anySearchConditionMatches = new StringBuilder();
          for (Column column : queryContext.getColumns().values()) {
            if (column.getSearch() != null) {
              conditionContext.setTarget(column.getName());
              String condition = column.getSearch().build(conditionBuilder);
              if (condition != null && !condition.isEmpty()) {
                if (anySearchConditionMatches.length() == 0) {
                  anySearchConditionMatches.append("   ( ");
                } else {
                  anySearchConditionMatches.append("\n                     OR ( ");
                }
                anySearchConditionMatches.append(condition);
                anySearchConditionMatches.append(" )");
              }
            }
          }
          if (anySearchConditionMatches.length() != 0) {
            if (groupCondition.length() == 0) {
              groupCondition.append("    ( ");
            } else {
              groupCondition.append("\n               AND ( ");
            }
            groupCondition.append(anySearchConditionMatches);
            groupCondition.append(")");
          }
        }
        if (groupCondition.length() > 0) {
          if (groupsCondition.length() == 0) {
            groupsCondition.append("   ( ");
          } else {
            groupsCondition.append("\n      OR ( ");
          }
          groupsCondition.append(groupCondition);
          groupsCondition.append(")");
        }
      }
      if (groupsCondition.length() > 0) {
        if (searchesCondition.length() == 0) {
          searchesCondition.append("SELECT * FROM (\n\n");
          searchesCondition.append(queryContext.getSql());
          searchesCondition.append("\n");
          searchesCondition.append("\n) WHERE ( ");
        } else {
          searchesCondition.append("\n    AND ( ");
        }
        searchesCondition.append(groupsCondition);
        searchesCondition.append(")");
      }
    }
    if (searchesCondition.length() == 0) {
      return queryContext.getSql();
    } else {
      return searchesCondition.toString();
    }
  }

  public String getSql() {
    return sql;
  }

  public Params getParams() {
    return params;
  }
}
