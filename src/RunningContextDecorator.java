import processing.data.JSONObject;

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class RunningContextDecorator extends SuspendableContextDecorator {

    /**
     * the index to count to in order to suspend the context.
     */
    protected float threshold;

    protected float prev_time;

    protected float prev_position;

    protected int prev_lap;

    protected float track_length;

    protected float max_dt;


    public RunningContextDecorator(ContextList context_list,
                                   JSONObject context_info,
                                   float track_length) {
        super(context_list);
        this.display_color_suspended = new int[] {100, 100, 100};

        this.threshold = context_info.getFloat("threshold", 0.0f);
        this.max_dt = context_info.getFloat("max_dt", 0.1f);

        this.prev_time = -this.max_dt;
        this.prev_position = 0;
        this.prev_lap = 0;
        this.track_length = track_length;
    }

    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, String[] msg_buffer) {

        if (lap != this.prev_lap) {
            position += (lap-this.prev_lap)*this.track_length;
            this.prev_lap = lap;
        }

        float dt = time-this.prev_time;
        float velocity = 0;
        velocity = Math.abs(position-this.prev_position)/(dt);

        if ((velocity != 0) || (dt > this.max_dt)) {
            this.prev_position = position;
            this.prev_time = time;

            if (velocity > this.threshold) {
                return false;
            }
        } else if (!this.isSuspended()) {
            return false;
        }

        return true;
    }
}
