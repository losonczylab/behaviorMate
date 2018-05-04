import processing.data.JSONObject;

/**
 * AlternatingContextList class. Disables contexts based on lap count.
 */
public class OperantContextDecorator extends ContextListDecorator {

    protected int last_active;
    protected boolean initial_open;
    protected String activateString;
    protected UdpClient comm;

    public OperantContextDecorator(ContextList context_list,
                                   JSONObject context_info, UdpClient comm) {
        super(context_list);

        this.last_active = -1;
        this.initial_open = context_info.getBoolean("initial_open", false);
        this.comm = comm;
        createMessages();
    }

    protected void createMessages() {
        JSONObject activate_message = new JSONObject();
        activate_message.setJSONObject("contexts", new JSONObject());
        activate_message.getJSONObject("contexts").setString(
            "action", "activate");
        activate_message.getJSONObject("contexts").setString(
            "id", this.context_list.getId());
        this.activateString = activate_message.toString();
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
        boolean started = this.context_list.check(
            position, time, lap, lick_count, msg_buffer);
        if (started && (this.last_active != this.context_list.activeIdx())) {
            this.comm.sendMessage(this.activateString);
            this.last_active = this.context_list.activeIdx();
        }
        return started;
    }
}
