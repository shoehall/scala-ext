package scala.io.jdbc

import java.io.PrintWriter

trait HasLog {
  def getLogWriter: PrintWriter = new PrintWriter(System.out, true)
}
