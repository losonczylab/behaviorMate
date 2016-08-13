import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.Iterator;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONException;

public class SettingsLoader extends JDialog implements ActionListener {
    private JButton fileChooserButton;
    private JComboBox comboBox;
    private JTextField pathTextField;
    private JButton okayButton;
    private JButton cancelButton;
    private String selectedFile;
    private String selectedTag;
    private ActionListener actionListener;

    public SettingsLoader(Component parent) {
        JPanel frame_container = new JPanel(new BorderLayout());
        JPanel input_container = new JPanel();
        selectedFile = null;
        selectedTag = null;
        actionListener = null;

        comboBox = new JComboBox<String>();
        comboBox.setPreferredSize(
            new Dimension(150,comboBox.getPreferredSize().height));
        File defaultFile = new File("settings.json");
        pathTextField = new JTextField(defaultFile.getAbsolutePath(), 25);

        JSONObject jsonObj = parseJsonFile(defaultFile);
        if (jsonObj != null) {
            Iterator<?> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!key.startsWith("_")) {
                    comboBox.addItem(key);
                }
            }

            selectedFile = defaultFile.getAbsolutePath();
            selectedTag = (String) comboBox.getSelectedItem();
        }

        pathTextField.setEnabled(false);
        input_container.add(pathTextField);
        fileChooserButton = new JButton("...");
        fileChooserButton.setPreferredSize(
            new Dimension(40,comboBox.getPreferredSize().height));
        input_container.add(fileChooserButton);
        fileChooserButton.addActionListener(this);
        input_container.add(comboBox);
        frame_container.add(input_container, BorderLayout.CENTER);

        okayButton = new JButton("OK");
        okayButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        JPanel accept_panel = new JPanel();
        accept_panel.add(okayButton);
        accept_panel.add(cancelButton);
        frame_container.add(accept_panel, BorderLayout.SOUTH);

        add(frame_container);
        setSize(550, 100);
    }

    public String getSelectedFile() {
        return selectedFile;
    }

    public String getSelectedTag() {
        return selectedTag;
    }

    public void addActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private JSONObject parseJsonFile(File jsonFile) {
        String jsonData = "";
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(jsonFile));
            while ((line = br.readLine()) != null) {
                jsonData += line + "\n";
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonData);
        } catch(JSONException e) {
            System.out.println(e.toString());
        }

        return jsonObj;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fileChooserButton) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(
                new FileNameExtensionFilter("JSON file", "json"));
            int returnVal = fileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                comboBox.removeAllItems();
                pathTextField.setText(file.getAbsolutePath());
                JSONObject json = parseJsonFile(file);
                if (json != null) {
                    Iterator<?> keys = json.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        if (!key.startsWith("_")) {
                        comboBox.addItem(key);
                        }
                    }
                }
            }
        } else if (e.getSource() == okayButton) {
            this.selectedFile = pathTextField.getText();
            this.selectedTag = (String) comboBox.getSelectedItem();
            if (actionListener != null) {
                actionListener.actionPerformed(
                    new ActionEvent(this, 0, "accepted"));
            }
            hide();
        } else if (e.getSource() == cancelButton) {
            hide();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test Form");
        final SettingsLoader loader = new SettingsLoader(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton launchButton = new JButton("press");
        launchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loader.show();
            }
        });

        frame.add(launchButton);
        frame.setSize(150,50);
        frame.setVisible(true);
    }
}
