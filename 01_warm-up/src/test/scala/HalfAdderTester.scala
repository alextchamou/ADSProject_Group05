// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/** 
  * Half adder tester
  * Use the truth table from the exercise sheet to test all possible input combinations and the corresponding results exhaustively
  */
class HalfAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "HalfAdder" should "work" in {
    test(new HalfAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val testCases = Seq(
        (false.B, false.B, false.B, false.B), // 0 + 0
        (false.B, true.B,  true.B,  false.B), // 0 + 1
        (true.B,  false.B, true.B,  false.B), // 1 + 0
        (true.B,  true.B,  false.B, true.B)   // 1 + 1
      )
      for ((a, b, expectedSum, expectedCarry) <- testCases) {
        dut.io.a.poke(a)
        dut.io.b.poke(b)

        // step 1 cycle so the waveform shows the change
        dut.clock.step()

        dut.io.sum.expect(expectedSum)
        dut.io.carry.expect(expectedCarry)
      }

    }
    } 
}

