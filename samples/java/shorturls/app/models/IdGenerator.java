package models;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.util.Timeout;
import net.spy.memcached.ops.OperationStatus;
import play.libs.F;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class IdGenerator {

    public static  class Counter {
        public Long value;

        public Counter() {}
        public Counter(Long value) {
            this.value = value;
        }
    }

    public static class IncrementAndGet {}

    public static ActorSystem system = ActorSystem.create("AgentSystem");
    public static Timeout t = new Timeout(Duration.create(5, TimeUnit.SECONDS));
    public static ActorRef generator = system.actorOf(new Props(Generator.class), "generator");
    public static String counterKey = "urlidgenerator";
    public static F.Promise<Object> nextId() {
        return new F.Promise(ask(generator, new IncrementAndGet(), t));
    }

    public static class Generator extends UntypedActor {

        public void onReceive(Object message) throws Exception {
            if (message instanceof IncrementAndGet) {
                final ActorRef ref = sender();
                ShortURL.bucket.get(counterKey, Counter.class).map(new F.Function<Counter, Object>() {
                    @Override
                    public Object apply(Counter counter) throws Throwable {
                        if (counter != null) {
                            final Counter newValue = new Counter(counter.value + 1);
                            ShortURL.bucket.set(counterKey, newValue).map(new F.Function<OperationStatus, Object>() {
                                @Override
                                public Object apply(OperationStatus operationStatus) throws Throwable {
                                    ref.tell(newValue.value, self());
                                    return null;
                                }
                            });
                            return null;
                        } else {
                            ShortURL.bucket.set(counterKey, 1L).map(new F.Function<OperationStatus, Object>() {
                                @Override
                                public Object apply(OperationStatus operationStatus) throws Throwable {
                                    ref.tell(1L, self());
                                    return null;
                                }
                            });
                            return null;
                        }
                    }
                });
            }
        }
    }
}