import processing.data.JSONObject;

/**
 * ?
 */
public class RunningContextDecorator extends SuspendableContextDecorator {

    /**
     * ?
     */
    protected float threshold;

    /**
     * ?
     */
    protected float prev_time;

    /**
     * ?
     */
    protected float prev_position;

    /**
     * ?
     */
    protected int prev_lap;

    /**
     * ?
     */
    protected float track_length;

    /**
     * ?
     */
    protected float max_dt;

    /**
     * ?
     */
    protected float min_dt;

    /**
     * ?
     */
    protected float min_dy;

    /**
     * ?
     */
    protected boolean use_abs_dy;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The following properties should be defined in the
     *                     settings file: <tt>threshold</tt>, <tt>max_dt</tt>, <tt>min_dt</tt>,
     *                     <tt>min_dy</tt>, <tt>use_abs_dy</tt>. If they are not defined they will
     *                     default to 0, 0.1, 0.2, 5, and false, respectively.
     * @param track_length The length of the track in millimeters.
     */
    public RunningContextDecorator(ContextList context_list, JSONObject context_info,
                                   float track_length) {
        super(context_list);
        this.display_color_suspended = new int[] {100, 100, 100};

        this.threshold = context_info.getFloat("threshold", 0.0f); // unused
        this.max_dt = context_info.getFloat("max_dt", 0.1f); // unused
        this.min_dt = context_info.getFloat("min_dt", 0.2f);
        this.min_dy = context_info.getFloat("min_dy", 5);
        this.use_abs_dy = context_info.getBoolean("use_abs_dy", false);

        this.prev_time = 0;
        this.prev_position = 0;
        this.prev_lap = 0;
        this.track_length = track_length;
        this.suspend();
    }

    /**
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count ?
     * @param msg_buffer A Java <code>String</code> array to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. Messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {

        if (lap != this.prev_lap) {
            position += (lap-this.prev_lap)*this.track_length;
        }

        float dt = time-this.prev_time;
        if (dt < this.min_dt) {
            return this.isSuspended();
        }
        this.prev_time = time;

        float dy = position - this.prev_position;
        this.prev_position = position;
        this.prev_lap = lap;

        float chk_dy = dy;
        if (this.use_abs_dy) {
            chk_dy = Math.abs(dy);
        }
        if (chk_dy < this.min_dy) {
            return true;
        }

        return false;
    }

    /**
     * Set instance attributes to their defaults and suspend the wrapped <code>ContextList</code>/
     *
     * @param time ?
     * @param msg_buffer ?
     */
    public void stop(float time, JSONObject[] msg_buffer) {
        this.prev_lap = 0;
        this.prev_position = 0;
        this.prev_time = 0;
        this.suspend();
    }
}
