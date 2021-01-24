import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public abstract class VrContext extends BasicContextList {
    protected TreadmillController tc;
    protected UdpClient[] comms;
    protected String[] comm_ids;
    protected JSONObject log_json;
    protected JSONObject vr_config;


    public VrContext(TreadmillController tc, JSONObject context_info,
                     float track_length) {
        super(context_info, track_length, null);
    
        this.tc = tc;
        this.comm_ids = context_info.getJSONArray(
            "display_controllers").getStringArray();
        this.context_info = context_info;

        this.log_json = new JSONObject();
        this.log_json.setJSONObject("context", new JSONObject());
        this.log_json.getJSONObject("context").setString("id", id);

        JSONObject json_msg = new JSONObject();
        json_msg.setString("action", "start");
        json_msg.setString("context", this.id);
        this.startString = json_msg.toString();

        json_msg.setString("action", "stop");
        this.stopString = json_msg.toString();
        this.vr_config = null;
    }

   
    public void setId(String id) {
        this.id = id;
    }


    public void setupVr() {
        if (this.vr_config != null) {
            this.sendMessage(vr_config.toString());
        }
        this.sendMessage(this.stopString);
    }


    public boolean setupComms(ArrayList<UdpClient> comms) {
        this.comms = new UdpClient[comm_ids.length];
        for (int i = 0; i < comm_ids.length; i++) {
            boolean found = false;
            for (UdpClient c : comms) {
                if (c.getId().equals(this.comm_ids[i])) {
                    found = true;
                    this.comms[i] = c;
                }
            }
            if (!found) {
                return false;
            }
        }

        setupVr();
        return true;
    }


    public void sendCreateMessages() {
        this.status = "off";
    }


    public void suspend() {
        this.status = "off";
        sendMessage(this.stopString);

        if (this.active != -1) {
            float time = this.tc.getTime();
            this.log_json.getJSONObject("context").setString("action", "stop");

            this.tc.writeLog(this.log_json);
            this.active = -1;
        }
    }


    public void stop(float time, JSONObject[] msg_buffer) {
        if (this.active != -1) {
            this.log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = this.log_json;
        }

        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);
    }


    public void sendMessage(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
        }
    }


    protected void updateVr(boolean is_active, float position, float time,
                            int lap) { }


    public boolean check(float position, float time, int lap,
                         JSONObject[] msg_buffer) {
        boolean prev_active = (this.active != -1);
        boolean is_active = super.check(position, time, lap, msg_buffer);
        this.waiting = false;

        if (is_active && !prev_active) {
            this.status = "on";
            this.log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = this.log_json;
        } else if (!is_active && prev_active) {
            this.status = "off";
            this.log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = this.log_json;
        }

        this.updateVr(is_active, position, time, lap);

        return is_active;
    }


    public void trialStart(JSONObject[] msg_buffer) {
        setupVr();

        if (this.vr_config != null) {
            JSONObject config_msg = new JSONObject();
            config_msg.setString("id", this.id);
            config_msg.setJSONObject("vr_config", this.vr_config);
            msg_buffer[0] = config_msg;
        }
    }


    public void end() {
        JSONObject end_msg = new JSONObject();
        end_msg.setString("context", this.id);
        end_msg.setString("action", "clear");
        this.sendMessage(end_msg.toString());
    }


    public void shutdown() {
        JSONObject end_msg = new JSONObject();
        end_msg.setString("context", this.id);
        end_msg.setString("action", "clear");
        this.sendMessage(end_msg.toString());
    }
}
