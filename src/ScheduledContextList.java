import java.util.ArrayList;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ScheduledContextList class. Disables contexts based on lap count.
 */
public class ScheduledContextList extends ContextList {

    /**
     * store weither the context is currently active for this lap, or suspended
     */
    protected boolean suspended;

    /**
     * the color to display the context as in the UI on laps when it is not
     * suspended.
     */
    protected int display_color_active;

    /**
     * the color to display the context as on laps when it is suspended.
     */
    protected int display_color_suspended;

    /**
     * the index to count to in order to suspend the context.
     */

    protected ArrayList<Integer> lap_list;

    /**
     * Constructor.
     *
     * @param display      display object which controlls the UI
     * @param context_info json object containing the configureation information
     *                     for this context from the settings.json file.
     *                     context_info should have the parameter
     *                     <tt>lap_list</tt> whih is a list of integers
     *                     corresponding to the laps in which this context is
     *                     not suspended.
     * @param track_length the length of the track (in mm).
     * @param comm         client to post messages which configure as well as
     *                     starts and stop the context
     */
    public ScheduledContextList(Display display, JSONObject context_info,
            float track_length, UdpClient comm) {
        super(display, context_info, track_length, comm);
        this.suspended = false;
        this.display_color_active = display_color;
        this.display_color_suspended = color(100, 100, 100);

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


    protected void suspend() {
        this.suspended = true;
        this.status = "suspended";
        this.display_color = this.display_color_suspended;

        if (this.active != -1) {
            this.active = -1;
            this.comm.sendMessage(this.stopString);
        }
    }

    /**
     * Check the state of the list as well as the  contexts contained in this
     * and decide if they should be actived or not. Send the start/stop messages
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
    public boolean check(float position, float time, int lap,
            String[] msg_buffer) {

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
