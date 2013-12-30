import controllers.ShortURLController;
import play.GlobalSettings;

public class Global extends GlobalSettings {

    public <A> A getControllerInstance(java.lang.Class<A> aClass) throws java.lang.Exception {
        if (aClass.equals(ShortURLController.class)) {
            return (A) new ShortURLController();
        }
        throw new RuntimeException("Cannot instanciate " + aClass.getName());
    }
}
