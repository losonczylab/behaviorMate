import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrCueContextList extends BasicContextList {
    private JSONObject log_json;

    private JSONArray cues;
    private VrContextList vr;

    protected ArrayList<Integer> lap_list;

    public VrCueContextList(JSONObject context_info,
            float track_length) throws Exception {
        super(context_info, track_length, null);

        this.cues = context_info.getJSONArray("cues");
        this.vr = null;
        this.active = -1;

        JSONArray lap_array = null;
        if (!context_info.isNull("lap_list")) {
            lap_array = context_info.getJSONArray("lap_list");
        }

        if (lap_array != null) {
           this.lap_list = new ArrayList<Integer>();
            for (int i=0; i < lap_array.size(); i++) {
                this.lap_list.add(lap_array.getInt(i));
            }

        }
   }

    public void setVrContextList(VrContextList vr) {
        this.vr = vr;
    }

    public void setId(String id) {
        this.id = id;

        log_json = new JSONObject();
        log_json.setJSONObject("context", new JSONObject());
        log_json.getJSONObject("context").setString("id", this.id);
    }


    public void sendCreateMessages() {
        this.status = "off";
    }

    public void sendMessage() { }

    public boolean check(float position, float time, int lap,
                         String[] msg_buffer) {

        if ((this.lap_list.indexOf(lap) != -1) && (this.active == -1)) {
            this.status = "on";
            active = 1;
            this.vr.addCues(this.cues, this.id);

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = log_json.toString().replace("\n","");
        } else if ((this.lap_list.indexOf(lap) == -1) && (this.active == 1)) {
            active = -1;
            this.status = "off";
            this.vr.clearCueList(this.id);

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "stop");
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
        this.vr.clearCueList(this.id);
    }
}
