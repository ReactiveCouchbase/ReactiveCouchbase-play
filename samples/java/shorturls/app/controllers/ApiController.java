package controllers;

import models.IdGenerator;
import models.ShortURL;
import net.spy.memcached.ops.OperationStatus;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivecouchbase.client.OpResult;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Collection;

public class ApiController extends Controller {

    public static Result getUrl(String id) {
        return async(
            ShortURL.findById(id).map(new F.Function<ShortURL, Result>() {
                @Override
                public Result apply(ShortURL shortURL) throws Throwable {
                    if (shortURL == null) {
                        return notFound();
                    }
                    return ok(Json.toJson(shortURL));
                }
            })
        );
    }

    public static Result delete(String id) {
        return async(
            ShortURL.remove(id).map(new F.Function<OpResult, Result>() {
                @Override
                public Result apply(OpResult status) throws Throwable {
                    if (status.isSuccess()) {
                        ObjectNode node = Json.newObject();
                        node.put("status", "deleted");
                        node.put("error", false);
                        node.put("deleted", true);
                        node.put("message", status.getMessage());
                        return ok(node);
                    } else {
                        ObjectNode node = Json.newObject();
                        node.put("status", "error");
                        node.put("error", true);
                        node.put("deleted", false);
                        node.put("message", status.getMessage());
                        return  badRequest(node);
                    }
                }
            })
        );
    }

    public static Result getAllUrls() {
        return async(
            ShortURL.findAll().map(new F.Function<Collection<ShortURL>, Result>() {
                @Override
                public Result apply(Collection < ShortURL > shortURLs) throws Throwable {
                    return ok(Json.toJson(shortURLs));
                }
            })
        );
    }

    public static Result createUrl() {
        String urlValue = null;
        try {
            urlValue = ShortURL.form.bindFromRequest().get().url;
        } catch(Exception e) {}

        if (urlValue == null) {
            ObjectNode node = Json.newObject();
            node.put("status", "error");
            node.put("error", true);
            node.put("deleted", false);
            node.put("message", "You need to pass a non empty url value");
            return badRequest(node);
        } else {
            final String url = urlValue;
            return async(ShortURL.findByURL(url).flatMap(new F.Function<F.Option<ShortURL>, F.Promise<Result>>() {
                @Override
                public F.Promise<Result> apply(F.Option<ShortURL> maybe) throws Throwable {
                    return maybe.map(new F.Function<ShortURL, F.Promise<Result>>() {
                        @Override
                        public F.Promise<Result> apply(ShortURL shortURL) throws Throwable {
                            ObjectNode node = Json.newObject();
                            node.put("status", "existing");
                            node.put("error", false);
                            node.put("deleted", true);
                            node.put("message", "already exists");
                            node.put("url", Json.toJson(shortURL));
                            return F.Promise.pure((Result) ok(node));
                        }
                    }).getOrElse(new F.Function<Void, F.Promise<Result>>() {
                        @Override
                        public F.Promise<Result> apply(Void aVoid) throws Throwable {
                            return IdGenerator.nextId().flatMap(new F.Function<Object, F.Promise<Result>>() {
                                @Override
                                public F.Promise<Result> apply(Object o) throws Throwable {
                                    Long val = (Long) o;
                                    final ShortURL shortUrl = new ShortURL(val.toString(), url);
                                    return ShortURL.save(shortUrl).flatMap(new F.Function<OpResult, F.Promise<Result>>() {
                                        @Override
                                        public F.Promise<Result> apply(OpResult status) throws Throwable {
                                            if (status.isSuccess()) {
                                                ObjectNode node = Json.newObject();
                                                node.put("status", "created");
                                                node.put("error", false);
                                                node.put("deleted", true);
                                                node.put("message", status.getMessage());
                                                node.put("url", Json.toJson(shortUrl));
                                                return F.Promise.pure((Result) ok(node));
                                            } else {
                                                ObjectNode node = Json.newObject();
                                                node.put("status", "error");
                                                node.put("error", true);
                                                node.put("deleted", false);
                                                node.put("message", status.getMessage());
                                                return F.Promise.pure((Result) badRequest(node));
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }.apply(null));
                }
            }));
        }
    }
}
