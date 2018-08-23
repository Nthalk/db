package com.iodesystems.db.logic;

import com.iodesystems.db.Db;
import com.iodesystems.db.errors.RollbackException;

public interface TransactionalResult<T> {

  T run(Db tDb) throws RollbackException;
}
