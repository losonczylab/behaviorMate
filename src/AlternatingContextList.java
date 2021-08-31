import processing.data.JSONObject;

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class AlternatingContextList extends BasicContextList {

    /**
     * True if the context is suspended, false otherwise.
     */
    protected boolean suspended;

    /**
     * The color to display the context as in the UI on laps when it is active.
     */
    protected int[] display_color_active;

    /**
     * The color to display the context as on laps when it is suspended.
     */
    protected int[] display_color_suspended;

    /**
     * The context will be suspended every <tt>n_lap</tt> laps.
     */
    protected int n_lap;

    /**
     * Delay toggling the suspend state of the context until <tt>offset_lap</tt>
     * laps have passed.
     */
    protected int offset_lap;

    /**
     * @param context_info JSONObject containing the configuration information
     *                     for this context from the settings file.
     *                     context_info should have the parameter <tt>n_lap</tt>
     *                     set in order to indicate when to turn off. If <tt>n_lap</tt>
     *                     and <tt>offset_lap</tt> are not defined in <tt>context_info</tt>,
     *                     the corresponding class attributes will be set to 2 and 0, respectively.
     * @param track_length the length of the track (in mm).
     * @param comm_id ?
     */
    public AlternatingContextList(JSONObject context_info, float track_length, String comm_id) {
        super(context_info, track_length, comm_id);
        this.suspended = false;
        this.display_color_active = display_color;
        this.display_color_suspended = new int[] {100, 100, 100};
        this.n_lap = context_info.getInt("n_lap", 2);
        this.offset_lap = context_info.getInt("offset_lap", 0);
    }

    public void suspend() {
        this.suspended = true;
        this.status = "suspended";
        this.display_color = this.display_color_suspended;

        if (this.active != -1) {
            this.active = -1;
            this.comm.sendMessage(this.stopString);
        }
    }

    /**
     * Check the state of the list as well as the contexts contained in this
     * and decide if they should be activated or not. Send the start/stop messages
     * as necessary. this method gets called for each cycle of the event loop
     * when a trial is started.
     *
     * @param position   current position along the track
     * @param time       time (in s) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the the tdml file being written for
     *                   this trial. messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           returns true to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {

        // check if the lap count means that the context list should be suspended or unsuspended.
        int adj_lap = lap - offset_lap;
        // Todo: what is the logic behind this if-else block?
        if ((adj_lap % this.n_lap == 0) && suspended) {
            this.suspended = false;
            this.status = "stopped";
            this.display_color = this.display_color_active;
        } else if ((adj_lap % this.n_lap != 0) && !suspended) {
            this.suspended = true;
            this.status = "suspended";
            this.display_color = this.display_color_suspended;

            if (this.active != -1) {
                this.active = -1;
                this.comm.sendMessage(this.stopString);
            }
        }

        if (this.suspended) {
            return false;
        }

        // if the context list is not suspended call the check method for the
        // default ContextList behavior.
        return super.check(position, time, lap, msg_buffer);
    }
}

