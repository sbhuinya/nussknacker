package pl.touk.esp.engine.process.functional


import java.util.Date

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.scalatest.{FlatSpec, Matchers}
import pl.touk.esp.engine.build.{EspProcessBuilder, GraphBuilder}
import pl.touk.esp.engine.process.ProcessTestHelpers.{MockService, SimpleRecord, SimpleRecordWithPreviousValue, processInvoker}
import pl.touk.esp.engine.spel

class CustomNodeProcessSpec extends FlatSpec with Matchers {

  import spel.Implicits._

  it should "fire alert when aggregate threshold exceeded" in {

    val process = EspProcessBuilder.id("proc1")
      .exceptionHandler()
      .source("id", "input")
      .customNode("custom", "outRec", "stateCustom", "keyBy" -> "#input.id", "stringVal" -> "'terefere'")
      .filter("delta", "#outRec.record.value1 > #outRec.previous + 5")
      .processor("proc2", "logService", "all" -> "#outRec")
      .sink("out", "monitor")

    val data = List(
      SimpleRecord("1", 3, "a", new Date(0)),
      SimpleRecord("1", 5, "b", new Date(1000)),
      SimpleRecord("1", 12, "d", new Date(4000)),
      SimpleRecord("1", 14, "d", new Date(10000)),
      SimpleRecord("1", 20, "d", new Date(10000))

    )

    val env = StreamExecutionEnvironment.createLocalEnvironment(1)
    processInvoker.invoke(process, data, env)

    val mockData = MockService.data.toList.map(_.asInstanceOf[SimpleRecordWithPreviousValue])
    mockData.map(_.record.value1) shouldBe List(12L, 20L)
    mockData.map(_.added) shouldBe List("terefere", "terefere")

  }

  it should "be able to split after custom node" in {
    val process = EspProcessBuilder.id("proc1")
      .exceptionHandler()
      .source("id", "input")
      .customNode("custom", "outRec", "stateCustom", "keyBy" -> "#input.id", "stringVal" -> "'terefere'")
      .split("split",
        GraphBuilder.processorEnd("proc3", "logService", "all" -> "'allRec-' + #outRec.record.value1"),
        GraphBuilder.filter("delta", "#outRec.record.value1 > #outRec.previous + 5")
                .processor("proc2", "logService", "all" -> "#outRec.record.value1 + '-' + #outRec.added").sink("out", "monitor")
      )

    val data = List(
      SimpleRecord("1", 3, "a", new Date(0)),
      SimpleRecord("1", 5, "b", new Date(1000)),
      SimpleRecord("1", 12, "d", new Date(4000)),
      SimpleRecord("1", 14, "d", new Date(10000)),
      SimpleRecord("1", 20, "d", new Date(10000))

    )

    val env = StreamExecutionEnvironment.createLocalEnvironment(1)
    processInvoker.invoke(process, data, env)

    val (allMocked, filteredMocked) = MockService.data.toList.map(_.asInstanceOf[String]).partition(_.startsWith("allRec-"))
    allMocked shouldBe List("allRec-3", "allRec-5", "allRec-12", "allRec-14", "allRec-20")
    filteredMocked shouldBe List("12-terefere", "20-terefere")

  }

  it should "retain context after split" in {
    val process = EspProcessBuilder.id("proc1")
      .exceptionHandler()
      .source("id", "input")
      .buildSimpleVariable("a", "tv", "'alamakota'")
      .split("split",
        GraphBuilder.processorEnd("proc3", "logService", "all" -> "'f1-' + #tv"),
        GraphBuilder.processorEnd("proc4", "logService", "all" -> "'f2-' + #tv")
      )

    val data = List(
      SimpleRecord("1", 3, "a", new Date(0))
    )

    val env = StreamExecutionEnvironment.createLocalEnvironment(1)
    processInvoker.invoke(process, data, env)

    val all = MockService.data.toSet
    all shouldBe Set("f1-alamakota", "f2-alamakota")


  }

  it should "be able to pass former context" in {
    val process = EspProcessBuilder.id("proc1")
      .exceptionHandler()
      .source("id", "input")
       .buildSimpleVariable("testVar", "beforeNode", "'testBeforeNode'")
      .customNode("custom", "outRec", "stateCustom", "keyBy" -> "#input.id", "stringVal" -> "'terefere'")
      .processorEnd("proc2", "logService", "all" -> "#beforeNode")

    val data = List(SimpleRecord("1", 3, "a", new Date(0)))
    val env = StreamExecutionEnvironment.createLocalEnvironment(1)
    processInvoker.invoke(process, data, env)

    MockService.data.toList shouldBe List("testBeforeNode")

  }

}