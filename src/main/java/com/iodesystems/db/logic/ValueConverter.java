package com.iodesystems.db.logic;

import com.iodesystems.db.errors.DbException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ValueConverter<FROM, TO> {

  private final Class<FROM> from;
  private final Class<TO> to;

  private static final Map<Class<?>, Map<Class<?>, ValueConverter<?, ?>>> CACHE = new HashMap<>();
  private static final List<ValueConverter<?, ?>> CONVERTERS = new ArrayList<>();

  public ValueConverter(Class<FROM> from, Class<TO> to) {
    this.from = from;
    this.to = to;
  }

  public static void register(ValueConverter<?, ?> fieldConverter) {
    CONVERTERS.add(fieldConverter);
  }

  private static <FROM, TO> ValueConverter<FROM, TO> get(Class<FROM> from, Class<TO> to) {
    Map<Class<?>, ValueConverter<?, ?>> dbCache = CACHE.get(from);
    if (dbCache != null) {
      ValueConverter<?, ?> fieldConverter = dbCache.get(to);
      if (fieldConverter != null) {
        return (ValueConverter<FROM, TO>) fieldConverter;
      }
    }
    // Add the cache for this case
    for (ValueConverter<?, ?> converter : CONVERTERS) {
      if (converter.from.isAssignableFrom(from) && converter.to.isAssignableFrom(to)) {
        if (dbCache == null) {
          dbCache = new HashMap<>();
          CACHE.put(from, dbCache);
        }
        dbCache.put(to, converter);
        return (ValueConverter<FROM, TO>) converter;
      }
    }
    throw new DbException(
        "Could not find converter for " + from.getSimpleName() + " to " + to.getSimpleName());
  }

  public static <T> T convert(Object value, Class<T> cls) {
    if (value == null) {
      return null;
    }
    if (cls.isAssignableFrom(value.getClass())) {
      return (T) value;
    }
    return get(value.getClass(), cls).sneakyConvert(value);
  }

  public abstract TO convert(FROM value);

  private TO sneakyConvert(Object value) {
    return convert((FROM) value);
  }
}
