import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrCueContextList3 extends VrContext {

    protected ArrayList<Integer> object_locations;
    protected int true_size;
    protected float display_radius_unscale;

    public VrCueContextList3(
            TreadmillController tc, JSONObject context_info,
            float track_length) throws Exception {
        super(tc, context_info, track_length);

        JSONArray objects = this.context_info.getJSONArray("objects");
        this.display_radius_unscale = this.context_info.getFloat("display_radius", 0);
        this.true_size = objects.size();
        this.shuffle_contexts = this.context_info.getBoolean("shuffle", false);
        object_locations = new ArrayList<Integer>();
        for (int i = 0; i < objects.size(); i++) {
            this.object_locations.add(
                objects.getJSONObject(i).getJSONArray("Position").getInt(1)*10);
        }
    }


    public void setupVr() {
        this.sendMessage(this.stopString);
        JSONObject setup_msg = new JSONObject();
        setup_msg.setString("action", "editContext");
        setup_msg.setString("context", this.id);
        setup_msg.setString("type", "cue");

        JSONArray objects = this.context_info.getJSONArray("objects");
        for (int i = 0; i < objects.size(); i++) {
            setup_msg.setJSONObject("object", objects.getJSONObject(i));
            this.sendMessage(setup_msg.toString());
        }
    }


    public void move(int index, int location) {
        this.contexts.get(index).move(location);
        this.object_locations.set(index, location);
        JSONObject setup_msg = new JSONObject();
        setup_msg.setString("action", "editContext");
        setup_msg.setString("context", this.id);
        setup_msg.setString("type", "move_cue");


        JSONArray objects = this.context_info.getJSONArray("objects");
        for (int i = 0; i < objects.size(); i++) {
            setup_msg.setJSONObject("object", objects.getJSONObject(i));
            JSONArray position = setup_msg.getJSONObject("object").getJSONArray("Position");
            position.setFloat(1, location/10.0f);
            setup_msg.getJSONObject("object").setJSONArray("Position", position);
            this.sendMessage(setup_msg.toString());
        }
    }


    public int size() {
        return this.true_size;
    }


    public int getLocation(int i) {
        return this.object_locations.get(i);
    }


    public void setDisplayScale(float scale) {
        this.scale = scale;
        this.display_radius = this.display_radius_unscale * scale;
    }
    /*
    public boolean check(float position, float time, int lap,
            JSONObject[] msg_buffer) {
        boolean inZone = true;
        int i=0;
        if ((this.active != -1) && (position != previous_location)) {
            //position_data.setFloat("y", position/10);
            //position_json.setJSONObject(
            //    "position", position_data);

            //sendMessage(position_json.toString());
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
            //if (this.startPosition != -1) {
            //    tc.setPosition(this.startPosition);
            //}

            //position_data.setFloat("y", position/10);
            //position_json.setJSONObject(
            //    "position", position_data);

            //sendMessage(position_json.toString());
            previous_location = position;

            this.log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = this.log_json;
        }


        return (this.active != -1);
    }
    */
}
