package org.citra.emu.settings;

import android.text.TextUtils;
import android.util.Log;

import org.citra.emu.settings.model.BooleanSetting;
import org.citra.emu.settings.model.FloatSetting;
import org.citra.emu.settings.model.IntSetting;
import org.citra.emu.settings.model.Setting;
import org.citra.emu.settings.model.SettingSection;
import org.citra.emu.settings.model.StringSetting;
import org.citra.emu.utils.DirectoryInitialization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public final class SettingsFile {
    // Core
    public static final String KEY_USE_CPU_JIT = "use_cpu_jit";
    public static final String KEY_IS_NEW_3DS = "is_new_3ds";
    public static final String KEY_USE_VIRTUAL_SD = "use_virtual_sd";
    public static final String KEY_SYSTEM_REGION = "region_value";
    // Renderer
    public static final String KEY_USE_GLES = "use_gles";
    public static final String KEY_USE_HW_RENDERER = "use_hw_renderer";
    public static final String KEY_USE_HW_SHADER = "use_hw_shader";
    public static final String KEY_USE_SHADER_JIT = "use_shader_jit";
    public static final String KEY_SHADERS_ACCURATE_MUL = "shaders_accurate_mul";
    public static final String KEY_SHADERS_ACCURATE_GS = "shaders_accurate_gs";
    public static final String KEY_RESOLUTION_FACTOR = "resolution_factor";
    public static final String KEY_USE_FRAME_LIMIT = "use_frame_limit";
    public static final String KEY_FRAME_LIMIT = "frame_limit";
    public static final String KEY_LAYOUT_OPTION = "layout_option";
    // Audio
    public static final String KEY_ENABLE_DSP_LLE = "enable_dsp_lle";
    public static final String KEY_AUDIO_STRETCHING = "enable_audio_stretching";
    public static final String KEY_AUDIO_VOLUME = "volume";
    public static final String KEY_AUDIO_ENGINE = "output_engine";
    public static final String KEY_AUDIO_DEVICE = "output_device";

    /**
     * Reads a given .ini file from disk and returns it as a HashMap of Settings, themselves
     * effectively a HashMap of key/value settings. If unsuccessful, outputs an error telling why it
     * failed.
     */
    public static HashMap<String, SettingSection> loadSettings(String gameId) {
        HashMap<String, SettingSection> sections = new Settings.SettingsSectionMap();
        File ini = new File(DirectoryInitialization.getConfigFile());
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(ini));

            SettingSection current = null;
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.startsWith("[") && line.endsWith("]")) {
                    current = new SettingSection(line.substring(1, line.length() - 1));
                    sections.put(current.getName(), current);
                } else if ((current != null)) {
                    Setting setting = settingFromLine(current, line);
                    if (setting != null) {
                        current.putSetting(setting);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("zhangwei", "[SettingsFile] File not found: " + ini.getAbsolutePath() + e.getMessage());
        } catch (IOException e) {
            Log.e("zhangwei", "[SettingsFile] Error reading from: " + ini.getAbsolutePath() + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("zhangwei", "[SettingsFile] Error closing: " + ini.getAbsolutePath() + e.getMessage());
                }
            }
        }

        return sections;
    }

    /**
     * For a line of text, determines what type of data is being represented, and returns
     * a Setting object containing this data.
     *
     * @param current The section currently being parsed by the consuming method.
     * @param line    The line of text being parsed.
     * @return A typed Setting containing the key/value contained in the line.
     */
    private static Setting settingFromLine(SettingSection current, String line) {
        String[] splitLine = line.split("=");

        if (splitLine.length != 2) {
            Log.w("zhangwei", "Skipping invalid config line \"" + line + "\"");
            return null;
        }

        String key = splitLine[0].trim();
        String value = splitLine[1].trim();

        try {
            int valueAsInt = Integer.valueOf(value);

            return new IntSetting(key, current.getName(), valueAsInt);
        } catch (NumberFormatException ex) {
        }

        try {
            float valueAsFloat = Float.valueOf(value);
            return new FloatSetting(key, current.getName(), valueAsFloat);
        } catch (NumberFormatException ex) {
        }

        switch (value) {
            case "True":
                return new BooleanSetting(key, current.getName(), true);
            case "False":
                return new BooleanSetting(key, current.getName(), false);
            default:
                return new StringSetting(key, current.getName(), value);
        }
    }

    /**
     * Saves a Settings HashMap to a given .ini file on disk. If unsuccessful, outputs an error
     * telling why it failed.
     *
     * @param sections The HashMap containing the Settings we want to serialize.
     */
    public static void saveFile(HashMap<String, SettingSection> sections) {
        File ini = new File(DirectoryInitialization.getConfigFile());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(ini, "UTF-8");

            Set<String> keySet = sections.keySet();
            Set<String> sortedKeySet = new TreeSet<>(keySet);

            for (String key : sortedKeySet) {
                SettingSection section = sections.get(key);
                writeSection(writer, section);
            }
        } catch (FileNotFoundException e) {
            Log.e("zhangwei", "[SettingsFile] File not found: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.e("zhangwei", "[SettingsFile] Bad encoding; please file a bug report: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Writes the contents of a Section HashMap to disk.
     *
     * @param writer  A PrintWriter pointed at a file on disk.
     * @param section A section containing settings to be written to the file.
     */
    private static void writeSection(PrintWriter writer, SettingSection section) {
        // Write this section's values.
        HashMap<String, Setting> settings = section.getSettings();
        if (settings.size() == 0)
            return;

        // Write the section header.
        String header = "[" + section.getName() + "]";
        writer.println(header);

        Set<String> sortedKeySet = new TreeSet<>(settings.keySet());
        for (String key : sortedKeySet) {
            Setting setting = settings.get(key);
            String valueAsString = setting.getValueAsString();
            if (!TextUtils.isEmpty(valueAsString)) {
                writer.println(setting.getKey() + " = " + valueAsString);
            }
        }
    }
}