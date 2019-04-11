/**
 * 
 */
package edu.jiangxin.apktoolbox.i18n;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.jiangxin.apktoolbox.swing.extend.JEasyPanel;

/**
 * @author jiangxin
 *
 */
public class I18NAddPanel extends JEasyPanel {
    
    private static final long serialVersionUID = 1L;

    public I18NAddPanel() throws HeadlessException {
        super();
        setPreferredSize(new Dimension(600, 160));
        setMaximumSize(new Dimension(600, 160));
        BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(boxLayout);

        JPanel sourcePanel = new JPanel();
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS));
        add(sourcePanel);

        JTextField srcTextField = new JTextField();
        srcTextField.setText(conf.getString("i18n.add.src.dir"));

        JButton srcButton = new JButton("Source Directory");
        srcButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jFileChooser.setDialogTitle("select a directory");
                int ret = jFileChooser.showDialog(new JLabel(), null);
                switch (ret) {
                case JFileChooser.APPROVE_OPTION:
                    File file = jFileChooser.getSelectedFile();
                    srcTextField.setText(file.getAbsolutePath());
                    break;
                default:
                    break;
                }

            }
        });

        sourcePanel.add(srcTextField);
        sourcePanel.add(srcButton);

        JPanel targetPanel = new JPanel();
        targetPanel.setLayout(new BoxLayout(targetPanel, BoxLayout.X_AXIS));
        add(targetPanel);

        JTextField targetTextField = new JTextField();
        targetTextField.setText(conf.getString("i18n.add.target.dir"));

        JButton targetButton = new JButton("Save Directory");
        targetButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.setDialogTitle("save to");
                int ret = jfc.showDialog(new JLabel(), null);
                switch (ret) {
                case JFileChooser.APPROVE_OPTION:
                    File file = jfc.getSelectedFile();
                    targetTextField.setText(file.getAbsolutePath());
                    break;

                default:
                    break;
                }

            }
        });

        targetPanel.add(targetTextField);
        targetPanel.add(targetButton);

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
        add(itemPanel);

        JTextField itemTextField = new JTextField();
        itemTextField.setText(conf.getString("i18n.add.items"));

        JLabel itemLabel = new JLabel("Items");

        itemPanel.add(itemTextField);
        itemPanel.add(itemLabel);

        JPanel operationPanel = new JPanel();
        operationPanel.setLayout(new BoxLayout(operationPanel, BoxLayout.X_AXIS));
        add(operationPanel);

        JButton addButton = new JButton(bundle.getString("i18n.add.title"));
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                File srcFile = new File(srcTextField.getText());
                if (!srcFile.exists() || !srcFile.isDirectory()) {
                    logger.error("srcFile is invalid");
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(I18NAddPanel.this, "Source directory is invalid", "ERROR",
                            JOptionPane.ERROR_MESSAGE);
                    srcTextField.requestFocus();
                    return;
                }
                String srcPath;
                try {
                    srcPath = srcFile.getCanonicalPath();
                } catch (IOException e2) {
                    logger.error("getCanonicalPath fail");
                    return;
                }
                conf.setProperty("i18n.add.src.dir", srcPath);

                List<String> targetPaths = new ArrayList<>();
                String[] tmps = targetTextField.getText().split(";");
                for (String tmp : tmps) {
                    File targetFile = new File(tmp);
                    if (!targetFile.exists() || !targetFile.isDirectory()) {
                        logger.error("targetFile is invalid");
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(I18NAddPanel.this, "Target directory is invalid", "ERROR",
                                JOptionPane.ERROR_MESSAGE);
                        targetTextField.requestFocus();
                        return;
                    }
                    try {
                        targetPaths.add(targetFile.getCanonicalPath());
                    } catch (IOException e1) {
                        logger.error("getCanonicalPath fail");
                        return;
                    }
                }
                conf.setProperty("i18n.add.target.dir", targetTextField.getText());

                List<String> items = new ArrayList<>();
                tmps = itemTextField.getText().split(";");
                for (String tmp : tmps) {
                    items.add(tmp);
                }
                conf.setProperty("i18n.add.items", itemTextField.getText());

                for (String targetPath : targetPaths) {
                    for (String item : items) {
                        int ret = innerProcessor(srcPath, targetPath, item);
                        if (ret != 0) {
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(I18NAddPanel.this, "Failed, please see the log", "ERROR",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }
        });

        operationPanel.add(addButton);
    }

    private static final String charset = "UTF-8";

    private static final boolean isRemoveLastLF = true;

    private static Map<String, String> replace = new HashedMap<String, String>();

    static {
        replace.put("&quot;", "jiangxin001");
        replace.put("&#160;", "jiangxin002");
    }

    private int innerProcessor(String sourceBaseStr, String targetBaseStr, String itemName) {
        if (StringUtils.isAnyEmpty(sourceBaseStr, targetBaseStr, itemName)) {
            logger.error("params are invalid: sourceBaseStr: " + sourceBaseStr + ", targetBaseStr: " + targetBaseStr
                    + ", itemName: " + itemName);
            return -1;
        }
        File sourceBaseFile = new File(sourceBaseStr);
        File targetBaseFile = new File(targetBaseStr);
        int count = 0;

        File[] sourceParentFiles = sourceBaseFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("values");
            }
        });
        if (sourceParentFiles == null) {
            logger.error("sourceParentFiles is null");
            return -1;
        }
        for (File sourceParentFile : sourceParentFiles) {
            File sourceFile = new File(sourceParentFile, "strings.xml");

            Element sourceElement = getSourceElement(sourceFile, itemName);
            if (sourceElement == null) {
                logger.warn("sourceElement is null: " + sourceFile);
                continue;
            }

            File targetFile = new File(new File(targetBaseFile, sourceParentFile.getName()), "strings.xml");
            if (!targetFile.exists()) {
                logger.warn("targetFile does not exist: " + sourceFile);
                continue;
            }
            try {
                prePocess(targetFile);
            } catch (IOException e) {
                logger.error("prePocess failed.", e);
                return -1;
            }
            boolean res = setTargetElement(targetFile, sourceElement, itemName);
            if (!res) {
                logger.error("setTargetElement failed.");
                return -1;
            }
            try {
                postProcess(targetFile);
            } catch (IOException e) {
                logger.error("postProcess failed.", e);
                return -1;
            }
            logger.info("count: " + (++count) + ", in path: " + sourceFile + ", out path: " + targetFile);
        }
        logger.info("finish one cycle");
        return 0;
    }

    private Element getSourceElement(File sourceFile, String itemName) {
        if (!sourceFile.exists()) {
            logger.warn("sourceFile does not exist: " + sourceFile);
            return null;
        }
        SAXBuilder builder = new SAXBuilder();
        Document sourceDoc = null;
        InputStream in = null;
        try {
            in = new FileInputStream(sourceFile);
            sourceDoc = builder.build(in);
            logger.info("build source document: " + sourceFile);
        } catch (JDOMException | IOException e) {
            logger.error("build source document failed: " + sourceFile, e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("close input stream exception", e);
                }
            }
        }
        Element sourceElement = null;
        for (Element sourceChild : sourceDoc.getRootElement().getChildren()) {
            String sourceValue = sourceChild.getAttributeValue("name");
            if (sourceValue != null && sourceValue.equals(itemName)) {
                sourceElement = sourceChild.clone();
                break;
            }
        }
        return sourceElement;
    }

    private boolean setTargetElement(File targetFile, Element sourceElement, String itemName) {
        SAXBuilder builder = new SAXBuilder();
        Document targetDoc;
        try {
            targetDoc = builder.build(targetFile);
            logger.info("build target document: " + targetFile);
        } catch (JDOMException | IOException e) {
            logger.error("build target document failed: " + targetFile, e);
            return false;
        }
        Element targetRoot = targetDoc.getRootElement();
        boolean isFinished = false;
        for (Element targetChild : targetRoot.getChildren()) {
            String targetValue = targetChild.getAttributeValue("name");
            if (targetValue != null && targetValue.equals(itemName)) {
                targetChild.setText(sourceElement.getText());
                isFinished = true;
                break;
            }
        }
        if (!isFinished) {
            targetRoot.addContent("    ");
            targetRoot.addContent(sourceElement);
            targetRoot.addContent("\n");
        }
        XMLOutputter out = new XMLOutputter();
        Format format = Format.getRawFormat();
        format.setEncoding("UTF-8");
        format.setLineSeparator("\n");
        out.setFormat(format);
        OutputStream os = null;
        try {
            os = new FileOutputStream(targetFile);
            out.output(targetDoc, os);
        } catch (IOException e) {
            logger.error("output fail", e);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.error("close output stream exception", e);
                }
            }
        }
        return true;
    }

    private static void prePocess(File file) throws IOException {
        String content = FileUtils.readFileToString(file, charset);
        for (Map.Entry<String, String> entry : replace.entrySet()) {
            content = content.replaceAll(entry.getKey(), entry.getValue());
        }
        FileUtils.writeStringToFile(file, content, charset);
    }

    private static void postProcess(File file) throws IOException {
        String content = FileUtils.readFileToString(file, charset);
        for (Map.Entry<String, String> entry : replace.entrySet()) {
            content = content.replaceAll(entry.getValue(), entry.getKey());
        }
        if (isRemoveLastLF) {
            content = StringUtils.removeEnd(content, "\n");
        }
        FileUtils.writeStringToFile(file, content, charset);
    }

}