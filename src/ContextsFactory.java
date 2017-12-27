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

        if (class_name.equals( "alternating_context")) {
            return new AlternatingContextDecorator(
                new BasicContextList(context_info, track_length, comm),
                context_info);
        } else if (class_name.equals( "random_context")) {
            return new RandomContextList(context_info,
                track_length, comm);
        } else if (class_name.equals( "timed_alt_context")) {
            return new TimedAltContextList(context_info,
                track_length, comm);
        } else if (class_name.equals( "scheduled_context")) {
            return new ScheduledContextList(context_info,
                track_length, comm);
        } else if (class_name.equals("vr")) {
            return new VrContextList(context_info, track_length);
        } else if (class_name.equals("vr_cues")) {
            return new VrCueContextList(context_info, track_length);
        } else if (class_name.equals("salience")) {
            return new SalienceContextList(tc, display, context_info,
                track_length, comm);
        } else {
            cl = new BasicContextList(context_info, track_length, comm);
        }

        if (!context_info.isNull("decorators")) {
            JSONArray decorators = context_info.getJSONArray("decorators");
            for (int i=0; i < decorators.size(); i++) {
                JSONObject decorator = decorators.getJSONObject(i);
                String decorator_class = decorator.getString("class", "");
                if (decorator_class.equals("alternating_context")) {
                    cl = new AlternatingContextDecorator(cl, decorator);
                } else if (decorator_class.equals("running_context")) {
                    cl = new RunningContextDecorator(cl, decorator, track_length);
                }
            }
        }

        return cl;
    }
}
