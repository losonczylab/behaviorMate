import java.util.ArrayList;

import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * ScheduledContextList class. Disables contexts based on lap count.
 */
public class ScheduledContextDecorator extends SuspendableContextDecorator {

    protected ArrayList<Integer> lap_list;

    protected int repeat;

    public ScheduledContextDecorator(ContextList context_list,
                                     JSONObject context_info) {
        super(context_list);

        this.lap_list = new ArrayList<Integer>();
        JSONArray lap_array = null;
        if (!context_info.isNull("lap_list")) {
            lap_array = context_info.getJSONArray("lap_list");
            this.lap_list = new ArrayList<Integer>();
            for (int i=0; i < lap_array.size(); i++) {
                this.lap_list.add(lap_array.getInt(i));
            }
        }

        this.repeat = context_info.getInt("repeat", 0);
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
                                 int lick_count, String[] msg_buffer) {
        if (this.repeat == 0) {
            return (this.lap_list.indexOf(lap) == -1);
        }

        return (this.lap_list.indexOf(lap%this.repeat) == -1);
    }
}
