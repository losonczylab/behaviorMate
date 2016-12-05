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

    /**
     * event listener to send messages back to the UI wrapper.
     */
    TrialListener trialListener;

    /**
     * object for writing behavior log - tdml file.
     */
    FileWriter fWriter;

    /**
     * udp client for reveiving position updates
     */
    UdpClient position_comm;

    /**
     * udp clident for behavior updates.
     */
    UdpClient behavior_comm;

    /**
     * object showing the current state of the trial.
     */
    Display display;

    /**
     * timer for converting the start time of the experiment with the start of
     * the current trial.
     */
    ExperimentTimer timer;
    
    /**
     * json object with all the settings related to this trail. Stored at the
     * top of each of the behavior logs
     */
    JSONObject settings_json;
    /**
     * json object with all the settings related to the computer specifically.
     * Not saved when each trial is ran.
     */
    JSONObject system_json;

    /**
     * 1-D position of the mouse along track.
     */
    float position;

    /**
     * distance run since last lap reset (allowed to be negative). Used to check that
     * animal is not backing over reset tag.
     */
    float distance;

    /** 
     * scale to convert position updates from rotary encoder to mm traversed on the
     * track
     */
    float position_scale;

    /**
     * length of the track in mm. "track_length" in settings.
     */
    float track_length;

    /** 
     * RFID tag string to indicate that a lap has been compleded and position
     * should be reset to 0.
     */
    String lap_tag;

    /**
     * force a lap_reset if the position is more then track_length*lap_tolerance past 
     * track_length. Defaults to 0 if lap_reset_tag is not set, 0.99 if not present in
     * settings.json 
     */
    float lap_tolerance;

    /**
     * number of laps the animal has run.
     */
    int lap_count;

    /**
     * length of the trial. signals the trial to end. "trial_length" in the settings
     * file.
     */
    int trial_duration;

    /**
     * Indicates weither the trial has been started. used to evaluate if contexts
     * should be turned on as well if the file writer to be used.
     */
    boolean started = false;

    /**
     * pin number for the reward locations
     */
    int reward_valve;

    /**
     * the pin number that the lickport is attached to.
     */
    int lickport_pin;

    /**
     * Read buffer for position messages
     */
    JSONBuffer position_buffer = new JSONBuffer();

    /**
     * Read buffer for behavior messages
     */
    JSONBuffer json_buffer = new JSONBuffer();

    /**
     * Date format for logging experiment start/stop
     */
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TreadmillController(String settings_string, String system_string,
            TrialListener el) {
        this.trialListener = el;
        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
    }

    public TreadmillController(TrialListener el) {
        this.trialListener = el;
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
        
        if (fWriter != null) {
            fWriter.close();
        }

        try {
            fWriter = new FileWriter(
                system_json.getString("data_directory", "data"),
                mouse_name, this.trialListener);
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
    
    public void addSettings(String settings) throws Exception {
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

        reconfigureExperiment();
    }

    public void RefreshSettings(String settings_string,
            String system_string) throws Exception {

        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
        RefreshSettings();
    }

    public void RefreshSettings(JSONObject settings_json,
            JSONObject system_json) throws Exception {
        this.settings_json = settings_json;
        this.system_json = system_json;
        RefreshSettings();
    }

    public void RefreshSettings() throws Exception {
        if (behavior_comm != null) {
            behavior_comm.closeSocket();
        }

        if (position_comm != null) {
            position_comm.closeSocket();
        }

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
    

    protected ContextList configure_vr(int color_val) throws Exception {
        VrContextList context = new VrContextList(display, color_val);
        JSONObject displays = settings_json.getJSONObject("display_controllers");

        UdpClient[] comms = new UdpClient[displays.size()];
        Iterator<String> itr = displays.keyIterator();
        for (int i=0; itr.hasNext(); i++) {
            JSONObject display_json = displays.getJSONObject(itr.next());
            UdpClient vr_client = new UdpClient(display_json.getString("ip"),
                display_json.getInt("port"));

            JSONObject view_json = new JSONObject();
            view_json.setInt("viewAngle", display_json.getInt("view_angle"));
            view_json.setInt("deflection", display_json.getInt("deflection"));
            JSONObject msg_json = new JSONObject();
            msg_json.setString("data", view_json.toString().replace("\n",""));
            msg_json.setString("type", "cameraSetup");

            vr_client.sendMessage(msg_json.toString());
            comms[i] = vr_client;
        }

        context.setComms(comms);
        return context;
    }

    void configure_context_list(JSONObject context_info) throws Exception {
        int color_val = -1;
        if (!context_info.isNull("display_color")) {
            JSONArray disp_color = context_info.getJSONArray("display_color");
            color_val = color(disp_color.getInt(0),
                disp_color.getInt(1), disp_color.getInt(2));
        } 

        ContextList context = null;
        if (context_info.getString("type", "").equals("vr")) {
            context = configure_vr(color_val);
        } else {
            context = new ContextList(display, color_val);
            context.setComm(behavior_comm);
        }

        context.setDuration(context_info.getInt("max_duration", -1));
        context.setRadius(context_info.getInt("radius", -1));
        JSONArray locations = null;
        try {
            locations = context_info.getJSONArray("locations");
        } catch (RuntimeException e) { }

        if (locations != null) {
            for (int i=0; i < locations.size(); i++) {
                context.add(locations.getInt(i));
            }
            context.setShuffle(false);
        } else {
            int num_contexts = context_info.getInt("locations", 
                    context_info.getInt("number", 0));
            if (num_contexts == 0) {
                context.add((int)(track_length/2.0));
                context.setRadius((int)(track_length/2.0) + 2);
                context.setShuffle(false);
            } else {
                for (int i=0; i < num_contexts; i++) {
                    context.add((int)(track_length/2.0));
                }
                context.setShuffle(true, track_length);
            }
        }
        display.setContextLocations(context);

        JSONArray valves = null;
        if (!context_info.isNull("valves")) {
            valves = context_info.getJSONArray("valves");
        }

        for (int i=0; ((valves != null) && (i < valves.size())); i++) {
            int valve_pin = valves.getInt(i);
            JSONObject valve_json = setup_valve_json(valve_pin);
            behavior_comm.sendMessage(valve_json.toString());
            JSONObject close_json = close_valve_json(valve_pin);
            behavior_comm.sendMessage(close_json.toString());
        }

        context.setId(context_info.getString("id"));
        context_info.setString("action", "create");

        JSONObject context_setup_json = new JSONObject();
        context_setup_json.setJSONObject("contexts", context_info);
        behavior_comm.sendMessage(context_setup_json.toString());

        contexts.add(context);
    }


    ArrayList<ContextList> contexts;
    void configure_contexts() throws Exception {
        if (!settings_json.isNull("contexts")) {
            JSONArray contexts_array = settings_json.getJSONArray("contexts");
            for (int i=0; i < contexts_array.size(); i++) {
                configure_context_list(contexts_array.getJSONObject(i));
            }
        }
    }


    /**
     * Configures the settings to trigger an opto-laser around the reward zone.
     * reward centered laser zone options is NOT currenly supported (TODO: build
     * nested context list class). This is to support legacy settings.json files
     * where laser zones where a seperate entry in settings.json
     */
    void configure_laser() throws Exception {
        JSONObject laser_info = settings_json.getJSONObject("laser");
        int laser_pin = laser_info.getInt("pin");

        JSONArray context_valves = new JSONArray();
        context_valves.append(laser_pin);
        laser_info.setJSONArray("valves", context_valves);

        JSONArray context_duration = new JSONArray();
        context_duration.append(-1);
        laser_info.setJSONArray("durations", context_duration);

        laser_info.setString("id", "laser_context");
        if (laser_info.isNull("display_color")) {
            JSONArray acolor = new JSONArray();
            acolor.append(0);
            acolor.append(204);
            acolor.append(204);
            laser_info.setJSONArray("display_color", acolor);
        }

        if (laser_info.getBoolean("reward_centered", false)) {
            throw new Exception("reward centered not currently supported");
        }

        configure_context_list(laser_info);
    }


    /**
     * Configures the reward zone contexts and establishes the initial reward zone
     * locations. Translates legacy settings.json files to use the new context_list
     * format.
     */
    void configure_rewards() throws Exception {
        JSONObject reward_info = settings_json.getJSONObject("reward");
        if (!reward_info.isNull("id")) {
            String reward_context = reward_info.getString("id");
            JSONArray contexts = settings_json.getJSONArray("contexts");
            for (int i=0; i < contexts.size(); i++) {
                JSONObject context = contexts.getJSONObject(i);
                if (context.getString("id").equals(reward_context)) {
                    JSONArray valve_list = context.getJSONArray("valves");
                    reward_valve = valve_list.getInt(i);
                    return;
                }
            }
        } 

        reward_info.setString("id", "reward");
        reward_valve = reward_info.getInt("pin");

        JSONArray context_valves = new JSONArray();
        context_valves.append(reward_valve);
        reward_info.setJSONArray("valves", context_valves);

        JSONArray context_duration = new JSONArray();
        context_duration.append(reward_info.getInt("drop_size"));
        reward_info.setJSONArray("durations", context_duration);

        if (!reward_info.isNull("type")) {
            String reward_type = reward_info.getString("type");
            if (reward_type.equals("fixed")) {
                reward_info.remove("number");
            } else if (reward_type.equals("moving")) {
                try {
                    reward_info.getJSONArray("locations");
                    reward_info.remove("locations");
                } catch (RuntimeException e) {}
                reward_info.setInt("locations", reward_info.getInt("number", 1));
            }
        }
        reward_info.setString("type", "operant");
        reward_info.setInt("sensor", lickport_pin);
        if (reward_info.isNull("display_color")) {
            JSONArray acolor = new JSONArray();
            acolor.append(0);
            acolor.append(204);
            acolor.append(0);
            reward_info.setJSONArray("display_color", acolor);
        }

        configure_context_list(reward_info);
    }


    void reload_settings(JSONObject settings_json, JSONObject system_json) throws Exception {
        this.settings_json = settings_json;
        system_json = this.system_json;
        reconfigureExperiment();
    }


    void reload_settings(String filename, String tag) throws Exception {
        settings_json = loadJSONObject(filename).getJSONObject(tag);
        system_json = loadJSONObject(filename).getJSONObject("_system");

        reconfigureExperiment();
    }

    void reconfigureExperiment() throws Exception {
        //TODO: diff the new settings from the old and only make necessary updates
        contexts = new ArrayList<ContextList>();
        if (display != null) {
            display.resetContexts();
        }
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
        lap_tag = settings_json.getString("lap_reset_tag", "");
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

        configure_contexts();
        if (!settings_json.isNull("reward")) {
            configure_rewards();
        }

        if (!settings_json.isNull("laser")) {
            configure_laser();
        }

        createSchedule();
    }

    void createSchedule() { }

    void reload_settings() throws Exception {
        reconfigureExperiment();
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
        trial_duration = 0;
        position = -1;
        distance = 0;
        lap_count = 0;
        lap_tag = "";
        fWriter = null;
        timer = new ExperimentTimer();

        PGraphics img = createGraphics(100,100);
        display = new Display();
        display.prepGraphics(this);
        
        position_comm = null;
        behavior_comm = null;
        prepareExitHandler();
        trialListener.initialized();
    }


    public void resetLap(String tag, float time) {
        if (distance < 0.5*track_length) {
            return;
        }

        distance = 0;
        if (started) {
            JSONObject lap_log = new JSONObject();
            lap_log.setFloat("time", time);
            lap_log.setInt("lap", lap_count);
            if (tag.equals("")) {
                lap_log.setString("message", "no tag");
            } else {
                display.setLastLap(position);
                display.setLapCount(lap_count);
            }

            fWriter.write(lap_log.toString());
            lap_count++;
            display.setLapCount(lap_count);

            for (int i=0; i < contexts.size(); i++) {
                contexts.get(i).reset();
            }
        }
    }


    protected float updatePosition(float time) {
        if (position_comm == null) {
            return 0;
        }

        float dy = 0;
        for (int i=0; ((i < 10) && (position_comm.receiveMessage(json_buffer)))
                ;i++) {
            if ((dy != 0) && (started)) {
                fWriter.write(json_buffer.json.toString());
            }

            JSONObject position_json =
                json_buffer.json.getJSONObject(position_comm.address);

            if (!position_json.isNull("position")) {
                dy += position_json.getJSONObject("position").getFloat("dy", 0);
            } else if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }

        dy /= position_scale;
        display.setPositionRate(dy);

        if (dy == 0) {
            return 0;
        }

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
        }

        return dy;
    }

    protected void updateBehavior(float time) {
        if (behavior_comm == null) {
            return;
        }

        for (int i=0; ((i < 10) && (behavior_comm.receiveMessage(json_buffer)))
                ; i++) {

        //if ((behavior_comm != null) &&
        //        ((behavior_comm.receiveMessage(json_buffer)))) {
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

    }

    /** processing function which is looped over continuously. Main logic of the
     * experiment is in the body of this function.
     */
    int display_update = 0;
    int display_rate = 50;
    String[] msg_buffer = {null};
    public void draw() {
        float time = timer.checkTime();;
        if (time > trial_duration) {
            endExperiment();
        }

        float dy = updatePosition(time);

        if (started) {
            for (int i=0; i < contexts.size(); i++) {
                contexts.get(i).check(position, time, msg_buffer);
                if (msg_buffer[0] != null) {
                    fWriter.write(msg_buffer[0]);
                    msg_buffer[0] = null;
                }
            }
        }

        updateBehavior(time);
        
        int t = millis();
        int display_check = t-display_update;
        if (display_check > display_rate) {
            display.update(this, dy, position, time);
            display_update = t;
        }
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
        for (int i=0; i < contexts.size(); i++) {
            contexts.get(i).stop(timer.getTime(), msg_buffer);
            contexts.get(i).reset();

            if (msg_buffer[0] != null) {
                fWriter.write(msg_buffer[0]);
                msg_buffer[0] = null;
            }
        }
        
        JSONObject end_log = new JSONObject();
        end_log.setFloat("time", timer.getTime());
        end_log.setString("stop", dateFormat.format(stopDate));
        fWriter.write(end_log.toString());
        display.setMouseName("");

        started = false;
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
                    contexts.get(i).stop(timer.getTime(), msg_buffer);
                    contexts.get(i).reset();

                    if (msg_buffer[0] != null) {
                        fWriter.write(msg_buffer[0]);
                        msg_buffer[0] = null;
                    }
                }

                if (started) {
                    JSONObject end_log = new JSONObject();
                    end_log.setFloat("time", timer.getTime());
                    end_log.setString("stop", dateFormat.format(stopDate));
                    if (fWriter !=  null) {
                        fWriter.write(end_log.toString());
                        fWriter.close();
                    }
                }

                println("closing");
            }
      }));
    }
}
