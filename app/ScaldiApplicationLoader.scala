import scaldi.Injectable

import play.api.ApplicationLoader.Context
import play.api.{Application, ApplicationLoader}

class ScaldiApplicationLoader extends ApplicationLoader {
  override def load(aContext: Context): Application = {
    implicit val lModule = new AppModule::new BaseAppModule(aContext)

    Injectable.inject[Application]
  }
}
