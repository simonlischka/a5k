package de.bht.lischka.adminTool5k.extractors

import utest._

//@TODO: Fix shared tests
//@TODO: Move this to shared tests
object PidMatcherSuite extends utest.TestSuite {
  def tests = TestSuite {
    'ExtractorTestPositive {
      ("PID", "123") match {
        case PidMatcher(id: Int) => assert(id == 123)
        case _ => assert(false)
      }
    }

    'ExtractorTestNegative1 {
      ("XYZ", "123") match {
        case PidMatcher(id: Int) => assert(false)
        case _ => assert(true)
      }
    }

    'ExtractorTestNegative2 {
      ("PID", "notANumber") match {
        case PidMatcher(id: Int) => assert(false)
        case _ =>  assert(true)
      }
    }
  }
}
