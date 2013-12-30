package controllers;

import models.ShortURL;

import org.reactivecouchbase.play.java.Couchbase;
import org.reactivecouchbase.play.java.crud.CrudSource;
import org.reactivecouchbase.play.java.crud.CrudSourceController;

public class ShortURLController extends CrudSourceController<ShortURL> {

    private final CrudSource<ShortURL> source = new CrudSource<ShortURL>( Couchbase.bucket("default"), ShortURL.class );

    @Override
    public CrudSource<ShortURL> getSource() {
        return source;
    }

    @Override
    public String defaultDesignDocname() {
        return "shorturls";
    }

    @Override
    public String defaultViewName() {
        return "by_url";
    }
}
