package com.iodesystems.db.logic;

public interface Handler<T> {

  void handle(T map);
}
