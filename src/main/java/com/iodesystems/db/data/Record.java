package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.logic.ValueConverter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Record {

  private final List<String> columns;
  private final Map<String, Integer> columnIndexesByName;
  private final Object[] data;

  public Record(List<String> columns, Map<String, Integer> columnIndexesByName, Object[] data) {
    this.columns = columns;
    this.columnIndexesByName = columnIndexesByName;
    this.data = data;
  }

  public List<String> getColumns() {
    return columns;
  }

  public Object[] getData() {
    return data;
  }

  public Object getObject(int fieldIndex) {
    return data[fieldIndex];
  }

  public int getFieldIndex(String field) {
    Integer fieldIndex = columnIndexesByName.get(field);
    if (fieldIndex == null) {
      StringBuilder columnsString = new StringBuilder();
      boolean first = true;
      for (String column : columns) {
        if (first) {
          first = false;
        } else {
          columnsString.append(", ");
        }
        columnsString.append(column);
      }
      throw new DbException(
          "Field " + field + " not found on Record with fields: " + columnsString.toString());
    }
    return fieldIndex;
  }

  public Object getObject(String field) {
    return getObject(getFieldIndex(field));
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    for (String column : columns) {
      map.put(column, getObject(column));
    }
    return map;
  }

  public <T> T get(String field, Class<T> cls) {
    return get(getFieldIndex(field), cls, null);
  }

  public <T> T get(int fieldIndex, Class<T> cls) {
    return get(fieldIndex, cls, null);
  }

  public <T> T get(int fieldIndex, Class<T> cls, T ifNull) {
    Object value = getObject(fieldIndex);
    if (value == null) {
      return ifNull;
    }
    return ValueConverter.convert(value, cls);
  }

  public Timestamp getTimestamp(String field) {
    return get(field, Timestamp.class);
  }

  public Date getDate(String field) {
    return get(field, Date.class);
  }

  public Boolean getBoolean(String field) {
    return get(field, Boolean.class);
  }

  public Character getChar(String field) {
    return get(field, Character.class);
  }

  public Integer getInteger(String field) {
    return get(field, Integer.class);
  }

  public Long getLong(String field) {
    return get(field, Long.class);
  }

  public Short getShort(String field) {
    return get(field, Short.class);
  }

  public String getString(String field) {
    return get(field, String.class);
  }

  public BigDecimal getBigDecimal(String field) {
    return get(field, BigDecimal.class);
  }

  public BigInteger getBigInteger(String field) {
    return get(field, BigInteger.class);
  }
}
