import javax.sound.sampled.*;
import java.net.*;
import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PSurface;
import processing.data.JSONObject;
import processing.data.JSONArray;

/**
 * Wrapper class for JSONObject to allow for JSONObjects to be returned by
 * reference during calls to UDPComm
 */
class JSONBuffer {
    /* Wrapper class for JSONObject which allows for json to be returned by 
       reference during method calls */

    public JSONObject json;
}


public class TreadmillController extends PApplet {

    TrialListener trialListener;

    FileWriter fWriter;
    UdpClient position_comm;
    UdpClient behavior_comm;
    UdpClient vr_comm;
    VrController vrController;

    Display display;
    ExperimentTimer timer;
    
    String settings_filename;
    String settings_tag;

    /**
     * json object with all the settings related to this trail. Stored at the top
     * of each of the behavior logs
     */
    JSONObject settings_json;
    /**
     * json object with all the settings related to the computer specifically. Not
     * saved when each trial is ran.
     */
    JSONObject system_json;

    String mouse_name;
    float position;
    /** 
     * scale to convert position updates from rotary encoder to mm traversed on the
     * track
     */
    float position_scale;
    /** length of the track in mm */
    float track_length;
    /** 
     *RFID tag string to indicate that a lap has been compleded and position
     * should be reset to 0.
     */
    String lap_tag;
    /**
     * force a lap_reset if the position is more then track_length*lap_tolerance past 
     * track_length. Defaults to 0 if lap_reset_tag is not set, 0.99 if not present in
     * settings.json 
     */
    float lap_tolerance;

    int lap_count;

    /** prevents the draw function from running */
    boolean nodraw;

    int trial_duration;

    boolean started = false;
    /** indicates if rewards are currently being operantly delivered */
    boolean rewarding;
    /** time at which rewards started to be operantly delivered */
    float reward_start;
    /** locations of the reward midpoints along the track */
    int[] reward_locations;
    /** radius around reward locations wich deliminates the reward zone*/
    int reward_radius;
    /** index of the next reward possible in int[] reward_locations */
    int next_reward;
    /** if true, reward locations change from lap to lap */
    boolean moving_rewards;
    /** pin number for the reward locations */
    int reward_valve;
    /** temporal limit on reward zone */
    float reward_duration;

    /** indicates weither the opto laser is currently on */
    boolean lasering;

    boolean laser_on_reward;

    float laser_start;

    int next_laser;

    int[] laser_locations;

    int laser_radius;

    int laser_pin;

    String start_laser_message;

    String stop_laser_message;

    int lickport_pin;

    /** stores the json string to start the reward context */
    String start_context_message;
    /** stores the json string to stop the reward context */
    String stop_context_message;

    /** Read buffer for position messages */
    JSONBuffer position_buffer = new JSONBuffer();
    /** Read buffer for behavior messages */
    JSONBuffer json_buffer = new JSONBuffer();

    /** Date format for logging experiment start/stop */
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TreadmillController(String filename, String tag, TrialListener el) {
        println(filename);
        this.trialListener = el;
        this.settings_filename = filename;
        this.settings_tag = tag;
    }

    public TreadmillController(TrialListener el) {
        this.trialListener = el;
        this.settings_filename = "settings.json";
        this.settings_tag = "default";
    }


    public int getRewardPin() {
        return reward_valve;
    }

    /**
     * Starts a new experiment. Linked to clicking the "Start" button on the UI.
     * Creates a New Log file, makes the initial entries, plays a tone (if specified
     * in the settings), starts the timer, and triggers the sync pin to start
     * imaging.
     */
    public boolean Start(String mouse_name, String experiment_group) {
        if ((position == -1) || mouse_name.equals("") ||
            (experiment_group.equals(""))) {
            return false;
        }
        
        display.setMouseName(mouse_name);
        display.setLickCount(0);
        display.setLapCount(0);
        display.setRewardCount(0);
        lap_count=0;
        
        fWriter = new FileWriter(system_json.getString("data_directory", "data"), mouse_name);

        Date startDate = Calendar.getInstance().getTime();
        
        JSONObject start_log = new JSONObject();
        start_log.setString("mouse", mouse_name);
        start_log.setString("experiment_group", experiment_group);
        start_log.setString("start", dateFormat.format(startDate));

        JSONObject position_log = new JSONObject();
        position_log.setFloat("time", 0);
        position_log.setFloat("y", position);

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());
        fWriter.write(position_log.toString());

        trialListener.started(fWriter.getFile());

