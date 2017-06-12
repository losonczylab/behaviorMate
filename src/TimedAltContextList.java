import processing.data.JSONObject;
import processing.data.JSONArray;


//TODO: update comments

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class TimedAltContextList extends AlternatingContextList {

    private int[] times;

    private int time_idx;

    private int zero_lap;

    /**
     * Constructor.
     *
     * @param display      display object which controlls the UI
     * @param context_info json object containing the configureation information
     *                     for this context from the settings.json file.
     *                     context_info should have the parameter <tt>n_lap</tt>
     *                     set in order to indicate when to turn off. this value
     *                     defaults to 2 (meaning the context will be active on
     *                     alternating laps)
     * @param track_length the length of the track (in mm).
     * @param comm         client to post messages which configure as well as
     *                     starts and stop the context
     */
    public TimedAltContextList(Display display, JSONObject context_info,
            float track_length, UdpClient comm) {
        super(display, context_info, track_length, comm);
        
        if (context_info.isNull("times")) {
            this.time_idx = -1;
            this.times = null;
        } else {
            JSONArray times_array = context_info.getJSONArray("times");

            this.times = new int[2*times_array.size()];
            for (int i = 0; i < times_array.size(); i++) {
                JSONArray start_stop = times_array.getJSONArray(i);
                this.times[2*i] = start_stop.getInt(0);
                this.times[2*i+1] = start_stop.getInt(1);
            }
            this.time_idx = 0;
        }
    }

    protected void reset() {
        if (this.times != null) {
            this.time_idx = 0;
        }
        super.reset();
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
        
        if (this.time_idx != -1) {
            if (this.time_idx >= this.times.length) {
                this.time_idx = -1;
                System.out.println("here!");
            } else if (time >= this.times[this.time_idx]) {
                this.time_idx++;
            }

            if ((this.time_idx%2 == 0) || (this.time_idx == -1)) {
                if (!this.suspended) {
                    this.suspend();
                }

                return false;
            }
        } else {
            return false;
        }


        // if the context list is not suspended call the check method for the
        // default AlternatingContextList behavior.
        return super.check(position, time, lap, msg_buffer);
    }
}
