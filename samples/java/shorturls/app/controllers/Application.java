package controllers;

import models.ShortURL;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render());
    }

    public static Result goTo(String id) {
        return async(
            ShortURL.findById(id).map(new F.Function<ShortURL, Result>() {
                @Override
                public Result apply(ShortURL shortURL) throws Throwable {
                    if (shortURL == null) {
                        return notFound();
                    }
                    return redirect(shortURL.originalUrl);
                }
            })
        );
    }
}