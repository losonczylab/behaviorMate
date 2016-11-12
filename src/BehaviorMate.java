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

import org.json.JSONObject;

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

        testValveButton = new JButton("Test Valve");
        testValveButton.addActionListener(this);
        JPanel button_container = new JPanel(new GridLayout(0,1));
        button_container.add(testValveButton);
        add(button_container);
    }

    public void setEnabled(boolean enabled) {
        testValveButton.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent e) {
        int valve = valveText.getInt();
        int duration = durationText.getInt();

        if ((valve != 0) && (duration != 0)) {
            treadmillController.TestValve(
                valveText.getInt(), durationText.getInt());
        }
    }
}

class TrialListener {
    private ControlPanel controlPanel;
    private CommentsBox commentsBox;
    
    public TrialListener() {
        controlPanel = null;
        commentsBox = null;
    }

    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    public void setCommentsBox(CommentsBox commentsBox) {
        this.commentsBox = commentsBox;
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
}


class ControlPanel extends JPanel implements ActionListener {
    private LabeledTextField mouseNameBox;
    private LabeledTextField experimentGroupBox;
    private ValveTestForm valveTestForm;
    private JButton showAttrsButton;
    private JButton refreshButton;
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
        add(Box.createVerticalStrut(125));

        JPanel buttonPanel = new JPanel(new GridLayout(0,1));
        showAttrsButton= new JButton("Edit Trial Attributes");
        showAttrsButton.addActionListener(this);
        buttonPanel.add(showAttrsButton);

        refreshButton = new JButton("Re-Load Settings");
        refreshButton.addActionListener(this);
        buttonPanel.add(refreshButton);

        startButton = new JButton("Start");
        startButton.addActionListener(this);
        buttonPanel.add(startButton);

        add(buttonPanel);

        trialAttrsForm = new TrialAttrsForm(this);
        trialAttrsForm.addActionListener(this);
        trialAttrsForm.loadForm(settingsLoader.getSelectedFile(),
            settingsLoader.getSelectedTag());
        showAttrsForm();
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

        //System.out.println(trialAttrs);
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

    public void refreshSettings() {
        parent.setTitle(settingsLoader.getSelectedTag());
        String filename = settingsLoader.getSelectedFile();
        String tag = settingsLoader.getSelectedTag();

        JSONObject settings = BehaviorMate.parseJsonFile(filename, tag);
        JSONObject system_settings = BehaviorMate.parseJsonFile(filename, "_system");

        try {
            treadmillController.RefreshSettings(settings.toString(),
                system_settings.toString(), true);
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

        trialAttrsForm.loadForm(filename, tag);
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

    private static void startTreadmill(SettingsLoader settingsLoader) {
        String settingsFile = settingsLoader.getSelectedFile();
        String settingsTag = settingsLoader.getSelectedTag();

        JSONObject settings = parseJsonFile(settingsFile, settingsTag);
        JSONObject system_settings = parseJsonFile(settingsFile, "_system");

        JFrame frame = new JFrame(settingsTag);
        JPanel frame_container = new JPanel(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TrialListener tl = new TrialListener();

        TreadmillController treadmillController;
        String experimentType = "treadmill";
        try {
            experimentType = settings.getString("experimentType");
        } catch (JSONException e) { }

        if (experimentType.equals("salience")) {
            treadmillController = 
                new SalienceController(settings.toString(),
                    system_settings.toString(), tl, true);
        } else {
            treadmillController = 
                new TreadmillController(settings.toString(),
                    system_settings.toString(), tl, true);
        }

        ControlPanel control_panel = new ControlPanel(frame,
            treadmillController, settingsLoader);
        tl.setControlPanel(control_panel);
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
