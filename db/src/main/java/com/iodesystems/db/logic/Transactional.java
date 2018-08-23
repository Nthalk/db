package com.iodesystems.db.logic;

import com.iodesystems.db.Db;
import com.iodesystems.db.errors.RollbackException;

public interface Transactional {

  void run(Db tDb) throws RollbackException;

}
