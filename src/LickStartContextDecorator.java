import processing.data.JSONObject;

public class LickStartContextDecorator extends ContextListDecorator {

    private int prev_lickcount;
    protected int last_position;
    protected float entered_time;
    protected float max_time;
    protected int timeInPosition;

    public LickStartContextDecorator(ContextList context_list,
                                     JSONObject context_info) {
        super(context_list);
        this.last_position = -1;
        this.entered_time = -1;
        this.prev_lickcount = 0;

        this.max_time = context_info.getInt("max_time", -1);
    }

    public void reset() {
        this.last_position = -1;
        this.entered_time = -1;
        this.context_list.reset();
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

        boolean inPosition = (this.max_time == -1);
        if (!this.context_list.isActive()) {
            for (int i=0;
                 ((!inPosition) && (i < this.context_list.size()));
                 i++) {
                if (this.context_list.getContext(i).checkPosition(position)) {
                    inPosition = true;
                    if (this.entered_time == -1) {
                        this.context_list.setStatus("no lick");
                        this.entered_time = time;
                    } else if (i == this.last_position) {
                        if (this.entered_time + this.max_time < time) {

                            prev_lickcount = lick_count;
                            this.context_list.setStatus("timed out");
                            return false;
                        }
                    }

                    this.last_position = i;
                    break;
                }
            }

            if (!inPosition) {
                this.entered_time = -1;
                this.context_list.setStatus("stopped");

                prev_lickcount = lick_count;
                return false;
            }

            if (lick_count != prev_lickcount) {
                prev_lickcount = lick_count;

                return this.context_list.check(position, time, lap, lick_count,
                                               msg_buffer);
            } else {

                prev_lickcount = lick_count;
                return false;
            }
        }

        prev_lickcount = lick_count;
        return this.context_list.check(position, time, lap, lick_count,
                                       msg_buffer);
    }
}
