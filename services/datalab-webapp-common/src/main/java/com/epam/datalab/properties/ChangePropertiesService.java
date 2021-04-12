package com.epam.datalab.properties;

import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChangePropertiesService {

    public String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.info("Trying to read {}, file from path {} :", serviceName, selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }

    public void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.DEFAULT_CHMOD, ChangePropertiesConst.WRITE_CHMOD);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.WRITE_CHMOD, ChangePropertiesConst.DEFAULT_CHMOD);
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
    }

    public void restart(RestartForm restartForm) {
        try {
            boolean billing = restartForm.isBilling();
            boolean provserv = restartForm.isProvserv();
            boolean ui = restartForm.isUi();
            String shCommand = buildSHRestartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {}, provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }


    private String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher m = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(currentConf);
        List<String> secrets = new ArrayList<>();
        String confWithReplacedSecretConf = removeLicence(currentConf);
        while (m.find()) {
            String secret = m.group().split(":")[ChangePropertiesConst.DEFAULT_VALUE_PLACE];
            if (!(secret.isEmpty() || secret.trim().isEmpty()))
                secrets.add(secret);
        }
        for (String secret : secrets) {
            confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secret, ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private String removeLicence(String conf) {
        return conf.split(ChangePropertiesConst.LICENCE_REGEX)[conf.split(ChangePropertiesConst.LICENCE_REGEX).length - 1];
    }


    private void changeCHMODE(String serviceName, String path, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(ChangePropertiesConst.CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, path);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private String addLicence() {
        return ChangePropertiesConst.LICENCE;
    }

    private String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, String> emptySecrets = findEmptySecret(newPropFile);
        return emptySecrets.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecrets);
    }

    private String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldProfMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(oldProf);
        while (oldProfMatcher.find()) {
            String[] s = oldProfMatcher.group().split(":");
            if (emptySecrets.containsKey(s[ChangePropertiesConst.DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[ChangePropertiesConst.DEFAULT_NAME_PLACE]), oldProfMatcher.group());
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private Map<String, String> findEmptySecret(String newPropFile) {
        Matcher newPropFileMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(newPropFile);
        Map<String, String> emptySecrets = new HashMap<>();
        while (newPropFileMatcher.find()) {
            String[] s = newPropFileMatcher.group().split(":");
            if (s[ChangePropertiesConst.DEFAULT_VALUE_PLACE].equals(ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[ChangePropertiesConst.DEFAULT_NAME_PLACE], newPropFileMatcher.group());
            }
        }
        return emptySecrets;
    }

    private String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(ChangePropertiesConst.SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(ChangePropertiesConst.BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(ChangePropertiesConst.PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(ChangePropertiesConst.SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

}