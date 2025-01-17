package com.jms.jmscore;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DatabaseService {
    int queryInsert(String query, Set<Object[]> datas) throws SQLException;

    int queryUpdate(String query, Set<String> datas) throws SQLException;

    List<List<String>> queryData(String query, String[] columnNameArr, boolean withHeader);
}
