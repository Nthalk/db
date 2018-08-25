package com.iodesystems.db.data;

import com.iodesystems.db.logic.Converter;

public interface RecordMapper<TO> extends Converter<RecordCursor, TO> {

  TO map(RecordCursor record);
}
