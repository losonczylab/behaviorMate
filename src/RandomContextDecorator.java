import java.util.Random;

import processing.data.JSONObject;

/**
 * RandomContextList class. Disables contexts based on lap count.
 */
public class RandomContextDecorator extends SuspendableContextDecorator {

    /**
     * the index to count to in order to suspend the context.
     */
    protected int n_lap;
    protected int min_lap;
    protected int limit_lap;

    private Random random;

    /**
     * Constructor.
     *
     * @param context_info json object containing the configureation information
     *                     for this context from the settings.json file.
     *                     context_info should have the parameter <tt>n_lap</tt>
     *                     set in order to indicate when to turn off. this value
     *                     defaults to 2 (meaning the context will be active on
     *                     alternating laps)
     * @param track_length the length of the track (in mm).
     * @param comm         client to post messages which configure as well as
     *                     starts and stop the context
     */
    public RandomContextDecorator(ContextList context_list,
                                  JSONObject context_info) {
        super(context_list);
        if (!context_info.isNull("seed")) {
            this.random = new Random(context_info.getLong("seed"));
        } else {
            this.random = new Random();
        }

        this.min_lap = context_info.getInt("min_lap", 1);
        this.limit_lap = context_info.getInt("limit_lap", 2);
        this.n_lap = this.min_lap + this.random.nextInt(this.limit_lap-this.min_lap);
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

        // check if the lap count means that the context list should be
        // suspended or unsuspended.
        if (lap > this.n_lap) {
            this.n_lap += (this.min_lap + this.random.nextInt(
                this.limit_lap-this.min_lap));
        } 
        
        return (lap != this.n_lap);
    }
}
