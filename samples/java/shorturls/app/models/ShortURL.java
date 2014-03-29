package models;

import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import org.reactivecouchbase.client.OpResult;
import org.reactivecouchbase.play.java.Couchbase;
import org.reactivecouchbase.play.java.CouchbaseBucket;
import play.data.Form;
import play.libs.F;

import java.util.Collection;

import static play.data.Form.form;

public class ShortURL {

    public static CouchbaseBucket bucket = Couchbase.bucket("default");

    public String id;
    public String originalUrl;
    public String t = "shorturl";

    public ShortURL() {}

    public ShortURL(String id, String originalUrl) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.t = "shorturl";
    }

    public static class URLValue {
        public String url;
    }

    public static Form<URLValue> form = form(URLValue.class);

    public static F.Promise<ShortURL> findById(String id) {
        return bucket.get(id, ShortURL.class);
    }

    public static F.Promise<Collection<ShortURL>> findAll() {
        return bucket.find("shorturls", "by_url", new Query().setIncludeDocs(true).setStale(Stale.FALSE), ShortURL.class);
    }

    public static F.Promise<F.Option<ShortURL>> findByURL(String url) {
        Query query = new Query()
                .setLimit(1)
                .setIncludeDocs(true)
                .setStale(Stale.FALSE)
                .setRangeStart(ComplexKey.of(url))
                .setRangeEnd(ComplexKey.of(url + "\uefff"));
        return bucket.find("shorturls", "by_url", query, ShortURL.class).map(new F.Function<Collection<ShortURL>, F.Option<ShortURL>>() {
            @Override
            public F.Option<ShortURL> apply(Collection<ShortURL> shortURLs) throws Throwable {
                if (shortURLs.isEmpty()) {
                    return F.Option.None();
                }
                return F.Option.Some(shortURLs.iterator().next());
            }
        });
    }

    public static F.Promise<OpResult> save(ShortURL url) {
        return bucket.set(url.id, url);
    }

    public static F.Promise<OpResult> remove(String id) {
        return bucket.delete(id);
    }

    public static F.Promise<OpResult> remove(ShortURL url) {
        return bucket.delete(url.id);
    }
}

