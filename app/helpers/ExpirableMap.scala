package helpers

import org.joda.time.DateTime

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}

import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.schedulers.ExecutionContextScheduler

import com.github.nscala_time.time.Implicits.{richReadableInstant,richDuration}

import helpers.ConcurrentMap.SimpleMap

object ExpirableMap {
  def apply[Id,Val](aMap: SimpleMap[Id,(DateTime,Val)])(implicit aContext: ExecutionContext) =
    new ExpirableMap[Id,Val](aMap,aContext)
}

class ExpirableMap[Id,Val](
  aMap    : SimpleMap[Id,(DateTime,Val)],
  aContext: ExecutionContext
) extends ConcurrentMap[Id,(DateTime,Val)](aMap, aContext) {

  protected val mMaxTime    = new DateTime(Long.MaxValue)
  protected val mScheduler  = ExecutionContextScheduler(aContext)
  protected val mPublisher  = PublishSubject[DateTime]()
  protected val mSubscriber =
    mPublisher.scan[(DateTime,DateTime)]((mMaxTime,mMaxTime)){
      case ((x,y),z) if z < y => (y,z)
      case ((_,x),_)          => (x,x)
    }.filter{case(x,y)=> x > y}
     .map{_._2}
     .switchMap{x=>
        Observable
          .timer{(DateTime.now to x).toDuration.toScalaDuration}
          .map{_=>x}
    }.subscribeOn(mScheduler)
     .observeOn(mScheduler)
     .subscribe{x=>
        aMap.filterNot{_._2._1 > x}
            .foreach{x => aMap.remove{x._1}}
      }

  override def run[T](aAction: Result[T]): Future[T] = super.run(aAction).andThen{
    case f@Failure(_) => f
    case s@Success(_) =>
      if(aMap.nonEmpty) mPublisher onNext {aMap.map{_._2._1}.minBy(_.millis)}
      s
  } (aContext)
}