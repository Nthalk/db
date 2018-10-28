package com.iodesystems.db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.iodesystems.db.logic.Converter;
import com.iodesystems.db.logic.Handler;
import com.iodesystems.db.query.Order.Direction;
import com.iodesystems.db.search.SearchConditionProvider;
import com.iodesystems.db.search.SearchFieldConditionProvider;
import com.iodesystems.db.search.SearchParser;
import com.iodesystems.db.search.SearchTableConditionProvider;
import com.iodesystems.db.search.model.SearchTerm;
import com.iodesystems.db.search.model.SearchTermGroup;
import com.iodesystems.db.search.model.TargetedSearch;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.RecordMapper;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class TypedQuery<T extends Table<R>, R extends Record, M> implements Result<M> {

  protected final DSLContext db;
  private final T table;
  private final List<Condition> conditions;
  private final int offset;
  private final int limit;
  private final RecordMapper<R, M> mapper;
  private final List<SortField<?>> order;
  private final Map<String, Field<?>> fields;
  private final Map<String, TargetedSearch> searches;
  private final List<String> searchesToApply;

  public TypedQuery(DSLContext db, T table, RecordMapper<R, M> mapper) {
    this(
        db,
        table,
        new ArrayList<>(),
        0,
        -1,
        mapper,
        new ArrayList<>(),
        new HashMap<>(),
        new HashMap<>(),
        new ArrayList<>());
  }

  public TypedQuery(DSLContext db, T table, Class<M> mapTo) {
    this(
        db,
        table,
        new ArrayList<>(),
        0,
        -1,
        r -> r.into(mapTo),
        new ArrayList<>(),
        new HashMap<>(),
        new HashMap<>(),
        new ArrayList<>());
  }

  public TypedQuery(TypedQuery<T, R, M> clone) {
    this(
        clone.db,
        clone.table,
        new ArrayList<>(clone.conditions),
        clone.offset,
        clone.limit,
        clone.mapper,
        clone.order,
        clone.fields,
        clone.searches,
        clone.searchesToApply);
  }

  public TypedQuery(
      DSLContext db,
      T table,
      List<Condition> conditions,
      int offset,
      int limit,
      RecordMapper<R, M> mapper,
      List<SortField<?>> order,
      Map<String, Field<?>> fields,
      Map<String, TargetedSearch> searches,
      List<String> searchesToApply) {
    this.db = db;
    this.table = table;
    this.conditions = conditions;
    this.offset = offset;
    this.limit = limit;
    this.mapper = mapper;
    this.order = order;
    this.searches = searches;
    this.fields = fields;
    this.searchesToApply = searchesToApply;
  }

  public static <R extends Record> TypedQuery<Table<R>, R, R> select(
      DSLContext db, Select<R> select) {
    return new TypedQuery<>(db, select.asTable(), r -> r);
  }

  @Override
  public String toString() {
    return query().toString();
  }

  public <F> TypedQuery<T, R, M> field(
      String name,
      org.jooq.Field<F> field,
      SearchFieldConditionProvider<F, org.jooq.Field<F>> searchFieldConditionProvider,
      boolean orderable) {
    fields.put(name, new Field<>(name, field, searchFieldConditionProvider, orderable));
    return this;
  }

  public <F> TypedQuery<T, R, M> field(
      org.jooq.Field<F> field,
      SearchFieldConditionProvider<F, org.jooq.Field<F>> searchFieldConditionProvider,
      boolean orderable) {
    return field(field.getName().toLowerCase(), field, searchFieldConditionProvider, orderable);
  }

  public <F> TypedQuery<T, R, M> field(
      FieldExtractor<T, F> extractor,
      SearchFieldConditionProvider<F, org.jooq.Field<F>> searchFieldConditionProvider,
      boolean orderable) {
    return field(extractor.fieldFrom(table), searchFieldConditionProvider, orderable);
  }

  public <F> TypedQuery<T, R, M> field(
      String name,
      FieldExtractor<T, F> extractor,
      SearchFieldConditionProvider<F, org.jooq.Field<F>> searchFieldConditionProvider,
      boolean orderable) {
    return field(name, extractor.fieldFrom(table), searchFieldConditionProvider, orderable);
  }

  public TypedQuery<T, R, M> search(String query) {
    List<String> searchesToApply = new ArrayList<>(this.searchesToApply);
    searchesToApply.add(query);
    return new TypedQuery<>(
        db, table, conditions, offset, limit, mapper, order, fields, searches, searchesToApply);
  }

  public TypedQuery<T, R, M> search(
      String name, SearchTableConditionProvider<T> searchTableConditionProvider) {
    searches.put(
        name.toLowerCase(),
        new TargetedSearch(name, s -> searchTableConditionProvider.search(s, table)));
    return this;
  }

  public TypedQuery<T, R, M> where(ConditionProvider<T> conditionProvider) {
    return where(conditionProvider.conditions(table));
  }

  public TypedQuery<T, R, M> where(Condition condition) {
    List<Condition> conditions = new ArrayList<>(this.conditions);
    conditions.add(condition);
    return new TypedQuery<>(
        db, table, conditions, offset, limit, mapper, order, fields, searches, searchesToApply);
  }

  public boolean empty() {
    return limit(1).count() != 1;
  }

  public long count() {
    return db.selectCount().from(table).where(getConditions()).fetchOne(0, Long.class);
  }

  public <F> Select<Record1<F>> select(Converter<T, ? extends org.jooq.Field<F>> fieldExtractor) {
    SelectQuery<R> query = query();
    return db.select(query.field(fieldExtractor.from(table))).from(query);
  }

  public M first() {
    List<M> result = limit(1).fetch();
    if (result.isEmpty()) {
      return null;
    }
    return result.get(0);
  }

  @SuppressWarnings("unchecked")
  public List<M> fetch() {
    org.jooq.Result<R> result = result();
    return result.map(mapper);
  }

  private List<Condition> getConditions() {
    List<Condition> conditions = new ArrayList<>(this.conditions);
    SearchParser searchParser = new SearchParser();
    for (String search : searchesToApply) {
      List<Condition> groups = new ArrayList<>();
      for (SearchTermGroup searchTermGroup : searchParser.parse(search)) {
        List<Condition> group = new ArrayList<>();
        for (SearchTerm term : searchTermGroup.getTerms()) {
          if (term.getTarget() != null && !term.getTarget().isEmpty()) {
            TargetedSearch searchConfiguration = searches.get(term.getTarget().toLowerCase());
            if (searchConfiguration != null) {
              Condition condition =
                  searchConfiguration.getSearchConditionProvider().search(term.getValue());
              if (condition != null) {
                group.add(condition);
              }
              continue;
            }
            Field<?> targetedField = fields.get(term.getTarget().toLowerCase());
            if (targetedField != null) {
              Condition condition =
                  targetedField.getSearchConditionProvider().search(term.getValue());
              if (condition != null) {
                group.add(condition);
              }
              continue;
            }
          }
          List<Condition> fieldCondition = new ArrayList<>();
          for (Field<?> field : fields.values()) {
            SearchConditionProvider searchConditionProvider = field.getSearchConditionProvider();
            if (searchConditionProvider != null) {
              Condition condition = searchConditionProvider.search(term.getRawTerm());
              if (condition != null) {
                fieldCondition.add(condition);
              }
            }
          }
          if (!fieldCondition.isEmpty()) {
            group.add(DSL.or(fieldCondition));
          }
        }
        groups.add(DSL.and(group));
      }
      if (!groups.isEmpty()) {
        conditions.add(DSL.or(groups));
      }
    }
    return conditions;
  }

  public SelectQuery<R> query() {
    SelectQuery<R> query = db.selectQuery(table);

    if (offset > 0) {
      query.addOffset(offset);
    }

    if (limit > -1) {
      query.addLimit(limit);
    }

    for (SortField<?> ordering : order) {
      query.addOrderBy(ordering);
    }

    query.addConditions(getConditions());
    return query;
  }

  private org.jooq.Result result() {
    return query().fetch();
  }

  public List<Column> getColumns() {
    Map<String, Order> ordering = new HashMap<>();
    for (SortField<?> order : this.order) {
      String orderFieldName = order.getName().toLowerCase();
      Field<?> orderField = fields.get(orderFieldName);
      if (orderField != null) {
        ordering.put(
            orderFieldName,
            new Order(
                orderFieldName,
                order.getOrder() == SortOrder.ASC ? Direction.ASC : Direction.DESC));
      }
    }
    List<Column> columns = new ArrayList<>();
    for (org.jooq.Field field : limit(0).result().recordType().fields()) {
      Field<?> configuredField = fields.get(field.getName().toLowerCase());
      if (configuredField == null) {
        columns.add(new Column(field.getName(), false, false, null));
      } else {
        Order order = ordering.get(field.getName().toLowerCase());
        columns.add(
            new Column(
                field.getName(),
                configuredField.getSearchConditionProvider() != null,
                configuredField.isOrderable(),
                order == null ? null : order.getOrder()));
      }
    }
    return columns;
  }

  public TypedQuery<T, R, M> order(String field, SortOrder sortOrder) {
    Field<?> fieldConfiguration = fields.get(field.toLowerCase());
    if (fieldConfiguration == null) {
      // Uhoh
      return this;
    }
    return order(fieldConfiguration.getField(), sortOrder);
  }

  public TypedQuery<T, R, M> order(FieldExtractor<T, ?> fieldExtractor, SortOrder order) {
    return order(fieldExtractor.fieldFrom(table), order);
  }

  public TypedQuery<T, R, M> order(org.jooq.Field field, SortOrder order) {
    List<SortField<?>> ordering = new ArrayList<>(this.order);
    ordering.add(field.sort(order));
    return new TypedQuery<>(
        db, table, conditions, offset, limit, mapper, ordering, fields, searches, searchesToApply);
  }

  public TypedQuery<T, R, M> offset(int offset) {
    return new TypedQuery<>(
        db, table, conditions, offset, limit, mapper, order, fields, searches, searchesToApply);
  }

  public TypedQuery<T, R, M> limit(int limit) {
    return new TypedQuery<>(
        db, table, conditions, offset, limit, mapper, order, fields, searches, searchesToApply);
  }

  @Override
  public void stream(Handler<M> handler) {
    try (Cursor<R> rs = query().fetchLazy()) {
      for (R r : rs) {
        handler.handle(mapper.map(r));
      }
    }
  }

  public List<M> page(int page, int pageSize) {
    return new TypedQuery<>(offset(page * pageSize).limit(pageSize)).fetch();
  }

  public <U> TypedQuery<T, R, U> map(Converter<M, U> converter) {
    return new TypedQuery<>(
        db,
        table,
        conditions,
        offset,
        limit,
        r -> converter.from(mapper.map(r)),
        order,
        fields,
        searches,
        searchesToApply);
  }
}
