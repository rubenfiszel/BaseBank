package models

import util.Random
import play.api.db._
import anorm._
import play.api.Play.current
import java.io._
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

case class Address(id:Int, street:String, city:String, state:String, country:String)
case class Client(ssn:String, fname: String, lname:String, location: Int)
case class Entity(id:Int, name:String, field:String)
case class Account(iban:String, sum: Int, branch:Int, client:String, pass:String)
case class Branch(id:Int, location: Int)
case class Transfert(id:Int, from:String, amount: Int, to: String, timestamp:String) 
case class Payment(id:Int, from:String, amount: Int, to: Int, timestamp:String)
case class Salary(id:Int, from:Int, amount: Int, to: String, timestamp:String)
case class BaseBank(branches:Seq[Branch], clients:Seq[Client], entities:Seq[Entity], transferts:Seq[Transfert], payments:Seq[Payment], salaries:Seq[Salary], accounts:Seq[Account], addresses:Seq[Address])

object Generate {

  var baseBank:BaseBank = _
  var bef = 0

  val N_ACC = 1000
  val N_BR = 10
  val N_TRANS = 10000
  val RATIO_TR_IN = 0.8
  val RATIO_SAL = 0.2
  val RATIO_TR = 0.1
  val RATIO_PAY = 0.7      

  val entityNames = List("Apple", "Facebook", "Google", "Wal-Mart", "Orange", "Total", "Carrefour")

  val N_ENT = entityNames.length

  val NOW:Long = 1416111260

  def rdE[A](s:Seq[A])(implicit rd:Random):A = s(rd.nextInt(s.length))


  val cities = Seq(("Baltimore", "MD", "US"), ("Paris", "IDF", "FR"), ("Marseille", "BDR", "FR"), ("Perpignan", "LR", "FR"), ("Poitiers", "PC", "FR"), ("Toulouse", "HG", "FR"), ("Lyon", "R", "FR"))
  val streetsName = Seq("Napoleon Str", "Carlus Magsen Ave", "Lavoisier Ave", "Pasteur Str", "Pascal Ave", "Lagrange Str", "Proudhon Ave", "Descartes Ave", "Hungara Str")
  val firstNames = Seq("Ruben", "Élodie", "Hannah", "Marianne", "Louis", "Jean", "Fernando", "André", "Gabriel")
  val lastNames = Seq("Fiszel", "Brito", "Bamberger", "Weil", "Weinberg", "Rotschild", "Alvim", "Campos")
  val entityFields = Seq("High-Tech", "Mining", "Supermarket")

