import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * ?
 */
public class VrContextList2 extends BasicContextList {
    /**
     * ?
     */
    protected float previous_location;

    /**
     * ?
     */
    protected UdpClient[] comms;

    /**
     * ?
     */
    protected JSONObject position_json;

    /**
     * ?
     */
    protected JSONObject position_data;

    /**
     * ?
     */
    protected String[] comm_ids;

    /**
     * ?
     */
    protected String sceneName;

    /**
     * ?
     */
    protected float startPosition;

    /**
     * ?
     */
    protected JSONObject vr_config;

    /**
     * ?
     */
    protected TreadmillController tc;

    /**
     * ?
     *
     * @param tc ?
     * @param context_info ?
     * @param track_length ?
     * @throws Exception
     */
    public VrContextList2(TreadmillController tc, JSONObject context_info, float track_length)
            throws Exception {
        super(context_info, track_length, null);

        this.tc = tc;
        this.vr_config = null;
        this.comm_ids = context_info.getJSONArray("display_controllers").getStringArray();
        this.context_info = context_info;

        this.sceneName = context_info.getString("scene_name", "_vrMate_main");

        position_data = new JSONObject();
        position_json = new JSONObject();
        this.log_json.getJSONObject("context").setString("scene", this.sceneName);

        this.previous_location = -1;

        JSONObject start_msg = new JSONObject();
        start_msg.setString("action", "start");
        start_msg.setString("context", this.id);
        this.startString = start_msg.toString();

        JSONObject stop_msg = new JSONObject();
        stop_msg.setString("action", "stop");
        stop_msg.setString("context", this.id);
        this.stopString = stop_msg.toString();

        this.startPosition = context_info.getFloat("start_position", -1.0f);
    }

    /**
     * ?
     */
    public void setupVr() {
        JSONObject scene_msg = new JSONObject();
        scene_msg.setString("action", "editContext");
        scene_msg.setString("type", "scene");
        scene_msg.setString("scene", this.sceneName);
        scene_msg.setString("context", this.id);
        sendMessage(scene_msg.toString());

        if (!this.context_info.isNull("vr_file")) {
            setupVr(this.context_info.getString("vr_file"));
        } else {
            this.vr_config = null;
        }

        if (!this.context_info.isNull("apply_filter")) {
            JSONArray filters = null;
            try {
                filters = context_info.getJSONArray("apply_filter");
            } catch (RuntimeException e) {
                filters = new JSONArray();
                try {
                    filters.append(context_info.getString("apply_filter"));
                } catch (RuntimeException e2) {
                    filters.append(context_info.getJSONObject("apply_filter"));
                }
            }

            for (int i = 0; i < filters.size(); i++) {
                JSONObject msg_json = new JSONObject();
                try {
                    msg_json = JSONObject.parse(
                        filters.getJSONObject(i).toString());
                } catch (RuntimeException e) {
                    msg_json.setString(
                        "id", filters.getString(i));
                }
                msg_json.setString("action", "editContext");
                msg_json.setString("context", this.id);
                msg_json.setString("type", "filter");
                sendMessage(msg_json.toString());
            }
        }
    }

    /**
     * ?
     *
     * @param vr_file ?
     */
    public void setupVr(String vr_file) {
        this.vr_config = parseJSONObject(BehaviorMate.parseJsonFile(vr_file).toString());
        JSONArray objects = vr_config.getJSONArray("objects");
        JSONObject msg_json = new JSONObject();
        msg_json.setString("action", "editContext");
        msg_json.setString("context", this.id);
        msg_json.setString("type", "cue");

        //TODO: if no id is set, set one here.
        for (int i=0; i < objects.size(); i++) {
            msg_json.setJSONObject("object", objects.getJSONObject(i));
            sendMessage(msg_json.toString());
        }

        if (!vr_config.isNull("skybox")) {
            msg_json = new JSONObject();
            msg_json.setString("action", "editContext");
            msg_json.setString("context", this.id);
            msg_json.setString("type", "skybox");
            msg_json.setString("skybox", vr_config.getString("skybox"));
            sendMessage(msg_json.toString());
        }

        if (!vr_config.isNull("apply_filter")) {
            msg_json = new JSONObject();
            try {
                msg_json = JSONObject.parse(
                    vr_config.getJSONObject("apply_filter").toString());
            } catch (Exception e) {
                msg_json.setString("id", vr_config.getString("apply_filter"));
            }

            msg_json.setString("action", "editContext");
            msg_json.setString("context", this.id);
            msg_json.setString("type", "filter");
            sendMessage(msg_json.toString());
        }
    }

