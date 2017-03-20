import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.collection.mutable.HashMap

import helpers.ConcurrentMap
import helpers.ConcurrentMap.{SimpleMap, get, put, remove}

import scala.util.{Failure, Success}

class MapTest extends AsyncFlatSpec with Matchers {
  type Id  = String
  type Val = String

  val mKey        = "Key"
  val mVal1       = "Val1"
  val mVal2       = "Val2"
  def Concurrent = ConcurrentMap(new HashMap[Id,Val] with SimpleMap[Id,Val])

  "Map" should "not contain a key" in {
    Concurrent.run{
      for{
        x<-get(mKey)
      } yield x
    }.map{x => assert(x == None)}
  }

  "Map" should "not contain a previous value" in {
    Concurrent.run{
      for{
        x<-put(mKey,mVal1)
      } yield x
    }.map{x => assert(x == None)}
  }

  "Value" should "be stored after put" in {
    Concurrent.run{
      for{
        _<-put(mKey,mVal1)
        x<-get(mKey)
      } yield x
    }.map{x => assert(x == Some(mVal1))}
  }

  "Stored value" should "be removed" in {
    Concurrent.run{
      for{
        _<-put(mKey,mVal1)
        x<-remove(mKey)
        y<-get(mKey)
      } yield (x,y)
    }.map{x => assert(x == (Some(mVal1),None))}
  }

  "Stored value" should "be replaced" in {
    Concurrent.run{
      for{
        _<-put(mKey,mVal1)
        x<-put(mKey,mVal2)
        y<-get(mKey)
      } yield (x,y)
    }.map{x => assert(x == (Some(mVal1),Some(mVal2)))}
  }

  "Stored value" should "be replaced2" in {
    val z = Concurrent.run{
      for{
        x<-get(mKey)
        y<-if(x.isDefined) put(mKey,mVal1) else throw new Exception()
      } yield x
    }.map{x => assert(x == (Some(mVal1),Some(mVal2)))}
    z.onComplete{
      case Success(x) => System.out.println(x)
      case Failure(x) => System.out.println(x+"AAAAA")
    }
    z
  }
}
