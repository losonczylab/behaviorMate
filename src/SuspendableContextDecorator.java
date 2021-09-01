import java.util.HashMap;
import processing.data.JSONObject;

/**
 * Allows the wrapped <code>ContextList</code> to be suspended.
 */
public abstract class SuspendableContextDecorator extends ContextListDecorator {

    /**
     * Store whether the context is currently active for this lap, or suspended.
     */
    private boolean suspended;

    /**
     * The color of the context on the display when it is suspended.
     */
    protected int[] display_color_suspended;

    /**
     * Constructs a new <code>SuspendableContextDecorator</code> that allows the wrapped ContextList
     * to be suspended.
     *
     * @param context_list The <code>ContextList</code> to be wrapped.
     */
    public SuspendableContextDecorator(ContextList context_list) {
        super(context_list);
        this.suspended = false;

        if (this.context_list.displayColor() != null) {
            this.display_color_suspended = new int[] {100, 100, 100};
        } else {
            this.display_color_suspended = null;
        }
    }

    /**
     * @return An array of 3 integers, representing the red, green, and blue pixels (in the order)
     *         used to display the wrapped ContextList's currently active context.
     */
    public int[] displayColor() {
        if (!this.suspended) {
            return this.context_list.displayColor();
        } else {
            return this.display_color_suspended;
        }
    }

    /**
     * Suspend all contexts.
     */
    public void suspend() {
        this.suspended = true;

        if (this.context_list.isActive()) {
            this.context_list.suspend();
        }
    }

    /**
     *
     * @return <code>true</code> if the wrapped ContextList is suspended, <code>false</code> otherwise.
     */
    public boolean isSuspended() {
        return this.suspended;
    }

    /**
     *
     * @return The string representing the current status of the contexts.
     */
    public String getStatus() {
        if (!this.suspended) {
            return this.context_list.getStatus();
        } else {
            return "suspended";
        }
    }

    /**
     * ?
     *
     * @param position Current position along the track
     * @param time Time (in seconds) since the start of the trial
     * @param lap Current lap number since the start of the trial
     * @param lick_count ?
     * @param msg_buffer A Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    protected boolean check_suspend(float position, float time, int lap, int lick_count,
                                    JSONObject[] msg_buffer) {

        return false;
    }

    /**
     * ?
     *
     * @param position Current position along the track
     * @param time Time (in seconds) since the start of the trial
     * @param lap Current lap number since the start of the trial
     * @param lick_count ?
     * @param sensor_counts ?
     * @param msg_buffer A Java array of type String to buffer and send messages
     *                   to be logged in the .tdml file being written for
     *                   this trial. Messages should be placed in index 0 of the
     *                   message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 HashMap<Integer, Integer> sensor_counts, JSONObject[] msg_buffer) {

        return this.check_suspend(position, time, lap, lick_count, msg_buffer);
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

        if (check_suspend(position, time, lap, lick_count, sensor_counts, msg_buffer)) {
            if (!this.suspended) {
                this.suspended = true;
                if (this.context_list.isActive()) {
                    this.suspend();
                }
            }
        } else {
            this.suspended = false;
        }

        if (this.suspended) {
            return false;
        }

        // If the context list is not suspended call the check method for the default ContextList
        // behavior.
        return this.context_list.check(position, time, lap, lick_count,
                                       sensor_counts, msg_buffer);
    }
}