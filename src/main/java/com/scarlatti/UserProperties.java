package com.scarlatti;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Friday, 10/5/2018
 */
public class UserProperties extends Properties {

    // property definitions are optional.
    // if we read without definitions we will get the raw values.
    // there would be no options for encoding.
    // as far as reading is concerned, the only thing that matters
    // is actually the secret properties.
    private List<PropertyDef> propertyDefs = new ArrayList<>();
    private boolean promptForMissingProperties = true;

    public UserProperties() {
    }

    public UserProperties(String properties) {
        load(properties);
    }

    public UserProperties(String properties, Consumer<UserProperties> config) {
        config.accept(this);
        load(properties);
    }

    public UserProperties(File file) {
        load(file);
    }

    public UserProperties(File file, Consumer<UserProperties> config) {
        config.accept(this);
        load(file);
    }

    public UserProperties(Properties defaults) {
        super(defaults);
    }

    public UserProperties def(String name, String description, boolean secret) {
        return def(new PropertyDef(name, description, secret));
    }

    public UserProperties def(PropertyDef propertyDef) {
        propertyDefs.add(propertyDef);
        return this;
    }

    public void load(File file) {
        Objects.requireNonNull(file, "File may not be null");

        if (file.exists()) {
            // load from file
            try(FileInputStream fis = new FileInputStream(file)) {
                load(fis);
            } catch (Exception e) {
                throw new RuntimeException("Error loading properties from file " + file, e);
            }
        }
        else {
            // create an empty file
            try {
                Files.write(file.toPath(), "".getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Error creating properties file " + file, e);
            }
        }
    }

    public void load(String properties) {
        try(StringReader reader = new StringReader(properties)) {
            load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from string " + properties, e);
        }
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        promptForMissingProperties();
        decodeSecretProperties();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        promptForMissingProperties();
        decodeSecretProperties();
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

    private static class EditPropertiesPanel {
        private JPanel jPanel;
        private List<PropertyUiData> properties;

        private EditPropertiesPanel(List<PropertyUiData> properties) {
            this.properties = properties;
            buildUi();
        }

        private void buildUi() {
            SwTable ui = new SwTable(table -> {
                for (PropertyUiData property : properties) {
                    table.tr(new Tr(tr -> {
                        tr.td(new Td(new SwLabel(property.getPropertyDef().getName())));

                        final String text = property.getValue() != null ? property.getValue() : "";
                        if (property.getPropertyDef().getSecret()) {
                            tr.td(new Td(new SwTextField(text)));
                        }
                        else {
                            tr.td(new Td(new SwPasswordField(text)));
                        }

                        tr.td(new Td(new SwLabel(property.getPropertyDef().getDescription())));
                    }));
                }
            });

            jPanel.add(ui.render());
        }

        public Component getUi() {
            return jPanel;
        }
    }

    public static class SwTextField implements CellUiComp<String> {
        private JTextField jLabel;

        public SwTextField(String text) {
             jLabel = new JTextField(text);
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
        }

        @Override
        public String getValue() {
            return jPasswordField.getText();
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

    public static class SwTable {
        private List<Tr> trs = new ArrayList<>();
        private JScrollPane jScrollPane = new JScrollPane();
        private JTable jTable = new JTable();
        private DefaultTableModel model;

        public SwTable(Consumer<SwTable> config) {
            config.accept(this);
            setupTableRendering();
            addData();
            updateRowHeights();
        }

        public void tr(Tr tr) {
            trs.add(tr);
        }

        public JComponent render() {
            jScrollPane.setViewportView(jTable);

            return jScrollPane;
        }

        private void setupTableRendering() {

            model = new DefaultTableModel(0, 4) {
                // all columns are strings.
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return String.class;
                }

                // all cells are editable.
                @Override
                public boolean isCellEditable(int row, int column) {
                    return true;
                }
            };
            jTable.setModel(model);

            TableColumnModel columnModel = new DefaultTableColumnModel();

            int clmIndex = 0;
            for (Tr tr : trs) {
                for (Td td : tr.tds) {
                    CstmCompRenderer renderer = new CstmCompRenderer(td.ui);
                    TableColumn tableColumn = new TableColumn(clmIndex);
                    tableColumn.setCellRenderer(renderer);
                    tableColumn.setCellEditor(renderer);
                    columnModel.addColumn(tableColumn);
                    columnModel.getColumn(clmIndex).setHeaderValue("header");
                    // paramNames.put(param.getId(), clmIndex);
                    clmIndex++;
                }
            }

            jTable.setColumnModel(columnModel);
            jTable.putClientProperty("terminateEditOnFocusLost", true);
            jTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
            for (int row = 0; row < jTable.getRowCount(); row++)
            {
                int rowHeight = jTable.getRowHeight();

                for (int column = 0; column < jTable.getColumnCount(); column++)
                {
                    Component comp = jTable.prepareRenderer(jTable.getCellRenderer(row, column), row, column);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }

                jTable.setRowHeight(row, rowHeight);
            }
        }
    }

    public static class Tr {
        private List<Td> tds = new ArrayList<>();

        Tr(Consumer<Tr> config) {
            config.accept(this);
        }

        void td(Td td) {
            tds.add(td);
        }
    }

    public static class Td {
        private CellUiComp ui;

        public Td(CellUiComp ui) {
            this.ui = ui;
        }
    }

    private interface CellUiComp<T> {
        T getValue();
        void setValue(T value);
        JComponent getUi();
    }

    private static class CstmCompRenderer extends DefaultCellEditor implements TableCellRenderer {
        private JComponent ui;
        private CellUiComp uiComponent;

        public CstmCompRenderer(CellUiComp uiComponent) {
            super(new JTextField());
            this.uiComponent = uiComponent;
            ui = uiComponent.getUi();
            ui.setOpaque(true);
            // ui.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isSelected) {
                ui.setForeground(table.getSelectionForeground());
                ui.setBackground(table.getSelectionBackground());
            } else {
                ui.setForeground(table.getForeground());
                ui.setBackground(table.getBackground());
            }

            uiComponent.setValue(value);
            return ui;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                ui.setForeground(table.getSelectionForeground());
                ui.setBackground(table.getSelectionBackground());
            } else {
                ui.setForeground(table.getForeground());
                ui.setBackground(UIManager.getColor("Button.background"));
            }
            uiComponent.setValue(value);
            return ui;
        }

        public Object getCellEditorValue() {
            return uiComponent.getValue();
        }
    }
}
