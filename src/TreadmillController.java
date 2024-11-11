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

    /**
     * The object to be wrapped.
     */
    public JSONObject json;
}

/**
 * Singleton to contain the main experimental logic. See:
 <code>void setup()</code> for startup routine and <code>void draw()</code>
 * for main event loop
 */
public class TreadmillController extends PApplet {

    /**
     * Next time (ms) to refresh the UI
     **/
    int display_update = 0;

    /**
     * Interval (ms) between UI refreshes
     **/
    int display_rate = 50;

    /**
     * Event listener to send messages back to the UI wrapper.
     */
    TrialListener trialListener;

    /**
     * Used to write to the behavior log and to the .tdml file.
     */
    FileWriter fWriter;

    /**
     * Buffer to pass messages from <code>ContextList</code> objects to the
     * .tdml file
     */
    JSONObject[] msg_buffer = {null};

    /**
     * Used to receive position updates.
     */
    UdpClient position_comm;

    /**
     * Used to receive behavior updates.
     */
    UdpClient behavior_comm;

    /**
     * Address to a separate circuit which can perform a hard reset on the
     * behavior controller if comms are disconnected (not used/needed for
     * most setups)
     */
    UdpClient reset_comm;

    /**
     * Time of last request to reset behavior controller if comms are
     * disconnected
     */
    long last_reset_time;

    //TODO: switch to HashMap and remove separate pointers to position,
    // behavior, and reset controllers
    /**
     * List of comms with open Udp ports
     */
    ArrayList<UdpClient> comms;

    // List to store behavior comms that haven't confirmed creation. Used to verify
    // system is ready to start an experiment
    ArrayList<String> unset_contexts;

    /**
     * Time (ms) of the last comms check
     */
    long comms_check_time;

    /**
     * Interval between sending out comms check messages to the behavior
     * controller. Only between trials, not during. Is reduced if a comms
     * check fails.
     */
    long comms_check_interval;

    /**
     * Used to show the current state of the trial.
     */
    Display display;

    /**
     * Timer to indicate the current time since trial was started.
     */
    ExperimentTimer timer;

    /**
     * JSONObject with all the settings related to this trail. Stored at the
     * top of each of the behavior logs
     */
    JSONObject settings_json;

    /**
     * Contains all settings related to the system BehaviorMate is being run on. This is not saved
     * when trials are run.
     */
    JSONObject system_json;

    //TODO: rename offset_position->position since this is confusing and remove
    // this
    /**
     * 1-D position of the mouse along track in millimeters relative to the
     * <code>lap_offset</code>
     */
    float position;

    /**
     * Actual 1-D position of the mouse along track in millimeters (this is the
     * position displayed in the UI)
     */
    float offset_position;

    /**
     * place a hard boundary at position 0 so the mouse won't accumulate
     * distance run in the backwards (generally <code>true</code> for VR and
     * <code>false</code> for circular treadmill)
     */
    boolean zero_position_boundary;

    // TODO: remove distance or offset_distance, having both is redundant and
    // confusing.
    /**
     * Distance run since last lap reset (allowed to be negative). Used to
     * ensure animal is not backing over reset tag.
     */
    float distance;

    /**
     * Distance run since last lap reset (allowed to be negative). Used to
     * ensure animal is not backing over reset tag. Offset by lap_offset
     */
    float offset_distance;

    /**
     * Used to convert position updates from rotary encoder ticks to millimeters
     * traversed along the track. units: ticks / mm
     */
    float position_scale;

    /**
     * <code>position_scale</code> as originally loaded from the settings file
     * (used if a calibration is run and then needed to be reset)
     */
    float stored_position_scale;

    /**
     * The length of the track in millimeters. Set to the "track_length"
     * property in the settings file.
     */
    float track_length;

    /**
     * RFID tag string indicating that a lap has been completed and position
     * should be reset to 0.
     */
    String lap_tag;

    /**
     * A lap_reset will be forced if
     * <tt>position</tt> + <tt>lap_tolerance</tt>*<tt>track_length</tt> >  <tt>track_length</tt>.
     * Defaults to 0.99 if the <tt>lap_reset_tag</tt> property is not set in
     * the settings file.
     */
    float lap_tolerance;

