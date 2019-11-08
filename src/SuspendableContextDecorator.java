import processing.data.JSONObject;

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public abstract class SuspendableContextDecorator extends ContextListDecorator {

    /**
     * store weither the context is currently active for this lap, or suspended
     */
    private boolean suspended;

    /**
     * the color to display the context as on laps when it is suspended.
     */
    protected int[] display_color_suspended;

    public SuspendableContextDecorator(ContextList context_list) {
        super(context_list);
        this.suspended = false;

        if (this.context_list.displayColor() != null) {
            this.display_color_suspended = new int[] {100, 100, 100};
        } else {
            this.display_color_suspended = null;
        }
    }

    public int[] displayColor() {
        if (!this.suspended) {
            return this.context_list.displayColor();
        } else {
            return this.display_color_suspended;
        }
    }


    public void suspend() {
        this.suspended = true;

        if (this.context_list.isActive()) {
            this.context_list.suspend();
        }
    }

    public boolean isSuspended() {
        return this.suspended;
    }

    public String getStatus() {
        if (!this.suspended) {
            return this.context_list.getStatus();
        } else {
            return "suspended";
        }
    }

    public boolean check_suspend(float position, float time, int lap,
                                 int lick_count, JSONObject[] msg_buffer) {

        return false;
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
    public boolean check(float position, float time, int lap, int lick_count,
                         JSONObject[] msg_buffer) {

        if (check_suspend(position, time, lap, lick_count, msg_buffer)) {
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

        // if the context list is not suspended call the check method for the
        // default ContextList behavior.
        return this.context_list.check(position, time, lap, lick_count,
                                       msg_buffer);
    }
}
