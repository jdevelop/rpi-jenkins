package pi.jenkins.build

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

/**
  * User: Eugene Dzhurinsky
  * Date: 3/14/17
  */
@RunWith(classOf[JUnitRunner])
class JenkinsResponseParserTest extends FunSpec with Matchers {

  describe("JenkinsResponseParser") {

    it("should parse the build result") {
      JenkinsAdapter.JenkinsResponseParser.
        resolveLastBuildUrl(io.Source.fromResource("builds.json").getLines().mkString) should be("https://example.jenkins/jenkins/job/project-build/103/")
    }

    it("should parse failed build") {
      JenkinsAdapter.JenkinsResponseParser.
        parseBuildStatus(io.Source.fromResource("failure.json").getLines().mkString) should be(JenkinsAdapter.BuildFailure)
    }

    it("should parse successful build") {
      JenkinsAdapter.JenkinsResponseParser.
        parseBuildStatus(io.Source.fromResource("success.json").getLines().mkString) should be(JenkinsAdapter.BuildSuccessful)
    }

  }

}
