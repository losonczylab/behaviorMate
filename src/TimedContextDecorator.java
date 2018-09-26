import processing.data.JSONObject;
import processing.data.JSONArray;


//TODO: update comments

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class TimedContextDecorator extends SuspendableContextDecorator {

    private float[] times;

    private int time_idx;

    private int zero_lap;

    private int actual_lap;

    public TimedContextDecorator(ContextList context_list,
                                 JSONObject context_info) {
        super(context_list);

        if (context_info.isNull("times")) {
            this.time_idx = -1;
            this.times = null;
        } else {
            JSONArray times_array = context_info.getJSONArray("times");

            this.times = new float[2*times_array.size()];
            for (int i = 0; i < times_array.size(); i++) {
                JSONArray start_stop = times_array.getJSONArray(i);
                this.times[2*i] = start_stop.getFloat(0);
                this.times[2*i+1] = start_stop.getFloat(1);
            }
            this.time_idx = 0;
        }

        if (context_info.getBoolean("no_display", false)) {
            this.display_color_suspended = null;
        }
        this.zero_lap = 0;
    }

    public void reset() {
        super.reset();
    }

    public void end() {
        if (this.times != null) {
            this.time_idx = 0;
        }
        super.end();

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
    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, String[] msg_buffer) {

        if (this.time_idx != -1) {
            if (this.time_idx >= this.times.length) {
                this.time_idx = -1;
            } else if (time >= this.times[this.time_idx]) {
                this.time_idx++;
            }

            if ((this.time_idx%2 == 0) || (this.time_idx == -1)) {
                this.zero_lap = this.actual_lap;
                return true;
            }
        } else {
            this.zero_lap = this.actual_lap;
            return true;
        }

        return false;
    }

    public boolean check(float position, float time, int lap, int lick_count,
            String[] msg_buffer) {

        this.actual_lap = lap;
        return super.check(position, time, lap-this.zero_lap,
                                       lick_count, msg_buffer);
    }
}
