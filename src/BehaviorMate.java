import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.StringBuilder;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.FileReader;

import java.lang.NumberFormatException;
import org.json.JSONException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.zip.DataFormatException;

import processing.awt.PSurfaceAWT.SmoothCanvas;
import processing.core.PApplet;
import processing.core.PSurface;


class LabeledTextField extends JPanel {
    private JTextField textField;

    public LabeledTextField(String text, String value, int width) {
        super(new BorderLayout());
        JLabel label = new JLabel(text);
        this.textField = new JTextField(value, width);
        add(label, BorderLayout.NORTH);
        JPanel text_container2 = new JPanel(new FlowLayout());
        text_container2.add(textField);
        add(text_container2, BorderLayout.CENTER);
    }

    public LabeledTextField(String text, int width) {
        this(text, "", width);
    }

    public void setEnabled(boolean enabled) {
        this.textField.setEnabled(enabled);
    }

    public String getText() {
        return this.textField.getText();
    }

    public void setText(String text) {
        this.textField.setText(text);
    }

    public int getInt() {
        try {
            return Integer.parseInt(this.getText());
        } catch (NumberFormatException e) {
            System.out.println(e.toString());
        }
        return 0;
    }
}

class CalibrateBeltForm extends JPanel implements ActionListener {
    private TreadmillController treadmillController;
    private JButton jButton;
    private JButton resetButton;
    private JButton zeroButton;
    private boolean calibrating;

    public CalibrateBeltForm(TreadmillController treadmillController) {
        super(new FlowLayout());
        this.treadmillController = treadmillController;


        jButton = new JButton("Calibrate Position");
        jButton.addActionListener(this);
        jButton.setPreferredSize(new Dimension(115, 25));
        jButton.setFont(new Font("Arial", Font.PLAIN, 10));
        resetButton = new JButton("Reset");
        resetButton.setFont(new Font("Arial", Font.PLAIN, 10));
        resetButton.addActionListener(this);
        zeroButton = new JButton("Zero Position");
        zeroButton.setFont(new Font("Arial", Font.PLAIN, 10));
        zeroButton.setPreferredSize(new Dimension(115, 25));
        zeroButton.addActionListener(this);

        GridBagLayout gridbag = new GridBagLayout();
        JPanel button_container = new JPanel(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.8;
        button_container.add(jButton, c);

        c.gridx = 1;
        c.weightx = 0.2;
        button_container.add(resetButton, c);

        c.gridx = 0;
        c.gridy = 2;
        button_container.add(zeroButton, c);
        //button_container.setPreferredSize(new Dimension(150, 50));

        JLabel formLabel = new JLabel("Position Controls", SwingConstants.LEFT);
        formLabel.setPreferredSize(new Dimension(120, 25));
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        button_container.add(formLabel, c);

        add(button_container);
        calibrating = false;
    }

    public void setEnabled(boolean enabled) {
        jButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        zeroButton.setEnabled(enabled);
    }

    public void endCalibration() {
        treadmillController.EndBeltCalibration();
        jButton.setText("Calibrate Position");
        this.calibrating = false;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jButton) {
            if (calibrating) {
                this.endCalibration();
            } else {
                treadmillController.CalibrateBelt();
                jButton.setText("End Calibration");
                this.calibrating = true;
            }
        } else if (e.getSource() == resetButton) {
            treadmillController.ResetCalibration();
        } else if (e.getSource() == zeroButton) {
            treadmillController.ZeroPosition();
        }
    }
}


class ZeroPositionForm extends JPanel implements ActionListener {
    private TreadmillController treadmillController;
    private JButton testValveButton;
    private boolean blocked;

    public ZeroPositionForm(TreadmillController treadmillController) {
        super(new FlowLayout());
        this.treadmillController = treadmillController;

        testValveButton = new JButton("Zero Position");
        testValveButton.addActionListener(this);
        JPanel button_container = new JPanel(new GridLayout(0,1));
        button_container.add(testValveButton);
        add(button_container);
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            this.blocked = true;
            testValveButton.setText("Enable");
        } else {
            this.blocked = false;
            testValveButton.setText("Zero Position");
        }
        //testValveButton.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent e) {
        if (blocked) {
            setEnabled(true);
        } else {
            treadmillController.ZeroPosition();
        }
    }
}

