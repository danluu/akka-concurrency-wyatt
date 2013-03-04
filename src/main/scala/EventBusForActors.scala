package zzz.akka.avionics

// This is a phantom type implementation of a concept that
// supplies default values to type parameters - something
// lacking in Scala.  Solution is not my own.  This is
// provided by Aaron Novstrup from the Stack Overflow post
// http://stackoverflow.com/a/6629984/230401
sealed class DefaultsTo[A, B]
trait LowPriorityDefaultsTo {
  implicit def overrideDefault[A, B] = new DefaultsTo[A, B]
}
object DefaultsTo extends LowPriorityDefaultsTo {
  implicit def default[B] = new DefaultsTo[B, B]
}
import akka.event.{ ActorEventBus, LookupClassification }
object EventBusForActors {
  val classify: Any => Class[_] = { event => event.getClass }
}
class EventBusForActors[EventType, ClassifierType](
  classifier: EventType => ClassifierType = EventBusForActors.classify)(implicit e: EventType DefaultsTo Any,
    c: ClassifierType DefaultsTo Class[_])
  extends ActorEventBus with LookupClassification {
  // Declares that this bus can publish events of any type
  type Event = EventType
  // We're going to classify our events by the class type
  type Classifier = ClassifierType
  // These next three methods are abstract in the
  // LookupClassification that we've mixed in.  The
  // LookupClassification fills in the methods that the
  // EventBus requires so that it can expose more specific
  // requirements on us that present less of a burden than
  // what it's managing for us.
  protected def classify(event: Event): Classifier = {
    classifier(event)
  }
  protected def mapSize(): Int = 32
  protected def publish(event: Event,
    subscriber: Subscriber): Unit = {
    subscriber ! event
  }
}
