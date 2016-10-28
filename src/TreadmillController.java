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
import processing.core.PGraphics;
import processing.core.PSurface;

//TODO: replace processing.data.JSONObject with json.org.JSONObject
import processing.data.JSONObject;
//TODO: replace processing.data.JSONArray with json.org.JSONObject
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
    float distance;
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
    //int[] reward_locations;
    /** radius around reward locations wich deliminates the reward zone*/
    //int reward_radius;
    /** index of the next reward possible in int[] reward_locations */
    int next_reward;
    /** if true, reward locations change from lap to lap */
    boolean moving_rewards;
    /** pin number for the reward locations */
    int reward_valve;
    /** temporal limit on reward zone */
    //int reward_duration;

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

    /** Read buffer for position messages */
    JSONBuffer position_buffer = new JSONBuffer();

    /** Read buffer for behavior messages */
    JSONBuffer json_buffer = new JSONBuffer();

    /** Date format for logging experiment start/stop */
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TreadmillController(String settings_string, String system_string,
            TrialListener el, boolean useless) {
        this.trialListener = el;
        this.settings_filename = "";
        this.settings_tag = "";
        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
    }

    public TreadmillController(String filename, String tag, TrialListener el) {
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
        
        try {
            fWriter = new FileWriter(system_json.getString("data_directory", "data"), mouse_name);
        } catch (IOException e) {
            return false;
        }

        Date startDate = Calendar.getInstance().getTime();
        
        JSONObject start_log = new JSONObject();
        start_log.setString("mouse", mouse_name);
        start_log.setString("experiment_group", experiment_group);
        start_log.setString("start", dateFormat.format(startDate));

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());

        trialListener.started(fWriter.getFile());

        int toneDuration = settings_json.getInt("tone_duration");
        if (toneDuration > 0) {
            SoundUtils soundUtil = new SoundUtils();
            try {
                soundUtil.tone(settings_json.getInt("tone_freq"),toneDuration);
            } catch(Exception e) {}
        }
        
        vrController.changeScene();
        vrController.setRewards(reward_list.toList());

        startContexts();
        timer.startTimer();
        JSONObject valve_json = open_valve_json(settings_json.getInt("sync_pin"), 100);
        behavior_comm.sendMessage(valve_json.toString());

        started = true;
        return true;
    }

    private JSONObject parseJSONFields(JSONArray fields) {
        JSONObject value = new JSONObject();
        for (int i=0; i<fields.size(); i++) {
            JSONObject setting = fields.getJSONObject(i);
            String key = setting.getString("key");
            String type = setting.getString("type");
            
            if (type.equals("String")) {
                value.setString(key, setting.getString("value"));
            } else if (type.equals("int")) {
                value.setInt(key, setting.getInt("value"));
            } else if (type.equals("float")) {
                value.setDouble(key, setting.getDouble("value"));
            } else if (type.equals("JSONObject")) {
                value.setJSONObject(key, parseJSONFields(
                    setting.getJSONArray("fields")));
            }
        }

        return value;
    }

    private JSONObject mergeJSONFields(JSONObject orig, JSONArray update_fields) {
        for (int i=0; i<update_fields.size(); i++) {
            JSONObject setting = update_fields.getJSONObject(i);
            String key = setting.getString("key");
            String type = setting.getString("type");
            
            if (type.equals("String")) {
                orig.setString(key, setting.getString("value"));
            } else if (type.equals("int")) {
                orig.setInt(key, setting.getInt("value"));
            } else if (type.equals("float")) {
                orig.setDouble(key, setting.getDouble("value"));
            } else if (type.equals("JSONObject")) {
                if (!orig.isNull(key)) {
                    orig.setJSONObject(key, mergeJSONFields(orig.getJSONObject(key),
                        setting.getJSONArray("fields")));
                } else {
                    orig.setJSONObject(key, parseJSONFields(
                        setting.getJSONArray("fields")));
                }
            } else if (type.equals("JSONArray")) {
                orig.setJSONArray(key, setting.getJSONArray("value"));
            }
        }

        return orig;
    }
    
    public void addSettings(String settings) {
        JSONArray new_settings = parseJSONArray(settings);
        for (int i=0; i<new_settings.size(); i++) {
            JSONObject setting = new_settings.getJSONObject(i);
            String key = setting.getString("key");
            String type = setting.getString("type");
            
            if (type.equals("String")) {
                settings_json.setString(key, setting.getString("value"));
            } else if (type.equals("int")) {
                settings_json.setInt(key, setting.getInt("value"));
            } else if (type.equals("float")) {
                settings_json.setDouble(key, setting.getDouble("value"));
            } else if (type.equals("JSONObject")) {
                settings_json.setJSONObject(key,
                    mergeJSONFields(settings_json.getJSONObject(key),
                    setting.getJSONArray("fields")));
            } else if (type.equals("JSONArray")) {
                settings_json.setJSONArray(key, setting.getJSONArray("value"));
            }
        }

        //TODO: diff and only reconfigure if an update is made
        behavior_comm.closeSocket();
        position_comm.closeSocket();

        try {
            reconfigureExperiment();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void RefreshSettings(String settings_string, String system_string,
            boolean useless) {

        this.settings_filename = "";
        this.settings_tag = "";
        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
        RefreshSettings();
    }

    public void RefreshSettings(String filename, String tag) {
        settings_filename = filename;
        settings_tag = tag;
        RefreshSettings();
    }

    public void RefreshSettings(JSONObject settings_json,
            JSONObject system_json) {
        this.settings_json = settings_json;
        this.system_json = system_json;
        RefreshSettings();
    }

    public void RefreshSettings() {
        behavior_comm.closeSocket();
        position_comm.closeSocket();
        reload_settings();
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
        reward_list.shuffle(track_length);
        vrController.setRewards(reward_list.toList());
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


    JSONObject setup_valve_json(int pin, int frequency) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setInt("frequency", frequency);
        valve_subjson.setString("action", "create");
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
        if (settings_json.isNull("sensors")) {
            return;
        }

        JSONArray sensors = settings_json.getJSONArray("sensors");
        for (int i=0; i < sensors.size(); i++) {
            JSONObject create_subjson = sensors.getJSONObject(i);
            if (create_subjson.getString("type", "").equals("lickport") ||
                    (create_subjson.getString("type", "").equals("piezoport"))) {
                lickport_pin = create_subjson.getInt("pin");
            }
            create_subjson.setString("action", "create");
            JSONObject create_json = new JSONObject();
            create_json.setJSONObject("sensors", create_subjson);

            behavior_comm.sendMessage(create_json.toString());
            delay(150);
        }
    }
    

    void configure_context_list(JSONObject context_info) {
        JSONArray disp_color = context_info.getJSONArray("display_color");
        ContextList context = new ContextList(display,
            color(disp_color.getInt(0), disp_color.getInt(1), disp_color.getInt(2)));

        context.setComm(behavior_comm);
        context.setDuration(context_info.getInt("max_duration"));
        context.setRadius(context_info.getInt("radius"));
        JSONArray locations = null;
        try {
            locations = context_info.getJSONArray("locations");
        } catch (RuntimeException e) {
            for (int i=0; i < context_info.getInt("locations"); i++) {
                context.add(0);
            }
            context.shuffle(track_length);
        }

        if (locations != null) {
            for (int i=0; i < locations.size(); i++) {
                context.add(locations.getInt(i));
            }
        }
        display.setContextLocations(context);

        JSONArray valves = context_info.getJSONArray("valves");
        for (int i=0; i < valves.size(); i++) {
            int valve_pin = valves.getInt(i);
            JSONObject valve_json = setup_valve_json(valve_pin);
            behavior_comm.sendMessage(valve_json.toString());
            JSONObject close_json = close_valve_json(valve_pin);
            behavior_comm.sendMessage(close_json.toString());
        }

        JSONArray context_duration = new JSONArray();
        context_info.setString("action", "create");

        JSONObject context_setup_json = new JSONObject();
        context_setup_json.setJSONObject("contexts", context_info);
        behavior_comm.sendMessage(context_setup_json.toString());

        context.setId(context_info.getString("id"));
        contexts.add(context);
    }

    ArrayList<String> startContextMessages;
    ArrayList<String> stopContextMessages;
    ArrayList<ContextList> contexts;
    void configure_contexts() {
        startContextMessages = new ArrayList<String>();
        stopContextMessages = new ArrayList<String>();
        if (settings_json.isNull("contexts")) {
            return;
        }
        
        JSONArray contexts = settings_json.getJSONArray("contexts");
        for (int i=0; i < contexts.size(); i++) {
            JSONObject create_subjson = contexts.getJSONObject(i);

            if (!create_subjson.isNull("locations")) {
                configure_context_list(create_subjson);
            } else {
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
    ContextList laser_list;
    void configure_laser() {
        laser_list.clear();
        if (settings_json.isNull("laser")) {
            laser_pin = -1;
            laser_radius = 0;
            laser_list.setRadius(0);
            laser_list.setDuration(0);

            //display.setContextLocations(laser_list);
            return;
        }

        JSONObject laser_info = settings_json.getJSONObject("laser");

        laser_pin = laser_info.getInt("pin");
        JSONObject valve_json = setup_valve_json(laser_pin);
        behavior_comm.sendMessage(valve_json.toString());
        JSONObject close_json = close_valve_json(laser_pin);
        behavior_comm.sendMessage(close_json.toString());

        laser_list.setComm(behavior_comm);
        laser_list.setDuration(laser_info.getInt("max_duration"));
        laser_list.setRadius(laser_info.getInt("radius"));

        JSONArray locations = laser_info.getJSONArray("locations");
        for (int i=0; i < locations.size(); i++) {
            laser_list.add(locations.getInt(i));
        }

        JSONArray context_valves = new JSONArray();
        context_valves.append(laser_pin);
        JSONArray context_duration = new JSONArray();
        context_duration.append(-1);
        //TODO: reward centered laser locations
        //if (laser_info.getBoolean("reward_centered", false)) {
        //    laser_locations = reward_locations;
        //    laser_on_reward = true;
        //} else {
        laser_on_reward = false;
        display.setContextLocations(laser_list);

        JSONObject context_setup = new JSONObject();
        context_setup.setString("action", "create");
        context_setup.setString("id", "laser_context");
        context_setup.setJSONArray("valves", context_valves);
        context_setup.setJSONArray("durations", context_duration);

        JSONObject context_setup_json = new JSONObject();
        context_setup_json.setJSONObject("contexts", context_setup);
        behavior_comm.sendMessage(context_setup_json.toString());

        laser_list.setId("laser_context");
        contexts.add(laser_list);

        //laser_pin = laser_info.getInt("pin");
        //JSONObject laser_json = setup_valve_json(laser_pin);
        
        /*
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
        behavior_comm.sendMessage(stop_laser_json.toString());
        */
    }


    /**
     * Configures the reward zone contexts and establishes the initial reward zone
     * locations
     */
    //ArrayList<Context> contexts;
    ContextList reward_list;
    void configure_rewards() {
        reward_list.clear();
        if (settings_json.isNull("reward")) {
            reward_valve = -1;
            reward_list.setRadius(0);
            reward_list.setDuration(0);

            //display.setContextLocations(reward_list);
            return;
        }

        JSONObject reward_info = settings_json.getJSONObject("reward");
        reward_valve = reward_info.getInt("pin");
        //reward_duration = reward_info.getInt("max_duration");

        if (!reward_info.isNull("locations")) {
            reward_list.setComm(behavior_comm);
            reward_list.setDuration(reward_info.getInt("max_duration"));
            reward_list.setRadius(reward_info.getInt("radius"));

            if (reward_info.getString("type").equals("fixed")) {
                moving_rewards = false;
                JSONArray locations = reward_info.getJSONArray("locations");
                for (int i=0; i < locations.size(); i++) {
                    reward_list.add(locations.getInt(i));
                }
            } else {
                moving_rewards = true;
                for (int i=0; i < reward_info.getInt("number"); i++) {
                    reward_list.add(0);
                }
                shuffle_rewards();
            }
            display.setContextLocations(reward_list);

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

            reward_list.setId("hidden_reward");
            contexts.add(reward_list);
        }
    }


    void reload_settings(JSONObject settings_json, JSONObject system_json) {
        this.settings_json = settings_json;
        system_json = this.system_json;
        try {
            reconfigureExperiment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void reload_settings(String filename, String tag) {
        try {
          settings_json = loadJSONObject(filename).getJSONObject(tag);
        } catch (Exception e) {
           println(e.toString());
           background(0);
           fill(color(255,0,0));
           textSize(24);
           text("Settings failed to load!", 50, 50);
           text(e.toString(), 50, 80);
           noLoop();
           nodraw = true;
           return;
        }

        system_json = loadJSONObject(filename).getJSONObject("_system");
        try {
            reconfigureExperiment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void reconfigureExperiment() throws Exception {
        //TODO: diff the new settings from the old and only make necessary updates
        contexts = new ArrayList<ContextList>();
        display.resetContexts();
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
        display.setTrackLength(track_length);

        if (!settings_json.isNull("display_controllers")) {
            vrController = new VrController(
                settings_json.getJSONObject("display_controllers"));


            JSONArray scenes = settings_json.getJSONArray("vr_scenes");
            for (int i=0; i < scenes.size(); i++) {
                vrController.addScene(scenes.getString(i));
            }
        } else {
            vrController = new VrController();
        }

        configure_rewards();
        //display.setRewardLocations(reward_locations, reward_radius);
        configure_contexts();
        configure_laser();

        vrController.loadScene("scene0");
        createSchedule();
    }

    void createSchedule() { }

    void reload_settings() {
        try {    
            reconfigureExperiment();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        frameRate(120);

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
        //reward_locations = new int[0];
        position = -1;
        distance = 0;
        lap_count = 0;
        lap_tag = "";
        fWriter = null;
        timer = new ExperimentTimer();

        PGraphics img = createGraphics(100,100);
        display = new Display();
        display.prepGraphics(this);
        
        reward_list = new ContextList(display, color(0, 204, 0));
        laser_list = new ContextList(display, color(0, 204, 204));
        reload_settings();
        prepareExitHandler();

    }


    public void resetLap(String tag, float time) {
        if (distance < 0.5*track_length) {
            return;
        }

        next_reward = 0;
        next_laser = 0;
        distance = 0;
        if (moving_rewards) {
            shuffle_rewards();
        }
        if (started) {
            JSONObject lap_log = new JSONObject();
            lap_log.setFloat("time", time);
            lap_log.setInt("lap", lap_count);
            if (tag.equals("")) {
                lap_log.setString("message", "no tag");
            } else {
                display.setLastLap(position);
                vrController.changeScene();
                display.setLapCount(lap_count);
            }

            vrController.changeScene();
            fWriter.write(lap_log.toString());
            lap_count++;
            display.setLapCount(lap_count);

            //reward_list.reset();
            for (int i=0; i < contexts.size(); i++) {
                contexts.get(i).reset();
            }
        }
    }


    protected float updatePosition(float time) {
        float dy = 0;
        while (position_comm.receiveMessage(json_buffer)) {
            if ((dy != 0) && (started)) {
                fWriter.write(json_buffer.json.toString());
            }

            JSONObject position_json =
                json_buffer.json.getJSONObject(position_comm.address);

            if (!position_json.isNull("position")) {
                dy += position_json.getJSONObject("position").getFloat("dy", 0);
            }
        }

        if (dy == 0) {
            return 0;
        }

        dy /= position_scale;
        distance += dy;
        if (position != -1) {
            if ((position + dy) < 0) {
                position += track_length;
            }
            position += dy;
        }

        if (position > track_length*(1 + lap_tolerance)) {
            position = track_length*lap_tolerance;
            resetLap("", time); 
            distance = position;
        }

        if (started) {
            json_buffer.json.setFloat("y", position);
            json_buffer.json.setFloat("time", time);
            fWriter.write(json_buffer.json.toString());
            vrController.update(position);
        }

        return dy;
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

        float dy = updatePosition(time);

        //boolean laserZone = false;
        if (started) {
            for (int i=0; i < contexts.size(); i++) {
                contexts.get(i).check(position, time);
            }
        }

        
        /*
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
        }*/

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
                    display.setMouseName("ERROR!!!! arduino code is out of date");
                } else if ((valveJson.getInt("pin", -1) == reward_valve) &&          // This check is needed for new arduino syntax
                        valveJson.getString("action", "close").equals("open")) {
                    display.addReward();
                }
            }

            if (!behavior_json.isNull("lap")) {
                display.setMouseName("ERROR!!!! arduino code is out of date");
            }

            if (!behavior_json.isNull("tag_reader") &&
                    !behavior_json.getJSONObject("tag_reader").isNull("tag")) {
                JSONObject tag = behavior_json.getJSONObject("tag_reader");
                String tag_id = tag.getString("tag");
                display.setCurrentTag(tag_id);
                if (tag_id.equals(lap_tag)) {
                    position = 0;
                    resetLap(lap_tag, time);
                }
            }

            if (!behavior_json.isNull("context")) {
                JSONObject context_json = behavior_json.getJSONObject("context");
                if (!context_json.isNull("id")) {
                    String context_id = context_json.getString("id");
                    if (!context_json.isNull("action")) {
                        for (int j=0; j<contexts.size(); j++) {
                            if (contexts.get(j).getId().equals(context_id)) {
                                if (context_json.getString("action").equals("start")) {
                                    contexts.get(j).setStatus("started");
                                } else if (context_json.getString("action").equals("stop")) {
                                    contexts.get(j).setStatus("stopped");
                                }

                                break;
                            }
                        }
                    }
                }
            }

            if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }
        
        display.update(this, dy, position, time, lasering);
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
        for (int i=0; i < contexts.size(); i++) {
            contexts.get(i).stop();
        }
        
        JSONObject end_log = new JSONObject();
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
        createSchedule();
    }

    /**
     * add a function hook to run at shutdown. this code runs of the program is
     * terminated unexpectidly, ensuring that the log files are closed out.
     */
    protected void prepareExitHandler () {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run () {
                Date stopDate = Calendar.getInstance().getTime();
                for (int i=0; i < contexts.size(); i++) {
                    contexts.get(i).stop();
                }

                JSONObject end_log = new JSONObject();
                end_log.setFloat("time", timer.getTime());
                end_log.setString("stop", dateFormat.format(stopDate));
                if (fWriter !=  null) {
                    fWriter.write(end_log.toString());
                }
                vrController.loadScene("scene0");
                println("closing");
            }
      }));
    }
}