class ValveTestForm extends JPanel implements ActionListener {
    private TreadmillController treadmillController;
    private LabeledTextField valveText;
    private LabeledTextField durationText;
    private JButton testValveButton;
    private boolean blocked;

    public ValveTestForm(TreadmillController treadmillController) {
        super(new FlowLayout());
        this.treadmillController = treadmillController;

        JPanel center_panel = new JPanel(new GridLayout(0,1));
        valveText = new LabeledTextField(
            "Valve", ""+treadmillController.getRewardPin(), 14);
        durationText = new LabeledTextField("Duration", "200", 14);
        center_panel.add(valveText);
        center_panel.add(durationText);
        add(center_panel);

        testValveButton = new JButton("Open Valve");
        testValveButton.addActionListener(this);
        JPanel button_container = new JPanel(new GridLayout(0,1));
        button_container.add(testValveButton);
        add(button_container);
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            this.blocked = true;
            testValveButton.setText("Enable");
        } else {
            this.blocked = false;
            testValveButton.setText("Open Valve");
        }
        //testValveButton.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent e) {
        int valve = valveText.getInt();
        int duration = durationText.getInt();

        if (blocked) {
            setEnabled(true);
        } else if ((valve != 0) && (duration != 0)) {
            treadmillController.TestValve(
                valveText.getInt(), durationText.getInt());
        }
    }

    public void setPin(int pin_number) {
        valveText.setText(""+pin_number);
    }
}

class TrialListener {
    private ControlPanel controlPanel;
    private CommentsBox commentsBox;
    private TreadmillController controller;
    private Process arduino_controller;
    private String arduino_controller_path;
    private String controller_settings;

    private Process position_controller;
    private String position_settings;

    class ArduinoProcess {
        private String arduino_controller_path;
        private Process p;
        private String settings;

        public ArduinoProcess(String arduino_controller_path, String settings)
                throws JSONException, DataFormatException {
            this.settings = createArduinoSettings(settings);
            this.arduino_controller_path = arduino_controller_path;
            startProcess();
        }

        public void destroy() {
            if (this.p != null) {
                if (this.p.isAlive()) {
                    this.p.destroy();
                }
                this.p = null;
            }
        }

        private String createArduinoSettings(String controller_info)
                throws JSONException, DataFormatException {
            JSONObject controller_json = new JSONObject(controller_info);

            if (controller_json.isNull("serial_port")) {
                throw new DataFormatException(
                    "Serial Port needs to be specified in order to start " +
                    "arduino controller process");
            } else {
                System.out.println(
                    "configuring " + controller_json.get("serial_port"));
            }

            int send_port = controller_json.getInt("send_port");
            controller_json.put("send_port",
                controller_json.getInt("receive_port"));
            controller_json.put("receive_port", send_port);

            controller_info = "\"" + controller_json.toString().replace(
                "\\", "\\\\").replace("\"", "\\\"") + "\"";

            return controller_info;
        }

        public void startProcess() {
            String[] cmd = {arduino_controller_path, "", ""};
            if (this.settings != null) {
                cmd[1] = "-settings";
                cmd[2] = this.settings;
            } else {
                exception("unable to parse arduino controller settings");
            }

            try {
                this.p = Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
                System.out.println(e);
                exception(e.toString());
            }
        }
    }

    private HashMap<String, ArduinoProcess> arduino_controllers;


    public TrialListener() {
        controlPanel = null;
        commentsBox = null;
        arduino_controller = null;
        position_controller = null;
        arduino_controller_path = null;
        arduino_controllers = null;
    }

    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    public void setCommentsBox(CommentsBox commentsBox) {
        this.commentsBox = commentsBox;
    }

    public void setController(TreadmillController controller) {
        this.controller = controller;
    }

