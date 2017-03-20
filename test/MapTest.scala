import org.scalatest.{AsyncFlatSpec, BeforeAndAfter, Matchers}

import scala.util.{Failure, Success}
import helpers.ConcurrentMap
import helpers.ConcurrentMap.{SimpleMap, get, put, remove}

abstract class MapTest extends AsyncFlatSpec with Matchers with BeforeAndAfter {
  type Id
  type Val

  type MapType = SimpleMap[Id,Val]

  val mKey :Id
  val mVal1:Val
  val mVal2:Val
  var mMap :ConcurrentMap[Id,Val] = _

  def Concurrent: ConcurrentMap[Id,Val]

  before {
    mMap = Concurrent
  }

  "Map" should "not contain a key" in {
    mMap.run{
      for{
        x<-get(mKey)
      } yield x
    }.map{x => assert(x == None)}
  }

  "Map" should "not contain a previous value" in {
    mMap.run{
      for{
        x<-put(mKey,mVal1)
      } yield x
    }.map{x => assert(x == None)}
  }

  "Value" should "be stored after put" in {
    mMap.run{
      for{
        _<-put(mKey,mVal1)
        x<-get(mKey)
      } yield x
    }.map{x => assert(x == Some(mVal1))}
  }

  "Stored value" should "be removed" in {
    mMap.run{
      for{
        _<-put(mKey,mVal1)
        x<-remove(mKey)
        y<-get(mKey)
      } yield (x,y)
    }.map{x => assert(x == (Some(mVal1),None))}
  }

  "Stored value" should "be replaced" in {
    mMap.run{
      for{
        _<-put(mKey,mVal1)
        x<-put(mKey,mVal2)
        y<-get(mKey)
      } yield (x,y)
    }.map{x => assert(x == (Some(mVal1),Some(mVal2)))}
  }
}
