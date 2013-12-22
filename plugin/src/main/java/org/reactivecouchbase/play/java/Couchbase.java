package org.reactivecouchbase.play.java;

import org.reactivecouchbase.play.plugins.CouchbasePlugin;
import play.Play;
import play.api.PlayException;
import play.libs.F;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterator;

import java.util.HashMap;
import java.util.Map;

public class Couchbase {

    private static String initMessage = "The CouchbasePlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:package org.ancelin.play2.couchbase.CouchbasePlugin' (400 is an arbitrary priority and may be changed to match your needs).";
    private static String connectMessage = "The CouchbasePlugin doesn't seems to be connected to a Couchbase server. Maybe an error occured!";

    private static Map<String, CouchbaseBucket> bucketsCache = new HashMap<String, CouchbaseBucket>();

    private static CouchbasePlugin plugin() {
        return Play.application().plugin(CouchbasePlugin.class);
    }

    public static Map<String, CouchbaseBucket> buckets() {
        if (bucketsCache.isEmpty()) {
            Iterator<Tuple2<String, org.reactivecouchbase.CouchbaseBucket>> iterator = plugin().buckets().iterator();
            while(iterator.hasNext()) {
                Tuple2<String, org.reactivecouchbase.CouchbaseBucket> tuple = iterator.next();
                bucketsCache.put(tuple._1(), new CouchbaseBucket(tuple._2()));
            }
        }
        return bucketsCache;
    }

    public static CouchbaseBucket bucket(String name) {
        Option<org.reactivecouchbase.CouchbaseBucket> opt = plugin().buckets().get(name);
        if (opt.isDefined()) {
            return new CouchbaseBucket(opt.get());
        }
        throw new PlayException("CouchbasePlugin Error", initMessage);
    }

    public static CouchbaseBucket defaultBucket() {
        Option<Tuple2<String,org.reactivecouchbase.CouchbaseBucket>> tuple2Option = plugin().buckets().headOption();
        if (tuple2Option.isDefined()) {
            return new CouchbaseBucket(tuple2Option.get()._2());
        }
        throw new PlayException("CouchbasePlugin Error", connectMessage);
    }

    public static <T> T withCouchbase(F.Function<CouchbaseBucket, T> block) {
        try {
            return block.apply(defaultBucket());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static <T> T withCouchbase(String bucket, F.Function<CouchbaseBucket, T> block) {
        try {
            return block.apply(bucket(bucket));
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
