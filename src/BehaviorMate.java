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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.StringBuilder;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.FileReader;

import java.lang.NumberFormatException;
import org.json.JSONException;
import java.io.IOException;
import java.io.FileNotFoundException;

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

    public TrialListener() {
        controlPanel = null;
        commentsBox = null;
        arduino_controller = null;
        arduino_controller_path = null;
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

    public void setArduinoController(String arduino_path) {
        if ((arduino_path != null) &&
                ((this.arduino_controller_path == null) ||
                 (!this.arduino_controller_path.equals(arduino_path)))) {

            if (this.arduino_controller != null) {
                this.arduino_controller.destroy();
            }

            try {
                this.arduino_controller = Runtime.getRuntime().exec(
                    arduino_path);
            } catch (Exception e) {
                System.out.println(e);
                exception(e.toString());
            }

        } else if ((arduino_path == null) &&
                (this.arduino_controller != null)) {
            this.arduino_controller.destroy();
            this.arduino_controller = null;
        }

        this.arduino_controller_path = arduino_path;
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
        controller.endExperiment();
        message = new StringBuilder(message).insert(100, "\n").toString();
        JOptionPane.showMessageDialog(null, message);
    }

    public void resetComms() {
        if ((arduino_controller != null) && (arduino_controller.isAlive())) {
            arduino_controller.destroy();
            arduino_controller = null;
            try {
                arduino_controller = Runtime.getRuntime().exec(
                    arduino_controller_path);
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            System.out.println("SOFT RESET");
            controller.resetArduino();
        }

    }

    public void shutdown() {
        if ((arduino_controller != null) && (arduino_controller.isAlive())) {
            System.out.println("DESTROY ARDUINO CONTROLLER");
            this.arduino_controller.destroy();
        }
    }
}


class ControlPanel extends JPanel implements ActionListener {
    private LabeledTextField mouseNameBox;
    private LabeledTextField experimentGroupBox;
    private ValveTestForm valveTestForm;
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

        add(Box.createVerticalStrut(100));
        valveTestForm = new ValveTestForm(treadmillController);
        valveTestForm.setPreferredSize(new Dimension(200, 250));
        add(valveTestForm);
        add(Box.createVerticalStrut(75));

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
            startButton.setText("Stop");
        } else {
            startButton.setText("Start");
            mouseNameBox.setEnabled(true);
            experimentGroupBox.setEnabled(true);
            valveTestForm.setEnabled(true);
            showAttrsButton.setEnabled(true);
            refreshButton.setEnabled(true);
        }
    }

    public JSONObject findSettings(String filename, String tag)
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
                JSONObject settings_update = findSettings(filename,
                        (String) settings_names.getString(i));

                Iterator<String> key_itr = settings.keys();
                while (key_itr.hasNext()) {
                    String key = key_itr.next();
                    settings_update.put(key, settings.get(key));
                }
                settings = settings_update;
            }
        } else {
            JSONObject settings_update = findSettings(filename,
                settings.getString("uses"));

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

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
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
                        "Unable to Start ... Scan RFID tag? or check behavior save directory");
                }
            } else {
                treadmillController.endExperiment();
                setEnabled(true);
            }
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

        JSONObject settings = parseJsonFile(settingsFile, settingsTag);
        JSONObject system_settings = parseJsonFile(settingsFile, "_system");
        if (system_settings == null) {
            system_settings = parseJsonFile("settings.json", "_system");
        }

        JFrame frame = new JFrame(settingsTag);
        JPanel frame_container = new JPanel(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TrialListener tl = new TrialListener();

        TreadmillController treadmillController;

        treadmillController = new TreadmillController(settings.toString(),
            system_settings.toString(), tl);

        ControlPanel control_panel = new ControlPanel(frame,
            treadmillController, settingsLoader);
        tl.setControlPanel(control_panel);
        tl.setController(treadmillController);

        if (!system_settings.isNull("arduino_controller")) {
            String process_path = null;
            try {
                process_path = system_settings.getString("arduino_controller");
            } catch (JSONException e) {
                System.out.println(e);
            }

            tl.setArduinoController(process_path);
        }

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

        startFrame = new JFrame("oi!");
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