        int toneDuration = settings_json.getInt("tone_duration");
        if (toneDuration > 0) {
            SoundUtils soundUtil = new SoundUtils();
            try {
                soundUtil.tone(settings_json.getInt("tone_freq"),toneDuration);
            } catch(Exception e) {}
        }
        
        vrController.loadScene("fond_dock");

        startContexts();
        timer.startTimer();
        JSONObject valve_json = open_valve_json(settings_json.getInt("sync_pin"), 100);
        behavior_comm.sendMessage(valve_json.toString());

        started = true;
        return true;
    }

    public void RefreshSettings(String filename, String tag) {
        settings_filename = filename;
        settings_tag = tag;
        RefreshSettings();
    }

    public void RefreshSettings() {
        behavior_comm.closeSocket();
        position_comm.closeSocket();
        reload_settings(settings_filename, settings_tag);
    }

    /**
     * Tests the valve specified in the Box on the UI. Linked to the TestValve
     * button the UI. Both creates and then opens the valve for the amount of
     * time specified in the duration box.
     */
    public void TestValve(int pin, int duration) {
        println("TEST VALVE");

        JSONObject valve_json = setup_valve_json(pin);
        behavior_comm.sendMessage(valve_json.toString());

        valve_json = open_valve_json(pin, duration);
        behavior_comm.sendMessage(valve_json.toString());
    }

    /**
     * Suffle the location of rewards along the track. Updates the 
     * int[] reward_locations array
     */
    public void shuffle_rewards() {
        if (reward_locations.length == 1) {
            reward_locations[0] = (int) random(reward_radius,track_length-reward_radius);
            display.setRewardLocations(reward_locations, reward_radius);
            if (vrController != null) {
                vrController.setRewards(reward_locations);
            }
            return;
        }

        int interval = (int)(track_length-2*reward_radius)/reward_locations.length;
        reward_locations[0] = reward_radius + interval/2;
        for (int i = 1; i < reward_locations.length; i++) {
            reward_locations[i] = reward_locations[i-1] + interval;
        }

        reward_locations[0] =
            (int) random(reward_radius,reward_locations[1]-2*reward_radius);
        for (int i = 1; i < reward_locations.length-1; i++) {
            reward_locations[i] =
                (int) random(reward_locations[i-1]+2*reward_radius, reward_locations[i+1]-2*reward_radius);
        }
        reward_locations[reward_locations.length-1] =
            (int) random(reward_locations[reward_locations.length-2]+2*reward_radius, track_length-reward_radius);

        display.setRewardLocations(reward_locations, reward_radius);
        if (vrController != null) {
            vrController.setRewards(reward_locations);
        }
    }

    /**
     * Generates the JSONObject necessary to create a valve.
     *
     * @param  pin the pin number to setup the valve on
     * @return     the JSONObject which will configure a valve when sent to arduino
     */
    JSONObject setup_valve_json(int pin) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action","create");
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Generates the JSONObject necessary to close a valve.
     *
     * @param  pin the pin number of the valve to close
     * @return     the JSONObject which will close the valve when sent to arduino
     */
    JSONObject close_valve_json(int pin) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action","close");
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Generates the JSONObject necessary to open a valve. Assumes that the valve
     * has already been configured.
     *
     * @param  pin      the pin number of the valve to open
     * @param  duration duration to keep the valve open for (milli-seconds)
     * @return          the JSONOBject which will open the valve when sent to
     *                  arduino
     */
    JSONObject open_valve_json(int pin, int duration) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();

        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action","open");
        valve_subjson.setInt("duration",duration);
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Configures the sensors as defined in the settings.json file. All sensors
     * must include pin, type, and report_pin must be specified as needed
     */
    void configure_sensors() {
        JSONArray sensors = settings_json.getJSONArray("sensors");
        for (int i=0; i < sensors.size(); i++) {
            JSONObject create_subjson = sensors.getJSONObject(i);
            if (create_subjson.getString("type", "").equals("lickport")) {
                lickport_pin = create_subjson.getInt("pin");
            }
            create_subjson.setString("action", "create");
            JSONObject create_json = new JSONObject();
            create_json.setJSONObject("sensors", create_subjson);

            behavior_comm.sendMessage(create_json.toString());
            delay(150);
        }
    }

    //ArrayList<UdpClient> vr_comms;
    /*void load_scene(String sceneName) {
        JSONObject sceneJson = new JSONObject();
        sceneJson.setString("type", "loadScene");
        sceneJson.setString("data", sceneName);

        Iterator<UdpClient> itr = vr_comms.iterator();
        while (itr.hasNext()) {
            itr.next().sendMessage(sceneJson.toString());
        }
    }

    void configure_vr() {
        vr_comms = new ArrayList<UdpClient>();
        if (settings_json.isNull("display_controllers")) {
            return;
        }

        JSONObject vr_settings = settings_json.getJSONObject("display_controllers");
        Iterator<String> itr = vr_settings.keyIterator();
        while (itr.hasNext()) {
            JSONObject vr_json = vr_settings.getJSONObject(itr.next());
            UdpClient vr_client = new UdpClient(vr_json.getString("ip"),
                vr_json.getInt("port"));

            JSONObject view_json = new JSONObject();
            view_json.setInt("viewAngle", vr_json.getInt("view_angle"));
            view_json.setInt("deflection", vr_json.getInt("deflection"));
            JSONObject msg_json = new JSONObject();
            msg_json.setString("data", view_json.toString());
            
            vr_client.sendMessage(msg_json.toString());
            vr_comms.add(vr_client);
        }
        
        load_scene("scene0");
    }*/

    
    ArrayList<String> startContextMessages;
    ArrayList<String> stopContextMessages;
    void configure_contexts() {
        startContextMessages = new ArrayList<String>();
        stopContextMessages = new ArrayList<String>();
        if (settings_json.isNull("contexts")) {
            return;
        }
        
        JSONArray contexts = settings_json.getJSONArray("contexts");
        for (int i=0; i < contexts.size(); i++) {
            JSONObject create_subjson = contexts.getJSONObject(i);
            create_subjson.setString("action", "create");
            JSONObject create_json = new JSONObject();
            create_json.setJSONObject("contexts", create_subjson);

            JSONArray valves = create_subjson.getJSONArray("valves");
            for (int j=0; j < valves.size(); j++) {
                JSONObject valve_json = setup_valve_json(valves.getInt(j));
                if (!create_subjson.isNull("frequency")) {
                    valve_json.getJSONObject("valves").setInt(
                        "frequency", create_subjson.getInt("frequency"));
                }
                behavior_comm.sendMessage(valve_json.toString());
            }
            behavior_comm.sendMessage(create_json.toString());
            delay(150);

            JSONObject start_subjson = new JSONObject();
            start_subjson.setString("action", "start");
            start_subjson.setString("id", create_subjson.getString("id"));
            JSONObject start_json = new JSONObject();
            start_json.setJSONObject("contexts", start_subjson);
            startContextMessages.add(start_json.toString());

            JSONObject stop_subjson = new JSONObject();
            stop_subjson.setString("action", "stop");
            stop_subjson.setString("id", create_subjson.getString("id"));
            JSONObject stop_json = new JSONObject();
            stop_json.setJSONObject("contexts", stop_subjson);
            stopContextMessages.add(stop_json.toString());
        }
    }

    void startContexts() {
        for (int i = 0; i < startContextMessages.size(); i++) {
            behavior_comm.sendMessage(startContextMessages.get(i));
        }
    }

    void stopContexts() {
        for (int i = 0; i < startContextMessages.size(); i++) {
            behavior_comm.sendMessage(stopContextMessages.get(i));
        }
    }

    /**
     * Configures the settings to trigger an opto-laser around the reward zone.
     */
    void configure_laser() {
        if (settings_json.isNull("laser")) {
            laser_pin = -1;
            laser_radius = 0;
            laser_locations = int[];
            display.setLaserLocations(laser_locations, laser_radius);

            return;
        }

        JSONObject laser_info = settings_json.getJSONObject("laser");

        JSONArray locations = new JSONArray();
        laser_radius = laser_info.getInt("radius");

        if (laser_info.getBoolean("reward_centered", false)) {
            laser_locations = reward_locations;
            laser_on_reward = true;
        } else {
            laser_on_reward = false;
            try {
                locations = laser_info.getJSONArray("locations");
            } catch (Exception e) {}

            laser_locations = new int[locations.size()];
            for (int i=0; i < locations.size(); i++) {
                laser_locations[i] = locations.getInt(i);
            }
        }   
        display.setLaserLocations(laser_locations, laser_radius);

        laser_pin = laser_info.getInt("pin");
        JSONObject laser_json = setup_valve_json(laser_pin);
        behavior_comm.sendMessage(laser_json.toString());
        
        JSONObject start_submessage = new JSONObject();
        start_submessage.setString("action","open");
        start_submessage.setInt("duration",-1);
        start_submessage.setInt("pin",laser_pin);
        JSONObject start_laser_json = new JSONObject();
        start_laser_json.setJSONObject("valves", start_submessage);
        start_laser_message = start_laser_json.toString();

        JSONObject stop_submessage = new JSONObject();
        stop_submessage.setString("action","close");
        stop_submessage.setInt("pin",laser_pin);
        JSONObject stop_laser_json = new JSONObject();
        stop_laser_json.setJSONObject("valves", stop_submessage);
        stop_laser_message = stop_laser_json.toString();
        println(start_laser_message);
        behavior_comm.sendMessage(stop_laser_json.toString());
    }


    /**
     * Configures the reward zone contexts and establishes the initial reward zone
     * locations
     */
    void configure_rewards() {
        JSONObject reward_info = settings_json.getJSONObject("reward");
        reward_valve = reward_info.getInt("pin");
        reward_radius = reward_info.getInt("radius");
        reward_duration = reward_info.getInt("max_duration");

        if (reward_info.getString("type").equals("fixed")) {
            moving_rewards = false;
            JSONArray locations = reward_info.getJSONArray("locations");
            reward_locations = new int[locations.size()];
            for (int i=0; i < locations.size(); i++) {
                reward_locations[i] = locations.getInt(i);
            }
            display.setRewardLocations(reward_locations, reward_radius);
            if (vrController != null) {
                vrController.setRewards(reward_locations);
            }
        } else {
            moving_rewards = true;
            reward_locations = new int[reward_info.getInt("number")];
            shuffle_rewards();
        }

        JSONObject valve_json = setup_valve_json(reward_valve);
        behavior_comm.sendMessage(valve_json.toString());
        JSONObject close_json = close_valve_json(reward_valve);
        behavior_comm.sendMessage(close_json.toString());

        JSONArray context_valves = new JSONArray();
        context_valves.append(reward_valve);
        JSONArray context_duration = new JSONArray();
        context_duration.append(reward_info.getInt("drop_size"));

        JSONObject context_setup = new JSONObject();
        context_setup.setString("action", "create");
        context_setup.setString("id", "hidden_reward");
        context_setup.setJSONArray("valves", context_valves);
        context_setup.setJSONArray("durations", context_duration);
        context_setup.setString("type", "operant");
        context_setup.setInt("operant_rate", reward_info.getInt("operant_rate"));
        context_setup.setInt("initial_open", reward_info.getInt("initial_open"));
        context_setup.setInt("sensor", lickport_pin);

        JSONObject context_setup_json = new JSONObject();
        context_setup_json.setJSONObject("contexts", context_setup);
        behavior_comm.sendMessage(context_setup_json.toString());

        JSONObject context_message = new JSONObject();
        context_message.setString("action", "start");
        context_message.setString("id", "hidden_reward");
        JSONObject context_message_json = new JSONObject();
        context_message_json.setJSONObject("contexts", context_message);
        start_context_message = context_message_json.toString();

        context_message.setString("action", "stop");
        context_message_json.setJSONObject("contexts", context_message);
        stop_context_message = context_message_json.toString();
    }

    /*void updateVr()
    {
        JSONObject position_json = new JSONObject();
        JSONObject position_data = new JSONObject();
        position_data.setFloat("x", 0);
        position_data.setFloat("y", position);
        position_data.setFloat("z", 0);
        position_json.setString("data", position_data.toString().replace("\n",""));
        position_json.setString("type", "position");

        vr_comm.sendMessage(position_json.toString().replace("\n",""));
    }*/


    void reload_settings(String filename, String tag) {
        try {
          settings_json = loadJSONObject(filename).getJSONObject(tag);
        } catch (Exception e) {
           println(e.toString());
           background(0);
           fill(color(255,0,0));
           textSize(24);
           text("Settings.json failed to load!", 50, 50);
           text(e.toString(), 50, 80);
           noLoop();
           nodraw = true;
           return;
        }

        system_json = loadJSONObject(filename).getJSONObject("_system");
        if (!settings_json.getString("lap_reset_tag", "").equals("")) {
            if (!settings_json.getString("lap_reset_tag").equals(lap_tag)) {
                position = -1;
            }
        } else if (position == -1) {
            position = 0;
        }

        trial_duration = settings_json.getInt("trial_length");
        position_scale = settings_json.getFloat("position_scale");
        track_length = settings_json.getFloat("track_length");
        lap_tag = settings_json.getString("lap_reset_tag");
        if (!lap_tag.equals("")) {
            lap_tolerance = settings_json.getFloat("lap_tolerance", 0.99f);
        } else {
            lap_tolerance = 0;
        }

        JSONObject behavior_json = settings_json.getJSONObject("behavior_controller");
        behavior_comm = new UdpClient(behavior_json
            .getInt("send_port"), behavior_json.getInt("receive_port"));
        behavior_json.setString("address", behavior_comm.address);
        settings_json.setJSONObject("behavior_controller", behavior_json);
        JSONObject position_json = settings_json.getJSONObject("position_controller");
        position_comm = new UdpClient(position_json
            .getInt("send_port"), position_json.getInt("receive_port"));
        position_json.setString("address", position_comm.address);
        settings_json.setJSONObject("position_controller", position_json);
      
        configure_sensors();

        JSONObject valve_json = setup_valve_json(settings_json.getInt("sync_pin"));
        behavior_comm.sendMessage(valve_json.toString());
        display.setRewardLocations(reward_locations, reward_radius);
        display.setTrackLength(track_length);

        configure_rewards();
        configure_contexts();
        configure_laser();
        if (!settings_json.isNull("display_controllers")) {
            vrController = new VrController(
                settings_json.getJSONObject("display_controllers"));
            if (vrController != null) {
                vrController.setRewards(reward_locations);
            }

            JSONArray scenes = settings_json.getJSONArray("vr_scenes");
            for (int i=0; i < scenes.size(); i++) {
                vrController.addScene(scenes.getString(i));
            }
        } else {
            vrController = new VrController();
        }
    }

    void reload_settings() {
        reload_settings(settings_filename, settings_tag);
    }

    public void addComment(String comment) {
        if (fWriter == null) {
            return;
        }

        JSONObject comment_json = new JSONObject();
        comment_json.setString("comments", comment);
        fWriter.write(comment_json.toString());
    }

    public PSurface getPSurface() {
        return this.initSurface();
    }

    /**
     * processing function which runs once each time the program is started up.
     * Configures state variavles to their initial states and reads in the settings
     * from the json file.
     */
    public void setup() {
        sketchPath("");
        textSize(12);
        background(0);

        started = false;
        laser_on_reward = false;
        rewarding = false;
        lasering = false;
        reward_start = 0;
        laser_start = 0;
        trial_duration = 0;
        next_reward = 0;
        next_laser = 0;
        laser_locations = new int[0];
        position = -1;
        lap_count = 0;
        lap_tag = "";
        fWriter = null;
        timer = new ExperimentTimer();

        display = new Display();
        //vr_comm = new UdpClient("192.168.1.55",8025, 8050);
        reload_settings();
        prepareExitHandler();
    }

    /** processing function which is looped over continuously. Main logic of the
     * experiment is in the body of this function.
     */
    public void draw() {
        if (nodraw) {
            return;
        }

        background(0);
        float time = timer.checkTime();;
        if (time > trial_duration) {
            endExperiment();
        }

        float dy = 0;
        if (position_comm.receiveMessage(json_buffer)) {
            JSONObject position_json =
                json_buffer.json.getJSONObject(position_comm.address);

            if (!position_json.isNull("position")) {
                dy = position_json.getJSONObject("position").getFloat("dy", 0);
                if (position != -1) {
                    position += dy/position_scale;
                    if (position < 0) {
                        position += track_length;
                    }
                }

                if (position > track_length*(1 + lap_tolerance)) {
                    position = track_length*lap_tolerance;
                    next_reward = 0;
                    next_laser = 0;
                    if (moving_rewards) {
                        shuffle_rewards();
                    }
                    if (started) {
                        JSONObject lap_log = new JSONObject();
                        lap_log.setFloat("time", time);
                        lap_log.setInt("lap", lap_count);
                        lap_log.setString("message", "no tag");
                        vrController.changeScene();
                        fWriter.write(lap_log.toString());
                        lap_count++;
                        display.setLapCount(lap_count);
                    }
                }

                if (started) {
                    json_buffer.json.setFloat("y", position);
                    json_buffer.json.setFloat("time", time);
                    fWriter.write(json_buffer.json.toString());
                    vrController.update(position);
                }

            }
        }

        boolean inZone = false;
        boolean laserZone = false;
        if (started) {
            for (int i = next_reward; i < reward_locations.length; i++) {
                int rewardZone = reward_locations[i];
                if ((position > (rewardZone - reward_radius)) &&
                        (position < (rewardZone + reward_radius))) {
                    if ((rewarding) && ((reward_start + reward_duration) < time)) {
                        inZone = false;
                        next_reward++;
                    } else {
                        next_reward = i;
                        inZone = true;
                    }
        
                    break;
                }
            }

            for (int i = next_laser; i < laser_locations.length; i++) {
                int zone = laser_locations[i];
                if ((position > (zone - laser_radius)) &&
                        (position < (zone + laser_radius))) {
                    if ((lasering) && ((laser_start + reward_duration) < time)) {
                        laserZone = false;
                        next_laser++;
                    } else {
                        next_laser = i;
                        laserZone = true;
                    }
        
                    break;
                }
            }
        }

        if ((!inZone) && (rewarding)) {
            rewarding = false;
            behavior_comm.sendMessage(stop_context_message);

        } else if((inZone) && (!rewarding)) {
            rewarding = true;
            behavior_comm.sendMessage(start_context_message);
            if (!laser_on_reward) {
                reward_start = time;
            }
        }

        if ((!laserZone) && (lasering)) {
            lasering = false;
            behavior_comm.sendMessage(stop_laser_message);
            
        } else if((laserZone) && (!lasering)) {
            lasering = true;
            behavior_comm.sendMessage(start_laser_message);
            laser_start = time;
            if (laser_on_reward) {
                reward_start = time;
            }
        }

        if (behavior_comm.receiveMessage(json_buffer)) {
            JSONObject behavior_json =
                json_buffer.json.getJSONObject(behavior_comm.address);
            
            if (!behavior_json.isNull("lick")) {
                if (behavior_json.getJSONObject("lick")
                        .getString("action", "stop").equals("start")) {
                    display.addLick(started);
                }
            }

            if (!behavior_json.isNull("valve")) {
                JSONObject valveJson = behavior_json.getJSONObject("valve");
                if (valveJson.getString(""+reward_valve, "close").equals("open")) {
                    display.addReward();
                } else if ((valveJson.getInt("pin", -1) == reward_valve) &&          // This check is needed for new arduino syntax
                        valveJson.getString("action", "close").equals("open")) {
                    display.addReward();
                }
            }

            if (!behavior_json.isNull("lap")) {
                String tag = behavior_json.getJSONObject("lap").getString("tag");
                display.setCurrentTag(tag);
                if (tag.equals(lap_tag)) {
                    display.setLastLap(position);
                    position = 0;
                    next_reward = 0;
                    next_laser = 0;
                    if (moving_rewards) {
                        shuffle_rewards();
                    }
                    if (started) {
                        JSONObject lap_log = new JSONObject();
                        lap_log.setFloat("time", time);
                        lap_log.setInt("lap", lap_count);
                        fWriter.write(lap_log.toString());
                        lap_count++;
                        vrController.changeScene();
                        display.setLapCount(lap_count);
                    }
                }
            }
            
            if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }
        
        display.update(this, dy/position_scale, position, time, rewarding, lasering);
    }

    /**
     * End the current experiment, reset the state to await the next trial
     */
    public void endExperiment() {
        if (!started) {
            return;
        }

        Date stopDate = Calendar.getInstance().getTime();
        trialListener.ended();
        stopContexts();
        
        JSONObject end_log = new JSONObject();
        behavior_comm.sendMessage(stop_context_message);
        end_log.setFloat("time", timer.getTime());
        end_log.setString("stop", dateFormat.format(stopDate));
        fWriter.write(end_log.toString());
        display.setMouseName("");

        vrController.update(0.0f);
        vrController.loadScene("scene0");

        started = false;
        rewarding = false;
        lasering = false;
        reward_start = 0;
        laser_start = 0;
        next_reward = 0;
        next_laser = 0;
        lap_count = 0;
        timer = new ExperimentTimer();
    }

    /**
     * add a function hook to run at shutdown. this code runs of the program is
     * terminated unexpectidly, ensuring that the log files are closed out.
     */
    private void prepareExitHandler () {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run () {
                Date stopDate = Calendar.getInstance().getTime();
                behavior_comm.sendMessage(stop_context_message);
                behavior_comm.sendMessage(stop_laser_message);

                JSONObject end_log = new JSONObject();
                end_log.setFloat("time", timer.getTime());
                end_log.setString("stop", dateFormat.format(stopDate));
                if (fWriter !=  null) {
                    fWriter.write(end_log.toString());
                }
                println("closing");
            }
      }));
    }
}
