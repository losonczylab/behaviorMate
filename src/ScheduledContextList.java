import java.util.ArrayList;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ?
 */
public class ScheduledContextList extends BasicContextList {

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
     * ?
     */
    protected ArrayList<Integer> lap_list;

    /**
     * @param context_info JSONObject containing the configuration information for this context from
     *                     the settings file. <tt>context_info</tt> should have the <tt>lap_list</tt>
     *                     key defined.
     * @param track_length The length of the track (in mm).
     * @param comm_id ?
     */
    public ScheduledContextList(JSONObject context_info, float track_length, String comm_id) {
        super(context_info, track_length, comm_id);
        this.suspended = false;
        this.display_color_active = display_color;
        this.display_color_suspended = new int[] {100, 100, 100};

        JSONArray lap_array = null;
        if (!context_info.isNull("lap_list")) {
            lap_array = context_info.getJSONArray("lap_list");
        }

        if (lap_array != null) {
            this.lap_list = new ArrayList<Integer>();
            for (int i=0; i < lap_array.size(); i++) {
                this.lap_list.add(lap_array.getInt(i));
            }
        }
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
     * Check the state of the contexts contained in this list and send the
     * start/stop messages as necessary. This method gets called for each cycle
     * of the event loop when a trial is started.
     *
     * @param position   current position along the track
     * @param time       time (in s) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the the tdml file being written for
     *                   this trial. messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return           <code>true</code> to indicate that the trial has started.
     *                   Note: all messages to the behavior comm are sent from
     *                   within this method returning true or false indicates
     *                   the state of the context, but does not actually
     *                   influence the connected arduinos or UI.
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {

        // check if the lap count means that the context list should be
        // suspended or unsuspended.
        if ((this.lap_list.indexOf(lap) == -1) && !suspended) {
            this.suspended = true;
            this.status = "suspended";
            this.display_color = this.display_color_suspended;

            if (this.active != -1) {
                this.active = -1;
                this.comm.sendMessage(this.stopString);
            }
        } else if ((this.lap_list.indexOf(lap) != -1) && suspended) {
            this.suspended = false;
            this.status = "stopped";
            this.display_color = this.display_color_active;
        }

        if (this.suspended) {
            return false;
        }

        // if the context list is not suspended call the check method for the
        // default ContextList behavior.
        return super.check(position, time, lap, msg_buffer);
    }
}
