import java.util.ArrayList;
import java.util.Iterator;

import processing.data.JSONObject;
import processing.data.JSONArray;

public class VrController {
    private ArrayList<UdpClient> comms;
    private ArrayList<String> scenes;
    private int sceneIndex;
    private boolean isNull;

    public VrController() {
        isNull = true;;
    }

    public VrController(JSONObject vr_settings) {

        comms = new ArrayList<UdpClient>();
        scenes = new ArrayList<String>();

        Iterator<String> itr = vr_settings.keyIterator();
        while (itr.hasNext()) {
            JSONObject vr_json = vr_settings.getJSONObject(itr.next());
            UdpClient vr_client = new UdpClient(vr_json.getString("ip"),
                vr_json.getInt("port"));

            JSONObject view_json = new JSONObject();
            view_json.setInt("viewAngle", vr_json.getInt("view_angle"));
            view_json.setInt("deflection", vr_json.getInt("deflection"));
            JSONObject msg_json = new JSONObject();
            msg_json.setString("data", view_json.toString().replace("\n",""));
            msg_json.setString("type", "cameraSetup");
            
            vr_client.sendMessage(msg_json.toString());
            comms.add(vr_client);
        }
        
        update(0.0f);
        loadScene("scene0");
        scenes.add("scene0");
    }

    private void sendMessage(JSONObject message) {
        if (isNull) {
            return;
        }

        Iterator<UdpClient> itr = comms.iterator();
        while (itr.hasNext()) {
            itr.next().sendMessage(message.toString().replace("\n",""));
        }
    }

    public void loadScene(String sceneName) {
        if (isNull) {
            return;
        }

        JSONObject sceneJson = new JSONObject();
        sceneJson.setString("type", "loadScene");
        sceneJson.setString("data", sceneName);

        sendMessage(sceneJson);
    }

    public void update(float position) {
        if (isNull) {
            return;
        }

        JSONObject position_json = new JSONObject();
        JSONObject position_data = new JSONObject();
        position_data.setFloat("x", 0);
        position_data.setFloat("y", position);
        position_data.setFloat("z", 0);
        position_json.setString("data", position_data.toString().replace("\n",""));
        position_json.setString("type", "position");

        sendMessage(position_json);
    }

    public void setRewards(int[] reward_locations) {
        if (isNull) {
            return;
        }

        JSONObject rewardJson = new JSONObject();
        rewardJson.setString("type", "rewardLocations");

        JSONArray locations = new JSONArray();
        for (int i=0; i < reward_locations.length; i++) {
            locations.setInt(i, reward_locations[i]);
        }
        rewardJson.setString("data", locations.toString().replace("\n",""));

        sendMessage(rewardJson);
    }

    public void addScene(String sceneName) {
        scenes.add(sceneName);
    }

    public void changeScene() {
        if (isNull) {
            return;
        }

        if (sceneIndex == 0) {
            sceneIndex = scenes.size();
        }
        
        sceneIndex = sceneIndex%(scenes.size()-1) + 1;
        loadScene(scenes.get(sceneIndex));
    }
}
