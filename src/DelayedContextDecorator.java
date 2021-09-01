import processing.data.JSONObject;

/**
 *
 */
public class DelayedContextDecorator extends SuspendableContextDecorator {

    protected float current_time;
    protected float delay;
    protected float start_time;

    public DelayedContextDecorator(ContextList context_list,
                                     JSONObject context_info) {
        super(context_list);

        this.delay = context_info.getFloat("delay");
        this.start_time = 0;
        this.current_time = 0;
    }

    public void suspend() {
        this.start_time = this.current_time + this.delay;
    }

    public void end() {
        this.start_time = 0;
        this.current_time = 0;
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
                                 int lick_count, JSONObject[] msg_buffer) {

        this.current_time = time;
        if (time > this.start_time) {
            return false;
        }

        return true;
    }
}
