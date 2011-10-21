package com.intellij.lang.javascript.flex.run;

import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.PlatformIcons;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class RemoteFlashRunConfigurationForm extends SettingsEditor<RemoteFlashRunConfiguration> {

  private JPanel myMainPanel;
  private JComboBox myBCsCombo;

  private final Project myProject;
  private FlexIdeBuildConfiguration[] myAllConfigs;
  private boolean mySingleModuleProject;
  private Map<FlexIdeBuildConfiguration, Module> myBCToModuleMap = new THashMap<FlexIdeBuildConfiguration, Module>();

  public RemoteFlashRunConfigurationForm(final Project project) {
    myProject = project;
    initBCCombo();
  }

  private void initBCCombo() {
    final Collection<FlexIdeBuildConfiguration> allConfigs = new ArrayList<FlexIdeBuildConfiguration>();

    final Module[] modules = ModuleManager.getInstance(myProject).getModules();
    mySingleModuleProject = modules.length == 1;
    for (final Module module : modules) {
      if (ModuleType.get(module) instanceof FlexModuleType) {
        for (final FlexIdeBuildConfiguration config : FlexBuildConfigurationManager.getInstance(module).getBuildConfigurations()) {
          allConfigs.add(config);
          myBCToModuleMap.put(config, module);
        }
      }
    }
    myAllConfigs = allConfigs.toArray(new FlexIdeBuildConfiguration[allConfigs.size()]);

    myBCsCombo.setRenderer(new ListCellRendererWrapper(myBCsCombo.getRenderer()) {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof Pair) {
          final String moduleName = (String)((Pair)value).first;
          final String configName = (String)((Pair)value).second;
          setIcon(PlatformIcons.ERROR_INTRODUCTION_ICON);
          setText("<html><font color='red'>" + getPresentableText(moduleName, configName, mySingleModuleProject) + "</font></html>");
        }
        else {
          assert value instanceof FlexIdeBuildConfiguration : value;
          final FlexIdeBuildConfiguration config = (FlexIdeBuildConfiguration)value;
          setIcon(config.getIcon());
          setText(getPresentableText(myBCToModuleMap.get(config).getName(), config.getName(), mySingleModuleProject));
        }
      }
    });

    myBCsCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        // remove invalid entry
        final Object selectedItem = myBCsCombo.getSelectedItem();
        final Object firstItem = myBCsCombo.getItemAt(0);
        if (selectedItem instanceof FlexIdeBuildConfiguration && !(firstItem instanceof FlexIdeBuildConfiguration)) {
          myBCsCombo.setModel(new DefaultComboBoxModel(myAllConfigs));
          myBCsCombo.setSelectedItem(selectedItem);
        }
      }
    });
  }

  private static String getPresentableText(String moduleName, String configName, final boolean singleModuleProject) {
    moduleName = moduleName.isEmpty() ? "[no module]" : moduleName;
    configName = configName.isEmpty() ? "[no configuration]" : configName;
    return singleModuleProject ? configName : configName + " (" + moduleName + ")";
  }

  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }

  protected void resetEditorFrom(final RemoteFlashRunConfiguration configuration) {
    final BCBasedRunnerParameters params = configuration.getRunnerParameters();

    final Module module = ModuleManager.getInstance(myProject).findModuleByName(params.getModuleName());
    final FlexIdeBuildConfiguration config =
      module != null && (ModuleType.get(module) instanceof FlexModuleType)
      ? FlexBuildConfigurationManager.getInstance(module).findConfigurationByName(params.getBCName())
      : null;

    if (config == null) {
      final Object[] model = new Object[myAllConfigs.length + 1];
      model[0] = Pair.create(params.getModuleName(), params.getBCName());
      System.arraycopy(myAllConfigs, 0, model, 1, myAllConfigs.length);
      myBCsCombo.setModel(new DefaultComboBoxModel(model));
      myBCsCombo.setSelectedIndex(0);
    }
    else {
      myBCsCombo.setModel(new DefaultComboBoxModel(myAllConfigs));
      myBCsCombo.setSelectedItem(config);
    }
  }

  protected void applyEditorTo(final RemoteFlashRunConfiguration configuration) throws ConfigurationException {
    final BCBasedRunnerParameters params = configuration.getRunnerParameters();

    final Object selectedItem = myBCsCombo.getSelectedItem();

    if (selectedItem instanceof Pair) {
      params.setModuleName((String)((Pair)selectedItem).first);
      params.setBCName((String)((Pair)selectedItem).second);
    }
    else {
      assert selectedItem instanceof FlexIdeBuildConfiguration : selectedItem;
      params.setModuleName(myBCToModuleMap.get(((FlexIdeBuildConfiguration)selectedItem)).getName());
      params.setBCName(((FlexIdeBuildConfiguration)selectedItem).getName());
    }
  }

  protected void disposeEditor() {
    myAllConfigs = null;
    myBCToModuleMap = null;
  }
}
