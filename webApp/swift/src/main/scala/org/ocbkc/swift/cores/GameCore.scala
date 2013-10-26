// change this when moving the project.// <&y2011.11.07.13:19:35& perhaps in future move fluencyChallenge to own package>
package org.ocbkc.swift.cores
{  
import org.ocbkc.swift.logilang._
import org.ocbkc.swift.logilang.query._
import org.ocbkc.swift.reas._
import org.ocbkc.swift.model._
import org.ocbkc.swift.global.GlobalConstant._
import org.ocbkc.swift.coord.ses._
import System._
import scala.sys.process._
import scala.util.matching._
import scala.util.matching.Regex._
import java.io._
//import java.lang._
import org.ocbkc.swift.parser._

import net.liftweb.json._
import net.liftweb.json.ext._
import scala.util.parsing.combinator.Parsers

/* Conventions:
- Names of classes correspond with design $JN/...

BS:
- Inforepresentations which form part of the *state* of the fluencyChallenge, are stored as property (and not passed as arguments to methods who are using them).
- For convenience, however, methods do return the requested inforeps, even if they can also be read from the fluencyChallenge state. 
*/

import Round._

// specifically intended for 'ad hoc' return types.
package fluencyChallengeHelperTypes
{  class QuestionAndCorrectAnswer(questionNL:String, questionCTLcomputer:String)
}

import fluencyChallengeHelperTypes._

trait FluencyChallenge
{  // SHOULDDO: how to initialize a val of this trait in a subclass of this trait? (would like to do that with playerId)
   val challengeName:String
   val playerId:Long
   var si:SessionInfo
   def getParserCTLsingleton:SWiFTparser
   val parserCTLsingleton = getParserCTLsingleton
   val parserCTL:Parser[SWiFTparser] // <String should be replaced with the right type>
   var parseWarningMsgTxtCTLplayer:String = ""
   var parseErrorMsgTextCTLplayer:String = ""

   def parseTextCTLbyPlayer:Boolean = 
   {  println("ParseTextCTLbyPlayer called")
      si.textCTLplayerUpdated4terParsing = false
      parseWarningMsgTxtCTLplayer = if(si.textCTLbyPlayer.equals("")) "Warning: empty file." else ""  // <&y2012.05.19.20:27:13& replace with regex for visually empty file (thus file with only space characters, like space, newline, tab etc.>

      // orginal: Folminqua2FOLtheoryParser.parseAll(Folminqua2FOLtheoryParser.folminquaTheory, textCTLbyPlayer) match
      parserCTLsingleton.parseAll(parserCTL, si.textCTLbyPlayer) match
         {  case parserCTLsingleton.Success(ftl,_)         => {  si.textCTLbyPlayerScalaFormat_ = Some(ftl)
                                                                        si.constantsByPlayer           = Some(ftl.constants.map({ case Constant(id) => id }))
                                                                        si.predsByPlayer               = Some(ftl.predicates.map(pred => pred.name))
                                                                        parseErrorMsgTextCTLplayer = ""
                                                                        true
                                                                     }
            case failMsg@getParserCTLsingleton.Failure(_,_)   => {  textCTLbyPlayerScalaFormat_   = None
                                                                        si.constantsByPlayer             = None
                                                                        si.predsByPlayer                 = None
                                                                        println("  parse error: " + failMsg.toString)
                                                                        parseErrorMsgTextCTLplayer = failMsg.toString
                                                                        false 
                                                                     }
         }
   }
   def initialiseSessionInfo:SessionInfo = // <does this really belong here?>
   {  si = new SessionInfo
      si.challengeName(challengeName).save
      si.userId(playerId).save
      si
   }
   def generateText:String
   def algorithmicDefenceGenerator:CTFqueryFolnuminquaQuery
   def generateQuestionAndCorrectAnswer:QuestionAndCorrectAnswer
   def doAlgorithmicDefence:(scala.Boolean, String, String, String)
   // <&y2011.11.17.18:49:46& or should I change the type of text and trans to the Text class etc. see model package.>
}

/*
<&y2011.12.12.16:16:58& refactor: either work with:
- providing input info by setting properties of the fluencyChallenge class, and then calling the methodwhich uses them
- provide all inputs through the parameters of the method...
Don't mix, that is confusing.
Or perhaps: find out a "design rule of thumb" which allows mixing them in a non-confusing way.
>
*/

// helper class for return type of generateQuestionAndCorrectAnswer

class EfeChallenge(val playerIdInit:Long) extends FluencyChallenge
{  val challengeName="efe"
   var si:SessionInfo = null
   val playerId = playerIdInit

   override def initialiseSessionInfo:SessionInfo =
   {  super.initialiseSessionInfo
      null // <finish>
      si.textNL = "Todo algorithm for generating efechallenge natural language text" 
      si.textCTLbyComputer = "TODO CTL text by Computer"
      si.questionNL = "TODO question NL"
      si.questionCTLcomputer = "TODO questionCTLcomputer"
      si.algoDefComputer = si.questionCTLcomputer
      si.answerComputerCTL = "answer computer ctl"
      si.answerComputerNL = "answer computer NL"
      si.questionRelatedBridgeStats = "questionRelatedBridgeStats"
      si.subjectNL = "subjectNL"
      // <&y2012.02.17.09:43:47& perhaps replace the first identifier match with a regular expression drawn from the parser (so that if you make changes their, it automatically gets changed here...>
      si.hurelanRole1NL = "hurelanRole1NL"
      si.hurelanRole2NL = "hurelanRole2NL"
      si.bridgeCTL2NLcomputer = "bridgeCTL2NLcomputer"
      si
   }
/*   { BUC
   def generateText = "todo"
   def algorithmicDefenceGenerator:FolnuminquaQuery = null // <TODO>
   def generateQuestionAndCorrectAnswer:QuestionAndCorrectAnswer = null // <TODO>
   def doAlgorithmicDefence:(scala.Boolean, String, String, String) = null // <TODO>
   // <&y2011.11.17.18:49:46& or should I change the type of text and trans to the Text class etc. see model package.>
*/// } EUC
}

class NotUnaChallenge(val playerIdInit:Long) extends FluencyChallenge
{  //var translation: String = ""
   val challengeName="NotUna"
   val playerId = playerIdInit
   var si:SessionInfo = null
   /* This doesn't only generate the text, but everything: the ctf text, the nl text, the question for the attack, and the answer based on the text. (Note that for the latter, the Clean program actually applies the reasoner to textCTLbyComputer, it is not "baked in".)      
   */

   /* <&y2012.09.26.12:38:17& COULDDO perhaps refactor: call the serialize method from SessionInfoMetaMapperObj.save (by overriding the latter method). Without additional checks, that will however be less efficient, because at each save invocation a lot will be written over and over to disk...> */

   override def initialiseSessionInfo:SessionInfo = 
   {  // regex must contain exactly 1 group which will be returned as a match.
      super.initialiseSessionInfo
      def extractRegExGroup(regexStr:String, sbc: String):String =
      {  val regex = new Regex(regexStr)
         val m     = regex.findFirstMatchIn(sbc)
         
         var errormsg = "NotUna.generateNewSessionBundle.extractRegExGroup: error: with regex = " + regex + ": "
         var result:String = ""
         if( m.isDefined )
         {  var group = m.get.group(1) // <&y2011.11.23.18:07:48& better: count groups first, must be one, other wise you can get exception>
            if( group != null ) 
               result = group
            else
               result = errormsg + " group not found"
         }
         else
            result = errormsg + " no match"

         err.println("NotUna.generateNewSessionBundle.extractRegExGroup: " + result)
         result
      }

      def extractNl(sbc: String):String = extractRegExGroup("""TextNL \[\(SentenceNL \"([^\"]+)\"""", sbc)
      // def extractNl(sbc: String):String = extractRegExGroup("""IADTstring \"TextNL ([^\"]*)\"""", sbc)

      def extractCTL(sbc: String):String = extractRegExGroup("""IADTstring \"TextCTL ([^\"]*)\"""", sbc)

      def extractNLquestion(sbc: String):String = extractRegExGroup("""QuestionAttackNL \[\(SentenceNL \"([^\"]*)""", sbc)
 
      def extractQRBS(sbc: String):String = extractRegExGroup("""IADTstring \"QuestionRelatedBridgeStats ([^\"]*)\"""", sbc)

/* gvim regex --> scala regex:
\( --> (
\) --> )
"  --> \"
(  --> \(
)  --> \)
[  --> \[ (only if intended to match a '[' symbol)
]  --> \[ (only if intended to match a ']' symbol)
\+ (for repitition) --> +
*/

//      def extractAnswerNL(sbc: String):String = extractRegExGroup("""AnswerNL \[\(SentenceNL \"([^\"]+)\"""", sbc)
      def extractAnswerNL(sbc: String):String = extractRegExGroup("""IADTstring \"AnswerNL ([^\"]*)\"""", sbc)

      def extractAnswerCTL(sbc: String):String = extractRegExGroup("""AnswerCTL ([^\]]*\][^0-9]*[0-9]*\)\))""", sbc)

      def extractBridgeCTL2NLcomputer(sbc: String):String = extractRegExGroup("""IADTstring \"BridgeComputer ([^\"]*)\"""", sbc)

      def extractQuestionCTLcomputer(sbc: String):String = extractRegExGroup("""IADTstring \"QuestionAttackCTL ([^\"]*)\"""", sbc)

      def extractSubjectNL(sbc: String):String = extractRegExGroup("""EntityStat \(_EntityStat_ \[[0-9]+\]""" + HurelanBridge.wordNLregexStr + """ \[[0-9]+\]([a-zA-Z\-]+)\)""", sbc)

      var sbClean:String = ""
      val ran:Int = currentTimeMillis().toInt
      // <&y2011.11.23.21:08:25& check whether scala Ints indeed fit in Clean ints>
      err.println("generateNewSessionBundle: use as random number = " + ran)
      sbClean = ( ( SWIFTBINARIES + "/textgenerator " + ran ) !!)
      
      si.textNL = extractNl(sbClean)
      si.textCTLbyComputer = extractCTL(sbClean)
      si.questionNL = extractNLquestion(sbClean)
      si.questionCTLcomputer = extractQuestionCTLcomputer(sbClean)
      si.algoDefComputer = si.questionCTLcomputer
      si.answerComputerCTL = extractAnswerCTL(sbClean)
      si.answerComputerNL = extractAnswerNL(sbClean)
      si.questionRelatedBridgeStats = extractQRBS(sbClean)
      si.subjectNL = extractSubjectNL(si.questionRelatedBridgeStats)
      // <&y2012.02.17.09:43:47& perhaps replace the first identifier match with a regular expression drawn from the parser (so that if you make changes their, it automatically gets changed here...>
      si.hurelanRole1NL = extractRegExGroup("""HurelanStat \(_HurelanStat_ \[[0-9]+\][a-zA-Z_]+ \[[0-9]+\](""" + HurelanBridge.wordNLregexStr + """)""", si.questionRelatedBridgeStats)
      si.hurelanRole2NL = extractRegExGroup("""HurelanStat \(_HurelanStat_ \[[0-9]+\][a-zA-Z_]+ \[[0-9]+\][a-zA-Z_]+ \[[0-9]+\](""" + HurelanBridge.wordNLregexStr +""")""", si.questionRelatedBridgeStats)
      si.bridgeCTL2NLcomputer = extractBridgeCTL2NLcomputer(sbClean)

      si
   }

   def algorithmicDefenceGenerator():CTFQuery = 
   {  // generate algo. defence (=, or better coincides with reasoning goal) for translation created by the player.     
   
      // <&y2011.12.11.19:40:54& make naming consistent (naming for algodef4player) throughout all source code>
      // <&y2011.12.12.16:27:40& Build in test whether all required FluencyChallenge properties are set>
      err.println("algorithmicDefenceGenerator: start")
      // <&y2012.05.07.18:49:04& rewrite in SWiFT format>
      val bridgeCTL2NLplayerCleanFormat = HurelanBridge.parseAll(HurelanBridge.bridge, si.bridgeCTL2NLplayer)  match { case HurelanBridge.Success(result,_) => result; case _ => throw new RuntimeException(" Error while parsing bridgeCTL2NLplayer") }
      // <&y2012.01.27.23:02:44& refactor this: put bridgeCTL2NLplayerCleanFormat in the SessionInfo model, and check there whether it needs updates or not.>
      val cmd_output = cleanBridge(SWIFTBINARIES + "/adGen", si.bridgeCTL2NLcomputer + NEWLINE + si.algoDefComputer + NEWLINE + bridgeCTL2NLplayerCleanFormat + NEWLINE) 
      /* Example in scala format (instance of FolnuminquaQuery): Sharpest(NumResPat(Geq(), PatVar(numpatvarname), Var(boundvarname), PredApp(p,consts)))
     */
      val algoDefPlayerSerializedWithLiftJson = cmd_output
      implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Var], classOf[Constant]))) + (new EnumSerializer(ComparisonOperator))
      println("   trying to deserialize:" + algoDefPlayerSerializedWithLiftJson)
      val algoDefPlayerScalaFormat = Serialization.read[Sharpest](algoDefPlayerSerializedWithLiftJson) // <&y2012.05.16.22:35:10& is it possible to use the name of superclass of the case class Sharpest after the read?>
      si.algoDefPlayer = Some(algoDefPlayerScalaFormat)
      algoDefPlayerScalaFormat
   }
   // for now it is assumed that only ONE question is generated per session (which is never improved or changed.)
   def generateText:String = 
   {  initialiseSessionInfo
      si.textNL
   }


   def generateQuestionAndCorrectAnswer:QuestionAndCorrectAnswer =
   {  new QuestionAndCorrectAnswer(si.questionNL, si.questionCTLcomputer) // it has already been done in this increment, so no additional calculations are required.
   }


   // <&y2011.12.24.12:42:26& move this function to general library>
   // <&y2011.12.26.12:44:14& use this function for all clean calls (thus rewrite some code)>
   // input: newline separated. For newline DO NOT use \n, but System.getProperty("line.separator");   
   def cleanBridge(function:String, input:String):String =
   {  val (functionoutput, _, _) = cleanBridgeExtended(function:String, input:String)
      functionoutput
   }

   def cleanBridgeExtended(function:String, input:String):(String,String,String) = 
   {  // err.println("cleanBridge start")
      val DEBUGCLEANCALLS = true
      var outFile = new File(function + ".clean.in")

      err.println("  creating file: " + outFile.getAbsolutePath)
      val out:PrintWriter = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))
      out.print(input)
      out.flush()
      out.close()

      // delete old output file of clean command
      var inFile = new File(function+".clean.out")
      err.println("  trying to delete old "+function+".clean.out file (if it exists) " + (if(inFile.delete()) "successful" else "failed"))   

      // Run external (Clean) command
      var errStr:String = ""
      var outStr:String = ""

      val pl = ProcessLogger( o => (outStr += o), e => (errStr += e) )
      val procBuilder = sys.process.Process(function, new java.io.File( SWIFTBINARIES ))
      val s:Int = procBuilder!(pl)
      // Now delete input file, to prevent reareading it in the future... This can be switched of temporarily for debugging purposes: you can then still read the file.
      err.println("  trying to run " + function + " on commandline...")
      err.println("  exit value (0 is good) = " + s)
      err.println("  errorstream: " + (if(errStr == "") "None" else errStr))
      err.println("  end errorstream")
      err.println("  outputstream: " + (if(outStr == "") "None" else outStr))
      err.println("  end outputstream")
      if(!DEBUGCLEANCALLS) err.println("  now trying to delete "+function+".clean.in: " + (if(outFile.delete()) "successful" else "failed"))
      //if(DEBUGCLEANCALLS && !(s == 0)) println("  errors during execution, so trying to delete output file "+function+".clean.out: " + (if(inFile.delete()) "successful" else "hmm, failed")) // normally preserve in and output files for debugging, but delete when there is an error otherwise an old output will be used leading to less transparent runtime bugs.

      // Read output of the Clean command
      inFile = new File(function+".clean.out")
      val in:BufferedReader = new BufferedReader(new FileReader(inFile))
      val output = in.readLine()
      
      // <&y2011.12.12.15:25:59& build in error check: was the file really created afresh, and doesn't it contain errors?>
      if(DEBUGCLEANCALLS && !(s == 0)) println("  errors during execution, so trying to delete output file "+function+".clean.out: " + (if(inFile.delete()) "successful" else "hmm, failed")) // normally preserve in and output files for debugging, but delete when there is an error otherwise an old output will be used leading to less transparent runtime bugs.
      in.close()
      // Now delete output file, to prevent reareading it in the future... This can be switched of temporarily for debugging purposes: you can then still read the file.
      if(!DEBUGCLEANCALLS) err.println("  now trying to delete "+function+".clean.out: " + (if(inFile.delete()) "successful" else "failed")) // <&y2011.12.21.15:45:38& BUG: this doesn't happen, while it returns "true". Strange...>
      (output, outStr, errStr)
   }
   def doAlgorithmicDefence:(scala.Boolean, String, String, String) =
   {  // 1. do algorithmic defence of player's translation  
      err.println("start doAlgorithmicDefence")
      // >>> SUC
      // <&y2012.05.18.17:00:18& perhaps better to do the parsing in the SessionInfos class at the moment the player's text ctl is provided. You have to parse it anyway to check for syntactic correctness.>

      si.textCTLbyPlayerScalaFormat match
      {  case Some(textCTLbyPlayerScalaFormat_Loc)  => 
         {  si.algoDefPlayer match
            {  case Some(algoDefPlayerLoc) => si.answerPlayerCTL = Folnuminqua.query(algoDefPlayerLoc, textCTLbyPlayerScalaFormat_Loc).toString // <&y2012.05.18.20:15:07& I shuld reteurn the result in the answer lang, not only a number (check this). For this rewrite tpfolnuminqua>
               case None                   => throw new RuntimeException(" no algorithmic defence available, should always be present in this stage of the game.")
            }
         }
         case None               => throw new RuntimeException(" Error happened during parsing textCTLbyPlayer")
      }

      // 2. translate the answer in CTL to NL as well <&y2012.05.19.18:26:13& SHOULDDO do this later, first a quick fix: simply return the answer. The problem is that the reasoner in scala currently doesn't return the answer in the answer lang, but it just returns a number.>
      /*
      err.println("   answerPlayerCTL = " + si.answerPlayerCTL)
      val bridgeCTL2NLplayerCleanFormat = HurelanBridge.parseAll(HurelanBridge.bridge, si.bridgeCTL2NLplayer) match { case HurelanBridge.Success(result,_) => result; case _ => throw new RuntimeException(" Error while parsing bridgeCTL2NLplayer") }
      if( !si.answerPlayerCTL.equals("Unknown") )
      {  si.answerPlayerNL = cleanBridge(SWIFTBINARIES + "/answerInCTL2NL_CI", si.answerPlayerCTL + NEWLINE + bridgeCTL2NLplayerCleanFormat + NEWLINE)
      }
      */

      // <&y2012.05.19.21:20:06& as soon as answerInCTL2NL is ported to scala, do the following differently (= translate answerPlayerCTL into answerPlayerNL> 
      si.answerPlayerNL = si.answerComputerNL.replaceAll("""(minimally )[0-9]+""", "$1" + si.answerPlayerCTL) // <_&y2012.05.19.21:11:46& dangerous, if there are other digits in the string...>

      si.answerPlayerCorrect(si.answerPlayerNL.equals(si.answerComputerNL)).save

      (si.answerPlayerCorrect.is, si.answerPlayerNL, "TODO: reasonercomment (needed?)", si.answerPlayerCTL)
      // <<< EUC
   }
}

// Helper return types, allows returning a subset of the above things in a type

// return 

}
