// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.util._


/** Controller: main state machine (Moore) */
class Controller extends Module {

  val io = IO(new Bundle {
    // inputs
    val rxd       = Input(Bool())   // serial input line
    val bitDone   = Input(Bool())   // high for one cycle when 8th bit has been counted
    val resetLine = Input(Bool())   // external reset for the receiver (abort frame)

    // control outputs
    val shiftEn   = Output(Bool())  // enable shifting in ShiftRegister
    val countEn   = Output(Bool())  // enable counting in Counter
    val clearRegs = Output(Bool())  // synchronous clear for counter + shift register
    val valid     = Output(Bool())  // pulse: new byte available
  })

  // States: Idle → Receiving → Valid (Moore machine)
  val sIdle :: sReceive :: sValid :: Nil = Enum(3)
  val state = RegInit(sIdle)

  // default outputs
  io.shiftEn   := false.B
  io.countEn   := false.B
  io.clearRegs := false.B
  io.valid     := false.B

  switch(state) {
    is(sIdle) {
      // Wait for start bit: line goes low (rxd = 0)
      // On resetLine, we just stay in Idle and ensure things will be cleared
      when(io.resetLine) {
        io.clearRegs := true.B
        state        := sIdle
      }.elsewhen(io.rxd === false.B) {  // start bit detected
        // Clear internal state (counter + shift register), start new frame
        io.clearRegs := true.B
        state        := sReceive
      }
    }

    is(sReceive) {
      // We are currently receiving 8 data bits
      io.shiftEn := true.B
      io.countEn := true.B

      when(io.resetLine) {
        // Abort current frame
        io.clearRegs := true.B
        state        := sIdle
      }.elsewhen(io.bitDone) {
        // 8th bit has just been processed
        state := sValid
      }
    }

    is(sValid) {
      // One complete byte has been received
      io.valid := true.B  // Moore: valid depends only on state

      when(io.resetLine) {
        io.clearRegs := true.B
        state        := sIdle
      }.otherwise {
        // After one cycle in VALID, go back to Idle and wait for next start bit
        state := sIdle
      }
    }
  }
}


/** Counter: counts 8 received data bits */
class Counter extends Module {

  val io = IO(new Bundle {
    val enable = Input(Bool()) // count this cycle
    val clear  = Input(Bool()) // reset counter to zero
    val done   = Output(Bool()) // one-cycle pulse when the 8th bit was counted
  })

  // 3-bit counter: 0 .. 7
  val cnt = RegInit(0.U(3.W))

  when(io.clear) {
    cnt := 0.U
  }.elsewhen(io.enable) {
    when(cnt === 7.U) {
      // We can wrap or keep it at 7; "done" is derived combinationally
      cnt := 0.U
    }.otherwise {
      cnt := cnt + 1.U
    }
  }

  // "done" when we are counting the 8th bit (counter was at 7 and enable is high)
  io.done := (cnt === 7.U) && io.enable
}


/** Shift Register: collects the 8 serial bits, MSB first */
class ShiftRegister extends Module {

  val io = IO(new Bundle {
    val shiftEn  = Input(Bool())    // shift on this cycle
    val clear    = Input(Bool())    // synchronous clear
    val serialIn = Input(Bool())    // rxd
    val dataOut  = Output(UInt(8.W)) // 8-bit parallel result
  })

  val reg = RegInit(0.U(8.W))

  when(io.clear) {
    reg := 0.U
  }.elsewhen(io.shiftEn) {
    // MSB is transmitted first.
    // We shift left and insert the new bit at LSB so that after 8 bits:
    // first bit received → bit 7, last bit → bit 0.
    reg := Cat(reg(6, 0), io.serialIn)
  }

  io.dataOut := reg
}


/**
 * ReadSerial: top-level serial receiver
 *
 * Behaviour:
 * - Idle line: rxd = 1
 * - Frame: start bit 0, then 8 data bits (MSB first)
 * - No parity, no stop bit
 * - After last data bit:
 *     - io.data contains the received byte
 *     - io.valid goes high for exactly one clock cycle
 * - If io.reset = 1 during reception: abort frame, clear internal state,
 *   wait for next start bit.
 */
class ReadSerial extends Module {

  val io = IO(new Bundle {
    val rxd   = Input(Bool())      // serial input
    val reset = Input(Bool())      // abort signal (not the global reset)
    val data  = Output(UInt(8.W))  // received byte
    val valid = Output(Bool())     // new-byte-ready pulse
  })

  // Instantiate submodules
  val controller    = Module(new Controller)
  val counter       = Module(new Counter)
  val shiftRegister = Module(new ShiftRegister)

  // Connect controller inputs
  controller.io.rxd       := io.rxd
  controller.io.bitDone   := counter.io.done
  controller.io.resetLine := io.reset

  // Connect counter
  counter.io.enable := controller.io.countEn
  counter.io.clear  := controller.io.clearRegs

  // Connect shift register
  shiftRegister.io.shiftEn  := controller.io.shiftEn
  shiftRegister.io.clear    := controller.io.clearRegs
  shiftRegister.io.serialIn := io.rxd

  // Top-level outputs
  io.data  := shiftRegister.io.dataOut
  io.valid := controller.io.valid
}
