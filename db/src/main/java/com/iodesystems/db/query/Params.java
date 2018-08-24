package com.iodesystems.db.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Params {

  private final Map<String, Object> source = new HashMap<>();
  private int generatedParamIndex;

  public Params(Params params) {
    this(params.generatedParamIndex);
    this.source.putAll(params.source);
  }

  public Params(int generatedParamIndex) {
    this.generatedParamIndex = generatedParamIndex;
  }

  public Params set(String key, Object value) {
    source.put(key, value);
    return this;
  }

  public String setPrefixed(String prefix, Object value) {
    String key = "_" + prefix + generatedParamIndex++;
    set(key, value);
    return ":" + key;
  }

  public Object get(String param) {
    return source.get(param);
  }

  public Set<String> keys() {
    return source.keySet();
  }
}
