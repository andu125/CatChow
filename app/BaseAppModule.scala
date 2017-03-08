import controllers._

import scaldi.Module

import akka.actor.ActorSystem

import play.api.http._
import play.api.ApplicationLoader.Context
import play.api.inject.ApplicationLifecycle
import play.api.routing.{Router, SimpleRouter}
import play.api.{Application, BuiltInComponentsFromContext, Configuration, Environment}

class BaseAppModule(aContext: Context) extends Module {
  bind[Router]               to injected[router.Routes]
  bind[Context]              to aContext
  bind[Application]          to inject[BuiltInComponentsFromContext].application
  bind[ActorSystem]          to inject[Application].actorSystem
  bind[Environment]          to aContext.environment
  bind[Configuration]        to aContext.initialConfiguration
  bind[ApplicationLifecycle] to aContext.lifecycle

  bind[AssetsBuilder]        to controllers.Assets
  bind[AssetsMetadata]       to injected[AssetsMetadataProvider].get
  bind[AssetsConfiguration]  to AssetsConfiguration()

  bind[FileMimeTypes]        to new DefaultFileMimeTypesProvider(inject[HttpConfiguration].fileMimeTypes).get
  bind[HttpErrorHandler]     to DefaultHttpErrorHandler
  bind[HttpConfiguration]    to HttpConfiguration.fromConfiguration(inject[Configuration],inject[Environment])

  bind[BuiltInComponentsFromContext] to new BuiltInComponentsFromContext(inject[Context]) {
    implicit val lInjector = BaseAppModule.this
    override def router    = SimpleRouter{case x => inject[Router].routes(x)}
  }
}
