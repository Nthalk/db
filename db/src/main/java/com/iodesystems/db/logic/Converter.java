package com.iodesystems.db.logic;

public interface Converter<FROM, TO> {

  TO map(FROM from);
}
