package helpers

import scalaz.Reader
import scala.concurrent.{ExecutionContext, Future}

import helpers.ConcurrentMap.{Action, SimpleMap}

object ConcurrentMap {
  trait SimpleMap[Id,Val] {
    /** Gets a key from a map
      *  @param aKey map's key
      *  @return Some(Value) if map contains aKey or None otherwise
      */
    def get(aKey:Id):Option[Val]

    /** Puts a value under a specified key
      *  @param aKey a key under which aVal should be available
      *  @param aVal a value to associate with aKey
      *  @return Some(Value) if aKey was already associate with other value or None otherwise
      */
    def put(aKey:Id,aVal:Val): Option[Val]

    /** Remove a specified key and a value associated with it
      *  @param aKey a key which should be removed
      *  @return Some(Value) if aKey was associate with value or None otherwise
      */
    def remove(aKey:Id): Option[Val]
  }
  type Action[Id,Val,T] = Reader[SimpleMap[Id,Val],T]

  def get[Id,Val](aKey:Id): Action[Id,Val,Option[Val]] =
    Reader(m=>m.get(aKey))

  def put[Id,Val](aKey:Id,aVal:Val): Action[Id,Val,Option[Val]] =
    Reader(m=>m.put(aKey,aVal))

  def remove[Id,Val](aKey:Id): Action[Id,Val,Option[Val]] =
    Reader(m=>m.remove(aKey))


  def apply[Id,Val](aMap: SimpleMap[Id,Val])(implicit aContext: ExecutionContext) =
    new ConcurrentMap[Id,Val](aMap,aContext)
}

class ConcurrentMap[Id,Val](aMap:SimpleMap[Id,Val], aContext: ExecutionContext) {
  type Result[T] = Action[Id, Val, T]

  def run[T](aAction: Result[T]): Future[T] = Future {
    aAction.run(aMap)
  }(aContext)
}