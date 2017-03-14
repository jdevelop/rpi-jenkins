package pi.jenkins.build

object JenkinsAdapter {

  sealed trait BuildStatus

  case object BuildSuccessful extends BuildStatus

  case object BuildFailure extends BuildStatus

  trait JenkinsBuildAdapter {

    def getLastBuildId(): Int

    def getLastBuildStatus(id: Int): BuildStatus

  }

}
