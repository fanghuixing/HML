import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * HML PACKAGE_NAME
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-14.
 */
public class FileSelectActionListener implements ActionListener {

    private JFrame frame;
    private String Suffix;
    private JTextArea jTextArea;
    public FileSelectActionListener(JFrame frame, String Suffix, JTextArea jTextArea) {
        this.frame = frame;
        this.Suffix = Suffix;
        this.jTextArea = jTextArea;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setName("untitled"+Suffix);
        fc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(Suffix);
            }

            public String getDescription() {
                return Suffix;
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File openedFile = fc.getSelectedFile();
            try {
                FileReader fr = new FileReader(openedFile);
                jTextArea.read(fr,null);
                Font x = new Font("Serif",0,20);
                jTextArea.setFont(x);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
    }
}
