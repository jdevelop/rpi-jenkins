package pi.jenkins.build

import com.pi4j.io.gpio.{GpioPinDigitalOutput, PinState}

object BuildStatusObserver {

  trait Observer {

    def buildSuccess(): Unit

    def buildFailure(): Unit

  }

  trait PIStatusObserver extends Observer {

    def pinSuccess(): GpioPinDigitalOutput

    def pinFailure(): GpioPinDigitalOutput

    override def buildSuccess(): Unit = {
      pinFailure().setState(PinState.LOW)
      pinSuccess().setState(PinState.HIGH)
    }

    override def buildFailure(): Unit = {
      pinFailure().setState(PinState.HIGH)
      pinSuccess().setState(PinState.LOW)
    }
  }

}