    /**
     * Number of laps completed by the test animal.
     */
    int lap_count;

    /**
     * Prevent laps from counting (used if there's an ITI forced by Context or
     * Decorator.)
     */
    boolean lock_lap;

    /**
     * Licks since the start of the last trial
     */
    int lick_count;

    /**
     * Map of sensor trigger counts since the last time a trial was started
     */
    HashMap<Integer, Integer> sensor_counts;

    /**
     * Length of the trial (s). Used to determine when the trial should end. Set
     * to the value of the <tt>trial_length</tt> property in the settings file.
    **/
    int trial_duration;

    /**
     * Force trial end after lap_limit is reached. -1 if no limit is placed on
     * the lap count
     */
    int lap_limit;

    /**
     * <tt>true</tt> indicates that the lap reset device is connected to the
     * <tt>position_controller</tt>
     */
    boolean position_reset;

    /**
     * <tt>true</tt> indicates that the UI is current preforming a belt
     * calibration. Only for treadmill systems with a lap reset device
     * connected.
     */
    boolean belt_calibration_mode;

    /**
     * Current value of the calibration. Used to calculate a rolling average
     * during a treadmill belt calibration.
     */
    float current_calibration;

    /**
     * Number of laps in the current treadmill belt calibration. Used to
     * calculate a rolling average during a treadmill belt calibration.
     */
    int n_calibrations;

    /**
     * Map of keyboard keys to comments. If comment keys are specified in the
     * settings write the mapped message on key presses.
     */
    HashMap<Character, String> commentKeys;

    /**
     * Position to set following a lap reset.
     */
    int lap_offset;

    /**
     * TODO: This might no longer be used.
     */
    float lap_correction;

    /**
     * A value of <code>indicates</code> the trial has started. Used to determine when contexts
     * should be enabled and if the file writer should be used.
     */
    boolean started = false;

    /**
     * Pin number for the reward valve.
     */
    int reward_valve;

    /**
     * Pin number lickport is attached to.
     */
    int lickport_pin;

    /**
     * Read buffer for position messages.
     */
    JSONBuffer position_buffer = new JSONBuffer();

    /**
     * Read buffer for behavior messages.
     */
    JSONBuffer json_buffer = new JSONBuffer();

    /**
     * Date format for logging experiment start/stop
     */
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * List of all context objects currently being checked.
     */
    ArrayList<ContextList> contexts;

    /**
     * Constructor for the treadmill controller object.
     *
     * @param settings_string JSON formatted string containing experiment
     *                        specific settings
     * @param system_string   JSON formatted string containing system specific
     *                        settings
     * @param el              pointer to the UI wrapper event listener
     */
    public TreadmillController(String settings_string, String system_string,
                               TrialListener el) {
        this.trialListener = el;
        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
    }

    /**
     * Constructor for the treadmill controller object.
     *
     * @param el pointer to the UI wrapper event listener
     */
    public TreadmillController(TrialListener el) {
        this.trialListener = el;
    }

    /**
     *
     * @return The pin number for the reward valve.
     */
    public int getRewardPin() {
        return reward_valve;
    }

    /**
     * Send a list of arbitrary messages to addresses NOT listed in the
     * settings <code>controllers</code>. May be used at trial start and stop to
     * communicate with third-party devices.
     *
     * @param messages List of JSONObject messages with addresses to send.
     *                 Each message may have an <code>ip</code> field to
     *                 specify the destination IP address (defaults to
     *                 <code>localhost</code>) and a <code>port</code>,
     *                 along with the <code>message</code>which contains the
     *                 text to send.
     * @param mouse_name Current mouse_name from the UI
     * @throws Exception Displays errors to the UI
     */
    protected void sendMessages(JSONArray messages, String mouse_name)
            throws Exception {
        for (int i= 0; i < messages.size(); i++) {
            JSONObject messageInfo = messages.getJSONObject(i);
            UdpClient client = new UdpClient(
                    messageInfo.getString("ip", "127.0.0.1"),
                    messageInfo.getInt("port"),
                    messageInfo.getString("temp_client"));
            JSONObject message = messageInfo.getJSONObject("message");
            message.setString("filename", fWriter.getFile().getName());
            message.setString("mouse", mouse_name);
            client.sendMessage(message.toString());
            client.closeSocket();
        }
    }

