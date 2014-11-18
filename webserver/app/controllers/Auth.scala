package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._

object Auth extends Controller {

  val loginForm = Form(
    tuple(
      "account" -> text,
      "password" -> text
    ) verifying ("Invalid account or password", result => result match {
      case (email, password) => check(email, password)
    })
  )

  def check(username: String, password: String) = {
    (username == "admin" && password == "1234") || true //if account exist
  }

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}


trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /**
   * This method shows how you could wrap the withAuth method to also fetch your user
   * You will need to implement UserDAO.findOneByUsername
   */
  def withUser(f: AccountAuth => Request[AnyContent] => Result) = withAuth { username => implicit request =>
    UserDAO.findOneByUsername(username).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }

  def withAdmin(f: AccountAuth => Request[AnyContent] => Result) = withAuth { username => implicit request =>
    UserDAO.findOneByUsername(username).filter(_.admin).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }
}

case class AccountAuth(acc:String, admin:Boolean)

object UserDAO {
  def findOneByUsername(name:String) =
    if(name == "admin")
      Some(AccountAuth("Admin", true))
    else {
      if (true) // if account exists
        Some(AccountAuth(name, false))
      else None
    }
}
