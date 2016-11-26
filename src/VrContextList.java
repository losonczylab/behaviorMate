import processing.core.PApplet;
import processing.data.JSONObject;
import java.util.ArrayList;

public class VrContextList extends ContextList {
    private float previous_location;
    private UdpClient[] comms;
    private JSONObject position_json;
    private JSONObject position_data;
    private JSONObject log_json;

    public VrContextList(Display display, int display_color) {
        super(display, display_color);

        previous_location = 0;
    }

    public VrContextList(int duration, int radius, int display_color) {
        super(duration, radius, display_color);

        previous_location = 0;
    }

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

    public boolean check(float position, float time, String[] msg_buffer) {
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
            position_json.setString("data", position_data.toString().replace("\n",""));
            
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
