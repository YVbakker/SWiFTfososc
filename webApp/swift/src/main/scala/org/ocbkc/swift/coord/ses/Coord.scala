/* <&y2012.04.09.13:58:55& for all scala code: change appending to Lists, because this is highly inefficient (use a ListBuffer instead)>/
(  importance = 9
)
*/
package org.ocbkc.swift.coord
{  
import org.ocbkc.swift.logilang.query._
import org.ocbkc.swift.logilang._
import org.ocbkc.swift.model._
import org.ocbkc.swift.general._
import org.ocbkc.swift.global.TestSettings._
import org.ocbkc.swift.global.Logging._
import org.ocbkc.swift.OCBKC._
import org.ocbkc.swift.OCBKC.scoring._
import org.ocbkc.swift.OCBKC.ConstitutionTypes._
import org.ocbkc.swift.test._
import System._
import org.ocbkc.swift.cores.{TraitFluencyChallenge, NotUna}
import org.ocbkc.swift.cores.fluencyChallengeHelperTypes._
import net.liftweb.json._
import java.io._

import org.ocbkc.swift.messages._
import org.ocbkc.swift.messages.MailMessage._
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer._

import net.liftweb.common.{Box,Empty,Failure,Full}
import org.ocbkc.swift.model._
import _root_.net.liftweb.mapper.By
import org.ocbkc.swift.global.Types._

//import scala.util.parsing.combinator.Parsers._
import org.ocbkc.swift.parser._

/* Conventions:
- Names of classes correspond with design $JN/...
- CTL = Computationally Transparent Language
- NL  = Natural Language

Part of the model-view-controller pattern:
- UR = User Request (so coming from the view)
- MU = Model Update (so relevant update coming from the model)
*/
package ses
{

//import Round._

case class RoundFluencySession
case object RoundTranslation extends RoundFluencySession
case object RoundBridgeConstruction extends RoundFluencySession
case object RoundQuestionAttack extends RoundFluencySession
case object RoundAlgorithmicDefenceStage1 extends RoundFluencySession
// case object RoundAlgorithmicDefenceStage2 extends RoundFluencySession
case object NotInFluencySession extends RoundFluencySession

// in trait, make for easy reuse for creating test simulation sessions.
trait CoreTrait
{  var si: SessionInfo = null
   val sesHis = new SessionHistory()
   val fluencyChallenge: TraitFluencyChallenge = new NotUna(currentPlayer.id.get)

   def currentPlayer:Player
   val currentPlayerId = currentPlayer.id.get

   var latestRoundFluencySession:RoundFluencySession = NotInFluencySession

   /** @param constiId Must be constiId of a constitution with at least one version released
     */
   def URchooseFirstConstitution(constiId:ConstiId):Unit =
   {  val player = currentPlayer
      URchooseFirstConstitution(currentPlayer, constiId)
   }

   def URchooseFirstConstitution(player:Player, constiId:ConstiId):Unit =
   {  player.firstChosenConstitution(constiId).save // note: apply of firstChosenConstitution has been overridden with an apply which does everything needed.
   }

   /** Set last version of consti with id constiId.
     * @param on, if true it switches on the ReleaseCandidate state for the latest version, otherwise it is switched off.
     */
   def URsetReleaseCandidate(consti:Constitution, on:Boolean) =
   {  if(on)
      {  consti.makeLatestVersionReleaseCandidateIfPossible
      } else
      {  consti.unmakeCurrentPotentialRelease
      }
   }

   def URstartTranslation:String =  
   {  latestRoundFluencySession = RoundTranslation
      si = fluencyChallenge.initialiseSessionInfo
      si.startTime(SystemWithTesting.currentTimeMillis).save
      si.startTimeTranslation(si.startTime.is).save
      si.textNL
   }

   def URstopTranslation =
   {  log("URstopTranslation called")
      si.stopTimeTranslation(SystemWithTesting.currentTimeMillis).save
      Unit
   }

   def URstartAlgorithmicDefenceStage1:FolnuminquaQuery =
   {  latestRoundFluencySession = RoundAlgorithmicDefenceStage1
      fluencyChallenge.algorithmicDefenceGenerator
   }

   /** @todo &y2013.05.09.17:31:41& perhaps better move session storing to URstopTranslation.
     */
   def URstartAlgorithmicDefenceStage2:(scala.Boolean, String, String, String) =
   {  val res = fluencyChallenge.doAlgorithmicDefence
      // Session completed: store this session for future analysis/score calculations
      // now:Calendar = System.currentTimeMillis()
      si.stopTime(System.currentTimeMillis).save
      sesHis.sessionInfos ::= si      
      si.serialize // serialize the JSON part
      PlayerSessionInfo_join.create.player(currentPlayer).sessionInfo(si).save

      // send update mail to followers that the score for a release of this constitution is updated
      val fCC = Constitution.getById(currentPlayer.firstChosenConstitution.get).get
      val rOFCC = currentPlayer.releaseOfFirstChosenConstitution.get
      if(accessToConstiGame && ConstiScores.sampleSizeSufficient4FluencyScore(rOFCC)) // note: after accessToConstiGame, the players new sessions are disregarded for calculating the score of the consti release he learned playing the game with, and before it, are all sessions disregarged.
      {  mailAllFollowersUpdate(fCC, newFluencyScore(fCC, rOFCC))
      }

      turnReleaseCandidateIntoVirginIfPossible
      
      latestRoundFluencySession = NotInFluencySession

      res
   }

   protected def turnReleaseCandidateIntoVirginIfPossible:Unit =
   {  currentPlayer.firstChosenConstitution.is match
      {  case -1 => logAndThrow("[BUG] No first chosen consti found, while player just has finished playing a translation session.")
         case id => { log("   firstChosenConstitution = "  + id); log("   playerHasAccessToAllConstis after this session = " + OCBKCinfoPlayer.playerHasAccessToAllConstis(currentPlayer)); Constitution.getById(id).getOrElse(logAndThrow("[BUG] Constitution with id " + id + " not found. Bug or broken database?")).turnReleaseCandidateIntoVirginIfPossible }
      }

      Unit
   }

   def numOfSessionsAfterConstiAccess =
   {  sesHis.totalNumber - OneToStartWith.minSessionsB4access2allConstis
   }

   def accessToConstiGame:Boolean =
   {  numOfSessionsAfterConstiAccess >= 0
   }

   def MUnewFluencyScore(consti:Constitution, releaseId:String) =
   {  mailAllFollowersUpdate(consti, newFluencyScore(consti, releaseId))
   }

   def URpublishConsti(consti:Constitution, text:String, description:String) =
   {  mailOtherFollowersUpdate(consti, MailMessage.newPublication(consti), currentPlayer)

      // in case no new versions occurred after the latest release, this publication may immediately become the next release.
      // {
      // <&y2013.02.10.17:13:40& COULDO optimisation here: the check is only necessary if the previous version (version prior to this publication) is a release.>
      val sufficientForNextRelease = consti.publish(text, description, currentPlayerId.toString)
     
      if( sufficientForNextRelease )
      {  MUnewFluencyScore(consti, ConstiScores.latestReleaseWithFluencyScore(consti.constiId).get)
      }
      // }
   }
}

class Core(/* val player: User, var text: Text,v ar round: Round */) extends CoreTrait
{  println("ses.Core.constructor called")
   
   /* <&y2012.08.08.20:00:20& following MUST be refactored as soon as Mapper framework is understood (see the tryMapperPersistency gitbranch). Now things are only retained during a session, but not accross sessions...> */
   // BEGIN temporary solution for constiSelectionProcedure
   var isFirstTimePlayer:Boolean = true // <&y2012.08.04.19:43:17& set this to true after first session has been completed (or other conditions?)>
   var timeFirstChosenConstitution:Option[Long] = None

   // for coming increment the following will not yet be used.
   var studyHistory:StudyHistory = new StudyHistory

   // END

   override def currentPlayer = Player.currentUser match // <&y2012.08.04.20:16:59& refactor rest of code to use this currentPlayer, instead of doing this again and again....>
   {  case Full(player) => player
      case _            => 
      {  println("   ERROR: I'm afraid no player is logged in..."); throw new RuntimeException("   ERROR: I'm afraid no player is logged in...") // there should always be a player if a Coord object is being created.
      }
   } // <&y2012.08.04.19:33:00& perhaps make it so that also this rewrite URL becomes visible in the browser URL input line>

   initialise

   // ...Touched: the user has edited this field in this session at least once.

   // object for holding state of the session
   object State
   /* var translationTouched:Boolean, var bridgeTouched:Boolean) */

   private def initialise = {  // load sessionhistory data from disk for this user (persistency info).
      println("Core.initialise called")
      var prefix:String = "" // <&y2012.01.10.09:36:56& coulddo: refactor this, because it is also used when making things persistent>
      Player.currentUserId match // <&y2012.06.23.14:41:16& refactor: put currentuserid in session var, and use that throughout the session-code>
      {  case Full(id)  => { prefix = id }
         case _         => { throw new RuntimeException("  No user id found.") }
      }
      println("   reading sessionInfo objects from database...")
      val sis = PlayerSessionInfo_join.findAll(By(PlayerSessionInfo_join.player, currentPlayer)).map{ join => join.sessionInfo.obj.open_! }

      sesHis.sessionInfos = sis
      println("   found " + sis.length + " SessionInfo objects for this player")
   }
   // var sesHis:SessionHistory = new SessionHistory 
   // <&y2012.01.02.23:15:26& initialise SessionHistory object with data made persistant in the past>

   // Communication with User Interface
   // UR = User Request
   // user requests to prepare session
   def URprepare =
   {  
   }

   /*
   override def URchooseFirstConstitution(constiId:ConstiId) =
   {  val player = currentPlayer
      URchooseFirstConstitution(currentPlayer, constiId)
   }
   */


/*
   def URstartTranslation:String =  
   {  round = Trans
      si = fluencyChallenge.initialiseSessionInfo
      si.startTime(System.currentTimeMillis).save
      si.startTimeTranslation(si.startTime.is).save
      si.textNL
   }
*/

   def URstartBridgeConstruction =
   {  latestRoundFluencySession = RoundBridgeConstruction
   }

   def URstopBridgeConstruction =
   {
   }

   def URstartQuestionAttack:QuestionAndCorrectAnswer = 
   {  latestRoundFluencySession = RoundQuestionAttack
      fluencyChallenge.generateQuestionAndCorrectAnswer
   }
/*
   def URstartAlgorithmicDefenceStage1:FolnuminquaQuery =
   {  fluencyChallenge.algorithmicDefenceGenerator
   }

   def URstartAlgorithmicDefenceStage2:(scala.Boolean, String, String, String) =
   {  val res = fluencyChallenge.doAlgorithmicDefence
      // Session completed: store this session for future analysis/score calculations
      // now:Calendar = System.currentTimeMillis()
      si.stopTime(System.currentTimeMillis).save
      sesHis.sessionInfos ::= si      
      si.serialize // serialize the JSON part
      PlayerSessionInfo_join.create.player(currentPlayer).sessionInfo(si).save
      res
   }
*/
// <&y2012.02.21.19:22:56& refactor by using built-in parser.?>
   def testSyntaxTranslation:String = 
   {  gameCore.parseTextCTLbyPlayer
      val warn = gameCore.parseWarningMsgTxtCTLplayer
      if(!warn.equals("")) warn else gameCore.parseErrorMsgTextCTLplayer
   }

   def testSyntaxBridge = 
   {  import scala.util.parsing.combinator.Parsers
      if( si.bridgeCTL2NLplayer == "" ) 
         None
      else
         Some(HurelanBridge.parseAll(HurelanBridge.bridge, si.bridgeCTL2NLplayer))
   }

   def addFollower(p:Player, c:Constitution) =
   {  val userId = p.userIdAsString
      c.addFollower(p)
   }

   def removeFollower(p:Player, c:Constitution) =
   {  val userId = p.userIdAsString
      c.removeFollower(p)
   }

   object Test
   {  var initConstitutions:Boolean = true
   }
/*
   override def MUnewFluencyScore(consti:Constitution) =
   {  mailAllFollowersUpdate(consti, newFluencyScore(consti))
   }
*/
}

// simulation of Core for testing purposes
class CoreSimu(val currentPlayerVal:Player) extends CoreTrait
{  override def currentPlayer = currentPlayerVal

   // the following is a simplification: it skips playing an actual game, but just determines whether the player has succeeded or not.
   def URalgorithmicDefenceSimplified(winSession:Boolean, duration:DurationInMillis) =
   {  val cTM = SystemWithTesting.currentTimeMillis
      log("   playerHasAccessToAllConstis just before this session = " + OCBKCinfoPlayer.playerHasAccessToAllConstis(currentPlayer))
      si.startTime(cTM).save
      si.startTimeTranslation(cTM).save
      si.stopTime(cTM + duration).save
      si.stopTimeTranslation(cTM + duration).save

      si.answerPlayerCorrect(winSession).save
      si.serialize
      PlayerSessionInfo_join.create.player(currentPlayer).sessionInfo(si).save
      sesHis.sessionInfos ::= si  // [SHOULDDO] &y2013.05.10.09:56:36& still needed?

      turnReleaseCandidateIntoVirginIfPossible
   }
/*
   override def MUnewFluencyScore(consti:Constitution) =
   { // do nothing
   }
*/
}


/* Assumptions and conventions regarding UI:
- UI is an abstract layer around the actual UI implementation. Or perhaps better: is a kind of API between the Session coordinator and the implementation of the UI. This allows the specific UI solution (web based, OS-based, etc.) to be changed when required. Only the definition of the methods in the UI object have to be changed, without having to make changes to the ses.Core class.
*/


}
}
