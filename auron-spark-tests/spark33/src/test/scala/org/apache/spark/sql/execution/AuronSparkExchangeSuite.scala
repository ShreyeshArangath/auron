/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution

import org.apache.spark.sql.SparkTestsSharedSessionBase
import org.apache.spark.sql.execution.exchange.{Exchange, ReusedExchangeExec}
import org.apache.spark.sql.internal.SQLConf

class AuronSparkExchangeSuite extends ExchangeSuite with SparkTestsSharedSessionBase {

  private def verifyExchangeReuse(
      plan: org.apache.spark.sql.execution.SparkPlan,
      expectedExchangeCount: Int,
      expectedReuseCount: Int): Unit = {
    val exchangeIds = plan.collectWithSubqueries { case e: Exchange => e.id }
    val reusedExchangeIds = plan.collectWithSubqueries { case re: ReusedExchangeExec =>
      re.child.id
    }

    assert(
      exchangeIds.size == expectedExchangeCount,
      s"Expected $expectedExchangeCount unique exchanges, got ${exchangeIds.size}")
    assert(
      reusedExchangeIds.size == expectedReuseCount,
      s"Expected $expectedReuseCount reused exchanges, got ${reusedExchangeIds.size}")
    assert(
      reusedExchangeIds.forall(exchangeIds.contains(_)),
      "All reused exchanges should reference existing exchanges")
  }

  testAuron("Exchange reuse across the whole plan with shuffle partition 2") {
    withSQLConf(
      SQLConf.ADAPTIVE_EXECUTION_ENABLED.key -> "false",
      SQLConf.SHUFFLE_PARTITIONS.key -> "2") {
      val df = sql("""
                     |SELECT
                     |  (SELECT max(a.key) FROM testData AS a JOIN testData AS b ON b.key = a.key),
                     |  a.key
                     |FROM testData AS a
                     |JOIN testData AS b ON b.key = a.key
      """.stripMargin)

      verifyExchangeReuse(
        df.queryExecution.executedPlan,
        expectedExchangeCount = 2,
        expectedReuseCount = 3)
    }
  }

  testAuron("Exchange reuse with multiple subqueries and different tables") {
    withSQLConf(
      SQLConf.ADAPTIVE_EXECUTION_ENABLED.key -> "false",
      SQLConf.SHUFFLE_PARTITIONS.key -> "2") {
      val df = sql("""
                     |SELECT
                     |  (SELECT min(a.key) FROM testData AS a JOIN testData AS b ON b.key = a.key),
                     |  (SELECT max(a.key) FROM testData AS a JOIN testData2 AS b ON b.a = a.key)
      """.stripMargin)

      verifyExchangeReuse(
        df.queryExecution.executedPlan,
        expectedExchangeCount = 4,
        expectedReuseCount = 2)
    }
  }
}
