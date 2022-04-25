import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class GainModifiedContextList extends BasicContextList {
    protected TreadmillController tc;

    protected float position_scale;
    protected ArrayList<Float> position_scale_list;
    protected float position_scale_mod;
    protected boolean variable_scale;
    protected int current_lap;

    public GainModifiedContextList(
            TreadmillController tc, JSONObject context_info,
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

    public void sendCreateMessages() { }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        return true;
    }

    public boolean check(float position, float time, int lap,
                         JSONObject[] msg_buffer) {

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

    public void suspend() {
        this.active = -1;
        this.status = "stopped";
        this.tc.setPositionScale(this.position_scale);
        this.log_json.getJSONObject("context")
                     .setString("action", "stop");
        this.tc.writeLog(this.log_json);
    }

    public void stop() {
        this.active = -1;
        this.status = "stopped";
        this.tc.setPositionScale(this.position_scale);
        this.log_json.getJSONObject("context")
                     .setString("action", "stop");
        this.tc.writeLog(this.log_json);
    }

    public void sendMessage(String message) { }
}


