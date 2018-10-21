package com.scarlatti;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;
import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.OK_OPTION;

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Friday, 10/5/2018
 */
public class SmartProperties extends Properties {

    // property definitions are optional.
    // if we read without definitions we will get the raw values.
    // there would be no options for encoding.
    // as far as reading is concerned, the only thing that matters
    // is actually the secret properties.
    private List<PropertyDef> propertyDefs = new ArrayList<>();
    private boolean promptForMissingProperties = true;
    private File file;
    private boolean displayBanner = true;
    private long timeoutMs = 60000L;

    static {
        try {
            boolean setLookAndFeel = true;
            String prop = System.getProperty("smartProperties.setLookAndFeel");
            if (prop != null) {
                setLookAndFeel = Boolean.parseBoolean(prop);
            }
            if (setLookAndFeel) {
                String lafClassName = UIManager.getSystemLookAndFeelClassName();
                if (!UIManager.getLookAndFeel().getClass().getName().equals(lafClassName)) {
                    UIManager.setLookAndFeel(lafClassName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SmartProperties() {
        overrideWithSystemProperties();
    }

    public SmartProperties(Properties defaults) {
        super(defaults);
        overrideWithSystemProperties();
    }

    private SmartProperties(Properties defaults,
                            File file,
                            boolean promptForMissingProperties,
                            List<PropertyDef> propertyDefs,
                            boolean displayBanner,
                            long timeoutMs) {
        super(defaults);
        this.file = file;
        this.promptForMissingProperties = promptForMissingProperties;
        this.propertyDefs = propertyDefs;
        this.displayBanner = displayBanner;
        this.timeoutMs = timeoutMs;
        overrideWithSystemProperties();
        load(file);
    }

    private void overrideWithSystemProperties() {
        String displayBannerStr = System.getProperty("smartProperties.displayBanner");
        if (displayBannerStr != null) {
            this.displayBanner = Boolean.parseBoolean(displayBannerStr);
        }

        String promptStr = System.getProperty("smartProperties.promptForMissingProperties");
        if (promptStr != null) {
            this.promptForMissingProperties = Boolean.parseBoolean(promptStr);
        }

        String timeoutStr = System.getProperty("smartProperties.timeoutMs");
        if (timeoutStr != null) {
            long timeoutMs = Long.parseLong(timeoutStr);
            if (timeoutMs < 0) {
                throw new IllegalArgumentException(timeoutMs + " not valid. smartProperties.timeoutMs should be greater than 0ms.");
            } else {
                this.timeoutMs = timeoutMs;
            }
        }
    }

    public static PropertiesBuilder get() {
        PropertiesBuilder builder = new PropertiesBuilder();
        return builder;
    }

    public boolean getDisplayBanner() {
        return displayBanner;
    }

    public void setDisplayBanner(boolean displayBanner) {
        this.displayBanner = displayBanner;
    }

    public void load(File file) {
        Objects.requireNonNull(file, "File may not be null");
        optionallyDisplayBanner();
        if (file.exists()) {
            // load from file
            try (FileInputStream fis = new FileInputStream(file)) {
                this.file = file;

                String message = "Reading properties from file " + file.getAbsolutePath() + " (delete this file to reset)";
                System.out.println(message);
                load(fis);
                System.out.println("Loaded SmartProperties from file " + file.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException("Error loading properties from file " + file.getAbsolutePath() + ".  You can delete the file if you want to reset.", e);
            }
        } else {
            // create an empty file
            try {
                System.out.println(file.getAbsoluteFile() + " does not exist (creating file)");
                Files.createDirectories(file.toPath().getParent());
                Files.write(file.toPath(), "".getBytes());
                promptForMissingProperties();
            } catch (IOException e) {
                throw new RuntimeException("Error creating properties file " + file.getAbsolutePath(), e);
            }
        }
    }

    public void load(String properties) {
        try (StringReader reader = new StringReader(properties)) {
            load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from string " + properties, e);
        }
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        decodeSecretProperties();
        promptForMissingProperties();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        decodeSecretProperties();
        promptForMissingProperties();
    }

    private void promptForMissingProperties() {
        if (!promptForMissingProperties) {
            System.out.println("Not prompting for missing properties.");
            return;
        }

        // Just create the dialog and fill out the data
        // This will let the user decide once what they actually want
        List<PropertyUiData> properties = new ArrayList<>();
        for (PropertyDef def : propertyDefs) {
            PropertyUiData property = new PropertyUiData(def, getProperty(def.getName()));
            properties.add(property);
        }

        boolean missingProperties = false;
        for (PropertyUiData property : properties) {
            if (property.getValue() == null) {
                missingProperties = true;
                break;
            }
        }

        if (!missingProperties) {
            return;
        }

        // build and show the dialog.
        EditPropertiesTable editPropertiesTable = new EditPropertiesTable(properties);
        System.out.println("Missing some properties.  Look for a dialog.");

        JFrame frame = new JFrame("Edit Properties");
        frame.setUndecorated(true);
        frame.setLocationRelativeTo(null);
        frame.setIconImages(getIcons());
        frame.setVisible(true);

        JComponent table = editPropertiesTable.render();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> responseFuture = executor.submit(() -> {
            int dlgResponse = JOptionPane.showOptionDialog(
                frame,
                table,
                "Edit Properties",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(getIcons().get(2)),
                new Object[]{"OK", "Cancel"},
                "OK"
            );
            return dlgResponse;
        });

        try {
            int response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (response == OK_OPTION) {
                properties = editPropertiesTable.getProperties();

                // update the properties...
                for (PropertyUiData property : properties) {
                    setProperty(property.getPropertyDef().getName(), property.value);
                }

                // can we save the properties during load??
                if (file != null) {
                    store(file);
                }
            }
        } catch (InterruptedException e) {
            new RuntimeException("Thread Interrupted while editing properties with \"Edit Properties\" dialog.", e).printStackTrace();
        } catch (ExecutionException e) {
            new RuntimeException("Error editing properties with \"Edit Properties\" dialog.", e).printStackTrace();
        } catch (TimeoutException e) {
            responseFuture.cancel(true);
            new IllegalStateException("Timed out waiting " + timeoutMs + "ms for \"Edit Properties\" dialog.", e).printStackTrace();
        } finally {
            executor.shutdown();
            frame.dispose();
        }
    }

    private void optionallyDisplayBanner() {
        if (displayBanner) {
            String banner =
                "      _______________________\n" +
                    "     /   //=================/`\"-._\n" +
                    "    |   ||=================|      D\n" +
                    "jgs  \\___\\\\_________________\\__.-\"";

            System.out.println();
            System.out.println("      .:. Smart Properties .:.");
            System.out.println(banner);
            System.out.println("...::::::::::::::::::::::::::::.......");
            System.out.println();
        }
    }

    private List<Image> getIcons() {
        String imageString = "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";

        // create a buffered image
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes;
        bytes = decoder.decode(imageString);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bis);
            return Arrays.asList(
                image.getScaledInstance(127, 127, Image.SCALE_SMOOTH),
                image.getScaledInstance(36, 63, Image.SCALE_SMOOTH),
                image.getScaledInstance(31, 31, Image.SCALE_SMOOTH),
                image.getScaledInstance(15, 15, Image.SCALE_SMOOTH)
            );
        } catch (Exception e) {
            System.err.println("Error getting icon.");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void decodeSecretProperties() {
        // if a property with that name exists AND is marked as secret,
        // decode it and change its getValue.
        for (PropertyDef def : propertyDefs) {
            if (getProperty(def.getName()) != null && def.getSecret()) {
                String decodedValue = new String(getDecoder().decode(getProperty(def.getName())));
                setProperty(def.getName(), decodedValue);
            }
        }
    }

    public void store(File file) {
        store(file, "Generated with SmartProperties.  Delete or edit this file to reset.");
    }

    public void store(File file, String comments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            store(writer, comments);
        } catch (Exception e) {
            throw new RuntimeException("Error storing properties.", e);
        }
    }

    @Override
    public synchronized void store(Writer writer, String comments) throws IOException {
        encodeSecretProperties();
        super.store(writer, comments);
        decodeSecretProperties();
    }

    @Override
    public synchronized void store(OutputStream out, String comments) throws IOException {
        encodeSecretProperties();
        super.store(out, comments);
        decodeSecretProperties();
    }

    private void encodeSecretProperties() {
        // if a property with that name exists AND is marked as secret,
        // decode it and change its getValue.
        for (PropertyDef def : propertyDefs) {
            if (getProperty(def.getName()) != null && def.getSecret()) {
                String encodedValue = new String(getEncoder().encode(getProperty(def.getName()).getBytes()));
                setProperty(def.getName(), encodedValue);
            }
        }
    }

    public boolean getPromptForMissingProperties() {
        return promptForMissingProperties;
    }

    public void setPromptForMissingProperties(boolean promptForMissingProperties) {
        this.promptForMissingProperties = promptForMissingProperties;
    }

    static class PropertyDef {
        private String name;
        private String description;
        private boolean secret;

        public PropertyDef() {
        }

        public PropertyDef(String name, String description, boolean secret) {
            this.name = name;
            this.description = description;
            this.secret = secret;
        }

        public PropertyDef(PropertyDef other) {
            this.name = other.name;
            this.description = other.description;
            this.secret = other.secret;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean getSecret() {
            return secret;
        }

        public void setSecret(boolean secret) {
            this.secret = secret;
        }
    }

    static class PropertyUiData {
        private PropertyDef propertyDef;
        private String value;

        public PropertyUiData(PropertyDef propertyDef, String value) {
            this.propertyDef = propertyDef;
            this.value = value;
        }

        public PropertyUiData(PropertyUiData other) {
            this.propertyDef = other.propertyDef;
            this.value = other.value;
        }

        public PropertyDef getPropertyDef() {
            return propertyDef;
        }

        public void setPropertyDef(PropertyDef propertyDef) {
            this.propertyDef = propertyDef;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static class EditPropertiesTable {
        private SwTable swTable;
        private JComponent ui;
        private List<PropertyUiData> properties;

        private EditPropertiesTable(List<PropertyUiData> properties) {
            this.properties = properties;
            buildUi();
        }

        private void buildUi() {
            swTable = new SwTable(table -> {
                for (PropertyUiData property : properties) {
                    table.tr(new Tr(property.getPropertyDef().getName(), tr -> {
                        tr.td(new Td(new SwLabel(property.getPropertyDef().getName())));

                        final String text = property.getValue() != null ? property.getValue() : "";
                        if (property.getPropertyDef().getSecret()) {
                            tr.td(new Td(new SwPasswordField(text)));
                        } else {
                            tr.td(new Td(new SwTextField(text)));
                        }

                        tr.td(new Td(new SwTextArea(property.getPropertyDef().getDescription())));
                    }));
                }
            });

            this.ui = swTable.render();
        }

        public JComponent render() {
            return ui;
        }

        public List<PropertyUiData> getProperties() {
            // rebuild the properties from what was last in the ui
            for (Tr tr : swTable.getTrs()) {
                properties.stream()
                    .filter(p -> p.getPropertyDef().getName().equals(tr.getId()))
                    .findFirst()
                    .ifPresent(property -> property.setValue((String) tr.getTds().get(1).getUi().getValue()));

            }

            return properties;
        }
    }

    public static class SwTextField implements CellUiComp<String> {
        private JTextField jTextField;

        public SwTextField(String text) {
            jTextField = new JTextField(text);

            Border emptyBorder = new EmptyBorder(5, 5, 5, 5);
            Border beveledBorder = BorderFactory.createSoftBevelBorder(BevelBorder.RAISED,
                UIManager.getColor("Button.background"),
                UIManager.getColor("Button.background")
            );
            Border combinedBorder = new CompoundBorder(emptyBorder, beveledBorder);
            jTextField.setBorder(emptyBorder);

            jTextField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    jTextField.setBorder(combinedBorder);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    jTextField.setBorder(emptyBorder);
                    jTextField.revalidate();
                    jTextField.repaint();
                }
            });
        }

        @Override
        public String getValue() {
            return jTextField.getText();
        }

        @Override
        public void setValue(String value) {
            jTextField.setText(value);
        }

        @Override
        public JComponent getUi() {
            return jTextField;
        }
    }

    public static class SwLabel implements CellUiComp<String> {
        private JLabel jLabel;

        public SwLabel(String text) {
            jLabel = new JLabel(text);
            jLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
            jLabel.setOpaque(true);
        }

        @Override
        public String getValue() {
            return jLabel.getText();
        }

        @Override
        public void setValue(String value) {
            jLabel.setText(value);
        }

        @Override
        public JComponent getUi() {
            return jLabel;
        }
    }


    private static class SwPasswordField implements CellUiComp<String> {
        private JPasswordField jPasswordField;

        public SwPasswordField(String text) {
            jPasswordField = new JPasswordField(text);
            Border emptyBorder = new EmptyBorder(5, 5, 5, 5);
            Border beveledBorder = BorderFactory.createSoftBevelBorder(BevelBorder.RAISED,
                UIManager.getColor("Button.background"),
                UIManager.getColor("Button.background")
            );
            Border combinedBorder = new CompoundBorder(emptyBorder, beveledBorder);
            jPasswordField.setBorder(emptyBorder);

            jPasswordField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    jPasswordField.setBorder(combinedBorder);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    jPasswordField.setBorder(emptyBorder);
                    jPasswordField.revalidate();
                    jPasswordField.repaint();
                }
            });
        }

        @Override
        public String getValue() {
            return new String(jPasswordField.getPassword());
        }

        @Override
        public void setValue(String value) {
            jPasswordField.setText(value);
        }

        @Override
        public JComponent getUi() {
            return jPasswordField;
        }
    }

    private static class SwTextArea implements CellUiComp<String> {
        private JScrollPane jScrollPane;
        private JTextArea jTextArea;

        public SwTextArea(String text) {
            jTextArea = new JTextArea(text);
            jTextArea.setWrapStyleWord(true);
            jTextArea.setLineWrap(true);
            jTextArea.setFont(new Font("Arial", Font.PLAIN, 11));
            jTextArea.setEditable(false);
            jTextArea.setOpaque(true);

            DefaultCaret caret = (DefaultCaret) jTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

            jScrollPane = new JScrollPane() {
                @Override
                public void setBackground(Color bg) {
                    super.setBackground(bg);
                    jTextArea.setBackground(bg);
                }
            };

            jScrollPane.setPreferredSize(new Dimension(0, 55));
            jTextArea.setBorder(null);
            jScrollPane.setViewportView(jTextArea);
            jScrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        }

        @Override
        public String getValue() {
            return jTextArea.getText();
        }

        @Override
        public void setValue(String value) {
            jTextArea.setText(value);
        }

        @Override
        public JComponent getUi() {
            return jScrollPane;
        }
    }

    public static class SwTable {
        private List<Tr> trs = new ArrayList<>();
        private JScrollPane jScrollPane = new JScrollPane();
        private JTable jTable;
        private DefaultTableModel model;

        public SwTable(Consumer<SwTable> config) {
            config.accept(this);
            setupTable();
            updateRowHeights();
        }

        public void tr(Tr tr) {
            trs.add(tr);
        }

        public List<Tr> getTrs() {
            return trs;
        }

        public JComponent render() {
            jScrollPane.setViewportView(jTable);
            return jScrollPane;
        }

        private void setupTable() {

            Object[][] data = new Object[trs.size()][trs.size() == 0 ? 0 : trs.get(0).getTds().size()];
            List<List<DefaultCellEditor>> cellEditors = new ArrayList<>();

            int trIndex = 0;
            for (Tr tr : trs) {
                data[trIndex] = tr.getTds().stream()
                    .map(td -> td.getUi().getValue()).toArray();

                List<DefaultCellEditor> cellEditorsForRow = tr.getTds().stream()
                    .map(td -> new CstmCompEditor(td.getUi()))
                    .collect(toList());
                cellEditors.add(cellEditorsForRow);
                trIndex++;
            }

            String[] columnNames = new String[]{"Property Name", "Value", "Description"};
            model = new DefaultTableModel(data, columnNames);

            jTable = new JTable(model) {
                @Override
                public TableCellEditor getCellEditor(int row, int column) {
                    int modelRow = convertRowIndexToModel(row);
                    int modelColumn = convertColumnIndexToModel(column);
                    return cellEditors.get(modelRow).get(modelColumn);
                }

                @Override
                public TableCellRenderer getCellRenderer(int row, int column) {
                    int modelRow = convertRowIndexToModel(row);
                    int modelColumn = convertColumnIndexToModel(column);
                    return (TableCellRenderer) cellEditors.get(modelRow).get(modelColumn);
                }

                @Override
                public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                    super.changeSelection(rowIndex, columnIndex, toggle, extend);
                    if (editCellAt(rowIndex, columnIndex)) {
                        Component editor = getEditorComponent();
                        editor.requestFocusInWindow();

                        if (editor instanceof JTextComponent) {
                            ((JTextComponent) editor).selectAll();
                        }
                    }
                }
            };

            jTable.putClientProperty("terminateEditOnFocusLost", true);
            ((DefaultTableCellRenderer) jTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
            jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        private void updateRowHeights() {
            for (int row = 0; row < jTable.getRowCount(); row++) {
                int rowHeight = jTable.getRowHeight();

                for (int column = 0; column < jTable.getColumnCount(); column++) {
                    Component comp = jTable.prepareRenderer(jTable.getCellRenderer(row, column), row, column);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }

                jTable.setRowHeight(row, rowHeight);
            }
        }
    }

    public static class Tr {
        private List<Td> tds = new ArrayList<>();
        private String id;

        Tr(String id, Consumer<Tr> config) {
            this.id = id;
            config.accept(this);
        }

        void td(Td td) {
            tds.add(td);
        }

        public List<Td> getTds() {
            return tds;
        }

        public String getId() {
            return id;
        }
    }

    public static class Td {
        private CellUiComp ui;

        public Td(CellUiComp ui) {
            this.ui = ui;
        }

        public CellUiComp getUi() {
            return ui;
        }
    }

    private interface CellUiComp<T> {
        T getValue();

        void setValue(T value);

        JComponent getUi();
    }

    private static class CstmCompEditor extends DefaultCellEditor implements TableCellRenderer {
        private JComponent ui;
        private CellUiComp uiComponent;

        public CstmCompEditor(CellUiComp uiComponent) {
            super(new JTextField());
            setClickCountToStart(1);
            this.uiComponent = uiComponent;
            ui = uiComponent.getUi();

            ui.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                }

                @Override
                public void focusLost(FocusEvent e) {
                    fireEditingStopped();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isSelected) {
                ui.setForeground(table.getForeground());
                ui.setBackground(table.getBackground());
            } else {
                ui.setForeground(table.getForeground());
                ui.setBackground(table.getBackground());
            }

            uiComponent.setValue(value);
            ui.revalidate();
            return ui;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                ui.setForeground(table.getForeground());
                ui.setBackground(table.getBackground());
            } else {
                ui.setForeground(table.getForeground());
                ui.setBackground(UIManager.getColor("Button.background"));
                if (ui instanceof JPasswordField) {
                    ui.setBorder(new EmptyBorder(5, 5, 5, 5));
                }
                if (ui instanceof JTextField) {
                    ui.setBorder(new EmptyBorder(5, 5, 5, 5));
                }
            }
            uiComponent.setValue(value);
            ui.revalidate();
            return ui;
        }

        public Object getCellEditorValue() {
            return uiComponent.getValue();
        }
    }

    public static class PropertiesBuilder {
        private Properties defaults;
        private boolean promptForMissingProperties = true;
        private List<PropertyDef> propertyDefs = new ArrayList<>();
        private boolean displayBanner = true;
        private long timeoutMs = 60000L;

        private PropertiesBuilder() {
        }

        public SmartProperties fromFile(File file) {
            return new SmartProperties(
                defaults,
                file,
                promptForMissingProperties,
                propertyDefs,
                displayBanner,
                timeoutMs
            );
        }

        public SmartProperties fromFile(Path path) {
            return fromFile(path.toFile());
        }

        public PropertiesBuilder withDefaults(Properties properties) {
            defaults = properties;
            return this;
        }

        public PropertiesBuilder promptForMissingProperties(boolean promptForMissingProperties) {
            this.promptForMissingProperties = promptForMissingProperties;
            return this;
        }

        public PropertiesBuilder displayBanner() {
            return withBanner(true);
        }

        public PropertiesBuilder noBanner() {
            return withBanner(false);
        }

        public PropertiesBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public PropertiesBuilder withBanner(boolean displayBanner) {
            this.displayBanner = displayBanner;
            return this;
        }

        public PropertiesBuilder property(String name, String description) {
            propertyDefs.add(new PropertyDef(name, description, false));
            return this;
        }

        public PropertiesBuilder property(String name, String description, boolean secret) {
            propertyDefs.add(new PropertyDef(name, description, secret));
            return this;
        }

        public PropertiesBuilder secretProperty(String name, String description) {
            propertyDefs.add(new PropertyDef(name, description, true));
            return this;
        }
    }
}
