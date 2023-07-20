import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

/**
 * ?
 */
public class GainModifiedContextList extends BasicContextList {
    /**
     * ?
     */
    protected TreadmillController tc;

    /**
     * ?
     */
    protected float position_scale;
    protected ArrayList<Float> position_scale_list;

    /**
     * ?
     */
    protected float position_scale_mod;
    protected boolean variable_scale;
    protected int current_lap;

    /**
     * ?
     *
     * @param tc ?
     * @param context_info JSONObject containing the configuration information for this context
     *                     from the settings file. <tt>context_info</tt> should have the parameter
     *                     <tt>position_scale</tt> set in order to ?.
     * @param track_length The length of the track (in mm).
     * @throws Exception
     */
    public GainModifiedContextList(TreadmillController tc, JSONObject context_info,
                                   float track_length) throws Exception {
        super(context_info, track_length, null);

        this.tc = tc;
        position_scale_list = null;
        variable_scale = false;
        position_scale = tc.getPositionScale();
        current_lap = -1;

        if (!context_info.isNull("position_scale")) {
            try {
                JSONArray scale_values = context_info.getJSONArray("position_scale");
                position_scale_list = new ArrayList<Float>(scale_values.size());
                for (int i = 0; i < scale_values.size(); i++) {
                    position_scale_list.add(scale_values.getFloat(i));
                }
                variable_scale = true;
            } catch (RuntimeException e) {
                position_scale_mod = context_info.getFloat("position_scale");
            }
        } else if (!context_info.isNull("gain")){
            try {
                JSONArray gain_values = context_info.getJSONArray("gain");
                position_scale_list = new ArrayList<Float>(gain_values.size());
                for (int i = 0; i < gain_values.size(); i++) {
                    position_scale_list.add(
                        position_scale / gain_values.getFloat(i));
                }
                variable_scale = true;
            } catch (RuntimeException e) {
                position_scale_mod = position_scale / context_info.getFloat("gain");
            }
        }
    }

    /**
     * Placeholder
     */
    public void sendCreateMessages() { }

    /**
     * Placeholder
     *
     * @param comms Channel to post messages for configuring, starting or stopping contexts.
     * @return
     */
    public boolean setupComms(ArrayList<UdpClient> comms) {
        return true;
    }

    /**
     * Check the state of the list as well as the contexts contained in this and decide if they
     * should be activated or not. Send the start/stop messages as necessary. this method gets
     * called for each cycle of the event loop when a trial is started.
     *
     * @param position   Current position along the track in millimeters.
     * @param time       Time (in s) since the start of the trial.
     * @param lap        Current lap number since the start of the trial.
     * @param msg_buffer A Java <code>String</code> array of type to buffer and send messages to be
     *                   logged in the .tdml file being written for this trial. messages should
     *                   be placed at index 0 of the message buffer and must be JSON-formatted strings.
     * @return           <code>true</code> to indicate that the trial has started. Note: all messages
     *                   to the behavior comm are sent from within this method returning true or false
     *                   indicates the state of the context, but does not actually influence the
     *                   connected arduinos or UI.
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {

        boolean inZone = false;
        int i = 0;

        if (variable_scale && (lap != current_lap)) {
            this.position_scale_mod = this.position_scale_list.get(
                lap % position_scale_list.size());
            current_lap = lap;
        }

        // This loop checks to see if any of the individual contexts are
        // triggered to be active both in space and time
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time, lap)) {
                inZone = true;
                break;
            }
        }

        // Decide if the context defined by this ContextList needs to swtich
        // state and send the message to the UdpClient accordingly
        if (!waiting) {
            if ((!inZone) && (this.active != -1)) {
                this.status = "stopped";
                this.active = -1;

                this.log_json.getJSONObject("context")
                             .setString("action", "stop");
                msg_buffer[0] = this.log_json;
                this.tc.setPositionScale(this.position_scale);
            } else if((inZone) && (this.active != i)) {
                this.active = i;
                this.status = "on";
                this.tc.setPositionScale(this.position_scale_mod);

                this.log_json.getJSONObject("context")
                             .setString("action", "start");
                this.log_json.getJSONObject("context")
                             .setFloat("position_scale", this.position_scale_mod);
                msg_buffer[0] = this.log_json;
            }
        }

        return (this.active != -1);
    }

    /**
     * Suspend all contexts.
     * Todo: why doesn't this send a message?
     */
    public void suspend() {
        this.active = -1;
        this.status = "stopped";
        this.tc.setPositionScale(this.position_scale);
        this.log_json.getJSONObject("context")
                     .setString("action", "stop");
        this.tc.writeLog(this.log_json);
    }

    /**
     * Todo: seems to do the same thing as suspend.
     * Stop this context. Called at the end of trials to ensure that the context is shut off.
     */
    public void stop() {
        this.active = -1;
        this.status = "stopped";
        this.tc.setPositionScale(this.position_scale);
        this.log_json.getJSONObject("context")
                     .setString("action", "stop");
        this.tc.writeLog(this.log_json);
    }

    // Todo: why is this unimplemented?
    public void sendMessage(String message) { }
}


