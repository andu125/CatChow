package helpers

import org.joda.time.DateTime

import scalaz.Scalaz._
import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.Zero
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.schedulers.ExecutionContextScheduler
import com.github.nscala_time.time.Implicits.richReadableInstant
import helpers.ConcurrentMap._

object ExpirableMap {
  def apply[Id,Val](aMap: SimpleMap[Id,(DateTime,Val)])(implicit aContext: ExecutionContext) =
    new ExpirableMap[Id,Val](aMap,aContext)
}

class ExpirableMap[Id,Val](
  aMap    : SimpleMap[Id,(DateTime,Val)],
  aContext: ExecutionContext
) extends ConcurrentMap[Id,(DateTime,Val)](aMap, aContext) with AutoCloseable {

  protected val mMaxTime      = new DateTime(Long.MaxValue)
  protected val mSubject      = PublishSubject[DateTime]()
  protected val mScheduler    = ExecutionContextScheduler(aContext)
  protected val mSubscription =
    mSubject.scan((true,mMaxTime)){
      case ((_,y),z) if z < y => (true,z)
      case ((_,x),_)          => (false,x)
    }.filter{_._1}
     .map{_._2}
     .switchMap{x=>
        Observable
          .timer{toDuration(x)}
          .map{_=>x}
    }.subscribeOn(mScheduler)
     .observeOn(mScheduler)
     .subscribe{x=>
        aMap.filterNot{_._2._1 > x}
            .foldLeft(().point[Result]){
              case (acc,(id,_))=> for {
                _<-acc
                _<-remove(id)
              } yield ()
            } |> run
      }

  def toDuration(aTime: DateTime) = {
    if(DateTime.now >= aTime) Zero
    else Try{(aTime.getMillis - DateTime.now.getMillis) millis}.getOrElse(365 days)
  }

  override def run[T](aAction: Result[T]): Future[T] = super.run(aAction).andThen{
    case f@Failure(_) => f
    case s@Success(_) =>
      if(aMap.nonEmpty) mSubject onNext {aMap.map{_._2._1}.minBy(_.millis)}
      s
  } (aContext)

  override def close(): Unit = mSubscription.unsubscribe()
}