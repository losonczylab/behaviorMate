import processing.data.JSONObject;

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class LickStartContextDecorator extends ContextListDecorator {

    private int prev_lickcount;

    public LickStartContextDecorator(ContextList context_list) {
        super(context_list);
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
            String[] msg_buffer) {

        if (!this.context_list.isActive()) {
            if (lick_count != prev_lickcount) {
                prev_lickcount = lick_count;

                return this.context_list.check(position, time, lap, lick_count,
                                               msg_buffer);
            } else {
                return false;
            }
        }

        prev_lickcount = lick_count;
        return this.context_list.check(position, time, lap, lick_count,
                                       msg_buffer);
    }
}
