package edu.jiangxin.apktoolbox.file.password.recovery;

import edu.jiangxin.apktoolbox.file.core.EncoderDetector;
import edu.jiangxin.apktoolbox.file.password.recovery.bruteforce.BruteForceFuture;
import edu.jiangxin.apktoolbox.file.password.recovery.bruteforce.BruteForceTask;
import edu.jiangxin.apktoolbox.file.password.recovery.bruteforce.BruteForceTaskConst;
import edu.jiangxin.apktoolbox.file.password.recovery.checker.*;
import edu.jiangxin.apktoolbox.file.password.recovery.dictionary.BigFileReader;
import edu.jiangxin.apktoolbox.file.password.recovery.dictionary.LineHandler;
import edu.jiangxin.apktoolbox.swing.extend.EasyPanel;
import edu.jiangxin.apktoolbox.swing.extend.filepanel.FilePanel;
import edu.jiangxin.apktoolbox.utils.Constants;
import edu.jiangxin.apktoolbox.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class RecoveryPanel extends EasyPanel {
    private JPanel optionPanel;

    private FilePanel recoveryFilePanel;

    private JTabbedPane categoryTabbedPane;

    private JPanel bruteForceCategoryPanel;

    private JPanel dictionaryCategoryPanel;

    private FilePanel dictionaryFilePanel;

    private Category currentCategory = Category.UNKNOWN;

    private JPanel operationPanel;

    private JCheckBox numberCheckBox;
    private JCheckBox lowercaseLetterCheckBox;
    private JCheckBox uppercaseLetterCheckBox;

    private JSpinner minSpinner;
    private JSpinner maxSpinner;

    private JSpinner threadNumSpinner;

    private JProgressBar progressBar;

    private JComboBox<FileChecker> checkerTypeComboBox;

    private FileChecker currentFileChecker;

    private JButton startButton;
    private JButton stopButton;

    private JLabel currentStateLabel;

    private JLabel currentPasswordLabel;

    private JLabel currentProgressLabel;

    private ExecutorService workerPool;

    private BigFileReader bigFileReader;
    private LineHandler lineHandler;

    private static State currentState = State.IDLE;

    public RecoveryPanel() {
        super();
        initUI();
    }

    private void initUI() {
        BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(boxLayout);

        createOptionPanel();
        add(optionPanel);
        add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));

        createOperationPanel();
        add(operationPanel);
    }

    private void createOptionPanel() {
        optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));

        checkerTypeComboBox = new JComboBox<>();
        checkerTypeComboBox.addItem(new ArchiveUsing7ZipChecker());
        checkerTypeComboBox.addItem(new ArchiveUsingWinRarChecker());
        checkerTypeComboBox.addItem(new RarUsingRarChecker());
        checkerTypeComboBox.addItem(new ZipChecker());
        checkerTypeComboBox.addItem(new RarChecker());
        checkerTypeComboBox.addItem(new SevenZipChecker());
        checkerTypeComboBox.addItem(new PdfChecker());
        checkerTypeComboBox.addItem(new XmlBasedOfficeChecker());
        checkerTypeComboBox.addItem(new BinaryOfficeChecker());
        checkerTypeComboBox.setSelectedIndex(0);
        checkerTypeComboBox.addItemListener(e -> {
            FileChecker fileChecker = (FileChecker)e.getItem();
            if (fileChecker == null) {
                logger.error("fileChecker is null");
                return;
            }
            recoveryFilePanel.setDescriptionAndFileExtensions(
                    fileChecker.getDescription(), fileChecker.getFileExtensions());
        });

        FileChecker fileChecker = (FileChecker) checkerTypeComboBox.getSelectedItem();
        if (fileChecker == null) {
            logger.error("fileChecker is null");
            return;
        }

        recoveryFilePanel = new FilePanel("Choose Recovery File");
        recoveryFilePanel.setDescriptionAndFileExtensions(fileChecker.getDescription(), fileChecker.getFileExtensions());

        categoryTabbedPane = new JTabbedPane();

        createBruteForcePanel();
        categoryTabbedPane.addTab("Brute Force", null, bruteForceCategoryPanel, "Brute Force");
        categoryTabbedPane.setSelectedIndex(0);

        createDictionaryPanel();
        categoryTabbedPane.addTab("Dictionary", null, dictionaryCategoryPanel, "Dictionary");

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setValue(0);

        optionPanel.add(checkerTypeComboBox);
        optionPanel.add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));
        optionPanel.add(recoveryFilePanel);
        optionPanel.add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));
        optionPanel.add(categoryTabbedPane);
        optionPanel.add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));
        optionPanel.add(progressBar);
    }

    private void createBruteForcePanel() {
        bruteForceCategoryPanel = new JPanel();

        JPanel topLevelPanel = new JPanel();
        topLevelPanel.setLayout(new BoxLayout(topLevelPanel, BoxLayout.Y_AXIS));
        bruteForceCategoryPanel.add(topLevelPanel);

        JPanel charsetPanel = new JPanel();
        charsetPanel.setLayout(new BoxLayout(charsetPanel, BoxLayout.X_AXIS));

        JPanel passwordLengthPanel = new JPanel();
        passwordLengthPanel.setLayout(new BoxLayout(passwordLengthPanel, BoxLayout.X_AXIS));

        topLevelPanel.add(charsetPanel);
        topLevelPanel.add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));
        topLevelPanel.add(passwordLengthPanel);

        numberCheckBox = new JCheckBox("Number");
        numberCheckBox.setSelected(true);
        lowercaseLetterCheckBox = new JCheckBox("Lowercase Letter");
        uppercaseLetterCheckBox = new JCheckBox("Uppercase Letter");

        charsetPanel.add(numberCheckBox);
        charsetPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        charsetPanel.add(lowercaseLetterCheckBox);
        charsetPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        charsetPanel.add(uppercaseLetterCheckBox);

        JLabel minLabel = new JLabel("Minimum Length: ");
        JLabel maxLabel = new JLabel("Maximum Length: ");

        minSpinner = new JSpinner();
        minSpinner.setModel(new SpinnerNumberModel(1, 1, 9, 1));
        minSpinner.setToolTipText("Minimum Length");

        maxSpinner = new JSpinner();
        maxSpinner.setModel(new SpinnerNumberModel(6, 1, 9, 1));
        maxSpinner.setToolTipText("Maximum Length");

        passwordLengthPanel.add(minLabel);
        passwordLengthPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        passwordLengthPanel.add(minSpinner);
        passwordLengthPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        passwordLengthPanel.add(maxLabel);
        passwordLengthPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        passwordLengthPanel.add(maxSpinner);
    }

    private void createDictionaryPanel() {
        dictionaryCategoryPanel = new JPanel();

        JPanel topLevelPanel = new JPanel();
        topLevelPanel.setLayout(new BoxLayout(topLevelPanel, BoxLayout.Y_AXIS));
        dictionaryCategoryPanel.add(topLevelPanel);

        dictionaryFilePanel = new FilePanel("Choose Dictionary File");
        dictionaryFilePanel.setDescriptionAndFileExtensions("*.dic;*.txt", new String[]{"dic", "txt"});

        JPanel threadNumPanel = new JPanel();
        threadNumPanel.setLayout(new BoxLayout(threadNumPanel, BoxLayout.X_AXIS));

        topLevelPanel.add(dictionaryFilePanel);
        topLevelPanel.add(Box.createVerticalStrut(Constants.DEFAULT_Y_BORDER));
        topLevelPanel.add(threadNumPanel);

        JLabel threadNumLabel = new JLabel("Thread Number: ");

        threadNumSpinner = new JSpinner();
        threadNumSpinner.setModel(new SpinnerNumberModel(1, 1, 1000, 1));
        threadNumSpinner.setToolTipText("Thread Number");

        threadNumPanel.add(threadNumLabel);
        threadNumPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        threadNumPanel.add(threadNumSpinner);
    }

    private void createOperationPanel() {
        operationPanel = new JPanel();
        operationPanel.setLayout(new BoxLayout(operationPanel, BoxLayout.X_AXIS));

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        currentStateLabel = new JLabel();
        currentPasswordLabel = new JLabel();
        currentProgressLabel = new JLabel();

        operationPanel.add(startButton);
        operationPanel.add(Box.createHorizontalStrut(Constants.DEFAULT_X_BORDER));
        operationPanel.add(stopButton);
        operationPanel.add(Box.createHorizontalStrut(2 * Constants.DEFAULT_X_BORDER));
        operationPanel.add(currentStateLabel);
        operationPanel.add(Box.createHorizontalStrut(2 * Constants.DEFAULT_X_BORDER));
        operationPanel.add(currentPasswordLabel);
        operationPanel.add(Box.createHorizontalStrut(2 * Constants.DEFAULT_X_BORDER));
        operationPanel.add(currentProgressLabel);
        operationPanel.add(Box.createHorizontalGlue());

        startButton.addActionListener(e -> new Thread(this::onStart).start());

        stopButton.addActionListener(e -> new Thread(this::onStop).start());

        setCurrentState(State.IDLE);
        setCurrentPassword("");
    }

    private void onStart() {
        FileChecker fileChecker = (FileChecker) checkerTypeComboBox.getSelectedItem();
        if (fileChecker == null) {
            JOptionPane.showMessageDialog(this, "fileChecker is null");
            return;
        }
        if (!fileChecker.prepareChecker()) {
            JOptionPane.showMessageDialog(this, "onStart failed: Checker condition does not been prepared!");
            return;
        }

        File selectedFile = recoveryFilePanel.getFile();
        if (!selectedFile.isFile()) {
            JOptionPane.showMessageDialog(this, "file is null");
            return;
        }

        String extension = FilenameUtils.getExtension(recoveryFilePanel.getFile().getName());
        if (!ArrayUtils.contains(fileChecker.getFileExtensions(), StringUtils.toRootLowerCase(extension))) {
            JOptionPane.showMessageDialog(this, "invalid file");
            return;
        }

        fileChecker.attachFile(selectedFile);
        currentFileChecker = fileChecker;

        Component selectedPanel = categoryTabbedPane.getSelectedComponent();
        if (selectedPanel.equals(bruteForceCategoryPanel)) {
            currentCategory = Category.BRUTE_FORCE;
        } else if (selectedPanel.equals(dictionaryCategoryPanel)) {
            int threadNum = (Integer) threadNumSpinner.getValue();
            if (threadNum == 1) {
                currentCategory = Category.DICTIONARY_SINGLE_THREAD;
            } else {
                currentCategory = Category.DICTIONARY_MULTI_THREAD;
            }
        } else {
            currentCategory = Category.UNKNOWN;
        }
        switch (currentCategory) {
            case BRUTE_FORCE:
                onStartByBruteForceCategory();
                break;
            case DICTIONARY_SINGLE_THREAD:
                onStartByDictionarySingleThreadCategory();
                break;
            case DICTIONARY_MULTI_THREAD:
                onStartByDictionaryMultiThreadCategory();
                break;
            default:
                JOptionPane.showMessageDialog(this, "onStart failed: Invalid category!");
                break;
        }
    }

    private void onStop() {
        switch (currentCategory) {
            case BRUTE_FORCE:
                onStopByBruteForceCategory();
                break;
            case DICTIONARY_SINGLE_THREAD:
                onStopByDictionarySingleThreadCategory();
                break;
            case DICTIONARY_MULTI_THREAD:
                onStopByDictionaryMultiThreadCategory();
                break;
            default:
                JOptionPane.showMessageDialog(this, "onStop failed: Invalid category!");
                break;
        }
    }

    private void onStartByBruteForceCategory() {
        logger.info("onStartByBruteForceCategory");
        StringBuilder sb = new StringBuilder();
        if (numberCheckBox.isSelected()) {
            sb.append("0123456789");
        }
        if (lowercaseLetterCheckBox.isSelected()) {
            sb.append("abcdefghijklmnopqrstuvwxyz");
        }
        if (uppercaseLetterCheckBox.isSelected()) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        if (sb.length() <= 0) {
            JOptionPane.showMessageDialog(this, "Character set is empty!");
            return;
        }
        final String charSet = sb.toString();

        int minLength = (Integer) minSpinner.getValue();
        int maxLength = (Integer) maxSpinner.getValue();
        if (minLength > maxLength) {
            JOptionPane.showMessageDialog(this, "Minimum length is bigger than maximum length!");
            return;
        }


        setCurrentState(State.WORKING);
        String password = null;
        for (int length = minLength; length <= maxLength; length++) {
            if (currentState == State.STOPPING || currentState == State.IDLE) {
                logger.info("Stop try next length");
                break;
            }
            setProgressMaxValue((int) Math.pow(charSet.length(), length));
            setProgressBarValue(0);
            long startTime = System.currentTimeMillis();
            int numThreads = getThreadCount(charSet.length(), length, currentFileChecker.getMaxThreadNum());
            logger.info("[" + currentFileChecker + "]Current attempt length: " + length + ", thread number: " + numThreads);

            workerPool = Executors.newFixedThreadPool(numThreads);
            BruteForceFuture bruteForceFuture = new BruteForceFuture(numThreads);
            BruteForceTaskConst consts = new BruteForceTaskConst(numThreads, length, currentFileChecker, charSet);

            for (int taskId = 0; taskId < numThreads; taskId++) {
                BruteForceTask task = new BruteForceTask(taskId, consts, bruteForceFuture, this);
                workerPool.execute(task);
            }
            try {
                password = bruteForceFuture.get();
            } catch (Exception e) {
                logger.info("Exception test: ", e);
            } finally {
                if (workerPool.isShutdown()) {
                    workerPool.shutdown();
                }
            }
            long endTime = System.currentTimeMillis();
            logger.info("Current attempt length: " + length + ", Cost time: " + (endTime - startTime) + "ms");
            if (password != null) {
                break;
            }
        }
        JOptionPane.showMessageDialog(this, Objects.requireNonNullElse(password, "Can not find password"));
        onStopByBruteForceCategory();
    }

    private void onStartByDictionarySingleThreadCategory() {
        logger.info("onStartByDictionarySingleThreadCategory");
        File dictionaryFile = dictionaryFilePanel.getFile();
        if (!dictionaryFile.isFile()) {
            JOptionPane.showMessageDialog(this, "Invalid dictionary file");
            return;
        }
        String charsetName = EncoderDetector.judgeFile(dictionaryFile.getAbsolutePath());
        logger.info("dictionary file: " + dictionaryFile.getAbsolutePath() + ", charset: " + charsetName);
        if (charsetName == null) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dictionaryFile), charsetName))) {
            setCurrentState(State.WORKING);
            setProgressMaxValue(Utils.getFileLineCount(dictionaryFile));
            setProgressBarValue(0);
            Predicate<String> isRecoveringPredicate = password -> getCurrentState() == State.WORKING;
            Function<String, Stream<String>> generator = password -> {
                setCurrentPassword(password);
                increaseProgressBarValue();
                return Stream.of(password.toLowerCase(), password.toUpperCase());
            };
            Predicate<String> verifier = currentFileChecker::checkPassword;
            // java.util.stream.BaseStream.parallel can not increase the speed in test
            Optional<String> password = br.lines().takeWhile(isRecoveringPredicate).flatMap(generator).filter(verifier).findFirst();
            if (password.isPresent()) {
                JOptionPane.showMessageDialog(this, password.get());
            } else {
                JOptionPane.showMessageDialog(this, "Can not find password");
            }
        } catch (FileNotFoundException e) {
            logger.info("FileNotFoundException");
        } catch (IOException e) {
            logger.info("IOException");
        }
        onStopByDictionarySingleThreadCategory();
    }

    private void onStartByDictionaryMultiThreadCategory() {
        logger.info("onStartByDictionaryMultiThreadCategory");
        File dictionaryFile = dictionaryFilePanel.getFile();
        if (!dictionaryFile.isFile()) {
            JOptionPane.showMessageDialog(this, "Invalid dictionary file");
            return;
        }
        int threadNum = (Integer) threadNumSpinner.getValue();
        setCurrentState(State.WORKING);
        int fileLineCount = Utils.getFileLineCount(dictionaryFile);
        logger.info("[TaskTracing]File line count: " + fileLineCount);
        setProgressMaxValue(fileLineCount);
        setProgressBarValue(0);
        lineHandler = new LineHandler(currentFileChecker, new AtomicBoolean(false), this);
        String charsetName = EncoderDetector.judgeFile(dictionaryFile.getAbsolutePath());
        BigFileReader.Builder builder = new BigFileReader.Builder(dictionaryFile.getAbsolutePath(), lineHandler);
        bigFileReader = builder.withThreadSize(threadNum).withCharset(charsetName).withBufferSize(1024 * 1024).build();
        bigFileReader.setCompleteCallback(() -> {
            if (lineHandler != null && !lineHandler.getSuccess().get()) {
                logger.error("Can not find password");
                JOptionPane.showMessageDialog(RecoveryPanel.this, "Can not find password");
                onStopByDictionaryMultiThreadCategory();
            }
        });
        bigFileReader.start();
    }

    private void onStopByBruteForceCategory() {
        logger.info("onStopByBruteForceCategory");
        if (workerPool != null && !workerPool.isShutdown()) {
            setCurrentState(State.STOPPING);
        }
        workerPool.shutdown();

        while (workerPool != null && !workerPool.isTerminated()) {
            try {
                final boolean isTimeout = !workerPool.awaitTermination(100, TimeUnit.SECONDS);
                logger.info("awaitTermination isTimeout: " + isTimeout);
            } catch (InterruptedException e) {
                logger.error("awaitTermination failed");
                Thread.currentThread().interrupt();
            }
        }
        setProgressBarValue(0);
        setCurrentState(State.IDLE);
    }

    private void onStopByDictionarySingleThreadCategory() {
        logger.info("onStopByDictionarySingleThreadCategory");
        setProgressBarValue(0);
        setCurrentState(State.IDLE);
    }

    private void onStopByDictionaryMultiThreadCategory() {
        logger.info("onStopByDictionaryMultiThreadCategory");
        setCurrentState(State.STOPPING);
        if (bigFileReader != null) {
            bigFileReader.shutdown();
        }
        setCurrentState(State.IDLE);
        setProgressBarValue(0);
    }

    private int getThreadCount(int charSetSize, int length, int maxThreadCount) {
        int result = 1;
        for (int i = 1; i <= length; i++) {
            result *= charSetSize;
            if (result >= maxThreadCount) {
                return maxThreadCount;
            }
        }
        return result;
    }

    public void setCurrentState(State currentState) {
        RecoveryPanel.currentState = currentState;
        if (currentStateLabel != null) {
            currentStateLabel.setText("State: " + currentState.toString());
        }
        if (currentState == State.WORKING) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else if (currentState == State.STOPPING) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
        } else if (currentState == State.IDLE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            currentPasswordLabel.setText("");
            currentProgressLabel.setText("");
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setProgressMaxValue(int maxValue) {
        if (progressBar != null) {
            progressBar.setMaximum(maxValue);
        }
    }

    public void setProgressBarValue(int value) {
        if (progressBar != null) {
            progressBar.setValue(value);
            setCurrentProgress(progressBar.getMaximum(), value);
        }
    }

    public void increaseProgressBarValue() {
        if (progressBar != null) {
            int currentValue = progressBar.getValue();
            progressBar.setValue(currentValue + 1);
            setCurrentProgress(progressBar.getMaximum(), currentValue + 1);
        }
    }

    public void setCurrentProgress(int total, int current) {
        if (currentProgressLabel != null) {
            NumberFormat numberFormat = NumberFormat.getPercentInstance();
            numberFormat.setMinimumFractionDigits(3);
            String value = numberFormat.format(((double)current) / total);
            currentProgressLabel.setText("Progress: " + value);
        }
    }

    public void setCurrentPassword(String password) {
        if (currentPasswordLabel != null) {
            currentPasswordLabel.setText("Trying: " + password);
        }
    }
}

