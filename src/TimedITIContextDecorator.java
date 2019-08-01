import java.util.Random;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ScheduledContextList class. Disables contexts based on lap count.
 */
public class TimedITIContextDecorator extends SuspendableContextDecorator {

    protected float next_start;

    protected int start_lap;

    protected float iti_time;

    protected int iti_time_min;

    protected int iti_time_max;

    private Random random;

    protected TreadmillController tc;

    protected boolean random_iti;

    public TimedITIContextDecorator(
            TreadmillController tc, ContextList context_list,
            JSONObject context_info) {
        super(context_list);

        this.tc = tc;
        this.random_iti = context_info.getBoolean("random_iti", false);

        if (this.random_iti) {
            this.iti_time_min = context_info.getInt("iti_time_min");
            this.iti_time_max = context_info.getInt("iti_time_max");
        } else {
            this.iti_time = context_info.getFloat("iti_time");
        }

        if (context_info.getBoolean("no_display", false)) {
            this.display_color_suspended = null;
        }

        this.next_start = 0;
        this.start_lap = 0;
    }


    public String getStatus() {
        if (!this.isSuspended()) {
            return this.context_list.getStatus();
        } else {
            return "Next Trial: " + this.next_start + "s";
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
    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, String[] msg_buffer) {
        if (this.isSuspended()) {
            if (time > this.next_start) {
                this.start_lap = lap;
                tc.setLapLock(false);
                return false;
            }
        } else if (lap > this.start_lap) {
            if (this.random_iti) {
                this.next_start = time + random.nextInt(
                    (this.iti_time_max - this.iti_time_min + 1)) + this.iti_time_min;
            } else {
                this.next_start = time + this.iti_time;
            }

            tc.setLapLock(true);
            return true;
        }

        return this.isSuspended();
    }

    public void end() {
        this.next_start = 0;
        this.start_lap = 0;
        tc.setLapLock(false);
    }
}