    public void setArduinoController(String arduino_path,
            JSONObject controllers) {
        if (arduino_controllers != null) {
            for (ArduinoProcess process : arduino_controllers.values()) {
                process.destroy();
            }

            if (arduino_path == null) {
                arduino_controllers = null;
            }
        }

        if (arduino_path != null) {
            if (arduino_controllers == null) {
                arduino_controllers = new HashMap<String, ArduinoProcess>();
            }

            for (Iterator<String> itr=controllers.keys(); itr.hasNext();) {
                String controller_key = itr.next();
                try {
                    if (!controllers.getJSONObject(controller_key).isNull("serial_port")) {
                        arduino_controllers.put(controller_key,
                            new ArduinoProcess(arduino_path,
                                controllers.get(controller_key).toString()));
                    }
                } catch (Exception e) {
                        System.out.println(e.toString());
                        exception(controller_key + " Exception: " + e.toString());
                }
            }
        }
    }

    public void setArduinoController(String arduino_path, String controllers)
            throws JSONException {
        setArduinoController(arduino_path, new JSONObject(controllers));
    }

    public void started(File logFile) {
        if (commentsBox != null) {
            commentsBox.setCurrentFile(logFile);
        }
    }

    public void initialized() {
        controlPanel.refreshSettings();
    }

    public void ended() {
        if (controlPanel != null) {
            controlPanel.setEnabled(true);
            controlPanel.showAttrsForm();
        }

        if (commentsBox != null) {
            commentsBox.addOption("next trial");
        }
    }

    public void exception(String message) {
        this.controller.endExperiment();
        if (message.length() > 100) {
            message = new StringBuilder(message).insert(100, "\n").toString();
        }
        JOptionPane.showMessageDialog(null, message);
    }

    public void alert(String message) {
        if (message.length() > 100) {
            message = new StringBuilder(message).insert(100, "\n").toString();
        }
        final String _message = message;
        Thread t = new Thread(new Runnable(){
            public void run() {
                JOptionPane.showMessageDialog(null, _message);
            }
        });
        t.start();
    }

    public void showDeleteDialog(String filepath) {
        final String _filepath = filepath;

        Thread t = new Thread(new Runnable() {
            public void run() {
                Object[] options = {"Delete", "Save"};

                int selectedValue = JOptionPane.showOptionDialog(
                    null, "<html>Save File<br>" + _filepath + "?</html>",
                   "Trial Ended", 0, JOptionPane.INFORMATION_MESSAGE, null,
                   options, options[1]);
                if (selectedValue == 0) {
                    int option_value = JOptionPane.showConfirmDialog(
                        null, "Confirm Delete\n" + _filepath, "Delete File",
                        JOptionPane.YES_NO_OPTION);
                    if (option_value == 0) {
                        File f = new File(_filepath);
                        if (!f.delete()) {
                            JOptionPane.showMessageDialog(
                                null, "Failed to delete file");
                        }

                    }
                }
            }
        });

        t.start();
    }

    public void resetComms() {
        if (arduino_controllers != null) {
            for (ArduinoProcess process : arduino_controllers.values()) {
                process.destroy();
                process.startProcess();
            }
        } else {
            System.out.println("SOFT RESET");
            this.controller.resetArduino();
        }
    }

    public void shutdown() {
        if (arduino_controllers != null) {
            for (ArduinoProcess process : arduino_controllers.values()) {
                process.destroy();
            }
        }
    }
}


class ControlPanel extends JPanel implements ActionListener {
    private LabeledTextField mouseNameBox;
    private LabeledTextField experimentGroupBox;
    private ValveTestForm valveTestForm;
    //private ZeroPositionForm zeroPositionForm;
    private CalibrateBeltForm calibrateBeltForm;
    private JButton showAttrsButton;
    private JButton refreshButton;
    private JButton restartCommButton;
    private JButton startButton;
    private TreadmillController treadmillController;
    private SettingsLoader settingsLoader;
    private TrialAttrsForm trialAttrsForm;
    private boolean attrsCompleted;
    private JFrame parent;

