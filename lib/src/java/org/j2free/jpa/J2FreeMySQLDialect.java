/**
 * J2FreeMySQLDialect.java
 * 
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.jpa;

import org.hibernate.Hibernate;
import org.hibernate.dialect.MySQLInnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;

/**
 * @author Ryan Wilson
 */
public class J2FreeMySQLDialect extends MySQLInnoDBDialect {

    public J2FreeMySQLDialect() {
        super();
        registerFunction(
            "date_sub_interval",
            new SQLFunctionTemplate(
                Hibernate.DATE,
                "DATE_SUB(?1, INTERVAL ?2 ?3)"
            )
        );

        registerFunction(
            "days_ago_date",
            new SQLFunctionTemplate(
                Hibernate.DATE,
                "DATE_SUB(CURRENT_DATE, INTERVAL ?1 DAY)"
            )
        );
        
        registerFunction(
            "days_ago_timestamp",
            new SQLFunctionTemplate(
                Hibernate.DATE,
                "DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ?1 DAY)"
            )
        );

        registerFunction(
            "date_diff",
            new SQLFunctionTemplate(
                Hibernate.INTEGER,
                "DATEDIFF(?1,?2)"
            )
        );
    }
}
