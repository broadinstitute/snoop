package org.broadinstitute.dsde.snoop

import java.io.File

import org.scalatest.{Matchers, FlatSpec}

import scala.collection.immutable.Vector

/**
 * Created by dvoet on 4/5/15.
 */
class GcsOutputRepositorySpec extends FlatSpec with Matchers {
  // ignored because travis does not have credentials
  ignore should "list correct files" in {
    val outputRepository = GcsOutputRepository("806222273987-s85bmsenu8ipjjgaj1jq4nna60ql1l1u@developer.gserviceaccount.com", new File("/Users/dvoet/.snoop/dsde-79e6ca4ff051.p12"))

    val results = outputRepository.listOutputs("broad-dsde-dev-private", "snoop-unit-test")

    assertResult(Vector("snoop-unit-test/", "snoop-unit-test/bar.txt", "snoop-unit-test/foo.txt", "snoop-unit-test/splat/", "snoop-unit-test/splat/bar.txt", "snoop-unit-test/splat/foo.txt")) { results.sorted }
  }
}
