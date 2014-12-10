package controllers

import models._
import play.api.templates._
import play.api._
import play.api.mvc._
import util.Random
import play.api.Play.current
import collection.mutable.Seq
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
import play.api.db._
import anorm._
import play.api.libs.json._

object Application extends Controller with Secured{

  def index = withUser { username => implicit request =>
    Ok(views.html.index("Overview", username))
  }

  def transactions = withUser { username => implicit request =>
    Ok(views.html.transactions(username))
  }


  def generate(seed: String) =
    withAdmin { username => implicit request =>
      if (Generate.baseBank == null || Generate.bef != seed.hashCode())
        Generate.gen(seed.hashCode())

      Ok(views.html.generate(Generate.baseBank)(username))
    }


  def admin_overview = withAdmin { username => implicit request =>
    Ok(views.html.overview(username))
  }

  def user_overview = withUser { username => implicit request =>
    Ok(views.html.overview(username))
  }

  implicit val connection = DB.getConnection()

  def jsonV(i:Int, j:Int): JsValue = {

    def f(x:Int) = i+j + i*x + j*x*x
    def g(x:Int) = i*j + j*x + i*x*x    
    Json.obj(
    "a" -> Json.arr(f(-3), f(-2), f(-1), f(0), f(1), f(2), f(3)),
    "b" -> Json.arr(g(-3), (-2), g(-1), g(0), g(1), g(2), g(3))
    )
  }

  def json(i:Int, j:Int) = Action {
    Ok(jsonV(i,j))
  }

  def json2V(i:Int): JsValue = {
    val j = i
    DB.withConnection { conn =>    
      val r = SQL(s"call GetSth($i);")().collect {
        case Row(Some(a),Some(b)) => (a.toString.toInt, b.toString.toFloat)
      }.toList.unzip

      def f(x:Int) = i+j + i*x + j*x*x
      def g(x:Int) = i*j + j*x + i*x*x
      Json.obj(
        "labels" -> Json.toJson(r._1),
        "a" -> Json.toJson(r._2)
      )
    }
  }

  def json2(i:Int, j:Int) = 
    withAdmin { username => implicit request =>
      Ok(json2V(i+j))
    }
 



}
