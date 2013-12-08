// <rename reas to prover>
package org.ocbkc.swift.reas.plosemo
{
import org.ocbkc.swift.reas
import org.ocbkc.swift.parser._
import org.ocbkc.swift.logilang._
import org.ocbkc.swift.logilang.query._
import org.ocbkc.swift.logilang.query.ComparisonOperator._
import org.ocbkc.swift.logilang.query.plosemo._
import org.ocbkc.swift.tpwrap._
import query._
import System.err.println
import java.io._
import org.ocbkc.swift.test.CLIwithFileInput
import org.ocbkc.swift.global.Logging._
import org.specs2.mutable._

/* &y2013.12.05.19:48:04& WIW:
- tried to run mvn specs2:run-specs, but there is nothing in target/test-classes, I think I have to put the PlosemoProverSpec class in another spot (see pom.xml!)

*/

/** @todo doesn't work yet, solve this
  */
class PlosemoProverSpec extends Specification
{  val predicateB = Predicate("B",1)
   val plosemoQuery = MostInfo(PatVar("s"), Forall(Var("x"), PatVar("s"), PredApp(predicateB, List(Var("x")))))
   val folTheory = new FOLtheory
   folTheory.addStat(PredApp_FOL(predicateB, List(Constant("makkelPowerConnect"))))

   val queryResult = Prover.query(plosemoQuery, folTheory)

   "Prover.query(plosemoQuery, folTheory) is null: " ! ( queryResult == null )
   
   /*
   "Prover.query(plosemoQuery, folTheory)" should
   {  " equal null, because we are still testing" in
      {  queryResult must beEqualTo(1)
      }
   }
   */
   /*
   "The 'Hello world' string" should {
 "contain 11 characters" in {
   "Hello world" must have size(11)
 }
 "start with 'Hello'" in {
   "Hello world" must startWith("Hello")
 }
 "end with 'world'" in {
   "Hello world" must endWith("world")
   */
}

object TestPlosemoProverCLI extends CLIwithFileInput
{  def main(args: Array[String]) =
   {  if( args.length != 0 ) println("Usage: command (without parameters)")
      else
      {  val predicateB = Predicate("B",1)
         val plosemoQuery = MostInfo(PatVar("s"), Forall(Var("x"), PatVar("s"), PredApp(predicateB, List(Var("x")))))
         val folTheory = new FOLtheory
         folTheory.addStat(PredApp_FOL(predicateB, List(Constant("makkelPowerConnect"))))
         Prover.query(plosemoQuery, folTheory)
      }
      /*
      if( args.length != 1 ) println("Usage: command filename")
      def f(folminquaFile:String):String =
      {  val ft:FOLtheory = Folminqua2FOLtheoryParser.parseAll(Folminqua2FOLtheoryParser.folminquaTheory, folminquaFile) match
         {  case Folminqua2FOLtheoryParser.Success(ftl,_)         => ftl
            case  failMsg@Folminqua2FOLtheoryParser.Failure(_,_)  => { "  parse error: " + failMsg.toString; new FOLtheory() }
         }
 
         var ret:String = ""

         val query = Sharpest(NumResPat(Geq, PatVar("n"), Var("x"), PredApp(Predicate("p",2),List(Constant("a"), Var("x"))))) // <&y2012.04.24.09:39:15& for test nice if you could also read this from the command line>

         ret += "#### Original theory:\n" + ft +"\n\n"
   
         ret += "#### Applying query\n\n"
         
         ret += "Query = " + query.toString + "\n"


         ret += "Answer = " + Prover.query(query, ft)

         ret
      }
      applyFunctionToFile(f, args(0))
      */
   }
}
object Prover extends reas.ProverTrait
{  def query(query:PlosemoPat, ft:FOLtheory) = // return type TODO
   {  log("reas.plosemo.Prover called")
      log("   query = " + query)

      // translate FOLtheory to FOF
      val ftFof = ft.exportToTPTPfof
      // translate query to FOF
      val queryFof = query match
      {  case MostInfo(patVar, forallPat) =>
         {  forallPat match
            {  case Forall(forallVar:Var, setPatVar:PatVar, predApp:PredApp) =>
               {  if( setPatVar != patVar )
                  {  log("   error in query, pattern variables are not the same")
                  } else
                  {  // transform to fof query
                     val fofPVname = "PV" + forallVar.name // <rename to official name of pattern variables in fof.> Possibly confusing: pattern variable in Plosemo != pattern variable in FOF (in this case)!
                     predApp match
                     {  case  PredApp(p, consts) =>
                        {  "fof(form" + ft.stats.length + ", question, ? [" + fofPVname + "] : " + p.name + consts.map( c => if( c.name.equals(forallVar) ) fofPVname else c.name ).mkString("(",",",")") + ")."
                        }
                     }
                  }
               }
            }
         }
      }
      val ftAndQueryFof = ftFof + queryFof
      println("\n####   FOLtheory translated to fof and added query in fof format:\n" + ftAndQueryFof)

      // write to file
      var outFile = new File("ft.fof")
      var fullpath = outFile.getAbsolutePath
      println("\n####  creating file: " + fullpath)
      var out:PrintWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
      out.print(ftAndQueryFof)
      out.flush
      out.close

      // apply eprover
      val eproverResult = Eprover("--cpu-limit=30 --memory-limit=Auto --tstp-format -s --answers " + fullpath)
      println("####   eprover's result =\n" + eproverResult)
      null // TODO finish
   }
}

}