    public ControlPanel(JFrame parent, TreadmillController treadmillController,
            SettingsLoader settingsLoader) {
        this.treadmillController = treadmillController;
        this.settingsLoader = settingsLoader;
        settingsLoader.addActionListener(this);
        this.parent = parent;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        experimentGroupBox = new LabeledTextField("Project Name", 14);
        add(experimentGroupBox);
        mouseNameBox = new LabeledTextField("Mouse Name", 14);
        add(mouseNameBox);

        add(Box.createVerticalStrut(25));

        calibrateBeltForm = new CalibrateBeltForm(treadmillController);
        calibrateBeltForm.setPreferredSize(new Dimension(200, 100));
        add(calibrateBeltForm);

        //zeroPositionForm = new ZeroPositionForm(treadmillController);
        //zeroPositionForm.setPreferredSize(new Dimension(200, 50));
        //add(zeroPositionForm);
        add(Box.createVerticalStrut(25));

        valveTestForm = new ValveTestForm(treadmillController);
        valveTestForm.setPreferredSize(new Dimension(200, 250));
        add(valveTestForm);

        add(Box.createVerticalStrut(50));

        JPanel buttonPanel = new JPanel(new GridLayout(0,1));
        showAttrsButton= new JButton("Edit Trial Attributes");
        showAttrsButton.addActionListener(this);
        buttonPanel.add(showAttrsButton);

        refreshButton = new JButton("Re-Load Settings");
        refreshButton.addActionListener(this);
        buttonPanel.add(refreshButton);

        restartCommButton = new JButton("Re-Start Comms");
        restartCommButton.addActionListener(this);
        buttonPanel.add(restartCommButton);

        startButton = new JButton("Start");
        startButton.addActionListener(this);
        buttonPanel.add(startButton);

        add(buttonPanel);

        trialAttrsForm = new TrialAttrsForm(this);
        trialAttrsForm.addActionListener(this);

        //JMenuBar menuBar = new JMenuBar();
        //this.parent.setJMenuBar(menuBar);

        showAttrsForm();
    }

    public void setTestValve(int pin_number) {
        valveTestForm.setPin(pin_number);
    }

    public void showAttrsForm() {
        if (trialAttrsForm.showForm()) {
            attrsCompleted = false;
        } else {
            attrsCompleted = true;
        }
    }

    private void updateAttrs() {
        String trialAttrs = "";
        try {
            trialAttrs = trialAttrsForm.getValues();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Unable to parse Trial Attributes\n" + e.getMessage());
            showAttrsForm();
            return;
        }

        try {
            treadmillController.addSettings(trialAttrs);
        } catch (Exception exc) {
            exc.printStackTrace();

            String msg = exc.toString();
            StackTraceElement[] elements = exc.getStackTrace();
            for (int i=0; ((i < 3) &&(i < elements.length)); i++) {
                msg += ("\n " + elements[i].toString());
            }
            JOptionPane.showMessageDialog(null, msg);
            return;
        }
    }

    public void setEnabled(boolean enabled) {
        if (!enabled) {
            mouseNameBox.setEnabled(false);
            experimentGroupBox.setEnabled(false);
            valveTestForm.setEnabled(false);
            refreshButton.setEnabled(false);
            showAttrsButton.setEnabled(false);
            calibrateBeltForm.setEnabled(false);
            startButton.setText("Stop");
        } else {
            startButton.setText("Start");
            mouseNameBox.setEnabled(true);
            experimentGroupBox.setEnabled(true);
            valveTestForm.setEnabled(true);
            showAttrsButton.setEnabled(true);
            refreshButton.setEnabled(true);
            calibrateBeltForm.setEnabled(true);
        }
    }

