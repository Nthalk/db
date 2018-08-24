package com.iodesystems.db.data;

import com.iodesystems.db.errors.DbException;
import com.iodesystems.db.logic.ValueConverter;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordCursor {

  private String query;
  private final ResultSet resultSet;
  private final Map<String, Integer> columnIndexesByName = new HashMap<>();
  private final List<String> columns = new ArrayList<>();

  public RecordCursor(String query, ResultSet resultSet) {
    this.query = query;
    this.resultSet = resultSet;

    try {
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      for (int i = 0; i < columnCount; i++) {
        String columnName = metaData.getColumnName(i + 1);
        columnIndexesByName.put(columnName, i);
        columns.add(columnName);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <T> T map(RecordMapper<T> recordMapper) {
    return recordMapper.map(this);
  }

  public boolean next() {
    try {
      return resultSet.next();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Object[] toArray() {
    try {
      int size = columns.size();
      Object[] array = new Object[size];
      for (int i = 0; i < size; i++) {
        array[i] = resultSet.getObject(i + 1);
      }
      return array;
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Record getRecord() {
    return new Record(columns, columnIndexesByName, toArray());
  }

  public int getFieldCount() {
    return columnIndexesByName.size();
  }

  public Set<String> getFieldNames() {
    return columnIndexesByName.keySet();
  }

  private int getColumnIndex(String fieldName) {
    Integer fieldIndex = columnIndexesByName.get(fieldName);
    if (fieldIndex == null) {
      throw new DbException("Field " + fieldName + " not found in query: " + query);
    }
    return fieldIndex;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    for (String field : getFieldNames()) {
      map.put(field, get(field));
    }
    return map;
  }

  public Object get(String fieldName) {
    return get(getColumnIndex(fieldName));
  }

  public Object get(int fieldIndex) {
    try {
      return resultSet.getObject(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <T> T get(String fieldName, Class<T> cls, T ifNull) {
    T value = get(fieldName, cls);
    if (value == null) {
      return ifNull;
    }
    return value;
  }

  public <T> T get(String fieldName, Class<T> cls) {
    Integer fieldIndex = columnIndexesByName.get(fieldName.toUpperCase());
    if (fieldIndex == null) {
      throw new DbException("Field " + fieldName + " not found in query: " + query);
    }
    return get(fieldIndex, cls);
  }

  public Boolean getBoolean(Integer fieldIndex) {
    try {
      return resultSet.getBoolean(fieldIndex + 1);
    } catch (SQLException e) {
      try {
        return ValueConverter.convert(resultSet.getObject(fieldIndex + 1), Boolean.class);
      } catch (SQLException ignore) {
        throw new DbException(e);
      }
    }
  }

  public Byte getByte(Integer fieldIndex) {
    try {
      return resultSet.getByte(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Short getShort(Integer fieldIndex) {
    try {
      return resultSet.getShort(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Integer getInteger(Integer fieldIndex) {
    try {
      return resultSet.getInt(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Long getLong(Integer fieldIndex) {
    try {
      return resultSet.getLong(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Float getFloat(Integer fieldIndex) {
    try {
      return resultSet.getFloat(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Double getDouble(Integer fieldIndex) {
    try {
      return resultSet.getDouble(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Date getDate(Integer fieldIndex) {
    try {
      return resultSet.getDate(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Timestamp getTimestamp(Integer fieldIndex) {
    try {
      return resultSet.getTimestamp(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Reader getReader(Integer fieldIndex) {
    try {
      return resultSet.getCharacterStream(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public InputStream getInputStream(Integer fieldIndex) {
    try {
      return resultSet.getBinaryStream(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public String getString(Integer fieldIndex) {
    try {
      return resultSet.getString(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public BigDecimal getBigDecimal(Integer fieldIndex) {
    try {
      return resultSet.getBigDecimal(fieldIndex + 1);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Character getCharacter(Integer fieldIndex) {
    try {
      String str = resultSet.getString(fieldIndex + 1);
      if (str == null) {
        return null;
      } else if (str.length() == 0) {
        return null;
      } else {
        return str.charAt(0);
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <T> T get(Integer fieldIndex, Class<T> cls) {
    if (cls == String.class) {
      return (T) getString(fieldIndex);
    } else if (cls == BigDecimal.class) {
      return (T) getBigDecimal(fieldIndex);
    } else if (cls == Boolean.class) {
      return (T) getBoolean(fieldIndex);
    } else if (cls == Byte.class) {
      return (T) getByte(fieldIndex);
    } else if (cls == Short.class) {
      return (T) getShort(fieldIndex);
    } else if (cls == Integer.class) {
      return (T) getInteger(fieldIndex);
    } else if (cls == Long.class) {
      return (T) getLong(fieldIndex);
    } else if (cls == Float.class) {
      return (T) getFloat(fieldIndex);
    } else if (cls == Double.class) {
      return (T) getDouble(fieldIndex);
    } else if (cls == BigInteger.class) {
      return (T) getBigInteger(fieldIndex);
    } else if (cls == Date.class) {
      return (T) getDate(fieldIndex);
    } else if (cls == Timestamp.class) {
      return (T) getTimestamp(fieldIndex);
    } else if (cls == Reader.class) {
      return (T) getReader(fieldIndex);
    } else if (cls == InputStream.class) {
      return (T) getInputStream(fieldIndex);
    } else if (cls == Character.class) {
      return (T) getCharacter(fieldIndex);
    }

    return ValueConverter.convert(get(fieldIndex), cls);
  }

  private BigInteger getBigInteger(Integer fieldIndex) {
    try {
      BigDecimal bigDecimal = resultSet.getBigDecimal(fieldIndex + 1);
      return bigDecimal == null ? null : bigDecimal.toBigInteger();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Boolean getBoolean(Integer fieldIndex, Boolean ifNull) {
    Boolean value = getBoolean(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Byte getByte(Integer fieldIndex, Byte ifNull) {
    Byte value = getByte(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Short getShort(Integer fieldIndex, Short ifNull) {
    Short value = getShort(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Integer getInteger(Integer fieldIndex, Integer ifNull) {
    Integer value = getInteger(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Long getLong(Integer fieldIndex, Long ifNull) {
    Long value = getLong(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Float getFloat(Integer fieldIndex, Float ifNull) {
    Float value = getFloat(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Double getDouble(Integer fieldIndex, Double ifNull) {
    Double value = getDouble(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Date getDate(Integer fieldIndex, Date ifNull) {
    Date value = getDate(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Timestamp getTimestamp(Integer fieldIndex, Timestamp ifNull) {
    Timestamp value = getTimestamp(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Reader getReader(Integer fieldIndex, Reader ifNull) {
    Reader value = getReader(fieldIndex);
    return value == null ? ifNull : value;
  }

  public InputStream getInputStream(Integer fieldIndex, InputStream ifNull) {
    InputStream value = getInputStream(fieldIndex);
    return value == null ? ifNull : value;
  }

  public String getString(Integer fieldIndex, String ifNull) {
    String value = getString(fieldIndex);
    return value == null ? ifNull : value;
  }

  public BigDecimal getBigDecimal(Integer fieldIndex, BigDecimal ifNull) {
    BigDecimal value = getBigDecimal(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Character getCharacter(Integer fieldIndex, Character ifNull) {
    Character value = getCharacter(fieldIndex);
    return value == null ? ifNull : value;
  }

  public BigInteger getBigInteger(Integer fieldIndex, BigInteger ifNull) {
    BigInteger value = getBigInteger(fieldIndex);
    return value == null ? ifNull : value;
  }

  public Boolean getBoolean(String columnName, Boolean ifNull) {
    Boolean value = getBoolean(columnName);
    return value == null ? ifNull : value;
  }

  public Boolean getBoolean(String columnName) {
    return getBoolean(getColumnIndex(columnName));
  }

  public Byte getByte(String columnName, Byte ifNull) {
    Byte value = getByte(columnName);
    return value == null ? ifNull : value;
  }

  public Byte getByte(String columnName) {
    return getByte(getColumnIndex(columnName));
  }

  public Short getShort(String columnName, Short ifNull) {
    Short value = getShort(columnName);
    return value == null ? ifNull : value;
  }

  public Short getShort(String columnName) {
    return getShort(getColumnIndex(columnName));
  }

  public Integer getInteger(String columnName, Integer ifNull) {
    Integer value = getInteger(columnName);
    return value == null ? ifNull : value;
  }

  public Integer getInteger(String columnName) {
    return getInteger(getColumnIndex(columnName));
  }

  public Long getLong(String columnName, Long ifNull) {
    Long value = getLong(columnName);
    return value == null ? ifNull : value;
  }

  public Long getLong(String columnName) {
    return getLong(getColumnIndex(columnName));
  }

  public Float getFloat(String columnName, Float ifNull) {
    Float value = getFloat(columnName);
    return value == null ? ifNull : value;
  }

  public Float getFloat(String columnName) {
    return getFloat(getColumnIndex(columnName));
  }

  public Double getDouble(String columnName, Double ifNull) {
    Double value = getDouble(columnName);
    return value == null ? ifNull : value;
  }

  public Double getDouble(String columnName) {
    return getDouble(getColumnIndex(columnName));
  }

  public Date getDate(String columnName, Date ifNull) {
    Date value = getDate(columnName);
    return value == null ? ifNull : value;
  }

  public Date getDate(String columnName) {
    return getDate(getColumnIndex(columnName));
  }

  public Timestamp getTimestamp(String columnName, Timestamp ifNull) {
    Timestamp value = getTimestamp(columnName);
    return value == null ? ifNull : value;
  }

  public Timestamp getTimestamp(String columnName) {
    return getTimestamp(getColumnIndex(columnName));
  }

  public Reader getReader(String columnName, Reader ifNull) {
    Reader value = getReader(columnName);
    return value == null ? ifNull : value;
  }

  public Reader getReader(String columnName) {
    return getReader(getColumnIndex(columnName));
  }

  public InputStream getInputStream(String columnName, InputStream ifNull) {
    InputStream value = getInputStream(columnName);
    return value == null ? ifNull : value;
  }

  public InputStream getInputStream(String columnName) {
    return getInputStream(getColumnIndex(columnName));
  }

  public String getString(String columnName, String ifNull) {
    String value = getString(columnName);
    return value == null ? ifNull : value;
  }

  public String getString(String columnName) {
    return getString(getColumnIndex(columnName));
  }

  public BigDecimal getBigDecimal(String columnName, BigDecimal ifNull) {
    BigDecimal value = getBigDecimal(columnName);
    return value == null ? ifNull : value;
  }

  public BigDecimal getBigDecimal(String columnName) {
    return getBigDecimal(getColumnIndex(columnName));
  }

  public Character getCharacter(String columnName, Character ifNull) {
    Character value = getCharacter(columnName);
    return value == null ? ifNull : value;
  }

  public Character getCharacter(String columnName) {
    return getCharacter(getColumnIndex(columnName));
  }

  public BigInteger getBigInteger(String columnName, BigInteger ifNull) {
    BigInteger value = getBigInteger(columnName);
    return value == null ? ifNull : value;
  }

  public BigInteger getBigInteger(String columnName) {
    return getBigInteger(getColumnIndex(columnName));
  }

  public List<Object> toList() {
    List<Object> data = new ArrayList<>(columns.size());
    for (int i = 0; i < columns.size(); i++) {
      data.add(get(i + 1));
    }
    return data;
  }
}
