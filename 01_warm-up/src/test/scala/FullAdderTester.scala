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
  * Full adder tester
  * Use the truth table from the exercise sheet to test all possible input combinations and the corresponding results exhaustively
  */
class FullAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "FullAdder" should "work" in {
    test(new FullAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val testCases = Seq(
        // a      b      cin    expSum  expCout
        (false.B, false.B, false.B, false.B, false.B), // 0 + 0 + 0
        (false.B, false.B, true.B,  true.B,  false.B), // 0 + 0 + 1
        (false.B, true.B,  false.B, true.B,  false.B), // 0 + 1 + 0
        (false.B, true.B,  true.B,  false.B, true.B),  // 0 + 1 + 1
        (true.B,  false.B, false.B, true.B,  false.B), // 1 + 0 + 0
        (true.B,  false.B, true.B,  false.B, true.B),  // 1 + 0 + 1
        (true.B,  true.B,  false.B, false.B, true.B),  // 1 + 1 + 0
        (true.B,  true.B,  true.B,  true.B,  true.B)   // 1 + 1 + 1
      )
      for ((a, b, cin, expectedSum, expectedCout) <- testCases) {
        // Apply inputs
        dut.io.a.poke(a)
        dut.io.b.poke(b)
        dut.io.cin.poke(cin)

        // One clock step so changes appear in the VCD and propagate
        dut.clock.step()

        // Check outputs
        dut.io.sum.expect(expectedSum)
        dut.io.cout.expect(expectedCout)
      }

        }
    } 
}