  def gen(seed: Int) = {

    bef = seed

    implicit val rd = new Random(seed)

    var payments:Seq[Payment] = Seq()
    var transferts:Seq[Transfert] = Seq()
    var salaries:Seq[Salary] = Seq()
    var addresses:Seq[Address] = Seq()
    var branches:Seq[Branch] = Seq()
    var entities:Seq[Entity] = Seq()
    var clients:Seq[Client] = Seq()
    var accounts:Seq[Account] = Seq()

    var timestamp:Long = 784959260 // 1994, 16 November, 5:14am, 20sec

    def genIban() = {      
      (IndexedSeq.fill(2){ (rd.nextInt(25) + 65).toChar} ++
        IndexedSeq.fill(2){ (rd.nextInt(9) + 49).toChar}).mkString("") + " " +
      IndexedSeq.fill(4){ (rd.nextInt(9) + 49).toChar}.mkString("") + " " +
      IndexedSeq.fill(4){ (rd.nextInt(9) + 49).toChar}.mkString("") + " " +
      IndexedSeq.fill(4){ (rd.nextInt(9) + 49).toChar}.mkString("")

    }

    def genTrIban() = {
      if (rd.nextDouble() < RATIO_TR_IN)
        rdE(accounts).iban
      else
        genIban()

    }
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")

    def genTimestamp() = {
      val nt = rd.nextInt(((NOW - timestamp)/1000).toInt) + timestamp
      timestamp = nt
      simpleDateFormat.format(new Date(new Timestamp(nt*1000).getTime()))
    }
    
    def genStreet() =
      rd.nextInt(150) + " " + rdE(streetsName)
    

    def genName() =
      (rdE(firstNames), rdE(lastNames))

    var idAd = -1
    def genAddress() = {
      idAd += 1
      val (cit, reg, co) = rdE(cities)
      val ad = Address(idAd, genStreet, cit, reg, co)
      addresses :+= ad
      ad
    }
    
    var idTr = -1
    var idPa = -1
    var idSa = -1    
    var idBa = -1    
    def genTrans() = {
      val am = rd.nextInt(1000) + 1
      val ts = genTimestamp()
      if (rd.nextDouble() < RATIO_PAY) {
        idPa += 1
        val iban = rdE(accounts).iban
        val to = rd.nextInt(N_ENT)
        payments :+= Payment(idPa, iban, am, to, ts)

      } else if (rd.nextDouble() < (RATIO_PAY + RATIO_SAL)) {
        idSa += 1
        val iban = rdE(accounts).iban        
        val from = rd.nextInt(N_ENT) 
        salaries :+= Salary(idSa, from, am, iban, ts)

      } else {
        idTr += 1
        val iban1 = genTrIban()        
        val iban2 = genTrIban()
        transferts  :+= Transfert(idTr, iban1, am, iban2, ts)
      }
    }

    var idBr = -1
    def genBranch() = {
      idBr += 1
      val adr = genAddress
      val br = Branch(idBr, adr.id)
      branches :+= br
      br
    }

    def toSSN(id:Int) = {
      val (a,b,c)= (id/1000000, id/10000%100, id%1000)
      f"$a%03d-$b%02d-$c%04d"
    }

    var idCl = -1
    def genClient() = {
      idCl += 1
      val adr = genAddress()
      val name = genName()
      val cl = Client(toSSN(idCl), name._1, name._2 , adr.id)
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


    def genAcc() = {
      val sum = 0
      val iban = genIban()
      val pass = iban + "pass"
      val b = rd.nextInt(100) <= 1
      val acc = Account(iban, sum, rd.nextInt(N_BR), toSSN(rd.nextInt(N_ACC - N_ENT)), pass)
      accounts :+= acc
    }

    def calcSum(x:Account) = {
      val nsum = salaries.filter(_.to == x.iban).map(_.amount).sum + transferts.filter(_.to == x.iban).map(_.amount).sum - transferts.filter(_.from == x.iban).map(_.amount).sum - payments.filter(_.from == x.iban).map(_.amount).sum
      x.copy(sum = nsum)
    }

    (1 to N_BR).foreach(_ => genBranch())
    (1 to (N_ACC - N_ENT)).foreach(_ => genClient())
    (1 to N_ENT).foreach(_ => genEntity())
    (1 to N_ACC).foreach(_ => genAcc())    
    (1 to N_TRANS).foreach(_ => genTrans())
    accounts = accounts.map(calcSum(_))


    implicit val connection = DB.getConnection()

    DB.withConnection { conn =>

      val sql =
"""
truncate table Client;
truncate table Entity;
truncate table Salary;
truncate table Transfer;
truncate table Payment;      
truncate table Account;
truncate table Branch;
truncate table Address;
truncate table Statement
"""

      sql.split(";")foreach(x=> SQL(x+";").execute())
      def load(l:List[List[String]], table:String) = {
        val writer = new PrintWriter(new File("/tmp/data.txt" ))
        writer.write(l.map(_.mkString("\t")).mkString("\n"))
        writer.close()
        SQL("LOAD DATA LOCAL INFILE '/tmp/data.txt' INTO TABLE " + table + ";").execute();
      }

      load(entities.map(x => (x.id::x.name::x.field::Nil).map(_.toString)).toList, "Entity")
      load(addresses.map(x => (x.id::x.street::x.city::x.state::x.country::Nil).map(_.toString)).toList, "Address")
      load(clients.map(x => (x.ssn::x.fname::x.lname::x.location::Nil).map(_.toString)).toList, "Client")
      load(accounts.map(x => (x.iban::x.client::x.branch::x.sum::x.pass::Nil).map(_.toString)).toList, "Account")
      load(branches.map(x => (x.id::x.location::Nil).map(_.toString)).toList, "Branch")
      load(salaries.map(x => (x.id::x.from::x.to::x.amount::x.timestamp::Nil).map(_.toString)).toList, "Salary")      
      load(transferts.map(x => (x.id::x.from::x.to::x.amount::x.timestamp::Nil).map(_.toString)).toList, "Transfer")
      load(payments.map(x => (x.id::x.from::x.to::x.amount::x.timestamp::Nil).map(_.toString)).toList, "Payment")



      //insertsL.foreach(x => {println(x.mkString("\n")); SQL("BEGIN; \n" + x.mkString("\n") + "END; \n").execute()})

    }
    baseBank = BaseBank(branches, clients, entities, transferts, payments, salaries, accounts, addresses)
    baseBank
  }


}
