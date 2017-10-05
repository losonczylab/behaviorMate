import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrContextList extends ContextList {
    private float previous_location;
    private UdpClient[] comms;
    private JSONObject position_json;
    private JSONObject position_data;
    private JSONObject log_json;

    public VrContextList(Display display, JSONObject context_info,
            float track_length) throws Exception {
        super(display, context_info, track_length, null);
        JSONObject displays = context_info.getJSONObject("display_controllers");

        this.comms = new UdpClient[displays.size()];
        Iterator<String> itr = displays.keyIterator();
        for (int i=0; itr.hasNext(); i++) {
            JSONObject display_json = displays.getJSONObject(itr.next());
            UdpClient vr_client = new UdpClient(display_json.getString("ip"),
                display_json.getInt("port"));

            //JSONObject view_json = new JSONObject();
            //view_json.setInt("viewAngle", display_json.getInt("view_angle"));
            //view_json.setInt("deflection", display_json.getInt("deflection"));
            JSONObject msg_json = new JSONObject();
            msg_json.setJSONObject(
                "data", display_json.getJSONObject("cameraSetup"));
            msg_json.setString("type", "cameraSetup");

            vr_client.sendMessage(msg_json.toString());
            comms[i] = vr_client;
        }

        if (!context_info.isNull("cues")) {
            setCues(context_info.getJSONArray("cues"));
        }
        this.previous_location = 0;
    }

    /*
    public VrContextList(Display display, int display_color) {
        super(display, display_color);
        this.previous_location = 0;
    }

    public VrContextList(int duration, int radius, int display_color) {
        super(duration, radius, display_color);
        this.previous_location = 0;
    }
    */

    public void setComms(UdpClient[] comms) {
        this.comms = comms;
    }

    public void setId(String id) {
        this.id = id;

        JSONObject sceneJson = new JSONObject();
        sceneJson.setString("type", "loadScene");
        sceneJson.setString("data", id);
        this.startString = sceneJson.toString();

        sceneJson = new JSONObject();
        sceneJson.setString("type", "loadScene");
        sceneJson.setString("data", "scene0");
        this.stopString = sceneJson.toString();

        position_json = new JSONObject();
        position_data = new JSONObject();
        position_data.setFloat("x", 0);
        position_data.setFloat("z", 0);
        position_json.setString("type", "position");

        log_json = new JSONObject();
        log_json.setJSONObject("context", new JSONObject());
        log_json.getJSONObject("context").setString("id", this.id);
    }

    public void setCues(JSONArray cues) {
        sendMessage(this.startString);

        JSONObject clearMessage = new JSONObject();
        clearMessage.setString("type", "cues");
        clearMessage.setString("action", "clear");
        sendMessage(clearMessage.toString());

        JSONObject createMessage = new JSONObject();
        createMessage.setString("type", "cues");
        createMessage.setString("action", "create");
        createMessage.setJSONArray("cues", cues);
        sendMessage(createMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }

    }

    public void addCues(JSONArray cues, String list_name) {
        sendMessage(this.startString);

        JSONObject createMessage = new JSONObject();
        createMessage.setString("type", "cues");
        createMessage.setString("action", "create");
        createMessage.setString("list_name", list_name);
        createMessage.setJSONArray("cues", cues);
        sendMessage(createMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }
    }

    public void clearCueList(String list_name) {
        if (this.active == -1) {
            sendMessage(this.startString);
        }

        JSONObject clearMessage = new JSONObject();
        clearMessage.setString("type", "cues");
        clearMessage.setString("action", "clear");
        clearMessage.setString("list_name", list_name);
        sendMessage(clearMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }
    }

    public boolean check(float position, float time, int lap,
                         String[] msg_buffer) {
        boolean inZone = false;
        int i=0;
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time)) {
                inZone = true;
                break;
            }
        }

        if ((this.active != -1) && (position != previous_location)) {
            position_data.setFloat("y", position);
            position_json.setString(
                "data", position_data.toString().replace("\n",""));

            sendMessage(position_json.toString());
            previous_location = position;
        }

        if ((!inZone) && (this.active != -1)) {
            this.active = -1;
            this.status = "off";
            sendMessage(this.stopString);

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = log_json.toString().replace("\n","");
        } else if((inZone) && (this.active != i)) {
            this.active = i;
            this.status = "on";
            sendMessage(this.startString);

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = log_json.toString().replace("\n","");
        }


        return (this.active != -1);
    }

    public void stop(float time, String[] msg_buffer) {
        if (this.active != -1) {
            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = log_json.toString().replace("\n","");
        }

        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);
    }

    private void sendMessage(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
        }
    }
}
