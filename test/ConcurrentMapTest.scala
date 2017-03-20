import scala.collection.mutable.HashMap

import helpers.ConcurrentMap

class ConcurrentMapTest extends MapTest {
  override type Id  = String
  override type Val = String

  override val mKey       = "Key"
  override val mVal1      = "Val1"
  override val mVal2      = "Val2"
  override def Concurrent = ConcurrentMap(new HashMap[Id,Val] with MapType)
}
