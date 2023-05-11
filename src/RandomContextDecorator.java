import java.util.Random;

import processing.data.JSONObject;

/**
 * ?
 */
public class RandomContextDecorator extends SuspendableContextDecorator {

    /**
     * the index to count to in order to suspend the context.
     */
    protected int n_lap;

    /**
     *
     */
    protected int min_lap;

    /**
     *
     */
    protected int limit_lap;

    private Random random;

    /**
     * ?
     *
     * @param context_list <code>ContextList</code> instance the decorator will wrap.
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. The <tt>seed</tt> property is optional.
     */
    public RandomContextDecorator(ContextList context_list, JSONObject context_info) {
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
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param lick_count ?
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           ?
     */
    public boolean check_suspend(float position, float time, int lap, int lick_count,
                                 JSONObject[] msg_buffer) {

        // check if the lap count means that the context list should be
        // suspended or unsuspended.
        if (lap > this.n_lap) {
            this.n_lap += (this.min_lap + this.random.nextInt(this.limit_lap - this.min_lap));
        } 
        
        return (lap != this.n_lap);
    }
}
