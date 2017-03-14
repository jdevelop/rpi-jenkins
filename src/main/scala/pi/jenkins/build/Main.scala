package pi.jenkins.build

import java.util.{Timer, TimerTask}

import com.pi4j.io.gpio._
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.ScallopException
import pi.jenkins.build.BuildStatusObserver.{Observer, PIStatusObserver}
import pi.jenkins.build.JenkinsAdapter.{BuildStatus, JenkinsBuildAdapter}

import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {

    object CLI extends ScallopConf(args) {

      val usePi = toggle("use-pi", default = Some(true))

      val jenkinsUrl = opt[String]("jenkins-url", required = false)

      val jenkinsOk = opt[String]("jenkins-debug-ok", required = false)

      val jenkinsFailed = opt[String]("jenkins-debug-failed", required = false)

      import org.rogach.scallop.singleArgConverter

      val jenkinsPollInterval = opt[Duration]("jenkins-poll-interval", required = true)(singleArgConverter(Duration.apply))

      val piSuccess = opt[Int]("pi-success")

      val piFailure = opt[Int]("pi-failure")

      override protected def onError(e: Throwable): Unit = e match {
        case ScallopException(cause) ⇒
          Console.err.println(cause)
          printHelp()
          sys.exit(1)
        case _ ⇒ super.onError(e)
      }
    }

    CLI.verify()

    case class Runner(buildObserver: JenkinsAdapter.JenkinsBuildAdapter, statusObserver: BuildStatusObserver.Observer) {

      def run(): Unit = {

        val buildId = buildObserver.getLastBuildId()

        buildObserver.getLastBuildStatus(buildId) match {
          case JenkinsAdapter.BuildFailure ⇒
            statusObserver.buildFailure(buildId)
          case JenkinsAdapter.BuildSuccessful ⇒
            statusObserver.buildSuccess(buildId)
        }

      }

    }

    val buildObserver: JenkinsBuildAdapter = if (!CLI.jenkinsUrl.isSupplied) {
      new JenkinsBuildAdapter {

        var build = 0

        override def getLastBuildId(): Int = build

        override def getLastBuildStatus(id: Int): BuildStatus = {
          build = id + 1
          id % 2 match {
            case 0 ⇒ JenkinsAdapter.BuildSuccessful
            case 1 ⇒ JenkinsAdapter.BuildFailure
          }
        }
      }
    } else {
      ???
    }

    val piAdapter = if (CLI.usePi()) {
      val gpio = GpioFactory.getInstance()
      val pinSuccessId = RaspiPin.allPins().find(_.getAddress == CLI.piSuccess()).get
      val pinFailureId = RaspiPin.allPins().find(_.getAddress == CLI.piFailure()).get

      def configurePin(p: Pin) = {
        val pin = gpio.provisionDigitalOutputPin(p, PinState.LOW)
        pin.setShutdownOptions(true, PinState.LOW)
        pin
      }

      new PIStatusObserver {
        override val pinFailure: GpioPinDigitalOutput = configurePin(pinFailureId)

        override val pinSuccess: GpioPinDigitalOutput = configurePin(pinSuccessId)
      }

    } else {

      new Observer {
        override def buildFailure(buildId: Int): Unit = Console.err.println(s"Failed build ${buildId}")

        override def buildSuccess(buildId: Int): Unit = Console.err.println(s"Successful build ${buildId}")
      }

    }

    val runner = Runner(buildObserver, piAdapter)

    val timer = new Timer(false)

    Console.out.println("Starting build monitoring")

    timer.schedule(new TimerTask {
      override def run(): Unit = runner.run()
    }, 0, CLI.jenkinsPollInterval().toMillis)

  }

}
