package interfaces

import play.api.mvc.{Controller, EssentialAction}

/**
  * Created by andy on 04/03/2017.
  */
trait IHomeController extends Controller {
  def index() : EssentialAction
}