    /**
     * Send a test message to the behavior controller and wait for reply. Note:
     * this method temporarily stops the event loop from processing to wait for
     * the response.
     *
     * @param alert if <code>true</code> display an alert popup to the ui
     * @return <code>true</code> if test message is acknowledged by the behavior
     *         controller, <code>false</code> otherwise
     */
    public boolean testComms(boolean alert) {
        if (behavior_comm == null) {
            System.out.println(
                "Warning: testComs failed since behavior controller is not defined");
            return false;
        }
        noLoop();
        System.out.println("testing comms");
        JSONObject test_arduino = new JSONObject();
        test_arduino.setJSONObject("communicator", new JSONObject());
        test_arduino.getJSONObject("communicator").setString("action", "test");
        behavior_comm.sendMessage(test_arduino.toString());

        int i;
        for (i = 0; i<50 && !behavior_comm.receiveMessage(json_buffer); i++) {
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

    /**
     * See <code>testComms(boolean alert)</code>. Performs comms test with popup
     * suppressed
     *
     */
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
        if ((position == -1) || mouse_name.equals("") || (experiment_group.equals(""))) {
            return false;
        }

        belt_calibration_mode = false;

        display.setMouseName(mouse_name);
        display.setLickCount(0);
        display.setLapCount(0);
        display.setRewardCount(0);
        display.resetFreeRewardCount();
        lap_count=0;

        if (fWriter != null) {
            fWriter.close();
            System.out.println("FILE WRITER OPEN");
        }

        try {
            String directory = system_json.getString("data_directory", "data");
            fWriter = new FileWriter(directory, mouse_name, this.trialListener);
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

        if (unset_contexts.size() > 0) {
            trialListener.alert("Failed to create Contexts\n" + String.join(", ", unset_contexts));
        }

        trialListener.started(fWriter.getFile());

        timer.startTimer();
        if (behavior_comm != null) {
            JSONObject valve_json = open_valve_json(
                settings_json.getInt("sync_pin"), 100);
            behavior_comm.sendMessage(valve_json.toString());
            behavior_comm.sendMessage(info_msg.toString());
        }

        started = true;
        return true;
    }

    /**
     * For parsing a form into a JSONObject.
     *
     * @param fields Array of JSONObjects each with 3 keys:
                     <code>key</code>: key to be converted to JSON
                     <code>value</code>: value to be converted to JSON
                     <code>type</code>: String containing the datatype of the
                     data stored in this field.
                     Valid types: <tt>[</tt>"int", "float", "String",
                                       "JSONObject"<tt>]</tt>
     * @return       JSONObject reflected the entries in the form data
     */
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

    /**
     * Update fields in a JSONObject from form data stored in a JSONArray.
     * Replace existing/add new. See <tt>parseJSONFields</tt> method for
     * description of JSONArray form data.
     *
     * @param orig          the original JSONObject to be updated
     * @param update_fields the form data
     * @return              the updated JSONObject
     */
    private JSONObject mergeJSONFields(JSONObject orig,
                                       JSONArray update_fields) {
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
                    orig.setJSONObject(
                        key, mergeJSONFields(orig.getJSONObject(key),
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

    /**
     * Update the current settings with additional parameters. Overwrites
     * currently loaded keys and adds new ones.
     *
     * @param settings   JSON formatted string with new/additional settings
     *                   values to update the current configuration
     * @throws Exception Displays any error to the UI
     */
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

    /**
     * Set the current configuration.
     *
     * @param settings_string JSON formatted string with experiment specific
                              configurations
     * @param system_string   JSON formatted string with the system specific
                                   settings information.
     * @throws Exception      Displays errors to the UI
     */
    public void RefreshSettings(String settings_string,
                                String system_string) throws Exception {

        this.settings_json = parseJSONObject(settings_string);
        this.system_json = parseJSONObject(system_string);
        RefreshSettings();
    }

    /**
     * Set the current configuration.
     *
     * @param settings_string JSONObject with experiment specific configurations
     * @param system_string   JSONObject with the system specific settings
     *                        information.
     * @throws Exception      Displays errors to the UI
     */
    public void RefreshSettings(JSONObject settings_json,
                                JSONObject system_json) throws Exception {
        this.settings_json = settings_json;
        this.system_json = system_json;
        RefreshSettings();
    }

    /**
     * Reset comms and reload all settings.
     *
     * @throws Exception throws any exceptions from loading the new settings
     *                   files to be displayed as a UI popup
     */
    public void RefreshSettings() throws Exception {
        delay(100);

        for (UdpClient c: comms) {
            c.closeSocket();
        }

        delay(250);
        reload_settings();
    }

    /**
     * Force the UI's position to a specific offset from the start of a lap.
     *
     * @param new_position position to set the UI to.
     */
    public void setPosition(float new_position) {
        println("setting position");
        position = new_position-lap_offset;
        distance = new_position-lap_offset;

        offset_position = new_position;
        offset_distance = new_position;
    }

    /**
     * Set the position to the <code>lap_offset</code> (which defaults to 0).
     */
    public void ZeroPosition() {
        println("ZERO POSITION");
        this.setPosition(lap_offset);
    }

    /**
     * Start a position scale calibration. Only for treadmill systems with a
     * lap reset device configured.
     */
    public void CalibrateBelt() {
        belt_calibration_mode = true;
        current_calibration = 0;
        n_calibrations = 0;
        display.setMouseName("New Calibration: " + current_calibration);
    }

    /**
     * Exit from position scale calibration mode.
     */
    public void EndBeltCalibration() {
        settings_json.setFloat("position_scale", position_scale);
        belt_calibration_mode = false;
        display.setMouseName("");
    }

    /**
     * Clear the current position scale calibration and reset to the value
     * specified in the settings file.
     */
    public void ResetCalibration() {
        if (position_scale != stored_position_scale) {
            position_scale = stored_position_scale;
            settings_json.setFloat("position_scale", position_scale);
            display.setPositionScale(position_scale);
        }
    }

    /**
     * Tests the valve specified in the UI text field. Linked to the TestValve
     * button in the UI. Creates then opens the valve for the amount of time
     * specified in the duration box.
     */
    public void TestValve(int pin, int duration) {
        println("TEST VALVE");

        JSONObject valve_json = open_valve_json(pin, duration);
        behavior_comm.sendMessage(valve_json.toString());

        if (pin == getRewardPin()) {
            this.display.incrementFreeRewardCount();
        }

        if ((started) && (fWriter != null)) {
            JSONObject comment_json = new JSONObject();
            comment_json.setJSONObject("behavior_mate", new JSONObject());
            comment_json.getJSONObject("behavior_mate").setJSONObject(
                    "test_valve", new JSONObject());
            comment_json.getJSONObject("behavior_mate").getJSONObject(
                "test_valve").setInt("pin", pin);
            comment_json.getJSONObject("behavior_mate").getJSONObject(
                "test_valve").setInt("duration", duration);
            comment_json.setFloat("time", timer.getTime());
            fWriter.write(comment_json.toString());

        }
    }

    /**
     * Generates the JSONObject necessary to create a valve.
     *
     * @param  pin Pin number to set up the valve on.
     * @return     JSONObject the arduino will use to configure the valve.
     */
    static JSONObject setup_valve_json(int pin) {
        JSONObject valve_json = new JSONObject();
        JSONObject valve_subjson = new JSONObject();
        valve_subjson.setInt("pin", pin);
        valve_subjson.setString("action", "create");
        valve_json.setJSONObject("valves", valve_subjson);

        return valve_json;
    }

    /**
     * Generates the JSONObject necessary to create a valve.
     *
     * @param pin      arduino pin number to setup the valve on
     * @param inverted if <code>true</code> configure the pin to stay HIGH
     *                 unless the <code>open</code> message is sent, on open
     *                 set to low (this is inverted from the default behavior).)
     * @return         JSONObject the arduino will use to configure the valve.
     */
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

    /**
     * Generates the JSONObject necessary to configure a pine to play a tone.
     *
     * @param pin       arduino pin number with speaker for tone to play on
     * @param frequency frequency (Hz) for the tone
     * @return          JSONObject the arduino will use to configure the tone.
     */
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
     * @param  pin Pin number of the valve to close.
     * @return     JSONObject the arduino will use to close the valve.
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
     * Generates the JSONObject necessary to open a valve. Assumes valve has
     * already been configured.
     *
     * @param  pin      Pin number of the valve to open.
     * @param  duration Amount of time to keep the valve open in milliseconds.
     * @return          JSONObject the arduino will use to open the valve.
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
     * Generates the JSONObject necessary to start a tone. Assumes tone has
     * already been configured.
     *
     * @param  pin        Pin number of the valve to open.
     * @param  duration   Amount of time to keep the valve open in milliseconds
     *                    (ms).
     * @param  frequency  Frequency of the tone to play in Hertz (hz).
     * @return            JSONObject the arduino will use to open the valve.
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
     * Configures sensors defined in the settings file. For all sensors the
     * following must be specified: pin, type, report_pin,
     */
    void configure_sensors() {
        // exit immediately if there is no behavior controller or if there are
        // no sensors specified in the settings
        if ((behavior_comm == null) || settings_json.isNull("sensors")) {
            return;
        }

        JSONObject clear_message = new JSONObject();
        clear_message.setJSONObject("sensors", new JSONObject());
        clear_message.getJSONObject("sensors").setString("action", "clear");
        behavior_comm.sendMessage(clear_message.toString());

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
     * zone locations. Converts legacy settings files to use the new
     * context_list format.
     *
     * @throws Exception Displays errors to the UI.
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
                    if (context.getString("type").equals("operant")) {
                        lickport_pin = context.getInt("sensor");
                    }
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

        if (!reward_info.isNull("sensor")) {
            lickport_pin = reward_info.getInt("sensor");
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
                reward_info.setInt("locations",
                                   reward_info.getInt("number", 1));
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

    /**
     * Set the settings and reconfigure the experiment
     *
     * @param settings_json settings specifically related to the experiment
     * @param system_json   settings specifically related to the system/PC
     *                      running behaviorMate
     * @throws Exception    Display errors to the UI
     */
    void reload_settings(JSONObject settings_json,
                         JSONObject system_json) throws Exception {
        this.settings_json = settings_json;
        current_calibration = 0;
        system_json = this.system_json;
        reconfigureExperiment();
    }


    /**
     * Set the settings from a settings file and reconfigure the experiment
     *
     * @param filename   name of the file with the JSON formatted settings
     * @param tag        key in the settings file to load
     * @throws Exception Display errors to the UI
     */
    void reload_settings(String filename, String tag) throws Exception {
        settings_json = loadJSONObject(filename).getJSONObject(tag);
        system_json = loadJSONObject(filename).getJSONObject("_system");
        current_calibration = 0;

        reconfigureExperiment();
    }

    /**
     * Initialize comms with the controllers specified in the settings file.
     *
     * @throws Exception Display errors in the UI
     */
    void startComms() throws Exception {
        comms = new ArrayList<UdpClient>();
        unset_contexts = new ArrayList<String>();
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

    /**
     * Erase current configuration and reload based on the current stored
     * settings information.
     *
     * @throws Exception Display errors in the UI
     */
    void reconfigureExperiment() throws Exception {
        //TODO: diff the new settings from the old and only make necessary
        // updates

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

        if (behavior_comm != null) {
            JSONObject valve_json = setup_valve_json(
                settings_json.getInt("sync_pin"));
            behavior_comm.sendMessage(valve_json.toString());
        }
        display.setTrackLength(track_length);

        if (!settings_json.isNull("reward")) {
            configure_rewards();
        }

        this.commentKeys = new HashMap<Character, String>();
        if (!settings_json.isNull("comment_keys")) {
            JSONObject quick_comments = settings_json.getJSONObject(
                "comment_keys");

            for (Object key : quick_comments.keys()) {
                Character key_char = ((String)key).charAt(0);
                commentKeys.put(key_char,
                                quick_comments.getString((String)key));
            }
        }

        if (!settings_json.isNull("contexts")) {
            unset_contexts.clear();
            delay(10);
            //VrContextList2 vr_context = null;
            //ArrayList<VrCueContextList3> cue_lists =
            //    new ArrayList<VrCueContextList3>();

            JSONArray contexts_array = settings_json.getJSONArray("contexts");
            for (int i = 0; i < contexts_array.size(); i++) {
                JSONObject context_info = contexts_array.getJSONObject(i);
                String context_class = context_info.getString("class",
                                                              "context");
                ContextList context_list = ContextsFactory.Create(
                        this, display, context_info, track_length,
                        behavior_comm, context_class);
                contexts.add(context_list);

                if (!context_list.setupComms(comms)) {
                    trialListener.alert(
                        "Context List: " + context_list.getId() +
                        " failed to connect to comm");
                }
                display.setContextLocations(context_list);

                if ((behavior_comm != null) && (context_list.getComm() == behavior_comm)) {
                    unset_contexts.add(context_list.getId());
                }
            }

            for (ContextList context : this.contexts) {
                context.registerContexts(this.contexts);
            }
        }
    }

    /**
     * Zero out the calibration and reconfigure the experiment.
     * TODO: this method is no longer needed
     *
     * @throws Exception
     */
    void reload_settings() throws Exception {
        current_calibration = 0;
        reconfigureExperiment();
    }

    /**
     * Log messages from behaviorMate into the .tdml file
     *
     * @param log  information to log
     * @param time time elapsed in the current trial

     * @return     <tt>true</tt> on successful write
     */
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

    /**
     * Log messages from behaviorMate into the .tdml file
     *
     * @param log  information to log
     * @param time time elapsed in the current trial

     * @return     <tt>true</tt> on successful write
     */
    public boolean writeLog(JSONObject log) {
        return writeLog(log, timer.getTime());
    }

    /**
     * Add a comment to the .tdml file
     *
     * @param comment text to write
     */
    public void addComment(String comment) {
        if (fWriter == null) {
            return;
        }

        JSONObject comment_message = new JSONObject();
        comment_message.setString("message", comment);

        JSONObject comment_json = new JSONObject();
        comment_json.setJSONObject("behavior_mate", new JSONObject());
        comment_json.getJSONObject("behavior_mate").setJSONObject(
            "comment", comment_message);
        comment_json.setFloat("time", timer.getTime());
        fWriter.write(comment_json.toString());
    }

    /**
     * Add comment to the .tdml file but force a specific key instead
     * of default <tt>behavior_mate</tt>
     *
     * @param key     key to use
     * @param comment comment to write
     */
    public void addComment(String key, String comment) {
        if (fWriter == null) {
            return;
        }

        JSONObject comment_json = new JSONObject();
        comment_json.setString(key, comment);
        comment_json.setFloat("time", timer.getTime());
        fWriter.write(comment_json.toString());
    }

    /**
     * If comment keys are configured in the settings, then log keystrokes
     *
     * @param key     key with which was pressed
     * @param pressed action: <code>true</code> for press <code>false</code> if
     *                release
     */
    public void commentKey(Character key, boolean pressed) {
        if (fWriter == null) {
            return;
        }

        if (this.commentKeys.containsKey(key)) {
            JSONObject comment_message = new JSONObject();
            comment_message.setString("key", Character.toString(key));
            if (pressed) {
                comment_message.setString("action", "start");
            } else {
                comment_message.setString("action", "stop");
            }
            comment_message.setString("message", this.commentKeys.get(key));

            JSONObject comment_json = new JSONObject();
            comment_json.setJSONObject("behavior_mate", new JSONObject());
            comment_json.getJSONObject("behavior_mate").setJSONObject(
                "comment_key", comment_message);
            comment_json.setFloat("time", timer.getTime());
            fWriter.write(comment_json.toString());
        }
    }

    /**
     * Get the current time elapsed during this trial from the Experiment Timer.
     *
     * @return current time (s)
     */
    public float getTime() {
        return timer.getTime();
    }

    /**
     * Write the current stored settings into the .tdml currently being recorded
     *
     * @param filename name of the currently loaded settings
     * @param key      key currently loaded in the settings file
     */
    public void writeSettingsInfo(String filename, String key) {
        if (fWriter == null) {
            return;
        }

        JSONObject settings_info = new JSONObject();
        settings_info.setString("settings_file", filename);
        settings_info.setString("settings_key", key);

        fWriter.write(settings_info.toString());
    }

    /**
     *
     * @return the PSurface object processing libraries are drawing to
     */
    public PSurface getPSurface() {
        return this.initSurface();
    }

    /**
     * Processing function called automatically once at startup. Initializes
     * state variables and reads the settings file.
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
        sensor_counts = new HashMap<Integer, Integer>();
        lick_count = 0;

        PGraphics img = createGraphics(100,100);
        display = new Display();
        display.prepGraphics(this);

        position_comm = null;
        behavior_comm = null;
        comms = new ArrayList<UdpClient>();
        prepareExitHandler();
        trialListener.initialized();
    }

    /**
     * Set or unset the lap lock. Setting lap lock prevents laps from updating.
     * Used if a Context List or Decorator forces in ITI to happen
     *
     * @param lock_status <code>true</code>: prevent laps from counting,
                          <code>false</code>: count laps normally
     */
    public void setLapLock(boolean lock_status) {
        lock_lap = lock_status;
    }

    /**
     * Indicate a lap reset in the UI and log.
     *
     * @param tag  tag read by the lap_reset device. Empty string ("") indicates
     *             no tag id was used to trigger the reset.
     * @param time time (s) of the lap reset
     */
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

    /**
     * Only behavior controller and position controller send messages to the UI
     * that need to be parsed to update the state of the trial. Messages from
     * other controllers are logged in the .tdml, but cannot affect the state
     * of the trial. This method dequeues messages and logs them. Note: if the
     * position or behavior controller is passed to this method then those
     * updates will be logged, but cannot result in updates to position or trial
     * state.
     *
     * @param comm comm to read messages from
     * @param time time from Experiment Timeer
     */
    protected void checkMessages(UdpClient comm, float time) {
        for (int i=0; ((i < 10) && (comm.receiveMessage(json_buffer))); i++) {
            JSONObject message_json =
                json_buffer.json.getJSONObject(comm.id);

            if (started) {
                json_buffer.json.setFloat("time", time);
                fWriter.write(json_buffer.json.toString());
            } else {
                // if messages are received when a trial is not running, then
                // they are not written to the .tdml file

                System.out.println("WARNING! message received, " +
                                   "trial not recording");
                System.out.println(json_buffer.json.toString());
            }
        }
    }

    /**
     * Update the <code>position_scale</code>
     *
     * @param scale new position scale to use
     */
    public void setPositionScale(float scale) {
        this.position_scale = scale;
        this.display.setPositionScale(scale);
    }

    /**
     *
     * @return the current value of the <code>position_scale</code>. Used to
     * convert between ticks on the rotary encoder and mm. units: ticks / mm
     */
    public float getPositionScale() {
        return this.position_scale;
    }

    /**
     * Read messages from the position controller and update position
     *
     * @param time current time associated with this cycle of the event loop
     * @return     the amount the animal has moved (mm)
     */
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

            // reset lap and process contexts before parsing the next position
            // update
            if (!position_json.isNull("lap_reset")) {
                display.setCurrentTag("", distance - track_length);
                if (position_reset) {
                    reset_lap = true;
                    break;
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
        if ((position != -1) && (!((zero_position_boundary) &&
            (position + dy < 0)))) {
            if ((position + dy) < 0) {
                position += track_length;
            }
            position += dy;
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
                        position_scale *
                        (1 + (distance - track_length)/track_length)
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

    /**
     * Read and process any updates from the behavior controller.
     *
     * @param time timestamp for the current cycle of the event loop.
     */
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
                String action = behavior_json.getJSONObject("lick")
                        .getString("action", "none");
                if (action.equals("start")) {
                    display.addLick(started);
                    lick_count++;
                } else if (
                    (action.equals("stop") || (action.equals("created")))) {
                    display.lickStop();
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
                    String action = sensorJson.getString("action", "stop");
                    if (action.equals("start")) {
                        display.setSensorState(sensor_pin, 1);
                        if (sensor_counts.containsKey(sensor_pin)) {
                            sensor_counts.put(sensor_pin,
                                              sensor_counts.get(sensor_pin) + 1);
                        }
                        if (sensor_pin == lickport_pin) {
                            display.addLick(started);
                            lick_count++;
                        }
                    } else if (action.equals("stop")) {
                        display.setSensorState(sensor_pin, -1);
                        if (sensor_pin == lickport_pin) {
                            display.lickStop();
                        }
                    } else if (action.equals("created")) {
                        sensor_counts.put(sensor_pin, 0);
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
                        "Behavior Controller: " +
                        behavior_json.getString("error"));
                } else {
                    trialListener.exception(
                        "Behavior Controller: " +
                        behavior_json.getString("error"));
                }
            }

            if (!behavior_json.isNull("context")) {
                JSONObject context_json =
                    behavior_json.getJSONObject("context");

                if (!context_json.isNull("id")) {
                    String context_id = context_json.getString("id");
                    if (!context_json.isNull("action")) {
                        String action = context_json.getString("action");
                        for (int j=0; j < contexts.size(); j++) {
                            if (contexts.get(j).getId().equals(context_id)) {
                                if (action.equals("start")) {
                                    contexts.get(j).setStatus("started");
                                } else if (action.equals("stop")) {
                                    contexts.get(j).setStatus("stopped");
                                } else if (action.equals("created")) {
                                    unset_contexts.remove(context_id);
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
        if ((!started) &&
            (behavior_comm != null) &&
            (_millis > (comms_check_time + comms_check_interval))) {

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

    /**
     * Processing function called automatically 60 times per second. Main
     * experiment logic is executed here.
     */

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
                                      lick_count, sensor_counts, msg_buffer);
                if (msg_buffer[0] != null) {
                    this.writeLog(msg_buffer[0], time);
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
        int display_check = t - display_update;
        if (display_check > display_rate) {
            display.update(this, dy, offset_position, time);
            display_update = t;
        }
    }

    /**
     * Reset the behavior controller
     *
     * @param hardware_reset if <code>true</code> send message to separate
     *                       reset controller to perform a hardware reset,
     *                       otherwise send the message to trigger a software
     *                       reset to the behavior controller
     */
    public void resetArduino(boolean hardware_reset) {
        JSONObject resetArduino = new JSONObject();
        resetArduino.setJSONObject("communicator", new JSONObject());
        resetArduino.getJSONObject("communicator").setString("action", "reset");
        for (int i=0; i < contexts.size(); i++) {
            UdpClient comm = contexts.get(i).getComm();
            if ((comm != null) &&
                (comm.getId().equals("behavior_controller"))) {
                contexts.get(i).setStatus("resetting");
            }
        }

        if ((reset_comm != null) && (hardware_reset)) {
            reset_comm.sendMessage(resetArduino.toString());
        } else {
            behavior_comm.sendMessage(resetArduino.toString());
        }
    }

    /**
     * Send arduino message to request a software reset.
     */
    public void resetArduino() {
        resetArduino(false);
    }

    /**
     * Close all comms and attempt to reconnect.
     */
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
     * End the current experiment and reset the state to await the next trial.
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
        sensor_counts = new HashMap<Integer, Integer>();

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
     * Adds a function hook which will be run if the program terminates
     * unexpectedly. This is meant to ensure log files are closed out.
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
