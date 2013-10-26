package org.ocbkc.swift.logilang
{
import org.ocbkc.swift.logilang.query._
import System._
import java.io._
// import scala.util.parsing.combinator.Parsers._

/* Conventions:
Abbreviation for constitution: consti (const is to much similar to constant).

*/

// not complete FOL yet

// each FOL theory is associated with its own list of predicate and constant symbols, I.e. there may be more constants with the same name and id, as long as they are part
class FOLtheory extends FOLutils // with CTLdocument // <&y2013.10.26.17:32:47& define type CTLdocument>
{  // <&y2012.04.03.22:31:27& constants and predicates could also be represented as hashmaps for more efficiency>
   var constants:List[Constant]     = Nil // [&y2012.04.13.09:39:59& as far as I can see, I don't really need this list.]
   var predicates:List[Predicate]   = Nil // [&y2012.04.13.09:40:13& this is really needed, to check whether a new predicate-application MAY be added (arity-check)]
   var stats:List[FOLstatement]     = Nil
/*
   def clone:FOLtheory = 
   {  var f = new FOLtheory
      f
   }
*/

   def copy:FOLtheory = 
   {  val f = new FOLtheory
      f.stats=stats // make a copy of the reference to the List of stats.
      f
   }
   // convenience function to directly add a predicate statement based on the name of it
   def addPredAppByString(predicateName:String, terms:List[Constant]):Boolean =
   {  gocPredicate(predicateName, terms.length) match
      {  case Some(p)    =>  { addPredApp(PredApp(p, terms)); true }
         case None       =>  false
      }
   }
   //  Optimised addStat for when you know the stat is correct
   def addStatUnchecked(stat:FOLstatement) =
   {  stats = stats :+ stat
   }

   // Optimised addStat for when you know the stat is a PredApp
   def addPredApp(pa:PredApp):Boolean =
   {  val p = pa.correct
      if( p )  stats = stats :+ pa
      p
   }
   /* IN: Closed FOLstatement
      OUT: Boolean: successful or not. Not successful: predicate's arity is different then in a statement added earlier...
   */
   def addStat(stat:FOLstatement):Boolean =
   {  // <&y2012.03.30.09:40:20& check whether predicate has same arity as when it was added before>
      // <_& &y2012.03.30.10:00:58& should I allow more "languages" (with the same predicate ID (name), having different arities.) or not?>[A &y2012.05.12.12:59:01& that is the design decision I have taken: there may be more predicates with the same name, as long as they are not used in the same theory...]
      // <&y2012.04.27.09:40:15& check here of formula is closed)>
      val add:Boolean = stat match
      {  case pa@PredApp(p, cs) => { 
                                    val correctArity = pa.correct
                                    val closed = ( cs.find( { case _:Constant => false; case _ => true } ) == None )
                                    if(correctArity && closed) 
                                    {  addConstants(cs.asInstanceOf[List[Constant]])
                                       addPredicate(p)
                                       true
                                    }  else false   
                                   }
         case Equal(c1,c2)      => { addConstant(c1); addConstant(c2); true }
         case Unequal(c1,c2)    => { addConstant(c1); addConstant(c2); true }
      }

      if(add)  stats = stats :+ stat // <&y2012.04.10.09:33:10& inefficient? change this.>
      add
   }

   /*
   def cleanUpConstants =
   {  constants.map( 
   }
   */

   def removeStats(filterFunc:FOLstatement => Boolean) =
   {  stats = stats.filterNot( filterFunc )
   }

   def removeStat(stat:FOLstatement) =
   {  stats = stats.filterNot( s => s.equals(stat) )
      // <&y2012.04.13.09:42:15& remove constants that are now not used anymore. This may not be strictly necessary, but it is my current definition of constants.>
      /* stat match
      {  c // WIW <&y2012.04.13.09:32:51& current implementation makes it expensive to eliminate constants, perhaps change way constants are stored in the future, or add redundant info (e.g. a count of how many statements make use of a certain constant)>
      } */
   }
   /* OUT: None: all statements successfully added
           Some(i, stat): statement at index I in input caused error
   */
   def addStats(stats:List[FOLstatement]):Option[(FOLstatement, Int)] =
   {  stats.zipWithIndex.map( { case Tuple2(s,i) => { val success = addStat(s); (s, i, success) } }).find( p => !p._3 ) match // <&y2012.04.05.17:40:46& should be quite efficient if scala does this lazy, otherwise not so. Check this perhaps.>
      {  case None => None
         case Some(Tuple3(s,i,success)) => Some(Tuple2(s,i))
      }
   }

   def convertAndAddEqualStats(cs:List[Constant]) =
   {  stats = stats ++ Equal.convertTo2place(cs)
   }

   def convertAndAddUnequalStats(cs:List[Constant]) =
   {  stats = stats ++ Unequal.convertTo2place(cs)
   }

   def gocPredicate(name:String, arity:Long):Option[Predicate] =
   {  predicates.find( p => p.name.equals(name) ) match
      {  case op@Some(p) => if( p.arity == arity ) op
                         else None
                         
         case None    => { val newpred = Predicate(name, arity)
                           predicates = predicates :+ newpred
                           Some(newpred) 
                         }
      }
   }

   def gocConstants(names:List[String]):List[Constant] =
   {  names.map(gocConstant(_))
   }

   def gocConstant(name:String):Constant =
   {  constants.find( c => c.name.equals(name) ) match
      {  case opt@Some(c)  => c
         case None         => {  val newconstant = Constant(name)
                                 constants = constants :+ newconstant
                                 newconstant
                              }
      }
   }



   def addConstant(c:Constant) = 
   {  constants = addIfNotPresent(c, constants) // <&y2012.04.09.14:18:17& how efficient is this, is constant completely copied or are its elements reused?>
   }

   def addPredicate(p:Predicate) =
   {  predicates = addIfNotPresent(p, predicates) // <&y2012.04.09.14:18:17& how efficient is this, is constant completely copied or are its elements reused?>

   }

   def addConstants(cs:List[Constant]) =
   {  cs.map(addConstant(_))
   }

   // <&y2012.04.07.18:08:11& refactor: move to general lib this method
   def addIfNotPresent[A](i:A, ls:List[A]):List[A] =
   {  //println("addIfNotPresent")
      ls.find(item => item == i) match
      {  case None => { val r = ls :+ i; /* println("   " + r); */ r } // <&y2012.04.09.14:14:36& inefficient, change to listbuffer?>
         case _    => {  /*println("   " + ls);*/ ls }
      }
   }

   def getConstant(name:String):Option[Constant] =
   {  constants.find( c => c.name.equals(name) )
   }

   // c1 is replaced with c2, and after that elimated from theory
   def substituteConstant(c1:Constant, c2:Constant) =
   {  println("substituteConstant")
      println("   constants in: c1=" + c1 + " c2=" + c2)
      println("   stats in:" + stats)
      stats = stats.map( 
         { case PredApp(a, cs)    =>
           {   PredApp(a, substituteConstantInList(cs))
           }
           case Unequal(d1,d2)   =>  Unequal(substituteConstantSingle(d1).asInstanceOf[Constant], substituteConstantSingle(d2).asInstanceOf[Constant])
           case Equal(d1,d2)     =>  Equal(substituteConstantSingle(d1).asInstanceOf[Constant], substituteConstantSingle(d2).asInstanceOf[Constant])
         }
         )
      stats = stats.distinct
      constants = constants.filterNot(_ == c1)
      println("   stats becomes:" + stats)

      def substituteConstantInList(cs:List[Term]) = cs.map( substituteConstantSingle )

      def substituteConstantSingle(t:Term) = if( t == c1 ) c2 else t
   }

   override def toString =
   {  "FOLtheory(\n stats =\n" + stats.map(stat => stat.toString + "\n\n") + "constants = \n" + constants + "\n\n" + "predicates = \n" + predicates + "\n)"
   }

   // example: fof(form1, axiom, P(a,b))
   // <_&y2012.05.07.12:28:15& empty theory is translated incorrectly to ".", should simply become an empty string>
   def exportToTPTPfof:String =
   {  def stat2fof(stat:FOLstatement):String = stat match 
      {  case PredApp(p, cs)        => p.name + cs.asInstanceOf[List[Constant]].map(_.name).mkString("(",",",")") // <&y2012.04.27.19:42:05& cs must be constants, because statements are all closed formulae.
         case Equal(c1, c2)         => c1.name + " = " + c2.name
         case Unequal(c1, c2)       => c1.name + " != " + c2.name
      }

      if(stats.length == 0) ""
      else
      {  stats.zipWithIndex.map( { case (stat,i) => "fof(form" + i + ", axiom, " + stat2fof(stat) + ")" } ).mkString(".\n") + ".\n"
      }
   }
}



class FOLutils
{  // Convenience method: converts a list of constants to 2-ary equal statements between all of them

   def pairsFrom2Lists[A](l1:List[A], l2:List[A]):List[(A,A)] =
   {  for( i1 <- l1; i2 <- l2 ) yield (i1, i2)
   }

   // <&y2012.04.04.16:10:41& refactor: move method to generic lib>
   def conseqPairs[A](list:List[A]):List[(A,A)] =
   {  list match
      {  case x::(y::ys)    => (x,y) :: conseqPairs(y::ys) // <&y2012.04.04.16:15:27& can I use @ here to name the (y::ys) part?>
         case x::Nil        => List()
         case Nil           => List()
      }
   }

}

object FOLutils extends FOLutils
sealed class FOLstatement
case class Equal(cv1:Constant, cv2: Constant)      extends FOLstatement
{  override def toString =
   {  "Equal(" + cv1 + ", " + cv2 + ")"
   }
}
object Equal
{  def convertTo2place(cs:List[Constant]):List[Equal] =
   {  FOLutils.conseqPairs(cs).map(p => Equal(p._1, p._2))
   }
}


case class Unequal(cv1:Constant, cv2: Constant)    extends FOLstatement
{  override def toString =
   {  "Unequal(" + cv1 + ", " + cv2 + ")"
   }
}

object Unequal
{  def convertTo2place(cs:List[Constant]):List[Unequal] =
   {  for( c1 <- cs; c2 <- cs if( c2.name.compareTo(c1.name) > 0 )) yield Unequal(c1,c2)
   }
}

case class PredApp(p:Predicate, terms:List[Term]) extends FOLstatement // PredApp = predicate application
{  def correct:Boolean =
   {  this match 
      {  case PredApp(p,t) => p.arity == t.size
      }
   }
}
/*
class Term(val name:String)
{  override def toString =
   {  //"Constant(name = " + name + ")"
      "Term(name = " + name + ", id = " + hashCode + ")"
   }
}
*/

trait Term
{  val name:String
}

/*
class TermSerializer extends CustomSerializer[Term](format => (
         { 
           case JObject(JField("start", JInt(s)) :: JField("end", JInt(e)) :: Nil) => 
             new Interval(s.longValue, e.longValue) 
         },
         { 
           case x: Var =>
             JObject(JField("start", JInt(BigInt(x.startTime))) :: 
                     JField("end",   JInt(BigInt(x.endTime))) :: Nil) 
         }
       ))
*/
// <&y2012.04.10.19:18:02& make the constructor of class Cons private if possible>
// <&y2012.04.24.09:52:25& why not use a case class?>
case class Constant(name:String) extends Term
{  override def toString =
   {  "Constant(name = " + name + ")"
      //"Constant(name = " + name + ", id = " + hashCode + ")"
   }
}

case class Var(name:String) extends Term
{  override def toString =
   {  //"Constant(name = " + name + ")"
      "Var(name = " + name + ")" // , id = " + hashCode + ")"
   }
}

/*
object Constant
{  var constants:List[Constant] = Nil

   def apply(name:String):Constant =
   {  //println("Constant.apply")
      constants.find( c => c.name.equals(name) ) match
      {  case Some(c) => { /* println("   " + c); */ c }
         case None    => { val newc = new Constant(name)
                           constants = constants :+ newc
                           /* println("   " + constants) */
                           newc
                         }
      }
   }
}
*/

class Predicate(val name:String, val arity:Long) // <&y2012.04.05.00:32:52& how to make constructor private?>
{  override def toString =
   {  "Predicate(name = " + name + ", arity = " + arity + ")"
   }
}

// Predicate factory, keeps track of all predicates, only creates a new one when it is a new (name, arity) combination, otherwise it will return existing predicate symbol. Factory contains all predicates used by all theories (and also allows usage in solist "theory-less" statements).
object Predicate
{  var predicates:List[Predicate] = Nil

   def apply(name:String, arity:Long):Predicate =
   {  predicates.find( p => p.name.equals(name) && p.arity == arity ) match
      {  case Some(p) => p
                         
         case None    => { val newpred = new Predicate(name, arity)
                           predicates = predicates :+ newpred
                           newpred
                         }
      }


      // &y2012.04.05.00:33:36& also do this for constants, and then change the gocPredicate and gocConstant in FOLtheory, to never use new Predicate/Constant, but always this apply method.
   }

}

}
