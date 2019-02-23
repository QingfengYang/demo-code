package com.robert.lab

object PartialFunctionApp {

  val divide = (x : Int) => 100/x

  val divide1 = new PartialFunction[Int, Int] {
    override def isDefinedAt(x: Int): Boolean = x != 0

    override def apply(v1: Int): Int = 100/v1
  }

  def abc(va: Int): Int = va

  def main(args: Array[String]): Unit = {
    val ret = divide1.applyOrElse(0, (x : Int) => 0)
    println(ret)
  }

}
