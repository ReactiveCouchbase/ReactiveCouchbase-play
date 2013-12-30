package es

import akka.actor.{ActorRef, Props, ActorSystem}
import org.reactivecouchbase.play.PlayCouchbase
import org.reactivecouchbase.eventstore.{EventStored, CouchbaseEventSourcing}
import models.Formatters
import play.api.Play.current
import play.api.{Logger, Mode, Play}
import java.util.concurrent.atomic.AtomicReference

object Bootstrap {

  implicit val ec = PlayCouchbase.couchbaseExecutor

  private val processorRef = new AtomicReference[Option[ActorRef]](None)
  private val validatorRef = new AtomicReference[Option[ActorRef]](None)
  private val ordersHandlerRef = new AtomicReference[Option[ActorRef]](None)
  private val systemRef = new AtomicReference[Option[ActorSystem]](None)

  def processor = processorRef.get().getOrElse(throw new IllegalStateException("Processor should have been here !!!"))
  def validator = validatorRef.get().getOrElse(throw new IllegalStateException("validator should have been here !!!"))
  def ordersHandler = ordersHandlerRef.get().getOrElse(throw new IllegalStateException("ordersHandler should have been here !!!"))

  def bootstrap() = {
    val system: ActorSystem = ActorSystem(s"es-system-${System.currentTimeMillis()}")
    val bucket = PlayCouchbase.bucket("es")

    val couchbaseEventSourcing = CouchbaseEventSourcing( system, bucket)
      .registerEventFormatter(Formatters.CreditCardValidatedFormatter)
      .registerEventFormatter(Formatters.CreditCardValidationRequestedFormatter)
      .registerEventFormatter(Formatters.OrderAcceptedFormatter)
      .registerEventFormatter(Formatters.OrderFormatter)
      .registerEventFormatter(Formatters.OrderSubmittedFormatter)
      .registerSnapshotFormatter(Formatters.StateFormatter)

    val processor = couchbaseEventSourcing.processorOf(Props(new OrderProcessor with EventStored))
    val validator = system.actorOf(Props(new CreditCardValidator(processor)))
    val ordersHandler = system.actorOf(Props(new OrdersHandler))

    systemRef.set(Some(system))
    processorRef.set(Some(processor))
    validatorRef.set(Some(validator))
    ordersHandlerRef.set(Some(ordersHandler))
    couchbaseEventSourcing.replayAll()
  }

  def shutdown() = {
    systemRef.get().map(_.shutdown())
  }
}
