/*
 *  Copyright 2017-2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.flint.timeseries

import com.twosigma.flint.timeseries.row.Schema
import org.apache.spark.sql.types.{ DoubleType, LongType, IntegerType }

class SummarizeIntervalsSpec extends MultiPartitionSuite with TimeSeriesTestData with TimeTypeSuite {

  override val defaultResourceDir: String = "/timeseries/summarizeintervals"

  "SummarizeInterval" should "pass `SummarizeSingleColumn` test." in {
    withAllTimeType {
      val volumeTSRdd = fromCSV(
        "Volume.csv", Schema("id" -> IntegerType, "volume" -> LongType, "v2" -> DoubleType)
      )

      volumeTSRdd.toDF.show()

      val clockTSRdd = fromCSV("Clock.csv", Schema())
      val resultTSRdd = fromCSV("SummarizeSingleColumn.results", Schema("volume_sum" -> DoubleType))

      def test(rdd: TimeSeriesRDD): Unit = {
        val summarizedVolumeTSRdd = rdd.summarizeIntervals(clockTSRdd, Summarizers.sum("volume"))
        summarizedVolumeTSRdd.toDF.show()
        assert(summarizedVolumeTSRdd.collect().deep == resultTSRdd.collect().deep)
      }

      withPartitionStrategy(volumeTSRdd)(DEFAULT)(test)
    }
  }

  it should "pass `SummarizeSingleColumnPerKey` test, i.e. with additional a single key." in {
    withAllTimeType {
      val volumeTSRdd = fromCSV(
        "Volume.csv", Schema("id" -> IntegerType, "volume" -> LongType, "v2" -> DoubleType)
      )

      val clockTSRdd = fromCSV("Clock.csv", Schema())
      val resultTSRdd = fromCSV(
        "SummarizeSingleColumnPerKey.results",
        Schema("id" -> IntegerType, "volume_sum" -> DoubleType)
      )

      val result2TSRdd = fromCSV(
        "SummarizeV2PerKey.results",
        Schema("id" -> IntegerType, "v2_sum" -> DoubleType)
      )

      def test(rdd: TimeSeriesRDD): Unit = {
        val summarizedVolumeTSRdd = rdd.summarizeIntervals(clockTSRdd, Summarizers.sum("volume"), Seq("id"))
        assertEquals(summarizedVolumeTSRdd, resultTSRdd)
        val summarizedV2TSRdd = rdd.summarizeIntervals(clockTSRdd, Summarizers.sum("v2"), Seq("id"))
        assertEquals(summarizedV2TSRdd, result2TSRdd)
      }

      withPartitionStrategy(volumeTSRdd)(DEFAULT)(test)
    }
  }

  it should "pass `SummarizeSingleColumnPerSeqOfKeys` test, i.e. with additional a sequence of keys." in {
    withAllTimeType {
      val volumeTSRdd = fromCSV(
        "VolumeWithIndustryGroup.csv",
        Schema("id" -> IntegerType, "group" -> IntegerType, "volume" -> LongType, "v2" -> DoubleType)
      )

      val clockTSRdd = fromCSV("Clock.csv", Schema())
      val resultTSRdd = fromCSV(
        "SummarizeSingleColumnPerSeqOfKeys.results",
        Schema("id" -> IntegerType, "group" -> IntegerType, "volume_sum" -> DoubleType)
      )

      def test(rdd: TimeSeriesRDD): Unit = {
        val summarizedVolumeTSRdd = rdd.summarizeIntervals(
          clockTSRdd,
          Summarizers.sum("volume"),
          Seq("id", "group")
        )
        assertEquals(summarizedVolumeTSRdd, resultTSRdd)
      }

      withPartitionStrategy(volumeTSRdd)(DEFAULT)(test)
    }
  }
}
