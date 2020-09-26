import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class GainModifiedContextList extends BasicContextList {
    protected TreadmillController tc;

    protected float position_scale;
    protected float position_scale_mod;

    public GainModifiedContextList(
            TreadmillController tc, JSONObject context_info,
            float track_length) throws Exception {
        super(context_info, track_length, null);

        this.tc = tc;

        position_scale = tc.getPositionScale();
        position_scale_mod = context_info.getFloat("position_scale");
    }

    public void sendCreateMessages() { }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        return true;
    }

    public boolean check(float position, float time, int lap,
                         JSONObject[] msg_buffer) {

        boolean inZone = false;
        int i = 0;

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
                this.status = "started";
                this.tc.setPositionScale(this.position_scale_mod);

                this.log_json.getJSONObject("context")
                             .setString("action", "start");
                msg_buffer[0] = this.log_json;
            }
        }

        return (this.active != -1);
    }

    public void suspend() {
        this.active = -1;
        this.status = "stopped";
    }

    public void stop() {
        this.active = -1;
        this.status = "stopped";
    }

    public void sendMessage(String message) { }
}


