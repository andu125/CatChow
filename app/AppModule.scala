import java.util.Date

import scaldi.Module

import controllers.WebJarAssets

class AppModule extends Module {
  bind[WebJarAssets] to injected[WebJarAssets]
}