    public static JSONObject findSettings(String filename, String tag)
            throws JSONException {
        JSONObject settings = BehaviorMate.parseJsonFile(filename, tag);
        if (settings.isNull("uses")) {
            return settings;
        }

        JSONArray settings_names = null;
        try {
            settings_names = settings.getJSONArray("uses");
        } catch (JSONException e) { }

        if (settings_names != null) {
            for (int i = 0; i < settings_names.length(); i++) {
                JSONObject settings_update;
                try {
                    JSONObject settings_info = settings_names.getJSONObject(i);
                    settings_update = findSettings(
                        settings_info.getString("file"),
                        settings_info.getString("tag"));
                } catch (JSONException e) {
                    settings_update = findSettings(filename,
                            (String) settings_names.getString(i));
                }

                Iterator<String> key_itr = settings.keys();
                while (key_itr.hasNext()) {
                    String key = key_itr.next();
                    settings_update.put(key, settings.get(key));
                }
                settings = settings_update;
            }
        } else {
            JSONObject settings_update;
            try {
                JSONObject settings_info = settings.getJSONObject("uses");
                settings_update = findSettings(
                    settings_info.getString("file"),
                    settings_info.getString("tag"));
            } catch (JSONException e) {
                settings_update = findSettings(filename,
                    settings.getString("uses"));
            }

            Iterator<String> key_itr = settings.keys();
            while (key_itr.hasNext()) {
                String key = key_itr.next();
                settings_update.put(key, settings.get(key));
            }
            settings = settings_update;
        }

        return settings;
    }

    public void refreshSettings() {
        String version = "Behavior Mate ";
        try {
            JSONObject version_json =
                BehaviorMate.parseJsonFile("version.json");
            version += (" " + version_json.getString("version") + " - ");
        } catch (Exception e) {
            System.out.println(e);
        }

        parent.setTitle(version + settingsLoader.getSelectedTag());
        String filename = settingsLoader.getSelectedFile();
        String tag = settingsLoader.getSelectedTag();

        JSONObject settings = null;
        try {
            settings = findSettings(filename, tag);
            JSONObject system_settings = BehaviorMate.parseJsonFile(filename,
                "_system");
            if (system_settings == null) {
                system_settings = BehaviorMate.parseJsonFile("settings.json",
                    "_system");
            }

            treadmillController.RefreshSettings(settings.toString(),
                system_settings.toString());
        } catch (Exception exc) {
            exc.printStackTrace();

            String msg = exc.toString();
            StackTraceElement[] elements = exc.getStackTrace();
            for (int i=0; ((i < 3) &&(i < elements.length)); i++) {
                msg += ("\n " + elements[i].toString());
            }
            JOptionPane.showMessageDialog(null, msg);
            return;
        }

        setTestValve(treadmillController.getRewardPin());
        trialAttrsForm.loadForm(settings);
        showAttrsForm();
    }

    public void startTrial() {
        this.calibrateBeltForm.endCalibration();

        if (startButton.getText().equals("Start")) {
            if (!attrsCompleted) {
                JOptionPane.showMessageDialog(this,
                    "Complete Trial Attributes Form");
                return;
            }

            if (mouseNameBox.getText().equals("")) {
                JOptionPane.showMessageDialog(this,
                    "Mouse Name is Blank");
                return;
            }

            if (experimentGroupBox.getText().equals("")) {
                JOptionPane.showMessageDialog(this,
                    "Project Name is Blank");
                return;
            }

            if (treadmillController.Start(mouseNameBox.getText(),
                                          experimentGroupBox.getText())) {
                setEnabled(false);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Unable to Start ... Scan lap reset? or check behavior save directory");
            }

            treadmillController.writeSettingsInfo(
                settingsLoader.getSelectedFile(),
                settingsLoader.getSelectedTag());
        } else {
            treadmillController.endExperiment();
            setEnabled(true);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startTrial();
        } else if (e.getSource() == showAttrsButton) {
            showAttrsForm();
        } else if (e.getSource() == refreshButton) {
            settingsLoader.setLocationRelativeTo(this);
            settingsLoader.show();
        } else if (e.getSource() == settingsLoader) {
            refreshSettings();
        } else if (e.getSource() == trialAttrsForm) {
            attrsCompleted = true;
            updateAttrs();
        } else if (e.getSource() == restartCommButton) {
            treadmillController.resetComms();
        }
    }
}

class CommentsBox extends JPanel implements ActionListener {
    JComboBox<String> fileSelect;
    JButton saveButton;
    File currentFile;
    String currentItem;
    String nextItem;
    String lastSelection;
    String savedString;

    String nextItemText;
    String currentItemText;

    JPanel formContainer;
    JTextArea commentArea;
    JTextArea nextCommentArea;
    TreadmillController treadmillController;

