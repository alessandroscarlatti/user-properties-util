package com.scarlatti;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
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
public class InteractiveProperties extends Properties {

    // property definitions are optional.
    // if we read without definitions we will get the raw values.
    // there would be no options for encoding.
    // as far as reading is concerned, the only thing that matters
    // is actually the secret properties.
    private List<PropertyDef> propertyDefs = new ArrayList<>();
    private boolean promptForMissingProperties = true;
    private File file;

    public InteractiveProperties() {
    }

    public InteractiveProperties(String properties) {
        load(properties);
    }

    public InteractiveProperties(Properties defaults,
                                 File file,
                                 boolean promptForMissingProperties,
                                 List<PropertyDef> propertyDefs) {
        super(defaults);
        this.file = file;
        this.promptForMissingProperties = promptForMissingProperties;
        this.propertyDefs = propertyDefs;
        load(file);
    }

    public InteractiveProperties(File file) {
        load(file);
    }

    public InteractiveProperties(File file, Consumer<InteractiveProperties> config) {
        config.accept(this);
        load(file);
    }

    public InteractiveProperties(Properties defaults) {
        super(defaults);
    }

    public static PropertiesBuilder get() {
        PropertiesBuilder builder = new PropertiesBuilder();
        return builder;
    }

    public InteractiveProperties def(String name, String description, boolean secret) {
        return def(new PropertyDef(name, description, secret));
    }

    public InteractiveProperties def(PropertyDef propertyDef) {
        propertyDefs.add(propertyDef);
        return this;
    }

    public void load(File file) {
        Objects.requireNonNull(file, "File may not be null");

        if (file.exists()) {
            // load from file
            try (FileInputStream fis = new FileInputStream(file)) {
                this.file = file;
                load(fis);
            } catch (Exception e) {
                throw new RuntimeException("Error loading properties from file " + file, e);
            }
        } else {
            // create an empty file
            try {
                Files.write(file.toPath(), "".getBytes());
                promptForMissingProperties();
            } catch (IOException e) {
                throw new RuntimeException("Error creating properties file " + file, e);
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
        int response = JOptionPane.showOptionDialog(
            null,
            editPropertiesTable.render(),
            "Edit Properties",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"OK", "Cancel"},
            "OK"
        );

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
        store(file, "");
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
        private JTextField jLabel;

        public SwTextField(String text) {
            jLabel = new JTextField(text);
            jLabel.setBorder(null);
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

    public static class SwLabel implements CellUiComp<String> {
        private JLabel jLabel;

        public SwLabel(String text) {
            jLabel = new JLabel(text);
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
            jPasswordField.setBorder(null);
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
        private JScrollPane jScrollPane = new JScrollPane();
        private JTextArea jTextArea;

        public SwTextArea(String text) {
            jTextArea = new JTextArea(text);
            jTextArea.setWrapStyleWord(true);
            jTextArea.setLineWrap(true);
            jTextArea.setFont(new Font("Arial", Font.PLAIN, 11));
            jTextArea.setEditable(false);
//            jTextArea.setBackground(jScrollPane.getBackground());
            jTextArea.setOpaque(false);

            DefaultCaret caret = (DefaultCaret) jTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

            jScrollPane.setPreferredSize(new Dimension(0, 55));
            jTextArea.setRows(jTextArea.getLineCount());
            jTextArea.setBorder(null);
            jScrollPane.setViewportView(jTextArea);
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
            setupTableRendering();
//            addData();
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

        private void setupTableRendering() {

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
            };

            jTable.putClientProperty("terminateEditOnFocusLost", true);
            ((DefaultTableCellRenderer) jTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
            jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        private void addData() {
            for (Tr tr : trs) {
                // this is the data for the row...
                Object[] values = new Object[tr.tds.size()];

                int clmIndex = 0;
                for (Td td : tr.tds) {
                    values[clmIndex] = td.ui.getValue();
                    clmIndex++;
                }

                model.addRow(values);
            }
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
            this.uiComponent = uiComponent;
            ui = uiComponent.getUi();
            ui.setOpaque(true);
            ui.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            // ui.addActionListener(e -> fireEditingStopped());
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

        public InteractiveProperties fromFile(File file) {
            return new InteractiveProperties(
                defaults,
                file,
                promptForMissingProperties,
                propertyDefs
            );
        }

        public PropertiesBuilder withDefaults(Properties properties) {
            defaults = properties;
            return this;
        }

        public PropertiesBuilder promptForMissingProperties(boolean promptForMissingProperties) {
            this.promptForMissingProperties = promptForMissingProperties;
            return this;
        }

        public PropertiesBuilder property(String name, String description) {
            propertyDefs.add(new PropertyDef(name, description, false));
            return this;
        }

        public PropertiesBuilder secretProperty(String name, String description) {
            propertyDefs.add(new PropertyDef(name, description, true));
            return this;
        }
    }
}
