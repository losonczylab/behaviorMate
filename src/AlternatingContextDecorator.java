import processing.data.JSONObject;

/**
 * Wraps a BasicContextList. Disables contexts based on lap count.
 * Todo: convert to be compatible with BasicContextList
 */
public class AlternatingContextDecorator extends SuspendableContextDecorator {

    /**
     * The context list will be suspended every <tt>n_lap</tt> laps, otherwise it will be active.
     */
    protected int n_lap;

    /**
     * Delay toggling the suspend state of the context list until <tt>offset_lap</tt>
     * laps have passed.
     */
    protected int offset_lap;

    /**
     * @param context_list ContextList instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information
     *                     for this instance's <code>ContextList</code> from the settings file.
     *                     context_info should have the parameter <tt>n_lap</tt>
     *                     set in order to indicate when to turn off. This value
     *                     defaults to 2, meaning the context will be active on
     *                     alternating laps.
     */
    public AlternatingContextDecorator(ContextList context_list, JSONObject context_info) {
        super(context_list);
        this.n_lap = context_info.getInt("n_lap", 2);
        this.offset_lap = context_info.getInt("offset_lap", 0);
    }


    /**
     * Check if the context list should be suspended based on the current lap.
     *
     * @param position   current position along the track
     * @param time       time (in seconds) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return           <code>true</code> if the context should be suspended, <code>false</code>
     *                   otherwise.
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {

        return !((lap - offset_lap) % n_lap == 0);
    }
}
