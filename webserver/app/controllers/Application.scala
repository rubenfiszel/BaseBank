package controllers

import models._
import play.api.templates._
import play.api._
import play.api.mvc._
import util.Random

import collection.mutable.Seq
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
import play.api.Play.current
import play.api.db._
import anorm._
import play.api.libs.json._

case class StatementUI(what:String, from:String, to:String, sum:Float, timestamp:String)
object Application extends Controller with Secured{

  def index = withUser { username => implicit request =>
    Ok(views.html.index("Overview", username))
  }

  def send(to:String, amount:Float) = withUser { username => implicit request =>
    val a = SQL(s"INSERT INTO Transfer (SenderAcc, ReceiverAcc, Amount, TimeTransfer) VALUES ('${username.acc}', '$to', '$amount', NOW());").execute()
    Redirect(routes.Application.transactions)
  }

  def transactions = withUser { username => implicit request =>
    val name = username.acc
    val a = SQL(s"SELECT * FROM Transfer WHERE SenderAcc='$name';")().collect {
      case Row(id, Some(b), Some(c), Some(d), e) => (b.toString, c.toString, -(d.toString.toFloat), e.toString)
    }.toList.map(x => StatementUI("TRANSFER", x._1, x._2, x._3, x._4))

    val e = SQL(s"SELECT * FROM Transfer WHERE ReceiverAcc='$name';")().collect {
      case Row(id, Some(b), Some(c), Some(d), e) => (b.toString, c.toString, d.toString.toFloat, e.toString)
    }.toList.map(x => StatementUI("TRANSFER", x._1, x._2, x._3, x._4))

    val b = SQL(s"SELECT * FROM Salary WHERE AccountId='$name';")().collect {
      case Row(id, Some(b), Some(c), Some(d), e) => (b.toString, c.toString, d.toString.toFloat, e.toString)
    }.toList.map(x => StatementUI("SALARY", x._1, x._2, x._3, x._4))


    val c = SQL(s"SELECT * FROM Payment WHERE AccountId='$name';")().collect {
      case Row(id, Some(b), Some(c), Some(d), e) => (b.toString, c.toString, -(d.toString.toFloat), e.toString)
    }.toList.map(x => StatementUI("PAYMENT", x._1, x._2, x._3, x._4))

    val statements = (a ::: b ::: c ::: e).sortBy(_.timestamp).map(x => (x, x.sum))
    Ok(views.html.transactions(username, statements.map{ var s = 0f; d => { s += d._2; (d._1, s)}}.reverse))
  }


  def generate(seed: String) =
    withAdmin { username => implicit request =>
      if (Generate.baseBank == null || Generate.bef != seed.hashCode())
        Generate.gen(seed.hashCode())

      Ok(views.html.generate(Generate.baseBank)(username))
    }


  def admin_overview = withAdmin { username => implicit request =>
    Ok(views.html.admin_overview(username))
  }

  def user_overview = withUser { username => implicit request =>
    Ok(views.html.user_overview(username))
  }



  implicit val connection = DB.getConnection()


  def json2V(i:Int): JsValue = {
    val j = i
    val r = SQL(s"call GetSth($i);")().collect {
      case Row(Some(a),Some(b)) => (a.toString.toInt, b.toString.toFloat)
    }.toList.unzip

      Json.obj(
        "labels" -> Json.toJson(r._1),
        "a" -> Json.toJson(r._2)
      )
  }

  def json2(i:Int, j:Int) = 
    withAdmin { username => implicit request =>
      Ok(json2V(i+j))
    }


  def jsonVEBF(i:String): JsValue = {
    val r = SQL(s"call ExpenditureByFields('$i');")().collect {
      case Row(Some(a),Some(b)) => (a.toString, b.toString.toFloat)
    }.toList.unzip
    println(r)

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }

  def jsonEBF = 
    withUser { username => implicit request =>
      Ok(jsonVEBF(username.acc))
    }  



  def jsonVME(i:String): JsValue = {
    val r = SQL(s"call MonthlyExpenditure('$i');")().collect {
      case Row(_, Some(a), Some(b)) => (a.toString, b.toString.toFloat )
    }.toList.unzip
    println(r)

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }


