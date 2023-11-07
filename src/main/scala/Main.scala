package auth0

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import typings.auth0Auth0SpaJs.distTypingsAuth0ClientMod.Auth0Client
import typings.auth0Auth0SpaJs.distTypingsGlobalMod.Auth0ClientOptions
import typings.auth0Auth0SpaJs.distTypingsGlobalMod.AuthorizationParams
import typings.auth0Auth0SpaJs.distTypingsGlobalMod.RedirectLoginOptions
import typings.auth0Auth0SpaJs.mod.createAuth0Client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*

// import javascriptLogo from "/javascript.svg"
@js.native @JSImport("/javascript.svg", JSImport.Default)
val javascriptLogo: String = js.native
@main
def App(): Unit = {
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    Main.appElement()
  )
}

@js.native
trait U extends js.Object {
  val name: String = js.native
  val nickname: String = js.native
  val sub: String = js.native
  val picture: String = js.native
}

object Main:
  def appElement(): Element = {
    val clientVar = Var[Option[Auth0Client]](None)
    val authChangedEvent = EventBus[Unit]()
    configAuth(clientVar, authChangedEvent)

    val inner = clientVar.signal.map {
      case None => div("loading...")
      case Some(client) =>
        div(
          a(
            href := "https://vitejs.dev",
            target := "_blank",
            img(src := "/vite.svg", className := "logo", alt := "Vite logo")
          ),
          a(
            href := "https://developer.mozilla.org/en-US/docs/Web/JavaScript",
            target := "_blank",
            img(
              src := javascriptLogo,
              className := "logo vanilla",
              alt := "JavaScript logo"
            )
          ),
          h1("Hello Laminar!"),
          userBanner(client, authChangedEvent),
          loginButton(client),
          logoutButton(client),
          p(
            className := "read-the-docs",
            "Click on the Vite logo to learn more"
          )
        )
    }

    div(
      child <-- inner
    )
  }

  def configAuth(
      clientVar: Var[Option[Auth0Client]],
      authChangedEvent: EventBus[Unit]
  ) = {
    val authParams =
      AuthorizationParams()
        .setRedirect_uri(org.scalajs.dom.window.location.origin)
    createAuth0Client(
      Auth0ClientOptions(
        "GCRr5W6ECWVOxDwxKOknmzQKSOUR5p1u",
        "windy.jp.auth0.com"
      ).setAuthorizationParams(authParams)
    ).toFuture.foreach { client =>
      // If we are came from redirect, save session
      val q = dom.window.location.search
      if (q.contains("code=") && q.contains("state=")) {
        client.handleRedirectCallback().toFuture.foreach { _ =>
          dom.window.history.replaceState(
            js.Dynamic.literal(),
            dom.document.title,
            "/"
          )
          authChangedEvent.emit(())
        }
      }

      clientVar.update(_ => Some(client))
    }
  }

  def userBanner(
      client: Auth0Client,
      authChangedBus: EventBus[Unit]
  ) = {
    // TODO: update after check authentication
    val v = EventStream.fromJsPromise(
      client
        .getUser()
        .asInstanceOf[js.Promise[U]]
    )
    p(
      img(
        src <-- v.map(_.picture),
        alt := "user icon"
      ),
      code(
        child.text <-- v.map(_.name)
      )
    )
  }

  def loginButton(client: Auth0Client): Element = {
    button(
      tpe := "button",
      "login",
      onClick --> { _ =>
        val authParams =
          AuthorizationParams()
            .setRedirect_uri(org.scalajs.dom.window.location.origin)
        val options =
          RedirectLoginOptions().setAuthorizationParams(authParams)
        client.loginWithRedirect(options)
      }
    )
  }

  def logoutButton(client: Auth0Client): Element = {
    button(
      tpe := "button",
      "logout",
      onClick --> { event =>
        client.logout()
      }
    )
  }
end Main
