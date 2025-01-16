package com.jms.jmscore;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DatabaseServiceImpl implements DatabaseService {
    private Connection connection;

    public DatabaseServiceImpl(Connection connection) throws SQLException {
        this.connection = connection;
    }

    @Override
    public int queryInsert(String query, Set<Object[]> datas) throws SQLException {
        PreparedStatement prepareStatement = null;
        int[] rowsAffected = new int[100];

        try {
            connection.setAutoCommit(false);
            prepareStatement = connection.prepareStatement(query);

            if (datas != null && datas.size() != 0) {
                for (Object[] data : datas) {
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] instanceof String) {
                            prepareStatement.setString(i + 1, (String) data[i]);
                        } else if (data[i] instanceof Long) {
                            prepareStatement.setLong(i + 1, (Long) data[i]);
                        } else if (data[i] instanceof Timestamp) {
                            prepareStatement.setTimestamp(i + 1, (Timestamp) data[i]);
                        } else if (data[i] instanceof Date) {
                            prepareStatement.setDate(i + 1, (Date) (data[i]));
                        } else {
                            prepareStatement.setNull(i + 1, java.sql.Types.NULL);
                        }
                    }
                    prepareStatement.addBatch();
                }
            }

            rowsAffected = prepareStatement.executeBatch();
            connection.commit();
        } catch (Exception e) {
            System.out.println("Error Insert : " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
                throw rollbackEx;
            }
            throw e;
        } finally {
            if (prepareStatement != null) {
                try {
                    prepareStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }

        return Arrays.stream(rowsAffected).sum();
    }

    @Override
    public List<List<String>> queryData(String query, String[] columnNameArr,
            boolean withHeader) {
        List<List<String>> results = new ArrayList<List<String>>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            int rowIndex = 0;

            if (columnNameArr != null && columnNameArr.length > 0) {
                while (resultSet.next()) {
                    List<String> row = new ArrayList<String>();

                    if (rowIndex == 0 && withHeader) {
                        List<String> headerRow = new ArrayList<String>();
                        for (int j = 0; j < columnNameArr.length; j++) {
                            headerRow.add(columnNameArr[j]);
                        }

                        results.add(headerRow);
                    }

                    for (int i = 0; i < columnNameArr.length; i++) {
                        String element = columnNameArr[i];
                        String data = "";
                        try {
                            data = resultSet.getString(element);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        row.add(data);
                    }

                    results.add(row);
                    rowIndex = rowIndex + 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return results;
    }

}
