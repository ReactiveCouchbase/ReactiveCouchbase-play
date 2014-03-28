package org.reactivecouchbase.play.java.crud;

import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CrudSourceController<T> extends Controller {

    public abstract CrudSource<T> getSource();
    public abstract String defaultDesignDocname();
    public abstract String defaultViewName();

    private String path    = "";
    private String Slash   = "/?";
    private String Id      = "/([^/]+)/?";
    private String Partial = "/([^/]+)/partial";
    private String Find    = "/find/?";
    private String Batch   = "/batch/?";

    private String extractId(String path) {
        return path.replace("/", "");
    }

    public Result onRequest(String other) {
        if (request().path().startsWith(path)) {
            String method = request().method();
            String partialPath = request().path().substring(0, path.length());
            if (method.equals("GET") && partialPath.matches(Id)) {
                return get(extractId(partialPath));
            } else if (method.equals("GET") && partialPath.matches(Slash)) {
                return find();
            } else if (method.equals("PUT") && partialPath.matches(Partial)) {
                return updatePartial(extractId(partialPath).replace("/partial", ""));
            } else if (method.equals("PUT") && partialPath.matches(Id)) {
                return update(extractId(partialPath));
            } else if (method.equals("PUT") && partialPath.matches(Batch)) {
                return batchUpdate();
            } else if (method.equals("POST") && partialPath.matches(Batch)) {
                return batchInsert();
            } else if (method.equals("POST") && partialPath.matches(Find)) {
                return find();
            } else if (method.equals("POST") && partialPath.matches(Slash)) {
                return insert();
            } else if (method.equals("DELETE") && partialPath.matches(Batch)) {
                return batchDelete();
            } else if (method.equals("DELETE") && partialPath.matches(Id)) {
                return delete(extractId(partialPath));
            } else {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    public Result get(final String id) {
        return async(getSource().get(id).map(new F.Function<F.Tuple<F.Option<T>, String>, Result>() {
            @Override
            public Result apply(F.Tuple<F.Option<T>, String> ts) throws Throwable {
                if (!ts._1.isDefined()) {
                   return notFound("ID '" + id + "' not found.");
                } else {
                    T t = ts._1.get();
                    JsonNode node = Json.toJson(t);
                    final JsonNode idField = node.get(getSource().ID);
                    if (idField != null) {
                        return ok(node);
                    } else {
                        ObjectNode idNode = Json.newObject();
                        idNode.put(getSource().ID, id);
                        idNode.putAll((ObjectNode) node);
                        return ok(idNode);
                    }
                }
            }
        }));
    }

    public Result insert() {
        T t = Json.fromJson(request().body().asJson(), getSource().clazz);
        return async(getSource().insert(t).map(new F.Function<String, Result>() {
            @Override
            public Result apply(String id) throws Throwable {
                return ok(Json.parse("{\"id\":\"" + id + "\"}"));
            }
        }));
    }

    public Result delete(final String id) {
        return async(getSource().delete(id).map(new F.Function<Void, Result>() {
            @Override
            public Result apply(Void aVoid) throws Throwable {
                return ok(Json.parse("{\"id\":\"" + id + "\"}"));
            }
        }));
    }

    public Result update(final String id) {
        T t = Json.fromJson(request().body().asJson(), getSource().clazz);
        return async(getSource().update(id, t).map(new F.Function<Void, Result>() {
            @Override
            public Result apply(Void aVoid) throws Throwable {
                return ok(Json.parse("{\"id\":\"" + id + "\"}"));
            }
        }));
    }

    public Result updatePartial(final String id) {
        JsonNode update = request().body().asJson();
        return async(getSource().updatePartial(id, update).map(new F.Function<Void, Result>() {
            @Override
            public Result apply(Void aVoid) throws Throwable {
                return ok(Json.parse("{\"id\":\"" + id + "\"}"));
            }
        }));
    }

    public Result find() {
        final F.Tuple<QueryObject, Query> tuple = QueryObject.extractQuery(request(), defaultDesignDocname(), defaultViewName());
        return async(getSource().view(tuple._1.docName, tuple._1.view).flatMap(new F.Function<View, F.Promise<Collection<F.Tuple<T, String>>>>() {
            @Override
            public F.Promise<Collection<F.Tuple<T, String>>> apply(View view) throws Throwable {
                return getSource().find(view, tuple._2);
            }
        }).map(new F.Function<Collection<F.Tuple<T, String>>, Result>() {
            @Override
            public Result apply(Collection<F.Tuple<T, String>> collectionPromise) throws Throwable {
                List<JsonNode> nodes = new ArrayList<JsonNode>();
                for (F.Tuple<T, String> tuple : collectionPromise) {
                    JsonNode node = Json.toJson(tuple._1);
                    final JsonNode idField = node.get(getSource().ID);
                    if (idField != null) {
                        nodes.add(node);
                    } else {
                        ObjectNode idNode = Json.newObject();
                        idNode.put(getSource().ID, tuple._2);
                        idNode.putAll((ObjectNode) node);
                        nodes.add(idNode);
                    }
                }
                return ok(Json.toJson(nodes));
            }
        }));
    }

    public Result batchInsert() {
        ArrayNode nodes = ((ArrayNode) request().body().asJson());
        List<T> values = new ArrayList<T>();
        for (JsonNode n : nodes) {
           values.add(Json.fromJson(n, getSource().clazz));
        }
        return async(getSource().batchInsert(values).map(new F.Function<Integer, Result>() {
            @Override
            public Result apply(Integer integer) throws Throwable {
                return ok(Json.parse("{\"nb\":" + integer + "}"));
            }
        }));
    }

    public Result batchDelete() {
        final F.Tuple<QueryObject, Query> tuple = QueryObject.extractQuery(request(), defaultDesignDocname(), defaultViewName());
        return async(getSource().view(tuple._1.docName, tuple._1.view).flatMap(new F.Function<View, F.Promise<Result>>() {
            @Override
            public F.Promise<Result> apply(View view) throws Throwable {
                return getSource().batchDelete(view, tuple._2).map(new F.Function<Integer, Result>() {
                    @Override
                    public Result apply(Integer integer) throws Throwable {
                        return ok("deleted");
                    }
                });
            }
        }));
    }

    public Result batchUpdate() {
        final F.Tuple<QueryObject, Query> tuple = QueryObject.extractQuery(request(), defaultDesignDocname(), defaultViewName());
        final JsonNode update = request().body().asJson();
        return async(getSource().view(tuple._1.docName, tuple._1.view).flatMap(new F.Function<View, F.Promise<Result>>() {
            @Override
            public F.Promise<Result> apply(View view) throws Throwable {
                return getSource().batchUpdate(view, tuple._2, update).map(new F.Function<Integer, Result>() {
                    @Override
                    public Result apply(Integer integer) throws Throwable {
                        return ok("updated");
                    }
                });
            }
        }));
    }


    private static class QueryObject {
        public String docName;
        public String view;
        public String q;
        public String from;
        public String to;
        public Integer limit;
        public Boolean descending;
        public Integer skip;

        private QueryObject(String docName, String view, String q, String from, String to, Integer limit, Boolean descending, Integer skip) {
            this.docName = docName;
            this.view = view;
            this.q = q;
            this.from = from;
            this.to = to;
            this.limit = limit;
            this.descending = descending;
            this.skip = skip;
        }

        @Override
        public String toString() {
            return "QueryObject{" +
                    "docName='" + docName + '\'' +
                    ", view='" + view + '\'' +
                    ", q='" + q + '\'' +
                    ", from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    ", limit=" + limit +
                    ", descending=" + descending +
                    ", skip=" + skip +
                    '}';
        }

        public QueryObject() {}

        private static String get(Http.Request request, String name, Object defaultValue) {
            String val = request.getQueryString(name);
            if (val == null) {
                return defaultValue == null ? null : defaultValue.toString();
            }
            return val;
        }

        public static QueryObject extractQueryObject(Http.Request request, String defaultDesignDocname, String defaultViewName) {
            QueryObject maybeQuery = request().body().asJson() == null ? new QueryObject() : Json.fromJson(request().body().asJson(), QueryObject.class);
            String q = get(request, "q", maybeQuery.q);
            Integer limit = get(request, "limit", maybeQuery.limit) == null ? null : Integer.valueOf(get(request, "limit", maybeQuery.limit));
            Boolean descending = get(request, "descending", maybeQuery.descending) == null ? null : Boolean.valueOf(get(request, "descending", maybeQuery.descending));
            Integer skip = get(request, "skip", maybeQuery.skip) == null ? null : Integer.valueOf(get(request, "skip", maybeQuery.skip));
            String from = get(request, "from", maybeQuery.from);
            String to = get(request, "to", maybeQuery.to);
            String v = get(request, "v", maybeQuery.view) == null ? defaultViewName : get(request, "v", maybeQuery.view);
            String doc = get(request, "doc", maybeQuery.docName) == null ? defaultDesignDocname : get(request, "doc", maybeQuery.docName);
            return new QueryObject(doc, v, q, from, to, limit, descending, skip);
        }

        public static F.Tuple<QueryObject, Query> extractQuery(Http.Request request, String defaultDesignDocname, String defaultViewName) {
            QueryObject q = extractQueryObject( request, defaultDesignDocname, defaultViewName );
            return new F.Tuple<QueryObject, Query>( q, extractQuery( q ) );
        }

        public static Query extractQuery(QueryObject queryObject) {
            Query query = new Query().setIncludeDocs(true).setStale(Stale.FALSE);
            if (queryObject.q != null) {
                query = query.setRangeStart(ComplexKey.of(queryObject.q))
                        .setRangeEnd(ComplexKey.of(queryObject.q + "\uefff"));
            } else if (queryObject.from != null && queryObject.to != null) {
                query = query.setRangeStart(queryObject.from).setRangeEnd(queryObject.to);
            }
            if (queryObject.limit != null) {
                query = query.setLimit(queryObject.limit);
            }
            if (queryObject.skip != null) {
                query = query.setSkip(queryObject.skip);
            }
            if (queryObject.descending != null) {
                query = query.setDescending(queryObject.descending);
            }
            return query;
        }
    }
}
