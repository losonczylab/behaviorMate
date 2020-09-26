import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrCueContextList2 extends BasicContextList {
    private UdpClient[] comms;
    private String[] comm_ids;
    private JSONObject log_json;

    private JSONArray cues;
    private VrContextList vr;
    private boolean displaying;

    public VrCueContextList2(JSONObject context_info,
            float track_length) throws Exception {
        super(context_info, track_length, null);

        this.comm_ids = context_info.getJSONArray("display_controllers").getStringArray();
        this.context_info = context_info;
        this.displaying = false;

        JSONObject start_msg = new JSONObject();
        start_msg.setString("type", context_info.getString("type"));
        if (!context_info.isNull("material")) {
            start_msg.setString("material", context_info.getString("material"));
        }
        start_msg.setFloat("y", context_info.getJSONArray("locations").getFloat(0)/10);
        if (!context_info.isNull("x")) {
            start_msg.setFloat("x", context_info.getFloat("x"));
        }
        if (!context_info.isNull("z")) {
            start_msg.setFloat("z", context_info.getFloat("z"));
        }

        start_msg.setString("id", this.id);
        this.startString = (new JSONObject()).setJSONObject("object", start_msg).toString();

        JSONObject stop_msg = new JSONObject();
        stop_msg.setString("action", "clearObject");
        stop_msg.setString("id", this.id);
        this.stopString = stop_msg.toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        this.comms = new UdpClient[comm_ids.length];
        for (int i = 0; i < comm_ids.length; i++) {
            boolean found = false;
            for (UdpClient c : comms) {
                if (c.getId().equals(this.comm_ids[i])) {
                    found = true;
                    this.comms[i] = c;
                    JSONObject msg_json = new JSONObject();
                    c.sendMessage(stopString);
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    public void sendCreateMessages() {
        this.status = "off";
    }

    public void suspend() {
        this.active = -1;
        this.sendMessage(this.stopString);
        this.displaying = false;
    }

    public void sendMessage(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
        }
    }

    public boolean check(float position, float time, int lap,
                         JSONObject[] msg_buffer) {

        if (!this.displaying) {
            sendMessage(this.startString);
            this.status = "on";
            this.displaying = true;
            this.active = 1;
        }
        return true;
    }

    public void stop(float time, JSONObject[] msg_buffer) {
        this.status = "off";
        this.active = -1;
        sendMessage(this.stopString);
        this.displaying = false;
    }
}
