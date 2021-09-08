import java.util.HashMap;
import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ?
 */
public class TimedContextDecorator extends SuspendableContextDecorator {

    /**
     * ?
     */
    private float[] times;

    /**
     * ?
     */
    private float[] times_original;

    /**
     * ?
     */
    private int time_idx;

    /**
     * ?
     */
    private int zero_lap;

    /**
     * ?
     */
    private int actual_lap;

    /**
     * ?
     */
    private boolean adjust_zero_lap;

    /**
     * ?
     */
    private float repeat;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The following JSON literal should be defined
     *                     in the settings file. The property key: <datatype, value> means that the key
     *                     is optional and will default to value if not provided and should be of type
     *                     datatype if provided.
     *
     * {
     * 	    "times": [ [<float>, <float>], [<float>, <float>], ... , [<float>, <float>] ],
     * 	    "repeat": <float, -1>,
     * 	    "no_display": <boolean, false>,
     * 	    "adjust_zero_lap": <boolean, true>
     * }
     */
    public TimedContextDecorator(ContextList context_list, JSONObject context_info) {
        super(context_list);

        if (context_info.isNull("times")) {
            this.time_idx = -1;
            this.times = null;
            this.times_original = null;
        } else {
            JSONArray times_array = context_info.getJSONArray("times");

            this.times = new float[2*times_array.size()];
            for (int i = 0; i < times_array.size(); i++) {
                JSONArray start_stop = times_array.getJSONArray(i);
                this.times[2*i] = start_stop.getFloat(0);
                this.times[2*i+1] = start_stop.getFloat(1);
            }

            this.time_idx = 0;
            this.times_original = this.times.clone();
        }

        this.repeat = context_info.getFloat("repeat", -1);

        if (context_info.getBoolean("no_display", false)) {
            this.display_color_suspended = null;
        }

        this.adjust_zero_lap = context_info.getBoolean("adjust_zero_lap", true);
        this.zero_lap = 0;
    }

    /**
     * Resets the wrapped <code>ContextList</code>.
     */
    public void reset() {
        super.reset();
    }

    /**
     * ?
     */
    public void end() {
        if (this.times != null) {
            this.time_idx = 0;

            if (this.repeat != -1) {
                this.times = this.times_original.clone();
            }
        }

        super.end();
    }

    /**
     * ?
     *
     * @param position Current position along the track
     * @param time Time (in seconds) since the start of the trial
     * @param lap Current lap number since the start of the trial
     * @param lick_count ?
     * @param sensor_counts ?
     * @param msg_buffer A Java array of type String to buffer and send messages to be logged in the
     *                   .tdml file being written for this trial. Messages should be placed in index
     *                   0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer) {

        if (this.time_idx != -1) {
            if (this.time_idx >= this.times.length) {
                if (this.repeat != -1) {
                    for (int i=0; i < this.times.length; i++) {
                        this.times[i] = this.times[i] + this.repeat;
                    }
                    this.time_idx = 0;
                } else {
                    this.time_idx = -1;
                }
            } else if (time >= this.times[this.time_idx]) {
                this.time_idx++;
            }

            if ((this.time_idx%2 == 0) || (this.time_idx == -1)) {
                if (this.adjust_zero_lap) {
                    this.zero_lap = this.actual_lap;
                }

                if (!this.isSuspended()) {
                   this.reset();
                }
                return true;
            }
        } else {
            if (this.adjust_zero_lap) {
                this.zero_lap = this.actual_lap;
            }

            return true;
        }

        return false;
    }

    /**
     * Check the state of the list as well as the contexts contained in this
     * and decide if they should be activated or not. Send the start/stop messages
     * as necessary. this method gets called for each cycle of the event loop
     * when a trial is started.
     *
     * @param position current position along the track
     * @param time time (in seconds) since the start of the trial
     * @param lap current lap number since the start of the trial
     * @param lick_count ?
     * @param sensor_counts ?
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check(float position, float time, int lap, int lick_count,
                         HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer) {

        this.actual_lap = lap;
        return super.check(position, time, lap-this.zero_lap, lick_count, sensor_counts, msg_buffer);
    }
}
