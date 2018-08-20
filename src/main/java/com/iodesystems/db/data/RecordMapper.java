package com.iodesystems.db.data;

public interface RecordMapper<TO> {

  TO map(RecordCursor record);
}
