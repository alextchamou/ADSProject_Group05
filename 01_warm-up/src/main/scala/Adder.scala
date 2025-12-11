// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._


/** 
  * Half Adder Class 
  * 
  * Your task is to implement a basic half adder as presented in the lecture.
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class HalfAdder extends Module{
  
  val io = IO(new Bundle {
    val a = Input(Bool())
    val b = Input(Bool())
    val sum = Output(Bool())
    val carry = Output(Bool())
    })
  io.sum := io.a ^ io.b  //XOR
  io.carry := io.a & io.b //AND

}

/** 
  * Full Adder Class 
  * 
  * Your task is to implement a basic full adder. The component's behaviour should 
  * match the characteristics presented in the lecture. In addition, you are only allowed 
  * to use two half adders (use the class that you already implemented) and basic logic 
  * operators (AND, OR, ...).
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FullAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(Bool())
    val b = Input(Bool())
    val cin = Input(Bool())
    val sum = Output(Bool())
    val cout = Output(Bool())
    })
  // Instanciate the two half adders that will be used to implement FA
  val ha1 = Module (new HalfAdder())
  val ha2 = Module (new HalfAdder())

  // connect input
  ha1.io.a := io.a
  ha1.io.b := io.b

  ha2.io.a := ha1.io.sum
  ha2.io.b := io.cin

  //output sum from second half adder
  io.sum := ha2.io.sum

  // OR the two carry outputs
  io.cout := ha1.io.carry | ha2.io.carry

}

/** 
  * 4-bit Adder class 
  * 
  * Your task is to implement a 4-bit ripple-carry-adder. The component's behaviour should 
  * match the characteristics presented in the lecture.  Remember: An n-bit adder can be 
  * build using one half adder and n-1 full adders.
  * The inputs and the result should all be 4-bit wide, the carry-out only needs one bit.
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FourBitAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val sum = Output(Vec(4,Bool()))
    val sumUInt = Output(UInt(4.W))   // I Would like to visualize the end result of the addition in the waves
    val carry = Output(Bool())
    })
  // Instance the modules
  val ha = Module(new HalfAdder())
  val fa1 = Module(new FullAdder())
  val fa2 = Module(new FullAdder())
  val fa3 = Module(new FullAdder())

 // Bit 0 : Half Adder
  ha.io.a := io.a(0)
  ha.io.b := io.b(0)
  io.sum(0) := ha.io.sum  //LSB sum
  val c0 = ha.io.carry  //carry out to next stage

  // Bit 1 : Fullader  connect a1 & b1 to the Fulladder and connect the output to internal signals
  fa1.io.a := io.a(1)
  fa1.io.b := io.b(1)
  fa1.io.cin := c0
  io.sum(1) := fa1.io.sum
  val c1 = fa1.io.cout

   // Bit 2 : Fulladder
  fa2.io.a := io.a(2)
  fa2.io.b := io.b(2)
  fa2.io.cin := c1
  io.sum(2) := fa2.io.sum
  val c2 = fa2.io.cout

  // Bit 3 : Fulladder
  fa3.io.a := io.a(3)
  fa3.io.b := io.b(3)
  fa3.io.cin := c2
  io.sum(3) := fa3.io.sum
  io.carry := fa3.io.cout
  //  Combine bits into a UInt for Surfer
  io.sumUInt := io.sum.asUInt
}
