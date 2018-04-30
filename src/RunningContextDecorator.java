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

    protected float min_dt;

    protected float min_dy;

    public RunningContextDecorator(ContextList context_list,
                                   JSONObject context_info,
                                   float track_length) {
        super(context_list);
        this.display_color_suspended = new int[] {100, 100, 100};

        this.threshold = context_info.getFloat("threshold", 0.0f);
        this.max_dt = context_info.getFloat("max_dt", 0.1f);
        this.min_dt = context_info.getFloat("min_dt", 0.2f);
        this.min_dy = context_info.getFloat("min_dy", 5);

        //this.prev_time = -this.max_dt;
        this.prev_time = 0;
        this.prev_position = 0;
        this.prev_lap = 0;
        this.track_length = track_length;
        this.suspend();
    }

    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, String[] msg_buffer) {

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
        if (dy < this.min_dy) {
            return true;
        }

        /*
        float velocity = Math.abs(dy)/dt;
        if (velocity > this.threshold) {
            return false;
            }
        } else if (!this.isSuspended()) {
            return false;
        }*/

        return false;
    }

    public void stop(float time, String[] msg_buffer) {
        this.prev_lap = 0;
        this.prev_position = 0;
        this.prev_time = 0;
        this.suspend();
    }
}
