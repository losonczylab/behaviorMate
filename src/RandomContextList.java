import java.util.Random;

import processing.data.JSONObject;

/**
 * ?
 */
public class RandomContextList extends BasicContextList {

    /**
     * <code>True</code> if the context is suspended, <code>false</code> otherwise.
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
     * the index to count to in order to suspend the context.
     */
    protected int n_lap;

    /**
     * ?
     */
    protected int min_lap;

    /**
     * ?
     */
    protected int limit_lap;

    private Random random;

    /**
     * @param context_info JSONObject containing the configuration information for this context from
     *                     the settings file. <tt>context_info</tt> should have the parameter
     *                     <tt>n_lap</tt> set in order to indicate when to turn off. If
     *                     <tt>n_lap</tt> and <tt>offset_lap</tt> are not defined in
     *                     <tt>context_info</tt>, the corresponding class attributes will be set to
     *                     2 and 0, respectively.
     * @param track_length The length of the track (in mm).
     * @param comm_id ?
     */
    public RandomContextList(JSONObject context_info, float track_length, String comm_id) {
        super(context_info, track_length, comm_id);
        this.display_color_active = display_color;
        this.display_color_suspended = new int[] {100, 100, 100};
        suspend();

        if (!context_info.isNull("seed")) {
            this.random = new Random(context_info.getLong("seed"));
        } else {
            this.random = new Random();
        }

        this.min_lap = context_info.getInt("min_lap", 1);
        this.limit_lap = context_info.getInt("limit_lap", 2);
        this.n_lap = (int) random(this.min_lap, this.limit_lap);
    }

    /**
     * Suspend the contexts wrapped by this <code>RandomContextList</code> and send a stop
     * message to the arduino.
     */
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
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           <code>true</code> to indicate that the trial has started. Note: all messages
     *                   to the behavior comm are sent from within this method returning true or false
     *                   indicates the state of the context, but does not actually influence the
     *                   connected arduinos or UI.
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {

        // check if the lap count means that the context list should be
        // suspended or unsuspended.
        if ((lap == this.n_lap) && suspended) {
            this.suspended = false;
            this.status = "stopped";
            this.display_color = this.display_color_active;
        } else if ((lap > this.n_lap) && !suspended) {
            this.suspended = true;
            this.status = "suspended";
            this.display_color = this.display_color_suspended;

            if (this.active != -1) {
                this.active = -1;
                this.comm.sendMessage(this.stopString);
            }

            this.n_lap += (int) random(this.min_lap, this.limit_lap);
        }

        if (this.suspended) {
            return false;
        }

        // if the context list is not suspended call the check method for the
        // default ContextList behavior.
        return super.check(position, time, lap, msg_buffer);
    }
}