    public CommentsBox(TreadmillController treadmillController) {
        currentFile = null;
        currentItem = null;
        this.treadmillController = treadmillController;
        nextItemText = "";
        currentItemText = "";
        savedString = "";

        setLayout(new BorderLayout());

        formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));

        fileSelect = new JComboBox<String>();
        fileSelect.addActionListener(this);
        formContainer.add(fileSelect);
        formContainer.add(Box.createVerticalStrut(15));

        saveButton = new JButton("save comment");
        saveButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        saveButton.setPreferredSize(new Dimension(200,
            saveButton.getPreferredSize().height));
        formContainer.add(buttonPanel);
        add(formContainer, BorderLayout.WEST);

        commentArea = new JTextArea(5,200);
        nextCommentArea = new JTextArea(5,200);
        add(commentArea, BorderLayout.CENTER);

        add(new JLabel("Comments"), BorderLayout.NORTH);
        nextItem = "next trial";
        lastSelection = nextItem;
        fileSelect.addItem(nextItem);
    }

    public void setCurrentFile(File file) {
        if (fileSelect.getSelectedItem() == nextItem) {
            currentItemText = commentArea.getText();
        } else {
            currentItemText = nextItemText;
            commentArea.setText(nextItemText);
        }

        if (!savedString.equals("")) {
            treadmillController.addComment(savedString);
            savedString = "";
        }

        fileSelect.removeActionListener(this);
        fileSelect.removeAllItems();
        currentFile = file;
        currentItem = file.getName();

        fileSelect.addItem(currentItem);
        fileSelect.setSelectedItem(currentItem);

        nextItemText = "";
        lastSelection = currentItem;

        fileSelect.addActionListener(this);
    }

    public void addOption(String option) {
        if (option.equals(nextItem)) {
            fileSelect.addItem(nextItem);
        } else {
            fileSelect.addItem(option);

        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            if (commentArea.getText().equals("")) {
                return;
            }

            if (fileSelect.getSelectedItem() == nextItem) {
                savedString = commentArea.getText();
            } else {
                treadmillController.addComment(commentArea.getText());
            }
        } else if (e.getSource() == fileSelect) {
            if (!fileSelect.getSelectedItem().equals(lastSelection)) {
                if (fileSelect.getSelectedItem() == nextItem) {
                    currentItemText = commentArea.getText();
                    commentArea.setText(nextItemText);
                } else {
                    nextItemText = commentArea.getText();
                    commentArea.setText(currentItemText);
                }

                lastSelection = (String)fileSelect.getSelectedItem();
            }
        }
    }
}


class KeyboardListener implements KeyEventDispatcher {
    private TreadmillController treadmillController;
    private HashMap<Character, String> commentTable;
    private TrialListener trialListener;
    private final Class textfieldclass = new JTextField().getClass();
    private final Class commentclass = new JTextArea().getClass();

    public KeyboardListener(TreadmillController treadmillController) {
        this.treadmillController = treadmillController;
        commentTable = new HashMap<Character, String>();
    }

    public boolean dispatchKeyEvent(KeyEvent e) {
        JTextField test = new JTextField();
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            Class sourceClass = e.getSource().getClass();
            if (!sourceClass.equals(textfieldclass) && !sourceClass.equals(commentclass)) {
                Character keyChar = e.getKeyChar();
                this.treadmillController.commentKey(keyChar, true);
            }
        }
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            Class sourceClass = e.getSource().getClass();
            if (!sourceClass.equals(textfieldclass) && !sourceClass.equals(commentclass)) {
                Character keyChar = e.getKeyChar();
                this.treadmillController.commentKey(keyChar, false);
            }
        }
        return false;
    }

    public void addKeyChar(Character key, String comment) {
        commentTable.put(key, comment);
    }
}

public class BehaviorMate {
    static SettingsLoader settingsLoader;
    static JFrame startFrame;

