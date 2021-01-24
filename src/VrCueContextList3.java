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
        if (this.comms == null) {
            return;
        }

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
}
