/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.components.jdbc.graalvm;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.IntStream;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class CreateDB {

    public static void main(final String[] args) throws Exception { // todo: adapt to h2
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        try (final Connection connection = DriverManager.getConnection("jdbc:derby:/tmp/db-test/my-db;create=true", "sa", "");
                final Statement statement = connection.createStatement()) {
            statement.addBatch("CREATE TABLE Student(" + "Id INT NOT NULL GENERATED ALWAYS AS IDENTITY," + "Age INT NOT NULL,"
                    + "First_Name VARCHAR(255)," + "last_name VARCHAR(255)," + "PRIMARY KEY (Id))");
            statement.addBatch("insert into Student(Age,First_Name,last_name) values(20,'Romain', 'Manni')");
            statement.addBatch("insert into Student(Age,First_Name,last_name) values(20,'Gary', 'Moore')");
            log.info("Executed: {}", IntStream.of(statement.executeBatch()).boxed().collect(toList()));
        }
    }
}