    public static JSONObject parseJsonFile(String filename) {
        String extension = filename.replaceAll("^.*\\.(.*)$", "$1");
        if (extension.equals("tdml")) {
            return parseTdmlSettings(filename);
        }

        String jsonData = "";
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("//")) {
                    jsonData += line.split("//")[0] + "\n";
                }
            }
        } catch (IOException e) {
            String message = "Failed to parse: " + filename +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);
        }

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonData);
        } catch(JSONException e) {
            String message = "Failed to parse: " + filename +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);
        }

        return jsonObj;
    }

    public static JSONObject parseJsonFile(String filename, String tag) {
        JSONObject json  = parseJsonFile(filename);

        try {
            json = json.getJSONObject(tag);
        } catch (JSONException e) {
            String message = "Failed to find tag: " + tag +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);

            json = null;
        }

        return json;
    }

    public static JSONObject parseTdmlSettings(String filename) {
        String jsonData = "";
        BufferedReader br = null;

        try {
            String line;
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("//")) {
                    jsonData = line.split("//")[0] + "\n";
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(jsonData);
                        if (!jsonObject.isNull("settings")) {
                            return jsonObject;
                        }
                    } catch(JSONException e) { }
                }
            }
        } catch (IOException e) {
            String message = "Failed to parse: " + filename +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);
        }

        return null;
    }

    public static JSONObject parseJson(String filename) {
        String jsonData = "";
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("//")) {
                    jsonData += line.split("//")[0] + "\n";
                }
            }
        } catch (IOException e) {
            String message = "Failed to parse: " + filename +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);
        }

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonData);
        } catch(JSONException e) {
            String message = "Failed to parse: " + filename +
                "\n" + e.toString();
            JOptionPane.showMessageDialog(null, message);
        }

        return jsonObj;
    }

    private static void startTreadmill(SettingsLoader settingsLoader) {
        String settingsFile = settingsLoader.getSelectedFile();
        String settingsTag = settingsLoader.getSelectedTag();
        JSONObject settings = null;
        TrialListener tl = new TrialListener();

        try {
            settings = ControlPanel.findSettings(settingsFile, settingsTag);
        } catch(JSONException e) {
            tl.exception(e.toString());
            System.out.println(e.toString());
        }
        JSONObject system_settings = parseJsonFile(settingsFile, "_system");
        if (system_settings == null) {
            system_settings = parseJsonFile("settings.json", "_system");
        }

        JFrame frame = new JFrame(settingsTag);
        JPanel frame_container = new JPanel(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        TreadmillController treadmillController;

        treadmillController = new TreadmillController(settings.toString(),
            system_settings.toString(), tl);

        ControlPanel control_panel = new ControlPanel(frame,
            treadmillController, settingsLoader);
        tl.setControlPanel(control_panel);
        tl.setController(treadmillController);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyboardListener dispatcher = new KeyboardListener(treadmillController);
        manager.addKeyEventDispatcher(dispatcher);

        frame_container.add(control_panel, BorderLayout.CENTER);

        JPanel container = new JPanel();
        PSurface ps = treadmillController.getPSurface();
        ps.setSize(600, 600);

        SmoothCanvas smoothCanvas = (SmoothCanvas)ps.getNative();
        container.add(smoothCanvas);
        container.setSize(800, 600);
        frame_container.add(container, BorderLayout.EAST);

        CommentsBox commentsBox = new CommentsBox(treadmillController);
        tl.setCommentsBox(commentsBox);
        frame_container.add(commentsBox, BorderLayout.SOUTH);

        frame.add(frame_container);

        frame.pack();
        frame.setSize(800, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ps.startThread();
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        String system_type = System.getProperty("os.name");
        if (system_type.toLowerCase().contains("windows")) {
            try {
                Runtime.getRuntime().exec("cmd /c start set_priority.bat");
            } catch (Exception e) {
                System.out.println(e);
            }

        }

        startFrame = new JFrame("BehaviorMate");
        settingsLoader = new SettingsLoader(startFrame);
        settingsLoader.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startTreadmill(settingsLoader);
                startFrame.setVisible(false);
            }
        });
        startFrame.setLocationRelativeTo(null);
        startFrame.setVisible(true);
        settingsLoader.setLocationRelativeTo(startFrame);
        settingsLoader.show();
    }
}
