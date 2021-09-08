import processing.data.JSONObject;

import java.util.HashMap;

/**
 * ?
 */
public class DelayedContextDecorator extends SuspendableContextDecorator {

    /**
     * ?
     */
    protected float current_time;

    /**
     * ?
     */
    protected float delay;

    /**
     * ?
     */
    protected float start_time;

    /**
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information
     *                     for this instance's <code>ContextList</code> from the settings file.
     *                     context_info should have the parameter <tt>delay</tt> set
     *                     to the amount of time (Todo: in ms?) to delay the start of the
     *                     contexts.
     */
    public DelayedContextDecorator(ContextList context_list, JSONObject context_info) {
        super(context_list);

        this.delay = context_info.getFloat("delay");
        this.start_time = 0;
        this.current_time = 0;
    }

    /**
     * Suspends the ContextList indirectly by causing
     * {@link DelayedContextDecorator#check(float, float, int, int, HashMap, JSONObject[])}
     * to return <code>true</code>.
     */
    public void suspend() {
        this.start_time = this.current_time + this.delay;
    }

    /**
     * Resets the state of the contexts. Contexts which have been triggered are
     * reactivated and allowed to be triggered again. If <code>shuffle_contexts</code>
     * is <code>true</code>, the contexts will be shuffled.
     */
    public void end() {
        this.start_time = 0;
        this.current_time = 0;
        super.end();
    }

    /**
     * Checks if the wrapped ContextList should be suspended at the given time and position.
     *
     * @param position Current position along the track
     * @param time Time (in seconds) since the start of the trial
     * @param lap Current lap number since the start of the trial
     * @param lick_count ?
     * @param msg_buffer A Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return <code>true</code> if the ContextList should be suspended, <code>false</code> otherwise.
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {

//        this.current_time = time;
//        if (time > this.start_time) {
//            return false;
//        }
//        return true;

        return !(current_time > start_time);
    }
}