  def jsonVMEBB(i:Int): JsValue = {
    val r = SQL(s"call MonthlyExpenditureByBranch('$i');")().collect {
      case Row(Some(a),Some(b) ) => (a.toString, b.toString.toFloat )
    }.toList.unzip
    println(r)

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }

  def jsonME = 
    withUser { username => implicit request =>
      Ok(jsonVME(username.acc))
    }  


  def jsonVPO(i:String): JsValue = {
    val r = SQL(s"call PaymentsOften('$i');")().collect {
      case Row(_, Some(a), b ) => (a.toString, b.toString.toFloat)
    }.toList.unzip


    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }

  def jsonPO = 
    withUser { username => implicit request =>
      Ok(jsonVPO(username.acc))
    }  


  def jsonVSP(i:String): JsValue = {
    val r = SQL(s"call StatementPrediction('$i');")().collect {
      case Row(a, Some(b) ) => (a.toString, b.toString.toFloat)
    }.toList.unzip


    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }

  def jsonSP =
    withUser { username => implicit request =>
      Ok(jsonVSP(username.acc))
    }




  def jsonVAMA(): JsValue = {
    val r = SQL(s"call AccountsMostActivity();")().collect {
      case Row(Some(a), Some(b) ) => (a.toString, b.toString.toFloat)
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }




  def jsonVNC(): JsValue = {
    val r = SQL(s"call NegativeClients();")().collect {
      case Row(a, _, _, Some(b) ) => (a.toString, b.toString.toFloat)
      case a@_ => { println(a); ("", 4f) }
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }



  def jsonVBM(): JsValue = {
    val r = SQL(s"call BranchMoney();")().collect {
      case Row(a, Some(b) ) => (a.toString, b.toString.toFloat)
      case a@_ => { println(a); ("", 4f) }
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }

  def jsonVWE(): JsValue = {
    val r = SQL(s"call WorkExpenditure();")().collect {
      case Row(a, Some(b) ) => (a.toString, b.toString.toFloat)
      case a@_ => { println(a); ("", 4f) }
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }  


  def jsonVNCBB(): JsValue = {
    val r = SQL(s"call NegativeClientsByBranch();")().collect {
      case Row(a,  b ) => (a.toString, b.toString.toFloat)
      case a@_ => { println(a); ("", 4f) }
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }  
  
  

  def jsonVABB(i: Int): JsValue = {
    val r = SQL(s"call AccountsByBranch($i);")().collect {
      case Row(a, b ) => (a.toString, b.toString.toFloat)
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }


  def jsonVMBB(i: Int): JsValue = {
    val r = SQL(s"call MoneyByBranch($i);")().collect {
      case Row(a, Some(b)) => (a.toString, b.toString.toFloat)
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }


  def jsonVCBB(i: Int): JsValue = {
    val r = SQL(s"call ClientsByBranch($i);")().collect {
      case Row(a, b ) => (a.toString, b.toString.toFloat)
    }.toList.unzip

    Json.obj(
      "labels" -> Json.toJson(r._1),
      "a" -> Json.toJson(r._2)
    )
  }    
  

  def jsonABB(i:Int) =
    withAdmin { username => implicit request =>
      Ok(jsonVABB(i))
    }

  def jsonCBB(i:Int) =
    withAdmin { username => implicit request =>
      Ok(jsonVCBB(i))      
    }

  def jsonMBB(i:Int) =
    withAdmin { username => implicit request =>
      Ok(jsonVMBB(i))            
    }

  def jsonMEBB(i:Int) =
    withAdmin { username => implicit request =>
      Ok(jsonVMEBB(i))            
    }  

  def jsonAMA =
    withAdmin { username => implicit request =>
      Ok(jsonVAMA())
    }

  def jsonNC =
    withAdmin { username => implicit request =>
      Ok(jsonVNC())
    }

  def jsonBM =
    withAdmin { username => implicit request =>
      Ok(jsonVBM())
    }    

  def jsonNCBB =
    withAdmin { username => implicit request =>
      Ok(jsonVNCBB())
    }

  def jsonWE =
    withAdmin { username => implicit request =>
      Ok(jsonVWE())
    }    

   



}