    /**
     * ?
     *
     * @param comms channel to post messages for configuring, starting or stopping contexts.
     * @return
     */
    public boolean setupComms(ArrayList<UdpClient> comms) {
        this.comms = new UdpClient[comm_ids.length];
        for (int i = 0; i < comm_ids.length; i++) {
            boolean found = false;
            for (UdpClient c : comms) {
                if (c.getId().equals(this.comm_ids[i])) {
                    found = true;
                    this.comms[i] = c;
                    JSONObject msg_json = new JSONObject();
                    msg_json.setJSONObject(
                        "view",
                        this.context_info.getJSONArray("views")
                                         .getJSONObject(i));
                    c.sendMessage(msg_json.toString());
                    c.sendMessage(stopString);
                    try {
                        Thread.sleep(10);
                    } catch (Exception e){}
                }
            }
            if (!found) {
                return false;
            }

        }

        setupVr();
        return true;
    }

    /**
     * ?
     *
     * @param position   current position along the track
     * @param time       time (in s) since the start of the trial
     * @param lap        current lap number since the start of the trial
     * @param msg_buffer a Java array of type String to buffer and send messages
     *                   to be logged in the the tdml file being written for
     *                   this trial. messages should be placed in index 0 of the
     *                   message buffer and must be JSON formatted strings.
     * @return ?
     */
    public boolean check(float position, float time, int lap, JSONObject[] msg_buffer) {
        boolean inZone = false;
        int i=0;
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time)) {
                inZone = true;
                break;
            }
        }

        if ((this.active != -1) && (position != previous_location)) {
            position_data.setFloat("y", position/10);
            position_json.setJSONObject(
                "position", position_data);

            sendMessage(position_json.toString());
            previous_location = position;
        }

        if ((!inZone) && (this.active != -1)) {
            this.active = -1;
            this.status = "off";
            sendMessage(this.stopString);

            this.log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = this.log_json;
        } else if((inZone) && (this.active != i)) {
            this.active = i;
            this.status = "on";
            sendMessage(this.startString);
            if (this.startPosition != -1) {
                tc.setPosition(this.startPosition);
            }
            position_data.setFloat("y", position/10);
            position_json.setJSONObject(
                "position", position_data);

            sendMessage(position_json.toString());
            previous_location = position;

            this.log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = this.log_json;
        }

        return (this.active != -1);
    }

    /**
     * ?
     *
     * @param msg_buffer ?
     */
    public void trialStart(JSONObject[] msg_buffer) {
        setupVr();

        JSONObject config_msg = new JSONObject();
        config_msg.setString("id", this.id);
        config_msg.setJSONObject("vr_config", this.vr_config);
        msg_buffer[0] = config_msg;
    }

    /**
     * ?
     */
    public void end() {
        JSONObject end_msg = new JSONObject();
        end_msg.setString("context", this.id);
        end_msg.setString("action", "clear");
        this.sendMessage(end_msg.toString());
    }

    /**
     * ?
     */
    public void suspend() {
        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);

        float time = this.tc.getTime();
        this.log_json.getJSONObject("context").setString("action", "stop");

        this.tc.writeLog(this.log_json);
    }

    /**
     * ?
     *
     * @param time ?
     * @param msg_buffer ?
     */
    public void stop(float time, JSONObject[] msg_buffer) {
        if (this.active != -1) {
            this.log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = this.log_json;
        }

        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);
    }

    /**
     * ?
     *
     * @param message ?
     */
    public void sendMessage(String message) {
        if (this.comms == null) {
            System.out.println("comms null");
            System.out.println(message);
            return;
        }

        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
        }
    }

    protected void sendMessageSlow(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
            try {
                Thread.sleep(10);
            } catch(Exception e) {

            }
        }
    }

    /**
     * ?
     */
    public void shutdown() {
        JSONObject end_msg = new JSONObject();
        end_msg.setString("context", this.id);
        end_msg.setString("action", "clear");
        this.sendMessage(end_msg.toString());
    }
}
