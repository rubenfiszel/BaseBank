package controllers

import play.api.templates._
import play.api._
import play.api.mvc._
import util.Random
import collection.mutable.Seq

object Application extends Controller with Secured{

  def index = withUser { username => implicit request =>
    Ok(views.html.index("Overview", username))
  }

  def transactions = withUser { username => implicit request =>
    Ok(views.html.transactions(username))
  }

  def overview = withUser { username => implicit request =>
    Ok(views.html.overview(username))
  }

  def generate(seed: String) =
    withAdmin { username => implicit request =>
      if (seed != "" || Generate.baseBank == null)
        Generate.gen(seed.hashCode())

      Ok(views.html.generate(Generate.baseBank)(username))
    }

}

//
case class Address(id:Int, street:String, city:String)
case class Client(id:Int, name: String, location: Int)
case class Entity(id:Int, name:String, field:String)
case class Account(id:Int, sum: Int, client:Option[Int], entity:Option[Int], isClient:Boolean, pass:String)
case class Bank(id:Int, name:String)
case class Branch(id:Int, bank:Int, location: Int)
case class Receive(id:Int, account:Int, amount: Int, from: Int, timestamp:Int)
case class Send(id:Int, account:Int, amount: Int, to: Int, timestamp:Int)
case class BaseBank(banks:Seq[Bank], branches:Seq[Branch], sends:Seq[Send], receives:Seq[Receive], clients:Seq[Client], entities:Seq[Entity], accounts:Seq[Account], addresses:Seq[Address])

object Generate {


  var baseBank:BaseBank = _

  val N_ACC = 1000
  val N_BR = 10
  val N_TRANS = 10000

  val banksName = List("Crédit Lyonnais", "Crédit Agricole", "Société Générale")

  val N_BK = banksName.length

  val entityNames = List("Apple", "Facebook", "Google", "Wal-Mart", "Orange", "Total", "Carrefour")

  val N_ENT = entityNames.length

  val NOW = 1416111260

  def rdE[A](s:Seq[A])(implicit rd:Random):A = s(rd.nextInt(s.length))


  val cities = Seq("Paris", "Marseille", "Perpignan", "Poitiers", "Toulouse", "Lyon")
  val streetsName = Seq("Napoleon Str", "Carlus Magsen Ave", "Lavoisier Ave", "Pasteur Str", "Pascal Ave", "Lagrange Str", "Proudhon Ave", "Descartes Ave")
  val firstNames = Seq("Ruben", "Élodie", "Hannah", "Marianne", "Louis", "Jean", "Fernando")
  val lastNames = Seq("Fiszel", "Brito", "Bamberger", "Weil", "Weinberg", "Rotschild")
  val entityFields = Seq("High-Tech", "Mining", "Supermarket")


  def gen(seed: Int) = {

    implicit val rd = new Random(seed)

    var receives:Seq[Receive] = Seq()
    var sends:Seq[Send] = Seq()
    var addresses:Seq[Address] = Seq()
    var banks:Seq[Bank] = Seq()
    var branches:Seq[Branch] = Seq()
    var entities:Seq[Entity] = Seq()
    var clients:Seq[Client] = Seq()
    var accounts:Seq[Account] = Seq()

    var timestamp = 784959260 // 1994, 16 November, 5:14am, 20sec

    def genTimestamp() = {
      val nt = rd.nextInt((NOW - timestamp)/20) + timestamp
      timestamp = nt
      nt
    }
    
    def genStreet() =
      rd.nextInt(150) + " " + rdE(streetsName)
    

    def genName() =
      rdE(firstNames) + " " + rdE(lastNames)

    var idAd = -1
    def genAddress() = {
      idAd += 1
      val ad = Address(idAd, genStreet, rdE(cities))
      addresses :+= ad
      ad
    }

    var bn = banksName
    var idBk = -1
    def genBank() = {
      idBk += 1
      val x::xs = bn
      bn = xs
      val bk = Bank(idBk, x)
      banks :+= bk
      bk
    }

    var idRc = -1
    def genReceive() = {
      idRc += 1
      val rec = Receive(idRc, rd.nextInt(N_ACC), rd.nextInt(1000) + 1, rd.nextInt(N_ACC), genTimestamp())
      receives :+= rec
      rec
    }

    var idTr = -1
    def genTrans() = {
      idTr += 1
      val am = rd.nextInt(1000) + 1
      val from =  rd.nextInt(N_ACC)
      val to =  rd.nextInt(N_ACC)
      val ts = genTimestamp()
      val sd = Send(idTr, to, am, from, ts)      
      val rc = Receive(idTr, from, am, to, ts)      
      sends :+= sd
      receives :+= rc
      (sd,rc)
    }

    var idBr = -1
    def genBranch() = {
      idBr += 1
      val adr = genAddress
      val br = Branch(idBr, rd.nextInt(N_BK), adr.id)
      branches :+= br
      br
    }

    var idCl = -1
    def genClient() = {
      idCl += 1
      val adr = genAddress()
      val cl = Client(idCl, genName(), adr.id)
      clients +:= cl
      cl
    }

    var enN = entityNames
    var idEnt = -1
    def genEntity() = {
      idEnt += 1
      val (x::xs) = enN
      enN = xs
      val en = Entity(idEnt, x, rdE(entityFields))
      entities :+= en
      en
    }


    var idAcc = -1
    def genAcc() = {
      idAcc += 1
      val sum = receives.filter(_.account == idAcc).map(_.amount).sum - sends.filter(_.account == idAcc).map(_.amount).sum
      val pass = idAcc.toString + "pass"
      val b = rd.nextInt(100) <= 1
      if (!b) {
        val acc = Account(idAcc, sum, Some(rd.nextInt(clients.length)), None, true, pass)
        accounts :+= acc
        acc
      } else {
        val acc = Account(idAcc, sum, None, Some(rd.nextInt(entities.length)), false, pass)
        accounts :+= acc
        acc
      }
    }

    (1 to N_BK).foreach(_ => genBank())
    (1 to N_BR).foreach(_ => genBranch())
    (1 to (N_ACC - N_ENT)).foreach(_ => genClient())
    (1 to N_ENT).foreach(_ => genEntity())
    (1 to N_TRANS).foreach(_ => genTrans())    
    (1 to N_ACC).foreach(_ => genAcc())

    baseBank = BaseBank(banks, branches, sends, receives, clients, entities, accounts, addresses)
    baseBank
  }


}
