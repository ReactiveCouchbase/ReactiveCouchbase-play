package org.reactivecouchbase.play.java.crud;

import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import net.spy.memcached.ops.OperationStatus;
import org.reactivecouchbase.play.java.CouchbaseBucket;
import play.libs.F;
import play.libs.Json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CrudSource<T> {

    public final CouchbaseBucket bucket;
    public final Class<T> clazz;
    public final String ID; // = "_id";

    public CrudSource(CouchbaseBucket bucket, Class<T> clazz) {
        this.bucket = bucket;
        this.clazz = clazz;
        this.ID = "_id";
    }

    public CrudSource(CouchbaseBucket bucket, String idKey, Class<T> clazz) {
        this.bucket = bucket;
        this.clazz = clazz;
        this.ID = idKey;
    }

    public F.Promise<F.Tuple<F.Option<T>, String>> get(final String id) {
        return bucket.getOpt(id, clazz).map(new F.Function<F.Option<T>, F.Tuple<F.Option<T>, String>>() {
            @Override
            public F.Tuple<F.Option<T>, String> apply(F.Option<T> ts) throws Throwable {
                return new F.Tuple<F.Option<T>, String>(ts, id);
            }
        });
    }

    public F.Promise<String> insert(T t) {
        String id = UUID.randomUUID().toString();
        JsonNode node = Json.toJson(t);
        JsonNode idField = node.findValue(ID);
        if (idField != null) {
            id = idField.asText();
        }
        final String finalId = id;
        return bucket.set(id, t).map(new F.Function<OperationStatus, String>() {
            @Override
            public String apply(OperationStatus operationStatus) throws Throwable {
                return finalId;
            }
        });
    }

    public F.Promise<Void> delete(String id) {
        return bucket.delete(id).map(new F.Function<OperationStatus, Void>() {
            @Override
            public Void apply(OperationStatus operationStatus) throws Throwable {
                return null;
            }
        });
    }

    public F.Promise<Void> update(String id, T t) {
        return bucket.replace(id, t).map(new F.Function<OperationStatus, Void>() {
            @Override
            public Void apply(OperationStatus operationStatus) throws Throwable {
                return null;
            }
        });
    }

    public F.Promise<Void> updatePartial(final String id, final JsonNode update) {
        return updatePartialWithStatus(id, update).map(new F.Function<OperationStatus, Void>() {
            @Override
            public Void apply(OperationStatus operationStatus) throws Throwable {
                return null;
            }
        });
    }

    private F.Promise<OperationStatus> updatePartialWithStatus(final String id, final JsonNode update) {
        return get(id).flatMap(new F.Function<F.Tuple<F.Option<T>, String>, F.Promise<OperationStatus>>() {
            @Override
            public F.Promise<OperationStatus> apply(F.Tuple<F.Option<T>, String> ts) throws Throwable {
                T updatedValue = ts._1.map(new F.Function<T, T>() {
                    @Override
                    public T apply(T t) throws Throwable {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ObjectReader updater = objectMapper.readerForUpdating(t);
                        try {
                            return updater.readValue(update);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                }).getOrElse(null);
                if (updatedValue != null) {
                    return bucket.replace(id, updatedValue);
                }
                return F.Promise.pure(null);
            }
        });
    }

    public F.Promise<Integer> batchInsert(Iterable<T> values) {
        List<F.Promise<? extends OperationStatus>> promises = new ArrayList<F.Promise<? extends OperationStatus>>();
        for (T t : values) {
            String id = UUID.randomUUID().toString();
            JsonNode node = Json.toJson(t);
            JsonNode idField = node.findValue(ID);
            if (idField != null) {
                id = idField.asText();
            }
            promises.add(bucket.set(id, t));
        }
        return F.Promise.sequence(promises).map(new F.Function<List<OperationStatus>, Integer>() {
            @Override
            public Integer apply(List<OperationStatus> operationStatuses) throws Throwable {
                AtomicInteger total = new AtomicInteger(0);
                for (OperationStatus status : operationStatuses) {
                    if (status.isSuccess()) {
                        total.incrementAndGet();
                    }
                }
                return total.get();
            }
        });
    }

    public F.Promise<Integer> batchDelete(View view, Query query) {
        return bucket.find(view, query, clazz).flatMap(new F.Function<Collection<T>, F.Promise<Integer>>() {
            @Override
            public F.Promise<Integer> apply(Collection<T> ts) throws Throwable {
                List<F.Promise<? extends OperationStatus>> promises = new ArrayList<F.Promise<? extends OperationStatus>>();
                for (T t : ts) {
                    JsonNode node = Json.toJson(t);
                    JsonNode idField = node.findValue(ID);
                    if (idField != null) {
                        promises.add(bucket.delete(idField.asText()));
                    }
                }
                return F.Promise.sequence(promises).map(new F.Function<List<OperationStatus>, Integer>() {
                    @Override
                    public Integer apply(List<OperationStatus> operationStatuses) throws Throwable {
                        AtomicInteger total = new AtomicInteger(0);
                        for (OperationStatus status : operationStatuses) {
                            if (status.isSuccess()) {
                                total.incrementAndGet();
                            }
                        }
                        return total.get();
                    }
                });
            }
        });
    }

    public F.Promise<Integer> batchUpdate(View view, Query query, final JsonNode update) {
        return bucket.find(view, query, clazz).flatMap(new F.Function<Collection<T>, F.Promise<Integer>>() {
            @Override
            public F.Promise<Integer> apply(Collection<T> ts) throws Throwable {
                List<F.Promise<? extends OperationStatus>> promises = new ArrayList<F.Promise<? extends OperationStatus>>();
                for (T t : ts) {
                    JsonNode node = Json.toJson(t);
                    final JsonNode idField = node.findValue(ID);
                    if (idField != null) {
                        promises.add(bucket.get(idField.asText(), clazz).flatMap(new F.Function<T, F.Promise<OperationStatus>>() {
                            @Override
                            public F.Promise<OperationStatus> apply(T t) throws Throwable {
                                return bucket.replace(idField.asText(), t);
                            }
                        }));
                    }
                }
                return F.Promise.sequence(promises).map(new F.Function<List<OperationStatus>, Integer>() {
                    @Override
                    public Integer apply(List<OperationStatus> operationStatuses) throws Throwable {
                        AtomicInteger total = new AtomicInteger(0);
                        for (OperationStatus status : operationStatuses) {
                            if (status.isSuccess()) {
                                total.incrementAndGet();
                            }
                        }
                        return total.get();
                    }
                });
            }
        });
    }

    public F.Promise<Collection<F.Tuple<T, String>>> find(View view, Query query) {
        return bucket.find(view, query, clazz).map(new F.Function<Collection<T>, Collection<F.Tuple<T, String>>>() {
            @Override
            public Collection<F.Tuple<T, String>> apply(Collection<T> objects) throws Throwable {
                Collection<F.Tuple<T, String>> values = new ArrayList<F.Tuple<T, String>>();
                for (T t : objects) {
                    String id = "";
                    JsonNode node = Json.toJson(t);
                    JsonNode idField = node.findValue(ID);
                    if (idField != null) {
                        id = idField.asText();
                    }
                    values.add(new F.Tuple<T, String>(t, id));
                }
                return values;
            }
        });
    }
    
    public F.Promise<View> view(String docName, String viewName) {
        return bucket.view(docName, viewName);
    }
}
