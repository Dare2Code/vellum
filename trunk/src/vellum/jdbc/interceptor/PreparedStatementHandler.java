/*
 * Source https://code.google.com/p/vellum by @evanxsummers

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements. See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.  
 */
package vellum.jdbc.interceptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 *
 * @author evan.summers
 */
public class PreparedStatementHandler {
    VConnection connection;
    PreparedStatement delegate;
    ArrayList<Object> parameterArray = new ArrayList();
    
    public PreparedStatementHandler(VConnection connection, PreparedStatement delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        return delegate.executeQuery(sql);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        parameterArray.set(parameterIndex, x);
        delegate.setTimestamp(parameterIndex, x);
        
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        parameterArray.set(parameterIndex, x);
        delegate.setString(parameterIndex, x);
    }

    public void set(PreparedStatement statement) throws SQLException {
        for (int i = 1; i < parameterArray.size(); i++) {
            statement.setObject(i, parameterArray.get(i));
        }
    }
    
}
