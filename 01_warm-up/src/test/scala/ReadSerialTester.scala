package readserial

import chisel3._
import chiseltest._
import chiseltest.simulator.WriteVcdAnnotation
import org.scalatest.flatspec.AnyFlatSpec

class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {

  "ReadSerial" should "work" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // Helper: send one byte (start bit + 8 data bits, MSB first)
      // and watch valid during and after the frame.
      def sendAndCheckByte(byte: Int): Unit = {
        var seenValid = false

        // small helper: step clock and watch valid+data
        def stepAndWatch(): Unit = {
          dut.clock.step(1)
          if (dut.io.valid.peek().litToBoolean) {
            seenValid = true
            dut.io.data.expect(byte.U, s"data should be 0x${byte.toHexString} when valid is high")
          }
        }

        // make sure we're in idle first
        dut.io.reset.poke(false.B)
        dut.io.rxd.poke(true.B)
        stepAndWatch() // idle cycle

        // start bit (0)
        dut.io.rxd.poke(false.B)
        stepAndWatch()

        // 8 data bits, MSB first
        for (i <- 7 to 0 by -1) {
          val bit = ((byte >> i) & 0x1) == 1
          dut.io.rxd.poke(bit.B)
          stepAndWatch()
        }

        // a few idle cycles after frame (in case valid is Moore-style and comes late)
        dut.io.rxd.poke(true.B)
        for (_ <- 0 until 4) {
          stepAndWatch()
        }

        assert(seenValid, s"valid was never high while sending 0x${byte.toHexString}")
        // after the pulse, valid should eventually be low again
        dut.io.valid.expect(false.B, "valid should go low again after the pulse")
      }

      //
      // Test 1: single byte 0xA5
      //
      sendAndCheckByte(0xA5)

      //
      // Test 2: two bytes back-to-back
      //
      sendAndCheckByte(0x3C)
      sendAndCheckByte(0xF0)

      //
      // Test 3: reset in the middle of a frame, then send a clean byte
      //
      val aborted  = 0xAA
      val finalOne = 0x55

      // start aborted frame
      dut.io.reset.poke(false.B)
      dut.io.rxd.poke(true.B)
      dut.clock.step(1)

      // start bit
      dut.io.rxd.poke(false.B)
      dut.clock.step(1)

      // send a few bits of aborted
      for (i <- 7 to 4 by -1) {
        val bit = ((aborted >> i) & 0x1) == 1
        dut.io.rxd.poke(bit.B)
        dut.clock.step(1)
      }

      // reset in the middle
      dut.io.reset.poke(true.B)
      dut.clock.step(1)
      dut.io.reset.poke(false.B)
      dut.io.rxd.poke(true.B)
      dut.clock.step(1)

      // make sure no stray valid pulse right after the aborted frame
      dut.io.valid.expect(false.B, "no valid pulse expected right after abort")

      // now a clean byte must work
      sendAndCheckByte(finalOne)
    }
  }
}
