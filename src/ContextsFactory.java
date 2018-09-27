import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ContextsFactory class. Entirely static class used to create a context list
 * based on the "class" attribute in the setting.json file.
 */
public final class ContextsFactory {

    /**
     * Creates a ContextList based on the "class" field in context_info field.
     *
     * @param tc           TreadmillController running the experiment
     * @param display      display object which controlls the UI
     * @param context_info json object containing the configureation information
     *                     for this context from the settings.json file
     * @param track_length the length of the track (in mm).
     * @param comm         client to post messages which configure as well as
     *                     starts and stop the context
     *
     * @return returns the ContextList matching the parameters specified in
     *         context_info.
     */

    public static ContextList Create(TreadmillController tc, Display display,
            JSONObject context_info, float track_length, UdpClient comm,
            String class_name) throws Exception {
        ContextList cl;
        JSONArray decorators = null;

        context_info = tc.parseJSONObject(context_info.toString());
        if (!context_info.isNull("decorators")) {
            decorators = context_info.getJSONArray("decorators");
            context_info.remove("decorators");
        }

        if (class_name.equals( "alternating_context")) {
            cl = new AlternatingContextDecorator(
                new BasicContextList(
                    context_info, track_length, "beahvior_controller"),
                context_info);
        } else if (class_name.equals( "random_context")) {
            cl = new RandomContextDecorator(
                new BasicContextList(
                    context_info, track_length, "behavior_controller"),
                context_info);
        } else if (class_name.equals( "timed_alt_context")) {
            cl = new TimedContextDecorator(
                new AlternatingContextDecorator(
                    new BasicContextList(
                        context_info, track_length, "behavior_controller"),
                    context_info), context_info);
        } else if (class_name.equals( "scheduled_context")) {
            cl = new ScheduledContextDecorator(
                new BasicContextList(
                    context_info, track_length, "behavior_controller"),
                context_info);
        } else if (class_name.equals("vr")) {
            cl = new VrContextList(context_info, track_length);
        } else if (class_name.equals("vr2")) {
            cl = new VrContextList2(context_info, track_length);
        } else if (class_name.equals("vr_cue2")) {
            cl = new VrCueContextList2(context_info, track_length);
        } else if (class_name.equals("vr_cues")) {
            return new VrCueContextList(context_info, track_length);
        } else if (class_name.equals("salience")) {
            cl = new SalienceContextList(tc, display, context_info,
                track_length, "behavior_controller");
        } else {
            if (context_info.getString("type", "").equals("operant") && false) {
                // this is so as not to confuse the arduino
                /*boolean initial_open = false;
                if (!context_info.isNull("initial_open")) {
                    initial_open = context_info.getBoolean("initial_open");
                    context_info.remove("initial_open");
                }
                cl = new OperantContextList(
                    context_info, track_length, initial_open, comm);
                */
            } else {
                cl = new BasicContextList(context_info, track_length, "behavior_controller");
            }
        }

        if (decorators != null) {
            JSONObject timed_context = null;
            for (int i=0; i < decorators.size(); i++) {
                JSONObject decorator = decorators.getJSONObject(i);
                String decorator_class = decorator.getString("class", "");
                if (decorator_class.equals("alternating_context")) {
                    cl = new AlternatingContextDecorator(cl, decorator);
                } else if (decorator_class.equals("running_context")) {
                    cl = new RunningContextDecorator(cl, decorator,
                                                     track_length);
                } else if (decorator_class.equals("scheduled_context")) {
                    cl = new ScheduledContextDecorator(cl, decorator);
                } else if (decorator_class.equals("timed_context")) {
                    timed_context = decorator;
                } else if (decorator_class.equals("random_context")) {
                    cl = new RandomContextDecorator(cl, decorator);
                } else if (decorator_class.equals("lickstart_context")) {
                    cl = new LickStartContextDecorator(cl);
                } else {
                    throw new IllegalArgumentException(
                        "Decorator " + decorator_class + " not found");
                }
            }

            if (timed_context != null) {
                cl = new TimedContextDecorator(cl, timed_context);
            }
        }

        return cl;
    }
}
