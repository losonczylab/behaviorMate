import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrExtendedContextList extends VrContextList2 {
    protected int lap_factor;
    protected int backtrack;
    protected float previous_position;

    public VrExtendedContextList(JSONObject context_info,
            float track_length) throws Exception {
        super(context_info, track_length);

        this.lap_factor = 2;
        this.backtrack = 0;
        this.previous_position = -1;
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

        if ((this.previous_position != -1) &&
                (position - this.previous_position > this.track_length/2)) {
            this.backtrack--;
        } else if ((this.backtrack < 0) &&
                (this.previous_position - position > this.track_length/2)) {
            this.backtrack++;
        }
        this.previous_position = position;

        lap = Math.abs(lap + this.backtrack);
        System.out.println(this.backtrack + " - " + lap);

        float adj_position = lap%this.lap_factor*this.track_length + position;
        if ((this.active != -1) && (position != previous_location)) {
            position_data.setFloat("y", adj_position/10);
            position_json.setJSONObject(
                "position", position_data);
            this.status = ""+(int)adj_position + " " + this.backtrack + " " + lap;

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
            //this.status = ""+(int)adj_position;
            this.status = ""+(int)adj_position + " " + this.backtrack + " " + lap;
            setupVr(context_info.getString("vr_file"));
            sendMessage(this.startString);
            position_data.setFloat("y", adj_position/10);
            position_json.setJSONObject(
                "position", position_data);

            sendMessage(position_json.toString());
            previous_location = position;

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = log_json.toString().replace("\n","");
        }


        return (this.active != -1);
    }

    public void stop(float time, String[] msg_buffer) {
        this.backtrack = 0;
        this.previous_position = -1;
        super.stop(time, msg_buffer);
    }
}
