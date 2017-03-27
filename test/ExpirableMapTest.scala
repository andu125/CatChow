import helpers.ExpirableMap
import helpers.ConcurrentMap.{get, put}

import scala.concurrent.Future
import scala.collection.mutable.HashMap

import org.joda.time.DateTime

import com.github.nscala_time.time.Implicits._

class ExpirableMapTest extends MapTest {
  override type Id = String
  override type Val = (DateTime,String)

  override val mKey  = "Key"
  override val mVal1 = (DateTime.now + 1.day, "Val1")
  override val mVal2 = (DateTime.now + 1.day, "Val2")

  override def Concurrent = ExpirableMap(new HashMap[Id,Val] with MapType)

  "Stored value" should "expire" in {
    val lVal = (DateTime.now + 5.seconds,"some")

    for{
     x <-mMap.run {
       for {
         _     <- put(mKey, lVal)
         value <- get(mKey)
       } yield value
     }
     _ <-Future {Thread.sleep(6000)}
     y <-Concurrent.run {
      for {
        value <- get(mKey)
      } yield value
     }
    } yield {assert(x == Some(lVal) && y==None)}
  }

  "Two stored values" should "expire" in {
    val lVal  = (DateTime.now + 5.seconds,"some")
    val lKey1 = "Key1"
    val lKey2 = "Key2"

    for{
      x <-mMap.run {
        for {
          _      <- put(lKey1, lVal)
          _      <- put(lKey2, lVal)
          value1 <- get(lKey1)
          value2 <- get(lKey2)
        } yield (value1, value2)
      }
      _ <-Future {Thread.sleep(6000)}
      y <-Concurrent.run {
        for {
          value1 <- get(lKey1)
          value2 <- get(lKey2)
        } yield (value1, value2)
      }
    } yield {assert(x == (Some(lVal),Some(lVal)) && y==(None,None))}
  }

  "Stored value" should "not expire" in {
    val lVal = (DateTime.now + 5.seconds,"some")

    for{
      x <-mMap.run {
        for {
          _     <- put(mKey, lVal)
          value <- get(mKey)
        } yield value
      }
      _ <-Future {Thread.sleep(2000)}
      y <-mMap.run {
        for {
          value <- get(mKey)
        } yield value
      }
    } yield {assert(x == Some(lVal) && y==Some(lVal))}
  }

  "Only one stored value" should "expire" in {
    val lKey1 = "Key1"
    val lVal1 = (DateTime.now + 10.seconds,"some1")
    val lKey2 = "Key2"
    val lVal2 = (DateTime.now + 2.seconds,"some2")

    for{
      _ <-mMap.run {
        for {
          omit <- put(lKey1, lVal1)
        } yield omit
      }
      _ <-Future {Thread.sleep(1000)}
      _ <-mMap.run {
        for {
          omit <- put(lKey2, lVal2)
        } yield omit
      }
      _     <-Future {Thread.sleep(3000)}
      (x,y) <-mMap.run {
        for {
          value1 <- get(lKey1)
          value2 <- get(lKey2)
        } yield (value1, value2)
      }
    } yield {assert(x==Some(lVal1) && y==None)}
  }
}
