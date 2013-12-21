package org.ancelin.play2.java.couchbase;

import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.ops.OperationStatus;
import org.ancelin.play2.couchbase.Couchbase$;
import play.Play;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import scala.concurrent.ExecutionContext;

import java.util.Collection;


public class CouchbaseBucket {

    public final org.ancelin.play2.couchbase.CouchbaseBucket client;

    private final Couchbase$ couchbase = Couchbase$.MODULE$;
    private final ExecutionContext ec = couchbase.couchbaseExecutor(Play.application().getWrappedApplication());

    CouchbaseBucket(org.ancelin.play2.couchbase.CouchbaseBucket client) {
        this.client = client;
    }

    public <T> Promise<Collection<T>> find(String docName, String viewName, Query query, Class<T> clazz) {
        return Promise.wrap(couchbase.javaFind(docName, viewName, query, clazz, client, ec));
    }

    public <T> Promise<Collection<T>> find(View view, Query query, Class<T> clazz) {
        return Promise.wrap(couchbase.javaFind(view, query, clazz, client, ec));
    }

    public <T> Promise<Collection<Row<T>>> search(String docName, String viewName, Query query, Class<T> clazz) {
        return Promise.wrap(couchbase.javaFullFind(docName, viewName, query, clazz, client, ec));
    }

    public <T> Promise<Collection<Row<T>>> search(View view, Query query, Class<T> clazz) {
        return Promise.wrap(couchbase.javaFullFind(view, query, clazz, client, ec));
    }

    public Promise<View> view(String docName, String view) {
        return Promise.wrap(couchbase.javaView(docName, view, client, ec));
    }

    public <T> Promise<T> get(String key, Class<T> clazz) {
        return Promise.wrap(couchbase.javaGet(key, clazz, client, ec));
    }

    public <T> Promise<F.Option<T>> getOpt(String key, Class<T> clazz) {
        return Promise.wrap(couchbase.javaOptGet(key, clazz, client, ec));
    }

    public Promise<OperationStatus> incr(String key, Integer of) {
        return Promise.wrap(couchbase.incr(key, of, client, ec));
    }

    public Promise<OperationStatus> incr(String key, Long of) {
        return Promise.wrap(couchbase.incr(key, of, client, ec));
    }

    public Promise<OperationStatus> decr(String key, Integer of) {
        return Promise.wrap(couchbase.decr(key, of, client, ec));
    }

    public Promise<OperationStatus> decr(String key, Long of) {
        return Promise.wrap(couchbase.decr(key, of, client, ec));
    }

    public Promise<Integer> incrAndGet(String key, Integer of) {
        return Promise.wrap(couchbase.asJavaInt(couchbase.incrAndGet(key, of, client, ec), ec));
    }

    public Promise<Long> incrAndGet(String key, Long of) {
        return Promise.wrap(couchbase.asJavaLong(couchbase.incrAndGet(key, of, client, ec), ec));
    }

    public Promise<Integer> decrAndGet(String key, Integer of) {
        return Promise.wrap(couchbase.asJavaInt(couchbase.decrAndGet(key, of, client, ec), ec));
    }

    public Promise<Long> decrAndGet(String key, Long of) {
        return Promise.wrap(couchbase.asJavaLong(couchbase.decrAndGet(key, of, client, ec), ec));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Set Operations
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> Promise<OperationStatus> set(String key, T value) {
        return Promise.wrap(couchbase.javaSet(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, int exp, T value) {
        return Promise.wrap(couchbase.javaSet(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, int exp, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaSet(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaSet(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, int exp, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaSet(key, exp, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaSet(key, -1, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, int exp, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaSet(key, exp, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> set(String key, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaSet(key, -1, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add Operations
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> Promise<OperationStatus> add(String key, T value) {
        return Promise.wrap(couchbase.javaAdd(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, int exp, T value) {
        return Promise.wrap(couchbase.javaAdd(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, int exp, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaAdd(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaAdd(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, int exp, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaAdd(key, exp, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaAdd(key, -1, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, int exp, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaAdd(key, exp, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> add(String key, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaAdd(key, -1, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Replace Operations
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> Promise<OperationStatus> replace(String key, int exp, T value) {
        return Promise.wrap(couchbase.javaReplace(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, int exp, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaReplace(key, exp, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, T value, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaReplace(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, int exp, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaReplace(key, exp, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, T value, PersistTo persistTo) {
        return Promise.wrap(couchbase.javaReplace(key, -1, Json.stringify(Json.toJson(value)), persistTo, ReplicateTo.ZERO, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, int exp, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaReplace(key, exp, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, T value, PersistTo persistTo, ReplicateTo replicateTo) {
        return Promise.wrap(couchbase.javaReplace(key, -1, Json.stringify(Json.toJson(value)), persistTo, replicateTo, client, ec));
    }

    public <T> Promise<OperationStatus> replace(String key, T value) {
        return Promise.wrap(couchbase.javaReplace(key, -1, Json.stringify(Json.toJson(value)), PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete Operations
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Promise<OperationStatus> delete(String key){
        return Promise.wrap(couchbase.delete(key, PersistTo.ZERO, ReplicateTo.ZERO, client, ec));
    }

    public Promise<OperationStatus> delete(String key, ReplicateTo replicateTo){
        return Promise.wrap(couchbase.delete(key, PersistTo.ZERO, replicateTo, client, ec));
    }

    public Promise<OperationStatus> delete(String key, PersistTo persistTo){
        return Promise.wrap(couchbase.delete(key, persistTo, ReplicateTo.ZERO, client, ec));
    }

    public Promise<OperationStatus> delete(String key, PersistTo persistTo, ReplicateTo replicateTo){
        return Promise.wrap(couchbase.delete(key, persistTo, replicateTo, client, ec));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Flush Operations
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Promise<OperationStatus> flush(int delay) {
        return Promise.wrap(couchbase.flush(delay, client, ec));
    }

    public Promise<OperationStatus> flush() {
       return Promise.wrap(couchbase.flush(client, ec));
    }
}
