import javax.swing.*;
import javax.swing.plaf.DimensionUIResource;
import java.awt.*;

/**
 * HML PACKAGE_NAME
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-14.
 */
public class Main {

    private static String Suffix = ".hml";
    private static JTextArea  jTextArea = new JTextArea();

    private static JScrollPane addTextArea(JTextArea jTextArea){

        jTextArea.setVisible(true);
        JScrollPane scrollPane =
                new JScrollPane(jTextArea,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Main");
        JPanel HML = new JPanel();

        HML.setLayout(new BorderLayout());
        frame.setContentPane(HML);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JMenuBar menuBar = new JMenuBar();

        // first the file menu
        JMenu fileMenu = new JMenu("File", true);
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);
        JMenuItem open = new JMenuItem("Open", 'o');
        open.setActionCommand("OPEN");


        open.addActionListener(new FileSelectActionListener(frame, Suffix, jTextArea));
        fileMenu.add(open);


        jTextArea.setVisible(true);
        HML.add(addTextArea(jTextArea));

        frame.setPreferredSize(new DimensionUIResource(1024, 768));
        frame.pack();
        frame.setVisible(true);
    }
}
