import processing.data.JSONObject;

/**
 * ContextsFactory class. Entirely static class used to create a context list
 * based on the "class" attribute in the setting.json file.
 */
public final class ContextsFactory {

    /**
     * Creates a ContextList based on the "class" field in context_info field.
     *
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

    public static ContextList Create(Display display, JSONObject context_info,
            float track_length, UdpClient comm, String class_name) {
        if (class_name.equals( "alternating_context")) {
            return new AlternatingContextList(display, context_info, track_length, comm);
        } else {
            return new ContextList(display, context_info, track_length, comm);
        }
    }
}
