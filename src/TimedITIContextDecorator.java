import java.util.Random;

import java.util.ArrayList;
import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ?
 */
public class TimedITIContextDecorator extends SuspendableContextDecorator {

    /**
     * ?
     */
    protected float next_start;

    /**
     * ?
     */
    protected int start_lap;

    /**
     * ?
     */
    protected float iti_time;

    /**
     * ?
     */
    protected float iti_time_min;

    /**
     * ?
     */
    protected float iti_time_max;

    /**
     * ?
     */
    private Random random;

    /**
     * ?
     */
    protected TreadmillController tc;

    /**
     * ?
     */
    protected boolean random_iti;

    protected ArrayList<Integer> long_delay_laps;

    protected float long_delay;

    /**
     * ?
     *
     * @param tc ?
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The following JSON literal should be defined
     *                     in the settings file. The property key: <datatype, value> means that the key
     *                     is optional and will default to value if not provided and should be of type
     *                     datatype if provided.
     */
    public TimedITIContextDecorator(TreadmillController tc,
                                    ContextList context_list,
                                    JSONObject context_info) {
        super(context_list);

        this.tc = tc;
        this.random_iti = context_info.getBoolean("random_iti", false);

        if (this.random_iti) {
            this.random = new Random();
            this.iti_time_min = context_info.getInt("iti_time_min");
            this.iti_time_max = context_info.getInt("iti_time_max");
        } else {
            this.iti_time = context_info.getFloat("iti_time");
        }

        if (context_info.getBoolean("no_display", false)) {
            this.display_color_suspended = null;
        }

        this.long_delay_laps = new ArrayList<Integer>();
        if (!context_info.isNull("long_delay_laps")) {
            for (int i = 0;
                 i < context_info.getJSONArray("long_delay_laps").size();
                 i++) {
                this.long_delay_laps.add(
                    context_info.getJSONArray("long_delay_laps").getInt(i));
            }
            this.long_delay = context_info.getFloat("long_delay");
        }

        this.next_start = 0;
        this.start_lap = 0;
    }

    /**
     *
     * @return The string representing the current status of the contexts.
     */
    public String getStatus() {
        if (!this.isSuspended()) {
            return this.context_list.getStatus();
        } else {
            return "Next Trial: " + this.next_start + "s";
        }
    }

    /**
     * ?
     *
     * @param position Current position along the track
     * @param time Time (in seconds) since the start of the trial
     * @param lap Current lap number since the start of the trial
     * @param lick_count ?
     * @param msg_buffer A Java array of type String to buffer and send messages to be logged in the
     *                   .tdml file being written for this trial. Messages should be placed in index
     *                   0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {
        if (this.isSuspended()) {
            if (time > this.next_start) {
                this.start_lap = lap;
                tc.setLapLock(false);
                return false;
            }
        } else if (lap > this.start_lap) {
            if (this.long_delay_laps.contains(this.start_lap)) {
                this.next_start = time + this.long_delay;
            } else if (this.random_iti) {
                this.next_start = time + this.random.nextFloat() *
                    (this.iti_time_max - this.iti_time_min) + this.iti_time_min;
            } else {
                this.next_start = time + this.iti_time;
            }

            tc.setLapLock(true);
            return true;
        }

        return this.isSuspended();
    }

    /**
     * Resets the state of the contexts. Contexts which have been triggered are
     * reactivated and allowed to be triggered again. If <code>shuffle_contexts</code>
     * is <code>true</code>, the contexts will be shuffled.
     */
    public void end() {
        this.next_start = 0;
        this.start_lap = 0;
        tc.setLapLock(false);
        this.context_list.end();
    }
}
