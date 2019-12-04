import javax.sound.sampled.*;
import java.net.*;
import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.NullPointerException;
import java.lang.*;

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

    JSONObject[] msg_buffer = {null};

    /**
     * udp client for reveiving position updates
     */
    UdpClient position_comm;

    /**
     * udp clident for behavior updates.
     */
    UdpClient behavior_comm;

    UdpClient reset_comm;

    long last_reset_time;

    //TODO: incorporate behavior_comm and position_comm into this list?
    ArrayList<UdpClient> comms;

    long comms_check_time;

    long comms_check_interval;

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

    float offset_position;

    boolean zero_position_boundary;

    /**
     * distance run since last lap reset (allowed to be negative). Used to check that
     * animal is not backing over reset tag.
     */
    float distance;

    float offset_distance;

    /**
     * scale to convert position updates from rotary encoder to mm traversed on the
     * track
     */
    float position_scale;

    float stored_position_scale;

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

    boolean lock_lap;

    int lick_count;

    /**
     * length of the trial. signals the trial to end. "trial_length" in the settings
     * file.
     */
    int trial_duration;

    int lap_limit;

    boolean position_reset;

    boolean belt_calibration_mode;

    float current_calibration;

    int n_calibrations;

    HashMap<Character, String> commentKeys;

    int lap_offset;

    float lap_correction;

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

    ArrayList<ContextList> contexts;

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

    protected void sendMessages(JSONArray messages, String mouse_name) throws Exception {
        for (int i= 0; i < messages.size(); i++) {
            JSONObject messageInfo = messages.getJSONObject(i);
            UdpClient client = new UdpClient(messageInfo.getString("ip"),
                messageInfo.getInt("port"), messageInfo.getString("id"));
            JSONObject message = messageInfo.getJSONObject("message");
            message.setString("filename", fWriter.getFile().getName());
            message.setString("mouse", mouse_name);
            client.sendMessage(message.toString());
            client.closeSocket();
        }
    }


    public boolean testComms(boolean alert) {
        noLoop();
        System.out.println("testing comms");
        JSONObject test_arduino = new JSONObject();
        test_arduino.setJSONObject("communicator", new JSONObject());
        test_arduino.getJSONObject(
            "communicator").setString("action", "test");
        behavior_comm.sendMessage(test_arduino.toString());

        int i;
        for (i=0; i<50 && !behavior_comm.receiveMessage(json_buffer); i++) {
            delay(20);
        }

        if (i == 50) {
            if (alert) {
                trialListener.alert("Failed to connect to behavior controller");
            }
            behavior_comm.setStatus(false);
            comms_check_interval = 10000;
            loop();
            delay(100);
            return false;
        } else {
            display.setBottomMessage("");
            behavior_comm.setStatus(true);
            comms_check_interval = 60000;
            loop();
            delay(100);
            return true;
        }

    }

    public boolean testComms() {
        return testComms(true);
    }

    /**
     * Starts a new experiment. Linked to clicking the "Start" button on the UI.
     * Creates a New Log file, makes the initial entries,
     * starts the timer, and triggers the sync pin to start
     * imaging.
     */
    public boolean Start(String mouse_name, String experiment_group) {
        if ((position == -1) || mouse_name.equals("") ||
            (experiment_group.equals(""))) {
            return false;
        }

        belt_calibration_mode = false;

        display.setMouseName(mouse_name);
        display.setLickCount(0);
        display.setLapCount(0);
        display.setRewardCount(0);
        lap_count=0;

        if (fWriter != null) {
            fWriter.close();
            System.out.println("FILE WRITER OPEN");
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

        JSONObject info_msg = new JSONObject();
        JSONObject info_sub_msg = new JSONObject();
        info_sub_msg.setString("action", "info");
        info_msg.setJSONObject("communicator", info_sub_msg);

        try {
            JSONObject version_info = loadJSONObject("version.json");
            fWriter.write(version_info.toString());
        } catch (NullPointerException e) {
            println(e);
        }

        JSONObject settings_log = new JSONObject();
        settings_log.setJSONObject("settings", settings_json);
        fWriter.write(settings_log.toString());
        fWriter.write(start_log.toString());

        if (!settings_json.isNull("trial_startup")) {
            try {
                sendMessages(settings_json.getJSONArray("trial_startup"),
                    mouse_name);
            } catch (Exception e) {
                System.out.println(e);
                return false;
            }
        }

        for (ContextList context : contexts) {
            context.trialStart(msg_buffer);
            if (msg_buffer[0] != null) {
                JSONObject log_message = new JSONObject();
                log_message.setJSONObject("behavior_mate", msg_buffer[0]);
                fWriter.write(log_message.toString().replace("\n", ""));
                msg_buffer[0] = null;
            }
        }

        testComms();

        trialListener.started(fWriter.getFile());

        timer.startTimer();
        JSONObject valve_json = open_valve_json(
            settings_json.getInt("sync_pin"), 100);
        behavior_comm.sendMessage(valve_json.toString());
        behavior_comm.sendMessage(info_msg.toString());

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

        delay(100);

        //TODO: diff and only reconfigure if an update is made
        for (UdpClient c : comms) {
            c.closeSocket();
        }
        delay(250);

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
        delay(100);

        for (UdpClient c: comms) {
            c.closeSocket();
        }

        delay(250);

        reload_settings();
    }

    public void setPosition(float new_position) {
        println("setting position");
        position = new_position-lap_offset;
        distance = new_position-lap_offset;

        offset_position = new_position;
        offset_distance = new_position;
    }

    public void ZeroPosition() {
        println("ZERO POSITION");
        this.setPosition(lap_offset);
        //position = 0;
        //offset_position = lap_offset;
        //distance = 0;
        //offset_distance = lap_offset;
    }

    public void CalibrateBelt() {
        belt_calibration_mode = true;
        current_calibration = 0;
        n_calibrations = 0;
        display.setMouseName("New Calibration: " + current_calibration);
    }

    public void EndBeltCalibration() {
        settings_json.setFloat("position_scale", position_scale);
        belt_calibration_mode = false;
        display.setMouseName("");
    }

    public void ResetCalibration() {
        if (position_scale != stored_position_scale) {
            position_scale = stored_position_scale;
            settings_json.setFloat("position_scale", position_scale);
            display.setPositionScale(position_scale);
        }
    }

    /**
     * Tests the valve specified in the Box on the UI. Linked to the TestValve
     * button the UI. Both creates and then opens the valve for the amount of
     * time specified in the duration box.
     */
    public void TestValve(int pin, int duration) {
        println("TEST VALVE");

        //JSONObject valve_json = setup_valve_json(pin);
        //behavior_comm.sendMessage(valve_json.toString());

        JSONObject valve_json = open_valve_json(pin, duration);
        behavior_comm.sendMessage(valve_json.toString());
    }

    /**
     * Generates the JSONObject necessary to create a valve.
     *
     * @param  pin the pin number to setup the valve on
     * @return     the JSONObject which will configure a valve when sent to arduino
     */
    static JSONObject setup_valve_json(int pin) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action", "create");
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    static JSONObject setup_valve_json(int pin, boolean inverted) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action","create");
        if (inverted) {
            valve_subjson.setBoolean("inverted", true);
        }
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    static JSONObject setup_valve_json(int pin, int frequency) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setInt("frequency", frequency);
        valve_subjson.setString("type", "tone");
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
    static JSONObject close_valve_json(int pin) {
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
    static JSONObject open_valve_json(int pin, int duration) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();

        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action","open");
        valve_subjson.setInt("duration",duration);
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Generates the JSONObject necessary to start a tone. Assumes that the
     * tone has already been configured.
     *
     * @param  pin      the pin number of the valve to open
     * @param  duration duration to keep the valve open for (milli-seconds)
     * @param  frequency  frequency of the tone to play (in Hz)
     * @return          the JSONOBject which will open the valve when sent to
     *                  arduino
     */
    static JSONObject open_valve_json(int pin, int duration, int frequency) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();

        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action", "open");
        valve_subjson.setInt("duration", duration);
        valve_subjson.setInt("frequency", frequency);
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Configures the sensors as defined in the settings.json file. All sensors
     * must include pin, type, and report_pin must be specified as needed
     */
    void configure_sensors() {
        JSONObject clear_message = new JSONObject();
        clear_message.setJSONObject("sensors", new JSONObject());
        clear_message.getJSONObject("sensors").setString("action", "clear");
        behavior_comm.sendMessage(clear_message.toString());

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


    /**
     * Configures the reward zone contexts and establishes the initial reward
     * zone locations. Translates legacy settings.json files to use the new
     * context_list format.
     *
     * @throws Exception propagate errors to the UI.
     */
    void configure_rewards() throws Exception {
        JSONObject reward_info = settings_json.getJSONObject("reward");

        JSONArray contexts_array;
        if (settings_json.isNull("contexts")) {
            settings_json.setJSONArray("contexts", new JSONArray());
        }

        contexts_array = settings_json.getJSONArray("contexts");

        if (!reward_info.isNull("id")) {
            String reward_context = reward_info.getString("id");
            for (int i=0; i < contexts_array.size(); i++) {
                JSONObject context = contexts_array.getJSONObject(i);
                if (context.getString("id").equals(reward_context)) {
                    JSONArray valve_list = context.getJSONArray("valves");
                    reward_valve = valve_list.getInt(0);
                    return;
                }
            }
        }

        reward_info.setString("id", "reward");
        JSONObject reward_id = new JSONObject();
        reward_id.setString("id", "reward");
        settings_json.setJSONObject("reward", reward_id);

        if (!reward_info.isNull("pin")) {
            reward_valve = reward_info.getInt("pin");
            JSONArray context_valves = new JSONArray();
            context_valves.append(reward_valve);
            reward_info.setJSONArray("valves", context_valves);
        }

        if (!reward_info.isNull("drop_size")) {
            JSONArray context_duration = new JSONArray();
            context_duration.append(reward_info.getInt("drop_size"));
            reward_info.setJSONArray("durations", context_duration);
        }

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

        contexts_array.append(reward_info);
    }


    void reload_settings(JSONObject settings_json, JSONObject system_json)
            throws Exception {
        this.settings_json = settings_json;
        current_calibration = 0;
        system_json = this.system_json;
        reconfigureExperiment();
    }


    void reload_settings(String filename, String tag) throws Exception {
        settings_json = loadJSONObject(filename).getJSONObject(tag);
        system_json = loadJSONObject(filename).getJSONObject("_system");
        current_calibration = 0;

        reconfigureExperiment();
    }

    void startComms() throws Exception {
        comms = new ArrayList<UdpClient>();
        position_comm = null;
        behavior_comm = null;
        reset_comm = null;
        last_reset_time = 0;

        JSONObject controllers;
        if (!settings_json.isNull("controllers")) {
            controllers = settings_json.getJSONObject("controllers");
        } else {
            controllers = new JSONObject();
        }

        System.out.println(controllers.toString());

        for (Object comm_key_o : controllers.keys()) {
            String comm_key = (String)comm_key_o;
            System.out.println(comm_key);
            JSONObject controller_json = controllers.getJSONObject(comm_key);
            UdpClient comm = new UdpClient(
                controller_json.getString("ip", "127.0.0.1"),
                controller_json.getInt("send_port"),
                controller_json.getInt("receive_port"),
                comm_key);
            controller_json.setString("address", comm.address);
            controllers.setJSONObject(comm_key, controller_json);
            comms.add(comm);
            if (comm_key.equals("position_controller")) {
                position_comm = comm;
            } else if (comm_key.equals("behavior_controller")) {
                behavior_comm = comm;
            } else if (comm_key.equals("reset_controller")) {
                reset_comm = comm;
            }
        }

        for (int i=0; i < contexts.size(); i++) {
            contexts.get(i).setupComms(comms);
        }

        if (position_comm == null) {
            System.out.println("position_comm null");
        }

        if ((behavior_comm != null) && (!started)) {
           testComms();
        }
    }

    void reconfigureExperiment() throws Exception {
        //TODO: diff the new settings from the old and only make necessary updates
        contexts = new ArrayList<ContextList>();
        if (display != null) {
            display.resetContexts();
            display.setSchedule("");
            display.clearValveStates();
            display.clearSensorStates();
        }

        if (!settings_json.getString("lap_reset_tag", "").equals("")) {
            if (!settings_json.getString("lap_reset_tag").equals(lap_tag)) {
                position = -1;
                offset_position = -1;
            }
        } else if ((!position_reset) &&
                (settings_json.getBoolean("position_lap_reader", false))) {
            position = -1;
            offset_position = -1;

        } else if ((position == -1) &&
                (!settings_json.getBoolean("position_lap_reader", false))) {
            position = 0;
            offset_position = lap_offset;
        }

        zero_position_boundary = settings_json.getBoolean(
            "zero_position_boundary", false);

        trial_duration = settings_json.getInt("trial_length", -1);
        display.setTotalTime(trial_duration);
        lap_limit = settings_json.getInt("lap_limit", -1);
        stored_position_scale = settings_json.getFloat("position_scale");
        if (Math.signum(current_calibration) == 0.0) {
            position_scale = settings_json.getFloat("position_scale");
            display.setPositionScale(position_scale);
        } else {
            settings_json.setFloat("position_scale", position_scale);
        }
        track_length = settings_json.getFloat("track_length");
        lap_tag = settings_json.getString("lap_reset_tag", "");
        if (!lap_tag.equals("")) {
            lap_tolerance = settings_json.getFloat("lap_tolerance", 0.99f);
        } else {
            lap_tolerance = 0;
        }
        lap_offset = settings_json.getInt("lap_offset", 0);
        if (settings_json.getBoolean("delay_lap_correction", false)) {
            lap_correction = 0;
        } else {
            lap_correction = -1;
        }
        position_reset = settings_json.getBoolean(
            "position_lap_reader", false);

        JSONObject controllers;
        if (!settings_json.isNull("controllers")) {
            controllers = settings_json.getJSONObject("controllers");
        } else {
            controllers = new JSONObject();
        }

        if (controllers.isNull("behavior_controller")) {
            if (!settings_json.isNull("behavior_controller")) {
                controllers.setJSONObject(
                    "behavior_controller", settings_json.getJSONObject(
                        "behavior_controller"));
                settings_json.remove("behavior_controller");
            }
        }

        if (controllers.isNull("position_controller")) {
            if (!settings_json.isNull("position_controller")) {
                controllers.setJSONObject(
                    "position_controller", settings_json.getJSONObject(
                        "position_controller"));
                settings_json.remove("position_controller");
            }
        }

        settings_json.setJSONObject("controllers", controllers);
        trialListener.setArduinoController(
            system_json.getString("arduino_controller", null),
            controllers.toString());
        startComms();
        configure_sensors();

        JSONObject valve_json = setup_valve_json(
            settings_json.getInt("sync_pin"));
        behavior_comm.sendMessage(valve_json.toString());
        display.setTrackLength(track_length);

        if (!settings_json.isNull("reward")) {
            configure_rewards();
        }

        this.commentKeys = new HashMap<Character, String>();
        if (!settings_json.isNull("comment_keys")) {
            JSONObject quick_comments = settings_json.getJSONObject("comment_keys");
            for (Object key : quick_comments.keys()) {
                Character key_char = ((String)key).charAt(0);
                commentKeys.put(key_char, quick_comments.getString((String)key));
            }
        }

        if (!settings_json.isNull("contexts")) {
            delay(10);
            VrContextList vr_context = null;
            ArrayList<VrCueContextList> cue_lists = new ArrayList<VrCueContextList>();;

            JSONArray contexts_array = settings_json.getJSONArray("contexts");
            for (int i=0; i < contexts_array.size(); i++) {
                JSONObject context_info = contexts_array.getJSONObject(i);
                ContextList context_list = ContextsFactory.Create(
                    this, display, context_info,
                    track_length, behavior_comm,
                    context_info.getString("class", "context"));
                contexts.add(context_list);

                if (!context_list.setupComms(comms)) {
                    trialListener.alert(
                        "Context List: " + context_list.getId() +
                        " failed to connect to comm");
                }
                display.setContextLocations(context_list);
            }

            for (ContextList context : this.contexts) {
                context.registerContexts(this.contexts);
            }
        }
    }

    void reload_settings() throws Exception {
        current_calibration = 0;
        reconfigureExperiment();
    }

    public boolean writeLog(JSONObject log, float time) {
        if (fWriter == null) {
            return false;
        }

        JSONObject log_message = new JSONObject();
        log_message.setJSONObject("behavior_mate", log);
        log_message.setFloat("time", timer.getTime());
        fWriter.write(log_message.toString().replace("\n", ""));

        return true;
    }

    public boolean writeLog(JSONObject log) {
        return writeLog(log, timer.getTime());
    }

    public void addComment(String comment) {
        if (fWriter == null) {
            return;
        }

        JSONObject comment_json = new JSONObject();
        comment_json.setString("comments", comment);
        comment_json.setFloat("time", timer.getTime());
        fWriter.write(comment_json.toString());
    }

    public void addComment(String key, String comment) {
        if (fWriter == null) {
            return;
        }

        JSONObject comment_json = new JSONObject();
        comment_json.setString(key, comment);
        comment_json.setFloat("time", timer.getTime());
        fWriter.write(comment_json.toString());
    }

    public void commentKey(Character key) {
        if (fWriter == null) {
            return;
        }

        if (this.commentKeys.containsKey(key)) {
            String comment = this.commentKeys.get(key);
            JSONObject comment_json = new JSONObject();
            comment_json.setString("_"+key, comment);
            comment_json.setFloat("time", timer.getTime());
            fWriter.write(comment_json.toString());
        }
    }

    public float getTime() {
        return timer.getTime();
    }

    public void writeSettingsInfo(String filename, String key) {
        if (fWriter == null) {
            return;
        }

        JSONObject settings_info = new JSONObject();
        settings_info.setString("settings_file", filename);
        settings_info.setString("settings_key", key);

        fWriter.write(settings_info.toString());
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
        frameRate(1024);

        started = false;
        belt_calibration_mode = false;

        comms_check_interval = 60000;
        comms_check_time = 0;

        current_calibration = 0;
        lap_offset = 0;
        position_reset = false;
        trial_duration = -1;
        lap_limit = -1;
        position = -1;
        offset_position = -1;
        distance = 0;
        offset_distance = 0;
        zero_position_boundary = false;
        lap_count = 0;
        lap_tag = "";
        lock_lap = false;
        fWriter = null;
        timer = new ExperimentTimer();

        PGraphics img = createGraphics(100,100);
        display = new Display();
        display.prepGraphics(this);

        position_comm = null;
        behavior_comm = null;
        comms = new ArrayList<UdpClient>();
        prepareExitHandler();
        trialListener.initialized();
    }

    public void setLapLock(boolean lock_status) {
        lock_lap = lock_status;
    }


    public void resetLap(String tag, float time) {
        if ((started) && (!lock_lap)) {
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


    protected void checkMessages(UdpClient comm, float time) {
        for (int i=0; ((i < 10) && (comm.receiveMessage(json_buffer))); i++) {
            JSONObject message_json =
                json_buffer.json.getJSONObject(comm.id);

            if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            } else {
                System.out.println("WARNING! message received, " +
                                   "trial not recording");
                System.out.println(json_buffer.json.toString());
            }
        }
    }

    public void setPositionScale(float scale) {
        this.position_scale = scale;
        this.display.setPositionScale(scale);
    }

    public float getPositionScale() {
        return this.position_scale;
    }

    protected float updatePosition(float time) {
        if (position_comm == null) {
            System.out.println("null pos comm");
            return 0;
        }

        boolean reset_lap = false;
        float dy = 0;
        JSONObject position_json = null;
        for (int i = 0;
             ((i < 10) && (position_comm.receiveMessage(json_buffer)));
             i++) {
            if ((dy != 0) && (started) && (position_json != null)) {
                fWriter.write(position_json.toString());
            }

            position_json = json_buffer.json.getJSONObject(position_comm.id);

            if (!position_json.isNull("lap_reset")) {
                display.setCurrentTag("", distance-track_length);
                if (position_reset) {
                    reset_lap = true;
                    break;
                    //if (distance > 0.5*track_length) {
                    /*
                    if (position != -1) {
                        float _lap_correction = track_length - distance;
                        distance = 0;
                        System.out.println(_lap_correction);

                        float per_error = _lap_correction/track_length;
                        System.out.println(per_error);
                        position_scale = position_scale*per_error;
                        System.out.println(position_scale);
                        System.out.println("");
                        display.setCurrentTag(""+position_scale);
                    }
                    if (lap_correction != -1) {
                        System.out.println(lap_correction);
                    } else {*/
                        //position = track_length + lap_offset;
                        //distance = track_length + lap_offset;
                        //}
                   /*     dy = 0;
                        distance = 0;
                        if (position > 0.5*track_length) {
                            resetLap("", time);
                        }
                        position = 0;
                    } else if (position == -1) {
                        position = 0;
                        distance = 0;
                        dy = 0;
                    }*/
                }
            }

            if (!position_json.isNull("position")) {
                dy += position_json.getJSONObject("position").getFloat("dy", 0);
            } else if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }

        dy /= position_scale;
        display.setPositionRate(dy);

        if ((dy == 0) && (!reset_lap)) {
            return 0;
        }

        distance += dy;
        offset_distance += dy;
        if ((position != -1) && (!((zero_position_boundary) && (position + dy < 0)))) {
            if ((position + dy) < 0) {
                position += track_length;
            }
            position += dy;
            //offset_position = position + lap_offset;
            offset_position += dy;
            if (offset_position >= track_length) {
                offset_position -= track_length;
                if (offset_distance > track_length/2) {
                    resetLap("", time);
                    offset_distance = 0;
                }
            } else if (offset_position < 0 ) {
                offset_position += track_length;
            }
        }

        //if (distance > track_length) {
            //distance = track_length*lap_tolerance;
            //position = distance + lap_offset;
        //}

        if (reset_lap) {
            if (position == -1) {
                position = 0;
                offset_position = lap_offset;
                distance = 0;
            // check that this is a legitimate lap reset read
            } else if (distance > track_length/2) {

                // position == -1 means that the lap reader has been initialized
                // since BehviorMate was started yet.
                if (belt_calibration_mode) {
                    current_calibration = (
                        (current_calibration * n_calibrations) +
                        position_scale*(1+(distance-track_length)/track_length)
                        )/(++n_calibrations);
                    position_scale = current_calibration;
                    display.setPositionScale(position_scale);
                    display.setMouseName(
                        "New Calibration: "+ current_calibration +
                        "\nLap Error: " + (distance-track_length));
                }

                // check to see if the lap number needs to be updated
                if (offset_distance >= track_length/2) {
                    resetLap("", time);
                    offset_distance = 0;
                }
                position = 0;
                offset_position = lap_offset;
                distance = 0;
            }
        } else if (position >= track_length) {
            position -= track_length;
        }

        if (started) {
            json_buffer.json.setFloat("y", offset_position);
            json_buffer.json.setFloat("time", time);
            fWriter.write(json_buffer.json.toString());
        }

        return dy;
    }

    protected void updateBehavior(float time) {
        if (behavior_comm == null) {
            return;
        }

        for (int i=0; ((i < 10) && (behavior_comm.receiveMessage(json_buffer)));
                i++) {

            if (!behavior_comm.getStatus()) {
                behavior_comm.setStatus(true);
                display.setBottomMessage("");
            }

            JSONObject behavior_json =
                json_buffer.json.getJSONObject(behavior_comm.id);

            if (!behavior_json.isNull("lick")) {
                if (behavior_json.getJSONObject("lick")
                        .getString("action", "stop").equals("start")) {
                    display.addLick(started);
                    lick_count++;
                }
            }

            if (!behavior_json.isNull("valve")) {
                JSONObject valveJson = behavior_json.getJSONObject("valve");
                int valve_pin = valveJson.getInt("pin", -1);
                if (valve_pin == reward_valve) {
                    if (valveJson.getString("action", "close").equals("open")) {
                        display.addReward();
                    }
                } else if (valve_pin != -1) {
                    if (valveJson.getString("action", "close").equals("open")) {
                        display.setValveState(valve_pin, 1);
                    } else {
                        display.setValveState(valve_pin, -1);
                    }
                }
            } else if (!behavior_json.isNull("tone")) {
                JSONObject valveJson = behavior_json.getJSONObject("tone");
                int valve_pin = valveJson.getInt("pin", -1);
                if (valve_pin != -1) {
                    if (valveJson.getString("action", "close").equals("open")) {
                        display.setValveState(valve_pin, 1);
                    } else {
                        display.setValveState(valve_pin, -1);
                    }
                }
            } else if (!behavior_json.isNull("sensor")) {
                JSONObject sensorJson = behavior_json.getJSONObject("sensor");
                int sensor_pin = sensorJson.getInt("pin", -1);
                if (sensor_pin != -1) {
                    if (sensorJson.getString("action", "stop").equals("start")) {
                        display.setSensorState(sensor_pin, 1);
                    } else {
                        display.setSensorState(sensor_pin, -1);
                    }
                }
            }


            if (!behavior_json.isNull("tag_reader") &&
                    !behavior_json.getJSONObject("tag_reader").isNull("tag")) {
                JSONObject tag = behavior_json.getJSONObject("tag_reader");
                String tag_id = tag.getString("tag");
                display.setCurrentTag(tag_id, distance-track_length);
                if (tag_id.equals(lap_tag)) {
                    position = 0;
                    offset_position = lap_offset;
                    resetLap(lap_tag, time);
                }
            }

            if (!behavior_json.isNull("error")) {
                if (started) {
                    trialListener.alert(
                        "Behavior Controller: " + behavior_json.getString("error"));
                } else {
                    trialListener.exception(
                        "Behavior Controller: " + behavior_json.getString("error"));
                }
            }

            if (!behavior_json.isNull("context")) {
                JSONObject context_json = behavior_json.getJSONObject("context");
                if (!context_json.isNull("id")) {
                    String context_id = context_json.getString("id");
                    if (!context_json.isNull("action")) {
                        for (int j=0; j < contexts.size(); j++) {
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

            if (!behavior_json.isNull("starting")) {
                System.out.println("RESETTING COMMS");
                try {
                    configure_sensors();
                    JSONObject valve_json = setup_valve_json(
                        settings_json.getInt("sync_pin"));
                    behavior_comm.sendMessage(valve_json.toString());
                    for (int j=0; j<contexts.size(); j++) {
                        if (contexts.get(j).getComm() == behavior_comm) {
                            contexts.get(j).sendCreateMessages();
                        }
                    }
                } catch (Exception e) {}
            }

            if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            }
        }

        long _millis = millis();
        if ((!started) && (_millis > (comms_check_time + comms_check_interval))) {
            testComms(false);
            comms_check_time = _millis;
        }

        if (!behavior_comm.getStatus()) {
            display.setBottomMessage("Behavior Controller Disconnected");

            if (_millis > (last_reset_time + 3000))
            {
                last_reset_time = _millis;
                resetArduino(true);
            }
        }

    }

    /** processing function which is looped over continuously. Main logic of the
     * experiment is in the body of this function.
     */
    int display_update = 0;
    int display_rate = 50;
    public void draw() {
        float time = timer.checkTime();
        if ((trial_duration != -1) && (time > trial_duration)) {
            endExperiment();
        }

        if ((lap_limit != -1) && (lap_count > lap_limit)) {
            endExperiment();
        }

        float dy = updatePosition(time);

        if (started) {
            for (int i=0; i < contexts.size(); i++) {
                contexts.get(i).check(offset_position, time, lap_count,
                                      lick_count, msg_buffer);
                if (msg_buffer[0] != null) {
                    this.writeLog(msg_buffer[0], time);
                    //JSONObject log_message = new JSONObject();
                    //log_message.setJSONObject("behavior_mate", msg_buffer[0]);
                    //log_message.setFloat("time", time);
                    //fWriter.write(log_message.toString().replace("\n", ""));
                    msg_buffer[0] = null;
                }
            }
        }

        updateBehavior(time);

        for (UdpClient c: comms) {
            if ((c != behavior_comm) && (c != position_comm)) {
                checkMessages(c, time);
            }

        }

        int t = millis();
        int display_check = t-display_update;
        if (display_check > display_rate) {
            display.update(this, dy, offset_position, time);
            display_update = t;
        }
    }

    public void resetArduino(boolean hardware_reset) {
        JSONObject resetArduino = new JSONObject();
        resetArduino.setJSONObject("communicator", new JSONObject());
        resetArduino.getJSONObject("communicator").setString("action", "reset");
        for (int i=0; i < contexts.size(); i++) {
            UdpClient comm = contexts.get(i).getComm();
            if ((comm != null) && (comm.getId().equals("behavior_controller"))) {
                contexts.get(i).setStatus("resetting");
            }
        }

        if ((reset_comm != null) && (hardware_reset)) {
            reset_comm.sendMessage(resetArduino.toString());
        } else {
            behavior_comm.sendMessage(resetArduino.toString());
        }
    }

    public void resetArduino() {
        resetArduino(false);
    }

    public void resetComms() {
        delay(100);

        for (UdpClient c : comms) {
            c.closeSocket();
        }

        delay(250);

        try {
            startComms();
        } catch(Exception e) {
            e.printStackTrace();
        }

        resetArduino(true);
    }

    /**
     * End the current experiment, reset the state to await the next trial
     */
    public void endExperiment() {
        if (!started) {
            return;
        }

        Date stopDate = Calendar.getInstance().getTime();
        display.setMouseName("");
        trialListener.ended();
        started = false;
        for (int i=0; i < contexts.size(); i++) {
            contexts.get(i).stop(timer.getTime(), msg_buffer);
            contexts.get(i).end();

            if (msg_buffer[0] != null) {
                //fWriter.write(msg_buffer[0].toString().replace("\n", ""));
                this.writeLog(msg_buffer[0]);
                msg_buffer[0] = null;
            }
        }

        System.out.println("writing end log");
        JSONObject end_log = new JSONObject();
        end_log.setFloat("time", timer.getTime());
        end_log.setString("stop", dateFormat.format(stopDate));
        fWriter.write(end_log.toString());
        fWriter.close();

        lap_count = 0;
        lick_count = 0;

        if (!settings_json.isNull("trial_shutdown")) {
            try {
                sendMessages(settings_json.getJSONArray("trial_shutdown"), "");
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        timer = new ExperimentTimer();
        resetArduino();
        if (!settings_json.getBoolean("disable_end_dialog", false)) {
            trialListener.showDeleteDialog(fWriter.getFile().getPath());
        }
    }

    /**
     * add a function hook to run at shutdown. this code runs of the program is
     * terminated unexpectidly, ensuring that the log files are closed out.
     */
    protected void prepareExitHandler () {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run () {
                Date stopDate = Calendar.getInstance().getTime();
                started = false;
                for (int i=0; i < contexts.size(); i++) {
                    contexts.get(i).stop(timer.getTime(), msg_buffer);
                    contexts.get(i).reset();
                    contexts.get(i).shutdown();

                    if (msg_buffer[0] != null) {
                        fWriter.write(
                            msg_buffer[0].toString().replace("\n", ""));
                        msg_buffer[0] = null;
                    }
                }
                contexts = new ArrayList<ContextList>();

                if (started) {
                    JSONObject end_log = new JSONObject();
                    end_log.setFloat("time", timer.getTime());
                    end_log.setString("stop", dateFormat.format(stopDate));
                    if (fWriter !=  null) {
                        fWriter.write(end_log.toString());
                        fWriter.close();
                    }
                }

                trialListener.shutdown();
            }
      }));
    }
}
