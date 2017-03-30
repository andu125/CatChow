package helpers

import org.joda.time.DateTime

import scalaz.Scalaz._
import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.Zero
import rx.lang.scala.Observable
import rx.lang.scala.Observable.{timer,using,just,never}
import rx.lang.scala.subjects.BehaviorSubject
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

  implicit def toDuration(aTime: DateTime) = {
    if(DateTime.now >= aTime) Zero
    else Try{(aTime.getMillis - DateTime.now.getMillis) millis}.getOrElse(365 days)
  }

  protected val mMaxTime      = new DateTime(Long.MaxValue)
  protected val mSubject      = BehaviorSubject[DateTime](mMaxTime)
  protected val mScheduler    = ExecutionContextScheduler(aContext)
  protected val mSubscription =
    mSubject
      .slidingBuffer(2,1)
      .scan(Observable.empty: Observable[DateTime]){
        case (o,h1+:h2+:_) if h2 != h1 => timer{h2}.merge{never}.map{_=>h2}.replay.refCount
        case (o,_)                     => o
     }.switchMap{x=>using(x.subscribe)(_=>just(x),_.unsubscribe)}
      .switchMap{x=>x}
      .subscribeOn(mScheduler)
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

  override def run[T](aAction: Result[T]): Future[T] = super.run(aAction).andThen{
    case f@Failure(_) => f
    case s@Success(_) =>
      if(aMap.nonEmpty) mSubject onNext {aMap.map{_._2._1}.minBy(_.millis)}
      s
  } (aContext)

  override def close(): Unit = mSubscription.unsubscribe()
}